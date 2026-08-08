package app.erp.pur.service;

import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：请购→订单转化（RC-R1.10 多供应商拆单）+ 幂等 + 回链 + 缺失字段补全。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpPurRequisition__convertToOrder} 与 {@code ErpPurOrder__submit/approve/cancel}，
 * 引擎负责建 session/事务/管道（直调缺 OrmSession 会报错，见 lessons/04）。
 * 覆盖：(a) APPROVED 请购 + 补充字段 → 订单(UNSUBMITTED) + 行/字段/requisitionId 回链/金额族正确；
 * (b) 非 APPROVED 请购转化拒绝；(c) 行供应商缺失拒绝（RC-R1.10 后仅缺失拒绝，混合拆单成功）；
 * (d) 已转化幂等拒绝（作废后可重新转化）；(e) 转化产物订单可被 Phase 1 审核状态机推进到 APPROVED（两阶段衔接）；
 * (f) RC-R1.10 多供应商拆分：2 供应商 → 2 订单 + 各组行归属 + per-supplier 头字段 + 单供应商全局字段回退。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurRequisitionConvertToOrder extends JunitAutoTestCase {

    static final Long ORG_ID = 1401L;
    static final Long REQUESTER_ID = 2401L;
    static final Long SUPPLIER_ID = 2411L;
    static final Long SUPPLIER_ID_2 = 2412L;
    static final Long WAREHOUSE_ID = 3401L;
    static final Long WAREHOUSE_ID_2 = 3402L;
    static final Long MATERIAL_ID = 4401L;
    static final Long UOM_ID = 5401L;
    static final Long CURRENCY_ID = 6401L;
    static final Long CURRENCY_ID_2 = 6402L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testConvertApprovedReqToOrder() {
        ErpPurRequisition req = newApprovedRequisition("PR-CONV-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10")));
        ormTemplate.runInSession(() -> saveRequisitionLine(req, 2, SUPPLIER_ID, new BigDecimal("5")));

        Map<String, Object> request = newRequest("5", "13");

        ApiResponse<?> resp = convertToOrder(req.getId(), request);
        assertEquals(0, resp.getStatus(), "转化应成功");
        ErpPurOrder order = daoProvider.daoFor(ErpPurOrder.class).getEntityById(firstIdOf(resp));

        assertEquals(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED, order.getApproveStatus(),
                "转化产物 approveStatus=UNSUBMITTED");
        assertEquals(ErpPurConstants.DOC_STATUS_DRAFT, order.getDocStatus(),
                "转化产物 docStatus=DRAFT");
        assertEquals(req.getId(), order.getRequisitionId(), "回链 requisitionId");
        assertEquals(SUPPLIER_ID, order.getSupplierId(), "订单供应商=请购行一致供应商");
        assertEquals(WAREHOUSE_ID, order.getWarehouseId(), "订单仓库=调用方提供");
        assertEquals(CURRENCY_ID, order.getCurrencyId(), "订单币种=调用方提供");
        assertEquals(req.getBusinessDate(), order.getBusinessDate(), "订单 businessDate=请购 businessDate");
        assertEquals(req.getOrgId(), order.getOrgId(), "订单 orgId=请购 orgId");

        List<ErpPurOrderLine> lines = loadOrderLines(order.getId());
        assertEquals(2, lines.size(), "两行转化");

        ErpPurOrderLine l1 = lines.stream().filter(l -> l.getLineNo() == 1).findFirst().orElseThrow();
        assertEquals(MATERIAL_ID, l1.getMaterialId(), "行1 materialId 复制");
        assertEquals(UOM_ID, l1.getUoMId(), "行1 uoMId 复制");
        assertEquals(0, new BigDecimal("10").compareTo(l1.getQuantity()), "行1 quantity 复制");
        assertEquals(0, new BigDecimal("5").compareTo(l1.getUnitPrice()), "行1 unitPrice=调用方提供");
        assertEquals(0, new BigDecimal("50").compareTo(l1.getAmount()), "行1 amount=50");
        assertEquals(0, new BigDecimal("13").compareTo(l1.getTaxRate()), "行1 taxRate=调用方提供");
        assertEquals(0, new BigDecimal("6.50").compareTo(l1.getTaxAmount()), "行1 taxAmount=6.50");
        assertEquals(0, new BigDecimal("56.50").compareTo(l1.getAmountWithTax()),
                "行1 amountWithTax=56.50");
    }

    @Test
    public void testConvertNotApprovedRejected() {
        ErpPurRequisition req = newRequisition("PR-NOTAPP-001");
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10")));

        Map<String, Object> request = newRequest("5", null);
        ApiResponse<?> bad = convertToOrder(req.getId(), request);
        assertEquals(ErpPurErrors.ERR_REQ_NOT_APPROVED.getErrorCode(), bad.getCode(),
                "非 APPROVED 请购转化应返回 ERR_REQ_NOT_APPROVED");
    }

    @Test
    public void testConvertMixedSupplierSplitsOrders() {
        ErpPurRequisition req = newApprovedRequisition("PR-MIX-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> {
            saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10"));
            saveRequisitionLine(req, 2, SUPPLIER_ID_2, new BigDecimal("5"));
        });

        Map<String, Object> request = newRequest("5", null);
        ApiResponse<?> resp = convertToOrder(req.getId(), request);
        assertEquals(0, resp.getStatus(), "RC-R1.10 混合供应商应拆单成功");
        List<Map<String, Object>> data = listDataOf(resp);
        assertEquals(2, data.size(), "混合供应商请购应生成 2 订单");

        ErpPurOrder order1 = daoProvider.daoFor(ErpPurOrder.class).getEntityById(idOf(data.get(0)));
        ErpPurOrder order2 = daoProvider.daoFor(ErpPurOrder.class).getEntityById(idOf(data.get(1)));
        assertEquals(SUPPLIER_ID, order1.getSupplierId(), "订单1 供应商=组1 供应商");
        assertEquals(SUPPLIER_ID_2, order2.getSupplierId(), "订单2 供应商=组2 供应商");
        assertEquals(req.getId(), order1.getRequisitionId(), "订单1 回链 requisitionId");
        assertEquals(req.getId(), order2.getRequisitionId(), "订单2 回链 requisitionId");
        assertEquals(1, loadOrderLines(order1.getId()).size(), "订单1 含 1 行");
        assertEquals(1, loadOrderLines(order2.getId()).size(), "订单2 含 1 行");
    }

    @Test
    public void testConvertMissingSupplierRejected() {
        ErpPurRequisition req = newApprovedRequisition("PR-MISS-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> {
            saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10"));
            saveRequisitionLine(req, 2, null, new BigDecimal("5"));
        });

        Map<String, Object> request = newRequest("5", null);
        ApiResponse<?> bad = convertToOrder(req.getId(), request);
        assertEquals(ErpPurErrors.ERR_REQ_MIXED_OR_MISSING_SUPPLIER.getErrorCode(), bad.getCode(),
                "行供应商缺失应返回 ERR_REQ_MIXED_OR_MISSING_SUPPLIER");
    }

    @Test
    public void testConvertIdempotentRejected() {
        ErpPurRequisition req = newApprovedRequisition("PR-IDEM-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10")));
        Map<String, Object> request = newRequest("5", null);

        ApiResponse<?> first = convertToOrder(req.getId(), request);
        assertEquals(0, first.getStatus());
        Long firstId = firstIdOf(first);
        assertNotNull(firstId);

        ApiResponse<?> bad = convertToOrder(req.getId(), request);
        assertEquals(ErpPurErrors.ERR_REQ_ALREADY_CONVERTED.getErrorCode(), bad.getCode(),
                "已转化请购重复转化应返回 ERR_REQ_ALREADY_CONVERTED");

        assertEquals(0, orderCancel(firstId).getStatus());

        ApiResponse<?> second = convertToOrder(req.getId(), request);
        assertEquals(0, second.getStatus());
        ErpPurOrder secondOrder = daoProvider.daoFor(ErpPurOrder.class).getEntityById(firstIdOf(second));
        assertEquals(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED, secondOrder.getApproveStatus());
    }

    @Test
    public void testConvertedOrderThenApprove() {
        ErpPurRequisition req = newApprovedRequisition("PR-LINK-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10")));
        ormTemplate.runInSession(() -> seedActiveSupplier(SUPPLIER_ID));

        Map<String, Object> request = newRequest("5", null);
        ApiResponse<?> conv = convertToOrder(req.getId(), request);
        assertEquals(0, conv.getStatus());
        Long orderId = firstIdOf(conv);

        assertEquals(0, orderSubmit(orderId).getStatus());
        ErpPurOrder submitted = daoProvider.daoFor(ErpPurOrder.class).getEntityById(orderId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus());

        assertEquals(0, orderApprove(orderId).getStatus());
        ErpPurOrder approved = daoProvider.daoFor(ErpPurOrder.class).getEntityById(orderId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "转化产物订单可走 Phase 1 状态机推进到 APPROVED");
    }

    // ---------- RC-R1.10 拆单矩阵 ----------

    @Test
    public void testConvertMultiSupplierSplitsOrders() {
        ErpPurRequisition req = newApprovedRequisition("PR-SPLIT-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> {
            saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10"));
            saveRequisitionLine(req, 2, SUPPLIER_ID, new BigDecimal("5"));
            saveRequisitionLine(req, 3, SUPPLIER_ID_2, new BigDecimal("8"));
        });

        Map<String, Object> request = newRequest("5", "13");
        ApiResponse<?> resp = convertToOrder(req.getId(), request);
        assertEquals(0, resp.getStatus(), "多供应商拆单应成功");

        List<Map<String, Object>> data = listDataOf(resp);
        assertEquals(2, data.size(), "2 供应商 → 2 订单");
        for (Map<String, Object> orderData : data) {
            assertNotNull(orderData.get("code"), "每单 code 非空");
            assertEquals(req.getId(), numberOf(orderData.get("requisitionId")), "每单回链 requisitionId");
        }

        ErpPurOrder order1 = daoProvider.daoFor(ErpPurOrder.class).getEntityById(idOf(data.get(0)));
        ErpPurOrder order2 = daoProvider.daoFor(ErpPurOrder.class).getEntityById(idOf(data.get(1)));

        assertEquals(SUPPLIER_ID, order1.getSupplierId(), "订单1 供应商=组1");
        List<ErpPurOrderLine> lines1 = loadOrderLines(order1.getId());
        assertEquals(2, lines1.size(), "订单1 含组1 两行");
        ErpPurOrderLine l1a = lines1.stream().filter(l -> l.getLineNo() == 1).findFirst().orElseThrow();
        ErpPurOrderLine l1b = lines1.stream().filter(l -> l.getLineNo() == 2).findFirst().orElseThrow();
        assertEquals(1, l1a.getLineNo(), "订单1 行1 lineNo 重排从 1");
        assertEquals(2, l1b.getLineNo(), "订单1 行2 lineNo 重排为 2");

        assertEquals(SUPPLIER_ID_2, order2.getSupplierId(), "订单2 供应商=组2");
        List<ErpPurOrderLine> lines2 = loadOrderLines(order2.getId());
        assertEquals(1, lines2.size(), "订单2 含组2 一行");
        assertEquals(1, lines2.get(0).getLineNo(), "订单2 行1 lineNo 重排从 1");

        assertEquals(0, new BigDecimal("50").compareTo(l1a.getAmount()), "订单1 行1 amount=50");
        assertEquals(0, new BigDecimal("25").compareTo(l1b.getAmount()), "订单1 行2 amount=25");
        assertEquals(0, new BigDecimal("40").compareTo(lines2.get(0).getAmount()),
                "订单2 行1 amount=40");
    }

    @Test
    public void testConvertPerSupplierHeaderFields() {
        ErpPurRequisition req = newApprovedRequisition("PR-HDR-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> {
            saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10"));
            saveRequisitionLine(req, 2, SUPPLIER_ID_2, new BigDecimal("5"));
        });

        Map<String, Object> request = newRequest("5", null);
        Map<String, Object> option1 = new LinkedHashMap<>();
        option1.put("warehouseId", WAREHOUSE_ID);
        option1.put("currencyId", CURRENCY_ID);
        option1.put("deliveryDate", "2026-08-20");
        Map<String, Object> option2 = new LinkedHashMap<>();
        option2.put("warehouseId", WAREHOUSE_ID_2);
        option2.put("currencyId", CURRENCY_ID_2);
        option2.put("deliveryDate", "2026-08-25");
        Map<Object, Object> supplierOptions = new LinkedHashMap<>();
        supplierOptions.put(String.valueOf(SUPPLIER_ID), option1);
        supplierOptions.put(String.valueOf(SUPPLIER_ID_2), option2);
        request.put("supplierOptions", supplierOptions);

        ApiResponse<?> resp = convertToOrder(req.getId(), request);
        assertEquals(0, resp.getStatus(), "per-supplier 头字段拆单应成功");
        List<Map<String, Object>> data = listDataOf(resp);
        assertEquals(2, data.size(), "2 供应商 → 2 订单");

        ErpPurOrder order1 = daoProvider.daoFor(ErpPurOrder.class).getEntityById(idOf(data.get(0)));
        ErpPurOrder order2 = daoProvider.daoFor(ErpPurOrder.class).getEntityById(idOf(data.get(1)));

        assertEquals(WAREHOUSE_ID, order1.getWarehouseId(), "订单1 warehouseId=per-supplier 选项");
        assertEquals(CURRENCY_ID, order1.getCurrencyId(), "订单1 currencyId=per-supplier 选项");
        assertEquals(LocalDate.of(2026, 8, 20), order1.getDeliveryDate(), "订单1 deliveryDate=per-supplier 选项");

        assertEquals(WAREHOUSE_ID_2, order2.getWarehouseId(), "订单2 warehouseId=per-supplier 选项");
        assertEquals(CURRENCY_ID_2, order2.getCurrencyId(), "订单2 currencyId=per-supplier 选项");
        assertEquals(LocalDate.of(2026, 8, 25), order2.getDeliveryDate(), "订单2 deliveryDate=per-supplier 选项");
    }

    @Test
    public void testConvertSingleSupplierFallback() {
        ErpPurRequisition req = newApprovedRequisition("PR-FALL-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10")));

        Map<String, Object> request = newRequest("5", null);
        ApiResponse<?> resp = convertToOrder(req.getId(), request);
        assertEquals(0, resp.getStatus(), "单供应商转化应成功");
        List<Map<String, Object>> data = listDataOf(resp);
        assertEquals(1, data.size(), "单供应商 → 1 订单");

        ErpPurOrder order = daoProvider.daoFor(ErpPurOrder.class).getEntityById(idOf(data.get(0)));
        assertEquals(WAREHOUSE_ID, order.getWarehouseId(), "未提供 per-supplier 映射 → 全局 warehouseId 生效");
        assertEquals(CURRENCY_ID, order.getCurrencyId(), "未提供 per-supplier 映射 → 全局 currencyId 生效");
        assertEquals(null, order.getDeliveryDate(), "未提供 per-supplier 映射 → deliveryDate 置空");
    }

    @Test
    public void testConvertSplitThenIdempotent() {
        ErpPurRequisition req = newApprovedRequisition("PR-SPLIT-IDEM-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> {
            saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10"));
            saveRequisitionLine(req, 2, SUPPLIER_ID_2, new BigDecimal("5"));
        });

        Map<String, Object> request = newRequest("5", null);
        ApiResponse<?> first = convertToOrder(req.getId(), request);
        assertEquals(0, first.getStatus());
        List<Long> orderIds = new java.util.ArrayList<>();
        for (Map<String, Object> orderData : listDataOf(first)) {
            orderIds.add(idOf(orderData));
        }
        assertEquals(2, orderIds.size(), "拆单生成 2 订单");

        ApiResponse<?> bad = convertToOrder(req.getId(), request);
        assertEquals(ErpPurErrors.ERR_REQ_ALREADY_CONVERTED.getErrorCode(), bad.getCode(),
                "拆单后重复转化应返回 ERR_REQ_ALREADY_CONVERTED");

        for (Long orderId : orderIds) {
            assertEquals(0, orderCancel(orderId).getStatus(), "作废订单应成功");
        }

        ApiResponse<?> second = convertToOrder(req.getId(), request);
        assertEquals(0, second.getStatus(), "全部订单作废后可重新转化");
        assertEquals(2, listDataOf(second).size(), "重新转化仍拆单生成 2 订单");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> convertToOrder(Long requisitionId, Map<String, Object> request) {
        return executeRpc(mutation, "ErpPurRequisition__convertToOrder",
                ApiRequest.build(Map.of("requisitionId", requisitionId, "request", request)));
    }

    private ApiResponse<?> orderSubmit(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> orderApprove(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__approve", ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> orderCancel(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__cancel", ApiRequest.build(Map.of("orderId", orderId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listDataOf(ApiResponse<?> resp) {
        Object data = resp.getData();
        assertTrue(data instanceof List, "convertToOrder 返回契约应为 List<ErpPurOrder>");
        return (List<Map<String, Object>>) data;
    }

    private Long firstIdOf(ApiResponse<?> resp) {
        return idOf(listDataOf(resp).get(0));
    }

    private Long idOf(Map<String, Object> orderData) {
        return numberOf(orderData.get("id"));
    }

    private Long numberOf(Object id) {
        return id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
    }

    // ---------- seed helpers ----------

    private ErpPurRequisition newRequisition(String code) {
        ErpPurRequisition req = new ErpPurRequisition();
        req.setCode(code);
        req.setOrgId(ORG_ID);
        req.setRequesterId(REQUESTER_ID);
        req.setBusinessDate(LocalDate.of(2026, 7, 1));
        req.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        return req;
    }

    private ErpPurRequisition newApprovedRequisition(String code, Long supplierId) {
        ErpPurRequisition req = newRequisition(code);
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        return req;
    }

    private void saveRequisitionWithLine(ErpPurRequisition req, int lineNo, Long supplierId, BigDecimal qty) {
        daoProvider.daoFor(ErpPurRequisition.class).saveEntity(req);
        saveRequisitionLine(req, lineNo, supplierId, qty);
    }

    private void saveRequisitionLine(ErpPurRequisition req, int lineNo, Long supplierId, BigDecimal qty) {
        IEntityDao<ErpPurRequisitionLine> dao = daoProvider.daoFor(ErpPurRequisitionLine.class);
        ErpPurRequisitionLine line = new ErpPurRequisitionLine();
        line.setRequisitionId(req.getId());
        line.setLineNo(lineNo);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setSuggestedSupplierId(supplierId);
        dao.saveEntity(line);
    }

    private Map<String, Object> newRequest(String unitPrice, String taxRate) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("warehouseId", WAREHOUSE_ID);
        request.put("currencyId", CURRENCY_ID);
        Map<Integer, String> prices = new LinkedHashMap<>();
        prices.put(1, unitPrice);
        prices.put(2, unitPrice);
        prices.put(3, unitPrice);
        request.put("lineUnitPrices", prices);
        if (taxRate != null) {
            Map<Integer, String> rates = new LinkedHashMap<>();
            rates.put(1, taxRate);
            rates.put(2, taxRate);
            rates.put(3, taxRate);
            request.put("lineTaxRates", rates);
        }
        return request;
    }

    private void seedActiveSupplier(Long id) {
        app.erp.md.dao.entity.ErpMdPartner partner = new app.erp.md.dao.entity.ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        daoProvider.daoFor(app.erp.md.dao.entity.ErpMdPartner.class).saveEntity(partner);
    }

    private List<ErpPurOrderLine> loadOrderLines(Long orderId) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return dao.findAllByQuery(q);
    }
}
