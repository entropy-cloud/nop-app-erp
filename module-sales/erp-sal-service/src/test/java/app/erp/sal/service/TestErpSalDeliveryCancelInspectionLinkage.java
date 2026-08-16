package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.service.ErpQaConstants;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
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
 * RC-R1.59 UC-QA-08 销售出库单作废联动取消质检接线测试（P1-RC-041）：cancel 后置调
 * {@code IErpQaInspectionBiz.cancelForBusinessBill}——关联 PENDING 质检单消失（findByRelatedBill 跨域查无）、
 * 终态质检单不动（历史完整）、config 关闭跳过、无关联零副作用。
 *
 * <p>测试落接线所在模块（erp-sal-service，qa-service test-scope 依赖提供 I*Biz Bean 实现）。
 * 质检单关联键用本域创建路径同源常量 {@code ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalDeliveryCancelInspectionLinkage extends JunitAutoTestCase {

    @RegisterExtension
    static SalFrozenClockExtension frozenClock = new SalFrozenClockExtension();

    static final Long ORG_ID = 1201L;
    static final Long CUSTOMER_ID = 2201L;
    static final Long WAREHOUSE_ID = 3201L;
    static final Long MATERIAL_ID = 4201L;
    static final Long UOM_ID = 5201L;
    static final Long CURRENCY_ID = 6201L;

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

    // ① cancel 后关联 PENDING 质检单消失；终态 REJECTED 不动（历史完整）
    @Test
    public void testCancelDeliveryCancelsPendingKeepsRejected() {
        ErpSalDelivery delivery = newDelivery("SD-BCL-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveDeliveryWithLine(delivery);
            seedInspection("INS-SAL-BCL-P", ErpQaConstants.INSPECTION_RESULT_PENDING,
                    ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, delivery.getCode());
            seedInspection("INS-SAL-BCL-R", ErpQaConstants.INSPECTION_RESULT_REJECTED,
                    ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, delivery.getCode());
        });

        assertEquals(0, cancel(delivery.getId()).getStatus(), "出库单作废应成功");
        ErpSalDelivery cancelled = daoProvider.daoFor(ErpSalDelivery.class).getEntityById(delivery.getId());
        assertEquals(ErpSalConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus(), "docStatus=CANCELLED");

        List<Map<String, Object>> remaining = findByRelatedBill(ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY,
                delivery.getCode());
        assertEquals(1, remaining.size(), "PENDING 已软删，仅 REJECTED 保留");
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED, remaining.get(0).get("result"),
                "终态质检单不动（历史完整）");
    }

    // ② config 关闭跳过：PENDING 保留
    @Test
    public void testCancelWithConfigDisabledKeepsPending() {
        AppConfig.getConfigProvider().assignConfigValue(
                app.erp.qa.service.ErpQaConstants.CONFIG_BUSINESS_CANCEL_LINKAGE_ENABLED, "false");
        ErpSalDelivery delivery = newDelivery("SD-BCL-002");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveDeliveryWithLine(delivery);
            seedInspection("INS-SAL-BCL-C", ErpQaConstants.INSPECTION_RESULT_PENDING,
                    ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, delivery.getCode());
        });

        assertEquals(0, cancel(delivery.getId()).getStatus(), "出库单作废应成功");
        assertEquals(1, findByRelatedBill(ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, delivery.getCode()).size(),
                "config 关闭时 PENDING 质检单保留");
    }

    // ③ 无关联质检单：作废零副作用
    @Test
    public void testCancelWithNoLinkedInspection() {
        ErpSalDelivery delivery = newDelivery("SD-BCL-003");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveDeliveryWithLine(delivery);
        });

        assertEquals(0, cancel(delivery.getId()).getStatus(), "无关联质检单作废应成功（零副作用）");
    }

    // ---------- helpers ----------

    private ApiResponse<?> cancel(Long deliveryId) {
        return executeRpc(mutation, "ErpSalDelivery__cancel", ApiRequest.build(Map.of("deliveryId", deliveryId)));
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

    private ErpSalDelivery newDelivery(String code) {
        ErpSalDelivery delivery = new ErpSalDelivery();
        delivery.setCode(code);
        delivery.setOrgId(ORG_ID);
        delivery.setCustomerId(CUSTOMER_ID);
        delivery.setWarehouseId(WAREHOUSE_ID);
        delivery.setBusinessDate(LocalDate.of(2026, 7, 1));
        delivery.setCurrencyId(CURRENCY_ID);
        delivery.setExchangeRate(new BigDecimal("1"));
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        delivery.setPosted(false);
        return delivery;
    }

    private void saveDeliveryWithLine(ErpSalDelivery delivery) {
        daoProvider.daoFor(ErpSalDelivery.class).saveEntity(delivery);
        IEntityDao<ErpSalDeliveryLine> lineDao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        ErpSalDeliveryLine line = new ErpSalDeliveryLine();
        line.setDeliveryId(delivery.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        lineDao.saveEntity(line);
    }

    private void seedActiveCustomer(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedInspection(String code, String result, String billType, String billCode) {
        Long id = 7800L + (long) (Math.abs(code.hashCode()) % 900);
        IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
        ErpQaInspection ins = new ErpQaInspection();
        ins.orm_propValueByName("id", id);
        ins.setCode(code);
        ins.setInspectionType(ErpQaConstants.INSPECTION_TYPE_OUTGOING);
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
