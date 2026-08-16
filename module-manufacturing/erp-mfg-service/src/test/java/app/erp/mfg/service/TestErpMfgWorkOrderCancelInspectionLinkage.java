package app.erp.mfg.service;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.59 UC-QA-08 工单作废联动取消质检接线测试（P1-RC-041）：cancel 后置调
 * {@code IErpQaInspectionBiz.cancelForBusinessBill}——关联 PENDING 质检单消失（findByRelatedBill 跨域查无）、
 * 终态质检单不动（历史完整）、config 关闭跳过、无关联零副作用。
 *
 * <p>测试落接线所在模块（erp-mfg-service，qa-service test-scope 依赖提供 I*Biz Bean 实现）。
 * 质检单关联键用本域创建路径同源常量 {@code ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgWorkOrderCancelInspectionLinkage extends JunitAutoTestCase {

    @RegisterExtension
    static MfgFrozenClockExtension frozenClock = new MfgFrozenClockExtension();

    static final Long ORG_ID = 1401L;
    static final Long CURRENCY_ID = 6401L;
    static final Long PRODUCT_ID = 1101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @AfterEach
    void clearConfig() {
        AppConfig.getConfigProvider().assignConfigValue(
                app.erp.qa.service.ErpQaConstants.CONFIG_BUSINESS_CANCEL_LINKAGE_ENABLED, "true");
    }

    // ① cancel 后关联 PENDING 质检单消失；终态 ACCEPTED 不动（历史完整）
    @Test
    public void testCancelWorkOrderCancelsPendingKeepsAccepted() {
        Long woId = seedWorkOrder("WO-BCL-001");
        ormTemplate.runInSession(() -> {
            seedInspection("INS-MFG-BCL-P", ErpQaConstants.INSPECTION_RESULT_PENDING,
                    ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-BCL-001");
            seedInspection("INS-MFG-BCL-A", ErpQaConstants.INSPECTION_RESULT_ACCEPTED,
                    ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-BCL-001");
        });

        assertEquals(0, cancel(woId).getStatus(), "工单作废应成功");
        ErpMfgWorkOrder cancelled = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED, cancelled.getDocStatus(), "docStatus=CANCELLED");

        List<Map<String, Object>> remaining = findByRelatedBill(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER,
                "WO-BCL-001");
        assertEquals(1, remaining.size(), "PENDING 已软删，仅 ACCEPTED 保留");
        assertEquals(ErpQaConstants.INSPECTION_RESULT_ACCEPTED, remaining.get(0).get("result"),
                "终态质检单不动（历史完整）");
    }

    // ② config 关闭跳过：PENDING 保留
    @Test
    public void testCancelWithConfigDisabledKeepsPending() {
        AppConfig.getConfigProvider().assignConfigValue(
                app.erp.qa.service.ErpQaConstants.CONFIG_BUSINESS_CANCEL_LINKAGE_ENABLED, "false");
        Long woId = seedWorkOrder("WO-BCL-002");
        ormTemplate.runInSession(() -> seedInspection("INS-MFG-BCL-C", ErpQaConstants.INSPECTION_RESULT_PENDING,
                ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-BCL-002"));

        assertEquals(0, cancel(woId).getStatus(), "工单作废应成功");
        assertEquals(1, findByRelatedBill(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-BCL-002").size(),
                "config 关闭时 PENDING 质检单保留");
    }

    // ③ 无关联质检单：作废零副作用
    @Test
    public void testCancelWithNoLinkedInspection() {
        Long woId = seedWorkOrder("WO-BCL-003");
        assertEquals(0, cancel(woId).getStatus(), "无关联质检单作废应成功（零副作用）");
    }

    // ---------- helpers ----------

    private ApiResponse<?> cancel(Long workOrderId) {
        return executeRpc(mutation, "ErpMfgWorkOrder__cancel", ApiRequest.build(Map.of("workOrderId", workOrderId)));
    }

    private List<Map<String, Object>> findByRelatedBill(String billType, String billCode) {
        ApiResponse<?> resp = executeRpc(query, "ErpQaInspection__findByRelatedBill",
                ApiRequest.build(Map.of("billType", billType, "billCode", billCode)));
        assertEquals(0, resp.getStatus(), "findByRelatedBill 应成功: " + resp);
        Object data = resp.getData();
        if (data == null) {
            return java.util.Collections.emptyList();
        }
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (Object item : (List<?>) data) {
            list.add((Map<String, Object>) item);
        }
        return list;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long seedWorkOrder(String code) {
        Long id = 8300L + (long) (Math.abs(code.hashCode()) % 700);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(PRODUCT_ID);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(new BigDecimal("2"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
            dao.saveEntity(wo);
        });
        return id;
    }

    private void seedInspection(String code, String result, String billType, String billCode) {
        Long id = 7800L + (long) (Math.abs(code.hashCode()) % 900);
        IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
        ErpQaInspection ins = new ErpQaInspection();
        ins.orm_propValueByName("id", id);
        ins.setCode(code);
        ins.setInspectionType(ErpQaConstants.INSPECTION_TYPE_FINAL);
        ins.setMaterialId(PRODUCT_ID);
        ins.setResult(result);
        ins.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
        ins.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
        ins.setPosted(Boolean.FALSE);
        ins.setInspectionDate(CoreMetrics.currentDate());
        ins.setBusinessDate(CoreMetrics.currentDate());
        ins.setRelatedBillType(billType);
        ins.setRelatedBillCode(billCode);
        dao.saveEntity(ins);
    }
}
