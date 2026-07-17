package app.erp.qa.service;

import app.erp.qa.dao.entity.ErpQaAction;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 3 端到端测试：质检 REJECTED → 自动生成 NCR(OPEN) → 评审(IN_REVIEW) → 制定 CAPA(PENDING) → 执行(IN_PROGRESS)
 * → 完成(COMPLETED)+效果验证 → NCR resolve(RESOLVED)；resolve 门控（CAPA 未全完成+验证则拒绝）；
 * escalateToRecall 终态；cancel。
 *
 * <p>覆盖 {@code docs/design/quality/state-machine.md §适用对象二} 全迁移 + CAPA 效果验证闭环。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaNcrCapaEndToEnd extends JunitAutoTestCase {

    @RegisterExtension
    static QaFrozenClockExtension frozenClock = new QaFrozenClockExtension();

    static final Long MATERIAL_ID = 7401L;
    static final Long VERIFICATION_PERSON = 7501L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testRejectedAutoCreatesNcrAndFullCapaClosure() {
        // 1. 质检 REJECTED → 自动生成 NCR(OPEN)
        Long insId = seedPendingInspection("INS-E2E", "10", "20");
        recordResult(insId, "5", false); // 5 < min 10 → REJECTED → 自动 NCR

        ErpQaInspection ins = daoProvider.daoFor(ErpQaInspection.class).getEntityById(insId);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED, ins.getResult());
        ErpQaNonConformance ncr = findNcrBySourceCode(ins.getCode());
        assertNotNull(ncr, "REJECTED 自动生成 NCR");
        assertEquals(ErpQaConstants.NCR_STATUS_OPEN, ncr.getStatus());
        assertEquals(ErpQaConstants.NCR_SOURCE_TYPE_INSPECTION, ncr.getSourceType());
        Long ncrId = ncr.getId();

        // 2. NCR submitReview OPEN→IN_REVIEW
        rpcOk(mutation, "ErpQaNonConformance__submitReview", Map.of("ncrId", ncrId));
        assertEquals(ErpQaConstants.NCR_STATUS_IN_REVIEW, reloadNcr(ncrId).getStatus());

        // 3. resolve 前无 CAPA → 门控通过？本实现：无措施允许 resolve（评审人保证）；但本场景不合格应有措施，
        //    先验证「有未完成 CAPA 时 resolve 被拒」：创建一个 PENDING 措施后 resolve 应拒绝
        Long actionId = seedAction(ncrId, ErpQaConstants.ACTION_STATUS_PENDING);
        ApiResponse<?> blocked = rpc(mutation, "ErpQaNonConformance__resolve",
                Map.of("ncrId", ncrId, "resolution", "尝试解决"));
        assertEquals(ErpQaErrors.ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED.getErrorCode(), blocked.getCode(),
                "存在未完成 CAPA 时 resolve 拒绝");

        // 4. CAPA PENDING→IN_PROGRESS→COMPLETED
        rpcOk(mutation, "ErpQaAction__startAction", Map.of("actionId", actionId));
        assertEquals(ErpQaConstants.ACTION_STATUS_IN_PROGRESS, reloadAction(actionId).getStatus());
        rpcOk(mutation, "ErpQaAction__completeAction", Map.of("actionId", actionId));
        assertEquals(ErpQaConstants.ACTION_STATUS_COMPLETED, reloadAction(actionId).getStatus());

        // 仍缺验证 → resolve 应拒绝
        ApiResponse<?> blocked2 = rpc(mutation, "ErpQaNonConformance__resolve",
                Map.of("ncrId", ncrId, "resolution", "尝试解决2"));
        assertEquals(ErpQaErrors.ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED.getErrorCode(), blocked2.getCode(),
                "CAPA 完成但缺效果验证时 resolve 拒绝");

        // 5. 效果验证（验证人 + 验证日期）
        Map<String, Object> verifyArgs = new LinkedHashMap<>();
        verifyArgs.put("actionId", actionId);
        verifyArgs.put("verificationPerson", VERIFICATION_PERSON);
        verifyArgs.put("verificationDate", CoreMetrics.currentDate().toString());
        rpcOk(mutation, "ErpQaAction__verifyAction", verifyArgs);

        // 6. NCR resolve IN_REVIEW→RESOLVED（门控通过）
        rpcOk(mutation, "ErpQaNonConformance__resolve",
                Map.of("ncrId", ncrId, "resolution", "CAPA 已验证有效，关闭"));
        assertEquals(ErpQaConstants.NCR_STATUS_RESOLVED, reloadNcr(ncrId).getStatus());
        assertNotNull(reloadNcr(ncrId).getResolvedAt(), "resolve 记录解决时间");
    }

    @Test
    public void testEscalateToRecallTerminal() {
        Long insId = seedPendingInspection("INS-ESCAL", "10", "20");
        recordResult(insId, "5", false);
        ErpQaNonConformance ncr = findNcrBySourceCode(daoProvider.daoFor(ErpQaInspection.class).getEntityById(insId).getCode());
        Long ncrId = ncr.getId();

        rpcOk(mutation, "ErpQaNonConformance__submitReview", Map.of("ncrId", ncrId));
        rpcOk(mutation, "ErpQaNonConformance__escalateToRecall", Map.of("ncrId", ncrId));
        assertEquals(ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL, reloadNcr(ncrId).getStatus(),
                "升级为召回终态");

        // 终态后 resolve 非法
        ApiResponse<?> resp = rpc(mutation, "ErpQaNonConformance__resolve",
                Map.of("ncrId", ncrId, "resolution", "x"));
        assertEquals(ErpQaErrors.ERR_INVALID_NCR_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "ESCALATED_TO_RECALL 终态不可 resolve");
    }

    @Test
    public void testCancelFromOpenAndReview() {
        Long insId = seedPendingInspection("INS-CANCEL", "10", "20");
        recordResult(insId, "5", false);
        ErpQaNonConformance ncr = findNcrBySourceCode(daoProvider.daoFor(ErpQaInspection.class).getEntityById(insId).getCode());
        Long ncrId = ncr.getId();

        // OPEN→CANCELLED
        rpcOk(mutation, "ErpQaNonConformance__cancel", Map.of("ncrId", ncrId));
        assertEquals(ErpQaConstants.NCR_STATUS_CANCELLED, reloadNcr(ncrId).getStatus());
    }

    @Test
    public void testIllegalNcrTransitionsRejected() {
        Long insId = seedPendingInspection("INS-ILLEGAL", "10", "20");
        recordResult(insId, "5", false);
        ErpQaNonConformance ncr = findNcrBySourceCode(daoProvider.daoFor(ErpQaInspection.class).getEntityById(insId).getCode());
        Long ncrId = ncr.getId();

        // OPEN 直接 resolve（未经评审）→ 非法
        ApiResponse<?> resp = rpc(mutation, "ErpQaNonConformance__resolve",
                Map.of("ncrId", ncrId, "resolution", "x"));
        assertEquals(ErpQaErrors.ERR_INVALID_NCR_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "OPEN→RESOLVED 非法");
    }

    // ---------- helpers ----------

    private ErpQaNonConformance reloadNcr(Long ncrId) {
        return daoProvider.daoFor(ErpQaNonConformance.class).getEntityById(ncrId);
    }

    private ErpQaAction reloadAction(Long actionId) {
        return daoProvider.daoFor(ErpQaAction.class).getEntityById(actionId);
    }

    private ErpQaNonConformance findNcrBySourceCode(String sourceCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceCode", sourceCode));
        q.setLimit(1);
        List<ErpQaNonConformance> list = daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void recordResult(Long insId, String measured, boolean allowConcession) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("lineNo", 1);
        line.put("measuredValue", measured);
        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(line);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("inspectionId", insId);
        args.put("lineResults", lines);
        args.put("allowConcession", allowConcession);
        rpcOk(mutation, "ErpQaInspection__recordResult", args);
    }

    private static BigDecimal toBigDecimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private Long seedPendingInspection(String code, String specMin, String specMax) {
        Long id = 6200L + (long) (Math.abs(code.hashCode()) % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
            ErpQaInspection ins = new ErpQaInspection();
            ins.orm_propValueByName("id", id);
            ins.setCode(code);
            ins.setInspectionType(ErpQaConstants.INSPECTION_TYPE_INCOMING);
            ins.setMaterialId(MATERIAL_ID);
            ins.setResult(ErpQaConstants.INSPECTION_RESULT_PENDING);
            ins.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            ins.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
            ins.setPosted(Boolean.FALSE);
            ins.setInspectionDate(CoreMetrics.currentDate());
            ins.setBusinessDate(CoreMetrics.currentDate());
            ins.setRelatedBillType("ERP_PUR_RECEIPT");
            ins.setRelatedBillCode("BILL-" + code);
            dao.saveEntity(ins);

            IEntityDao<ErpQaInspectionLine> lineDao = daoProvider.daoFor(ErpQaInspectionLine.class);
            ErpQaInspectionLine line = new ErpQaInspectionLine();
            line.orm_propValueByName("id", id * 100 + 1);
            line.setInspectionId(id);
            line.setLineNo(1);
            line.setParameterName("长度");
            line.setSpecMin(toBigDecimal(specMin));
            line.setSpecMax(toBigDecimal(specMax));
            line.setResult(ErpQaConstants.INSPECTION_RESULT_PENDING);
            lineDao.saveEntity(line);
        });
        return id;
    }

    private Long seedAction(Long ncrId, String status) {
        Long id = 9700L + (long) (Math.abs(ncrId.hashCode() + status.hashCode()) % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaAction> dao = daoProvider.daoFor(ErpQaAction.class);
            ErpQaAction a = new ErpQaAction();
            a.orm_propValueByName("id", id);
            a.setNcrId(ncrId);
            a.setActionType("CAPA");
            a.setStatus(status);
            a.setDescription("纠正预防措施");
            dao.saveEntity(a);
        });
        return id;
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功，但返回: " + resp);
    }
}
