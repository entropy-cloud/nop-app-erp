package app.erp.qa.service;

import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 测试：质检单 4 态状态机（行级评测 specMin/specMax vs measuredValue + 结果汇总 ACCEPTED/CONDITIONAL/REJECTED
 * + posted + 终态不可恢复 + findByRelatedBill）。
 *
 * <p>覆盖 {@code docs/design/quality/state-machine.md §适用对象一} 主路径与异常路径。
 * 经 {@link IGraphQLEngine} 调 {@code ErpQaInspection__recordResult/findByRelatedBill/isInspectionCleared}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaInspectionStateMachine extends JunitAutoTestCase {

    @RegisterExtension
    static QaFrozenClockExtension frozenClock = new QaFrozenClockExtension();

    static final Long MATERIAL_ID = 7001L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testAllAcceptedGoesAccepted() {
        Long insId = seedInspection("INS-ACCEPT", withLine(null, "10", "20"), withLine(null, "0", "100"));
        recordMeasured(insId, false, lineInput(1, "15"), lineInput(2, "50"));

        ErpQaInspection ins = loadInspection(insId);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_ACCEPTED, ins.getResult(), "全合格→ACCEPTED");
        assertTrue(Boolean.TRUE.equals(ins.getPosted()), "posted=true");
    }

    @Test
    public void testPartialRejectedWithConcessionGoesConditional() {
        Long insId = seedInspection("INS-COND", withLine("长度", "10", "20"), withLine("重量", "0", "100"));
        // 第一行 15（合格），第二行 200（超 max 100，不合格）+ 让步
        recordMeasured(insId, true, lineInput(1, "15"), lineInput(2, "200"));

        ErpQaInspection ins = loadInspection(insId);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_CONDITIONAL, ins.getResult(), "部分不合格+让步→CONDITIONAL");
        assertEquals(ErpQaConstants.APPROVE_STATUS_APPROVED, ins.getApproveStatus(), "让步须审批 APPROVED");
    }

    @Test
    public void testRejectedCriticalGoesRejected() {
        Long insId = seedInspection("INS-REJ", withLine("长度", "10", "20"));
        // 5 < min 10 → 不合格，未让步
        recordMeasured(insId, false, lineInput(1, "5"));

        ErpQaInspection ins = loadInspection(insId);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED, ins.getResult(), "关键不合格未让步→REJECTED");
    }

    @Test
    public void testLineSpecEvaluationParseFailureTreatedAsRejected() {
        // 实测值非数值（解析失败）→ 视为不合格。measuredValue 列域强转 BigDecimal，非数值无法落库，
        // 故直接构造内存行（不经 ORM 落库）调用评测器验证解析失败路径
        app.erp.qa.dao.entity.ErpQaInspectionLine line = new app.erp.qa.dao.entity.ErpQaInspectionLine();
        line.orm_propValueByName("id", 99001L);
        line.setSpecMin(new BigDecimal("10"));
        line.setSpecMax(new BigDecimal("20"));
        line.setMeasuredValue("abc");
        String result = app.erp.qa.service.entity.InspectionResultEvaluator.evaluateLine(line);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED, result, "实测值解析失败→REJECTED");

        // 无规格上下限（外观类）+ 实测值非空 → ACCEPTED
        app.erp.qa.dao.entity.ErpQaInspectionLine appearance = new app.erp.qa.dao.entity.ErpQaInspectionLine();
        appearance.orm_propValueByName("id", 99002L);
        appearance.setMeasuredValue("OK");
        assertEquals(ErpQaConstants.INSPECTION_RESULT_ACCEPTED,
                app.erp.qa.service.entity.InspectionResultEvaluator.evaluateLine(appearance),
                "外观类（无规格）+ 非空实测 → ACCEPTED");
    }

    @Test
    public void testTerminalResultCannotReRecord() {
        Long insId = seedInspection("INS-TERM", withLine("长度", "10", "20"));
        recordMeasured(insId, false, lineInput(1, "15")); // → ACCEPTED（终态）
        // 终态不可恢复：再次 recordResult 应拒绝
        ApiResponse<?> resp = rpc(mutation, "ErpQaInspection__recordResult", recordResultArgs(insId, new ArrayList<>(), false));
        assertEquals(ErpQaErrors.ERR_INVALID_INSPECTION_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "终态不可恢复重复 recordResult 抛错");
    }

    // ---------- P0-MA2-017：silent flip 守卫（passInspection/failInspection 终态拒绝）----------

    @Test
    public void testPassInspectionRejectsTerminalState() {
        Long insId = seedInspection("INS-PASS-REJ", withLine("长度", "10", "20"));
        recordMeasured(insId, false, lineInput(1, "5")); // 5 < min 10 → REJECTED（终态）
        // silent flip REJECTED→ACCEPTED 必须被拒绝
        ApiResponse<?> resp = rpc(mutation, "ErpQaInspection__passInspection",
                ApiRequest.build(Map.of("inspectionId", insId)));
        assertEquals(ErpQaErrors.ERR_INVALID_INSPECTION_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "REJECTED 终态 passInspection 应抛 illegal-status-transition");
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED, loadInspection(insId).getResult(),
                "源单 result 保持 REJECTED 不被覆写");
    }

    @Test
    public void testFailInspectionRejectsTerminalState() {
        Long insId = seedInspection("INS-FAIL-ACC", withLine("长度", "10", "20"));
        recordMeasured(insId, false, lineInput(1, "15")); // 15 ∈ [10,20] → ACCEPTED（终态）
        // silent flip ACCEPTED→REJECTED 必须被拒绝
        ApiResponse<?> resp = rpc(mutation, "ErpQaInspection__failInspection",
                ApiRequest.build(Map.of("inspectionId", insId)));
        assertEquals(ErpQaErrors.ERR_INVALID_INSPECTION_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "ACCEPTED 终态 failInspection 应抛 illegal-status-transition");
        assertEquals(ErpQaConstants.INSPECTION_RESULT_ACCEPTED, loadInspection(insId).getResult(),
                "源单 result 保持 ACCEPTED 不被覆写");
    }

    @Test
    public void testPassInspectionFromPendingSetsPosted() {
        Long insId = seedInspection("INS-PASS-OK", withLine("长度", "10", "20"));
        ApiResponse<?> resp = rpc(mutation, "ErpQaInspection__passInspection",
                ApiRequest.build(Map.of("inspectionId", insId)));
        assertEquals(0, resp.getStatus(), "PENDING→passInspection 应成功: " + resp);
        ErpQaInspection ins = loadInspection(insId);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_ACCEPTED, ins.getResult(), "PENDING→ACCEPTED");
        assertTrue(Boolean.TRUE.equals(ins.getPosted()), "passInspection 设 posted=true");
        assertNotNull(ins.getPostedAt(), "passInspection 设 postedAt");
        assertNotNull(ins.getPostedBy(), "passInspection 设 postedBy");
    }

    @Test
    public void testFailInspectionFromPendingSetsPostedAndTriggersNcr() {
        Long insId = seedInspection("INS-FAIL-NCR", withLine("长度", "10", "20"));
        ApiResponse<?> resp = rpc(mutation, "ErpQaInspection__failInspection",
                ApiRequest.build(Map.of("inspectionId", insId)));
        assertEquals(0, resp.getStatus(), "PENDING→failInspection 应成功: " + resp);
        ErpQaInspection ins = loadInspection(insId);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED, ins.getResult(), "PENDING→REJECTED");
        assertTrue(Boolean.TRUE.equals(ins.getPosted()), "failInspection 设 posted=true");
        ErpQaNonConformance ncr = findNcrBySourceCode(ins.getCode());
        assertNotNull(ncr, "failInspection 触发 autoCreateNcrFromInspection 建 NCR");
        assertEquals(ErpQaConstants.NCR_STATUS_OPEN, ncr.getStatus(), "NCR 初始 OPEN");
        assertEquals(ErpQaConstants.NCR_SOURCE_TYPE_INSPECTION, ncr.getSourceType(), "NCR sourceType=INSPECTION");
    }

    // ---------- P0-MA2-017 方案 A：reInspect 已删除 + 复检走新建关联质检单 ----------

    @Test
    public void testReInspectActionRemoved() {
        Long insId = seedInspection("INS-REINSPECT-GONE", withLine("长度", "10", "20"));
        // reInspect 方法 + 接口签名已删除：GraphQL action 不再注册，引擎抛 unknown-operation
        NopException ex = assertThrows(NopException.class, () -> rpc(mutation, "ErpQaInspection__reInspect",
                ApiRequest.build(Map.of("inspectionId", insId))));
        assertEquals("nop.err.graphql.unknown-operation", ex.getErrorCode(), "reInspect action 已删除");
        // 源单 result 保持 PENDING（无任何状态迁移发生）
        assertEquals(ErpQaConstants.INSPECTION_RESULT_PENDING, loadInspection(insId).getResult(),
                "reInspect 不存在，源单 result 不变");
    }

    @Test
    public void testReinspectionViaNewIndependentInspection() {
        // 复检语义（owner doc §3）：原单 REJECTED 终态保留，复检经新建独立质检单（关联同一业务单据）
        Long originalId = seedInspection("INS-ORIG-REJ", withLine("长度", "10", "20"));
        recordMeasured(originalId, false, lineInput(1, "5")); // 原单 → REJECTED（终态）

        // 新建复检单（同一业务单据 ERP_PUR_RECEIPT/BILL-REINSPECT，独立 PENDING）
        Long reinspectId = seedInspection("INS-REINSPECT-OK", withLine("长度", "10", "20"));
        recordMeasured(reinspectId, false, lineInput(1, "15")); // 复检单 → ACCEPTED

        // 两单 result 独立：原单保持 REJECTED，复检单 ACCEPTED
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED, loadInspection(originalId).getResult(),
                "原单 REJECTED 终态不被复检覆写");
        assertEquals(ErpQaConstants.INSPECTION_RESULT_ACCEPTED, loadInspection(reinspectId).getResult(),
                "复检单独立结果 ACCEPTED");
    }

    @Test
    public void testFindByRelatedBillReturnsResult() {
        Long insId = seedInspection("INS-FIND", withLine("长度", "10", "20"));
        recordMeasured(insId, false, lineInput(1, "15"));

        ApiResponse<?> resp = rpc(query, "ErpQaInspection__findByRelatedBill",
                ApiRequest.build(Map.of("billType", "ERP_PUR_RECEIPT", "billCode", "BILL-FIND")));
        assertEquals(0, resp.getStatus());
        List<?> list = (List<?>) resp.getData();
        assertEquals(1, list.size(), "findByRelatedBill 返回关联质检单");
        assertEquals(ErpQaConstants.INSPECTION_RESULT_ACCEPTED,
                ((Map<?, ?>) list.get(0)).get("result"));

        // isInspectionCleared：ACCEPTED → true
        ApiResponse<?> cleared = rpc(query, "ErpQaInspection__isInspectionCleared",
                ApiRequest.build(Map.of("billType", "ERP_PUR_RECEIPT", "billCode", "BILL-FIND")));
        assertEquals(0, cleared.getStatus());
        assertEquals(Boolean.TRUE, cleared.getData(), "ACCEPTED 后放行");
    }

    @Test
    public void testIsInspectionClearedFalseWhenPending() {
        Long insId = seedInspection("INS-CLEAR", withLine("长度", "10", "20"));
        // 未录入结果（PENDING）→ isInspectionCleared=false
        ApiResponse<?> resp = rpc(query, "ErpQaInspection__isInspectionCleared",
                ApiRequest.build(Map.of("billType", "ERP_PUR_RECEIPT", "billCode", "BILL-CLEAR")));
        assertEquals(0, resp.getStatus());
        assertEquals(Boolean.FALSE, resp.getData(), "PENDING 时强制质检阻塞");
        assertFalse(Boolean.TRUE.equals(resp.getData()));
    }

    // ---------- helpers ----------

    private ErpQaInspection loadInspection(Long insId) {
        return daoProvider.daoFor(ErpQaInspection.class).getEntityById(insId);
    }

    private ErpQaNonConformance findNcrBySourceCode(String sourceCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceCode", sourceCode));
        q.setLimit(1);
        List<ErpQaNonConformance> list = daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void recordMeasured(Long insId, boolean allowConcession, Map<String, Object>... lines) {
        List<Map<String, Object>> lineList = new ArrayList<>();
        for (Map<String, Object> l : lines) {
            lineList.add(l);
        }
        ApiResponse<?> resp = rpc(mutation, "ErpQaInspection__recordResult", recordResultArgs(insId, lineList, allowConcession));
        assertEquals(0, resp.getStatus(), "recordResult 应成功，但返回: " + resp);
    }

    private void recordMeasured(Long insId, boolean allowConcession) {
        ApiResponse<?> resp = rpc(mutation, "ErpQaInspection__recordResult",
                recordResultArgs(insId, new ArrayList<>(), allowConcession));
        assertEquals(0, resp.getStatus(), "recordResult 应成功，但返回: " + resp);
    }

    private Map<String, Object> lineInput(int lineNo, String measuredValue) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lineNo", lineNo);
        m.put("measuredValue", measuredValue);
        return m;
    }

    private ApiRequest<?> recordResultArgs(Long insId, List<Map<String, Object>> lines, boolean allowConcession) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("inspectionId", insId);
        args.put("lineResults", lines);
        args.put("allowConcession", allowConcession);
        return ApiRequest.build(args);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long seedInspection(String code, LineSpec... lines) {
        Long id = 6000L + (long) (Math.abs(code.hashCode()) % 1000);
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
            ins.setRelatedBillCode("BILL-" + code.replace("INS-", ""));
            dao.saveEntity(ins);

            IEntityDao<ErpQaInspectionLine> lineDao = daoProvider.daoFor(ErpQaInspectionLine.class);
            int lineNo = 1;
            for (LineSpec spec : lines) {
                ErpQaInspectionLine line = new ErpQaInspectionLine();
                line.orm_propValueByName("id", id * 100 + lineNo);
                line.setInspectionId(id);
                line.setLineNo(lineNo);
                line.setParameterName(spec.parameterName == null ? "参数" + lineNo : spec.parameterName);
                line.setSpecMin(spec.specMin);
                line.setSpecMax(spec.specMax);
                line.setResult(ErpQaConstants.INSPECTION_RESULT_PENDING);
                lineDao.saveEntity(line);
                lineNo++;
            }
        });
        return id;
    }

    private LineSpec withLine(String parameterName, String specMin, String specMax) {
        return new LineSpec(parameterName, toBigDecimal(specMin), toBigDecimal(specMax));
    }

    private static BigDecimal toBigDecimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static final class LineSpec {
        final String parameterName;
        final BigDecimal specMin;
        final BigDecimal specMax;

        LineSpec(String parameterName, BigDecimal specMin, BigDecimal specMax) {
            this.parameterName = parameterName;
            this.specMin = specMin;
            this.specMax = specMax;
        }
    }
}
