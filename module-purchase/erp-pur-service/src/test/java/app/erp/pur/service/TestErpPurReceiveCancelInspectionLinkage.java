package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
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
 * RC-R1.59 UC-QA-08 采购入库单作废联动取消质检接线测试（P1-RC-041）：cancel 后置调
 * {@code IErpQaInspectionBiz.cancelForBusinessBill}——关联 PENDING 质检单消失（findByRelatedBill 跨域查无）、
 * 终态质检单不动（历史完整）、config 关闭跳过、无关联零副作用。
 *
 * <p>测试落接线所在模块（erp-pur-service，qa-service test-scope 依赖提供 I*Biz Bean 实现）。
 * 质检单关联键用本域创建路径同源常量 {@code ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReceiveCancelInspectionLinkage extends JunitAutoTestCase {

    @RegisterExtension
    static PurFrozenClockExtension frozenClock = new PurFrozenClockExtension();

    static final Long ORG_ID = 1101L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long WAREHOUSE_ID = 3101L;
    static final Long MATERIAL_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;

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
    public void testCancelReceiveCancelsPendingKeepsAccepted() {
        ErpPurReceive receive = newReceive("PR-BCL-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveReceiveWithLine(receive);
            seedInspection("INS-PUR-BCL-P", ErpQaConstants.INSPECTION_RESULT_PENDING,
                    ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receive.getCode());
            seedInspection("INS-PUR-BCL-A", ErpQaConstants.INSPECTION_RESULT_ACCEPTED,
                    ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receive.getCode());
        });

        assertEquals(0, cancel(receive.getId()).getStatus(), "入库单作废应成功");
        ErpPurReceive cancelled = daoProvider.daoFor(ErpPurReceive.class).getEntityById(receive.getId());
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus(), "docStatus=CANCELLED");

        List<Map<String, Object>> remaining = findByRelatedBill(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE,
                receive.getCode());
        assertEquals(1, remaining.size(), "PENDING 已软删，仅 ACCEPTED 保留");
        assertEquals(ErpQaConstants.INSPECTION_RESULT_ACCEPTED, remaining.get(0).get("result"),
                "终态质检单不动（历史完整）");
    }

    // ② config 关闭跳过：PENDING 保留
    @Test
    public void testCancelWithConfigDisabledKeepsPending() {
        AppConfig.getConfigProvider().assignConfigValue(
                app.erp.qa.service.ErpQaConstants.CONFIG_BUSINESS_CANCEL_LINKAGE_ENABLED, "false");
        ErpPurReceive receive = newReceive("PR-BCL-002");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveReceiveWithLine(receive);
            seedInspection("INS-PUR-BCL-C", ErpQaConstants.INSPECTION_RESULT_PENDING,
                    ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receive.getCode());
        });

        assertEquals(0, cancel(receive.getId()).getStatus(), "入库单作废应成功");
        assertEquals(1, findByRelatedBill(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receive.getCode()).size(),
                "config 关闭时 PENDING 质检单保留");
    }

    // ③ 无关联质检单：作废零副作用
    @Test
    public void testCancelWithNoLinkedInspection() {
        ErpPurReceive receive = newReceive("PR-BCL-003");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveReceiveWithLine(receive);
        });

        assertEquals(0, cancel(receive.getId()).getStatus(), "无关联质检单作废应成功（零副作用）");
    }

    // ---------- helpers ----------

    private ApiResponse<?> cancel(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__cancel", ApiRequest.build(Map.of("receiveId", receiveId)));
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

    private ErpPurReceive newReceive(String code) {
        ErpPurReceive receive = new ErpPurReceive();
        receive.setCode(code);
        receive.setOrgId(ORG_ID);
        receive.setSupplierId(SUPPLIER_ID);
        receive.setWarehouseId(WAREHOUSE_ID);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(CURRENCY_ID);
        receive.setExchangeRate(new BigDecimal("1"));
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        receive.setPosted(false);
        return receive;
    }

    private void saveReceiveWithLine(ErpPurReceive receive) {
        daoProvider.daoFor(ErpPurReceive.class).saveEntity(receive);
        IEntityDao<ErpPurReceiveLine> lineDao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setReceiveId(receive.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("5"));
        lineDao.saveEntity(line);
    }

    private void seedActiveSupplier(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedInspection(String code, String result, String billType, String billCode) {
        Long id = 7800L + (long) (Math.abs(code.hashCode()) % 900);
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
    }
}
