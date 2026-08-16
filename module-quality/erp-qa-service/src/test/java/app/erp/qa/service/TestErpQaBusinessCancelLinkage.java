package app.erp.qa.service;

import app.erp.qa.dao.entity.ErpQaInspection;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.59 业务单据作废联动取消质检测试（P1-RC-041，UC-QA-08 L1 use-cases.md:133-145）：
 * {@code IErpQaInspectionBiz.cancelForBusinessBill} Facade 单测——仅 PENDING 软删取消，终态不动（历史完整），
 * 无匹配零副作用，幂等，config 门控。
 *
 * <p>覆盖：①PENDING 取消（findByRelatedBill 查无 + delVersion 置位可审计）；②ACCEPTED/REJECTED 不动；
 * ③无匹配零副作用；④幂等重复取消；⑤config 关闭跳过。真实 wiring 测试落接线所在模块
 * （pur/sal/mfg 各新增 cancel 联动测试类，本类不反向依赖业务域）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaBusinessCancelLinkage extends JunitAutoTestCase {

    @RegisterExtension
    static QaFrozenClockExtension frozenClock = new QaFrozenClockExtension();

    static final Long MATERIAL_ID = 7001L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @AfterEach
    void clearConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_BUSINESS_CANCEL_LINKAGE_ENABLED, "true");
    }

    // ① PENDING 取消 → findByRelatedBill 查无 + delVersion 置位（软删审计可追溯）
    @Test
    public void testPendingCancelledAndNoLongerVisibleByRelatedBill() {
        Long insId = seedInspection("INS-BCL-1", ErpQaConstants.INSPECTION_RESULT_PENDING,
                "RCV-BCL-1", ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT);

        int cancelled = cancelForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT, "RCV-BCL-1");
        assertEquals(1, cancelled, "PENDING 质检单被取消");

        assertTrue(findByRelatedBill("RCV-BCL-1").isEmpty(), "软删后 findByRelatedBill 查无（delVersion=0 自动过滤）");
        // 审计可追溯：行仍在表（非物理删除），delVersion 置位
        ErpQaInspection ins = loadInspectionIncludingDeleted(insId);
        assertTrue(ins != null && ins.orm_logicalDeleted(), "软删行仍存在且 orm_logicalDeleted=true（审计经 delVersion 可追溯）");
    }

    // ② ACCEPTED/REJECTED 不动（历史完整，L1 use-cases.md:141）
    @Test
    public void testAcceptedAndRejectedUntouched() {
        seedInspection("INS-BCL-2A", ErpQaConstants.INSPECTION_RESULT_ACCEPTED, "RCV-BCL-2",
                ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT);
        seedInspection("INS-BCL-2R", ErpQaConstants.INSPECTION_RESULT_REJECTED, "RCV-BCL-2",
                ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT);

        int cancelled = cancelForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT, "RCV-BCL-2");
        assertEquals(0, cancelled, "终态质检单不取消");

        List<ErpQaInspection> remaining = findByRelatedBill("RCV-BCL-2");
        assertEquals(2, remaining.size(), "ACCEPTED/REJECTED 质检单保留（历史完整）");
    }

    // ③ 无匹配零副作用
    @Test
    public void testNoMatchZeroSideEffects() {
        int cancelled = cancelForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_SAL_DELIVERY, "DLV-NOMATCH");
        assertEquals(0, cancelled, "无匹配零副作用");
    }

    // ④ 幂等重复取消
    @Test
    public void testIdempotentRepeatCancel() {
        seedInspection("INS-BCL-4", ErpQaConstants.INSPECTION_RESULT_PENDING,
                "RCV-BCL-4", ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT);

        assertEquals(1, cancelForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT, "RCV-BCL-4"),
                "首次取消 1 条");
        assertEquals(0, cancelForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT, "RCV-BCL-4"),
                "重复取消零副作用（幂等）");
    }

    // ⑤ config 关闭跳过
    @Test
    public void testConfigDisabledSkips() {
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_BUSINESS_CANCEL_LINKAGE_ENABLED, "false");
        seedInspection("INS-BCL-5", ErpQaConstants.INSPECTION_RESULT_PENDING,
                "RCV-BCL-5", ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT);

        int cancelled = cancelForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT, "RCV-BCL-5");
        assertEquals(0, cancelled, "config 关闭时跳过（零副作用）");
        assertEquals(1, findByRelatedBill("RCV-BCL-5").size(), "config 关闭时 PENDING 质检单保留");
    }

    // ---------- helpers ----------

    private ErpQaInspection loadInspectionIncludingDeleted(Long insId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("id", insId));
        q.setDisableLogicalDelete(true);
        List<ErpQaInspection> list = daoProvider.daoFor(ErpQaInspection.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpQaInspection> findByRelatedBill(String billCode) {
        ApiResponse<?> resp = rpc(query, "ErpQaInspection__findByRelatedBill",
                ApiRequest.build(Map.of("billType", ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT,
                        "billCode", billCode)));
        assertEquals(0, resp.getStatus(), "findByRelatedBill 应成功: " + resp);
        Object data = resp.getData();
        if (data == null) {
            return java.util.Collections.emptyList();
        }
        return (List<ErpQaInspection>) data;
    }

    private int cancelForBusinessBill(String billType, String billCode) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("billType", billType);
        args.put("billCode", billCode);
        ApiResponse<?> resp = rpc(mutation, "ErpQaInspection__cancelForBusinessBill", ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), "cancelForBusinessBill 应成功: " + resp);
        Object data = resp.getData();
        return data instanceof Number ? ((Number) data).intValue() : Integer.parseInt(String.valueOf(data));
    }

    private Long seedInspection(String code, String result, String billCode, String billType) {
        Long id = 7800L + (long) (Math.abs(code.hashCode()) % 900);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
            ErpQaInspection ins = new ErpQaInspection();
            ins.orm_propValueByName("id", id);
            ins.setCode(code);
            ins.setInspectionType(ErpQaConstants.INSPECTION_TYPE_INCOMING);
            ins.setMaterialId(MATERIAL_ID);
            ins.setResult(result);
            ins.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            ins.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
            ins.setPosted(Boolean.FALSE);
            ins.setInspectionDate(CoreMetrics.currentDate());
            ins.setBusinessDate(CoreMetrics.currentDate());
            ins.setRelatedBillType(billType);
            ins.setRelatedBillCode(billCode);
            dao.saveEntity(ins);
        });
        return id;
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
