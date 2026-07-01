package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 3 端到端集成测试：请购→订单前端循环打通 + 与 Phase 1/2 衔接。
 *
 * <p>完整链路经 {@link IGraphQLEngine} 调 {@code ErpPurRequisition__submit/approve/convertToOrder} 与
 * {@code ErpPurOrder__submit/approve/cancel}（引擎建 session/事务/管道，直调缺 OrmSession 会报错，见 lessons/04）：
 * 建请购→提交→审核 APPROVED→转化生成订单(UNSUBMITTED)→提交→审核 APPROVED
 * →（订单审核纯状态，不下游触发）→作废订单→可重新转化。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurRequisitionToOrderEnd extends JunitAutoTestCase {

    static final Long ORG_ID = 1501L;
    static final Long REQUESTER_ID = 2501L;
    static final Long SUPPLIER_ID = 2511L;
    static final Long WAREHOUSE_ID = 3501L;
    static final Long MATERIAL_ID = 4501L;
    static final Long UOM_ID = 5501L;
    static final Long CURRENCY_ID = 6501L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testRequisitionToOrderToEnd() {
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            seedRequisitionWithLine();
        });

        Long reqId = 8501L;
        assertEquals(0, submit(reqId).getStatus());
        ErpPurRequisition submitted = daoProvider.daoFor(ErpPurRequisition.class).getEntityById(reqId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus());
        assertEquals(0, approve(reqId).getStatus());
        ErpPurRequisition approved = daoProvider.daoFor(ErpPurRequisition.class).getEntityById(reqId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());

        Map<String, Object> request = newRequest();
        ApiResponse<?> conv = convertToOrder(reqId, request);
        assertEquals(0, conv.getStatus());
        Long orderId = idOf(conv);
        ErpPurOrder order = daoProvider.daoFor(ErpPurOrder.class).getEntityById(orderId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED, order.getApproveStatus());
        assertEquals(reqId, order.getRequisitionId(), "回链 requisitionId");

        assertEquals(0, orderSubmit(orderId).getStatus());
        ErpPurOrder orderSubmitted = daoProvider.daoFor(ErpPurOrder.class).getEntityById(orderId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, orderSubmitted.getApproveStatus());
        assertEquals(0, orderApprove(orderId).getStatus());
        ErpPurOrder orderApproved = daoProvider.daoFor(ErpPurOrder.class).getEntityById(orderId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, orderApproved.getApproveStatus());
        assertEquals(false, orderApproved.getPosted(), "订单审核 posted=false（纯状态推进，不触发库存/凭证）");

        assertEquals(0, orderCancel(orderId).getStatus());
        ErpPurOrder cancelled = daoProvider.daoFor(ErpPurOrder.class).getEntityById(orderId);
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus());

        ApiResponse<?> secondConv = convertToOrder(reqId, request);
        assertEquals(0, secondConv.getStatus());
        ErpPurOrder secondOrder = daoProvider.daoFor(ErpPurOrder.class).getEntityById(idOf(secondConv));
        assertNotNull(secondOrder.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED, secondOrder.getApproveStatus(),
                "作废原订单后可重新转化");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submit(Long requisitionId) {
        return executeRpc(mutation, "ErpPurRequisition__submit",
                ApiRequest.build(Map.of("requisitionId", requisitionId)));
    }

    private ApiResponse<?> approve(Long requisitionId) {
        return executeRpc(mutation, "ErpPurRequisition__approve",
                ApiRequest.build(Map.of("requisitionId", requisitionId)));
    }

    private ApiResponse<?> convertToOrder(Long requisitionId, Map<String, Object> request) {
        return executeRpc(mutation, "ErpPurRequisition__convertToOrder",
                ApiRequest.build(Map.of("requisitionId", requisitionId, "request", request)));
    }

    private ApiResponse<?> orderSubmit(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__submit", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> orderApprove(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__approve", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> orderCancel(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__cancel", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long idOf(ApiResponse<?> resp) {
        Object id = ((Map<?, ?>) resp.getData()).get("id");
        return id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
    }

    // ---------- seed helpers ----------

    private void seedActiveSupplier(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType(10);
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedRequisitionWithLine() {
        Long reqId = 8501L;
        ErpPurRequisition req = new ErpPurRequisition();
        req.setId(reqId);
        req.setCode("PR-E2E-001");
        req.setOrgId(ORG_ID);
        req.setRequesterId(REQUESTER_ID);
        req.setBusinessDate(LocalDate.of(2026, 7, 1));
        req.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        daoProvider.daoFor(ErpPurRequisition.class).saveEntity(req);

        ErpPurRequisitionLine line = new ErpPurRequisitionLine();
        line.setRequisitionId(reqId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setSuggestedSupplierId(SUPPLIER_ID);
        daoProvider.daoFor(ErpPurRequisitionLine.class).saveEntity(line);
    }

    private Map<String, Object> newRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("warehouseId", WAREHOUSE_ID);
        request.put("currencyId", CURRENCY_ID);
        Map<Integer, String> prices = new LinkedHashMap<>();
        prices.put(1, "5");
        request.put("lineUnitPrices", prices);
        return request;
    }
}
