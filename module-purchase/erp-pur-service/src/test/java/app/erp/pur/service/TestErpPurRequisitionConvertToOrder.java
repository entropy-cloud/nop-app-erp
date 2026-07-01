package app.erp.pur.service;

import app.erp.pur.biz.ConvertToOrderRequest;
import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.biz.IErpPurRequisitionBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Phase 2 服务层集成测试：请购→订单转化 + 幂等 + 回链 + 缺失字段补全。
 *
 * <p>覆盖：(a) APPROVED 请购 + 补充字段 → 订单(UNSUBMITTED) + 行/字段/requisitionId 回链/金额族正确；
 * (b) 非 APPROVED 请购转化拒绝； (c) 行供应商不一致/缺失拒绝； (d) 已转化幂等拒绝（作废后可重新转化）；
 * (e) 转化产物订单可被 Phase 1 审核状态机推进到 APPROVED（两阶段衔接）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurRequisitionConvertToOrder extends JunitAutoTestCase {

    static final Long ORG_ID = 1401L;
    static final Long REQUESTER_ID = 2401L;
    static final Long SUPPLIER_ID = 2411L;
    static final Long WAREHOUSE_ID = 3401L;
    static final Long MATERIAL_ID = 4401L;
    static final Long UOM_ID = 5401L;
    static final Long CURRENCY_ID = 6401L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPurRequisitionBiz reqBiz;
    @Inject
    IErpPurOrderBiz orderBiz;

    @Test
    public void testConvertApprovedReqToOrder() {
        ErpPurRequisition req = newApprovedRequisition("PR-CONV-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10")));
        ormTemplate.runInSession(() -> saveRequisitionLine(req, 2, SUPPLIER_ID, new BigDecimal("5")));

        ConvertToOrderRequest request = newRequest("5", "13");

        ErpPurOrder order = reqBiz.convertToOrder(req.getId(), request);
        assertNotNull(order.getId(), "订单已持久化");
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
        assertEquals(0, new BigDecimal("5").compareTo(new BigDecimal(l1.getUnitPrice())), "行1 unitPrice=调用方提供");
        // amount = 5 × 10 = 50；taxRate=13 → taxAmount=6.50；amountWithTax=56.50（VARCHAR 存储）
        assertEquals(0, new BigDecimal("50").compareTo(new BigDecimal(l1.getAmount())), "行1 amount=50");
        assertEquals(0, new BigDecimal("13").compareTo(new BigDecimal(l1.getTaxRate())), "行1 taxRate=调用方提供");
        assertEquals(0, new BigDecimal("6.50").compareTo(new BigDecimal(l1.getTaxAmount())), "行1 taxAmount=6.50");
        assertEquals(0, new BigDecimal("56.50").compareTo(new BigDecimal(l1.getAmountWithTax())),
                "行1 amountWithTax=56.50");
    }

    @Test
    public void testConvertNotApprovedRejected() {
        ErpPurRequisition req = newRequisition("PR-NOTAPP-001");
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10")));

        ConvertToOrderRequest request = newRequest("5", null);
        assertThrows(NopException.class, () -> reqBiz.convertToOrder(req.getId(), request),
                "非 APPROVED 请购转化应抛 ERR_REQ_NOT_APPROVED");
    }

    @Test
    public void testConvertMixedSupplierRejected() {
        Long anotherSupplier = 2412L;
        ErpPurRequisition req = newApprovedRequisition("PR-MIX-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> {
            saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10"));
            saveRequisitionLine(req, 2, anotherSupplier, new BigDecimal("5"));
        });

        ConvertToOrderRequest request = newRequest("5", null);
        assertThrows(NopException.class, () -> reqBiz.convertToOrder(req.getId(), request),
                "行供应商不一致应抛 ERR_REQ_MIXED_OR_MISSING_SUPPLIER");
    }

    @Test
    public void testConvertMissingSupplierRejected() {
        ErpPurRequisition req = newApprovedRequisition("PR-MISS-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> {
            saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10"));
            // 第二行无 suggestedSupplierId
            saveRequisitionLine(req, 2, null, new BigDecimal("5"));
        });

        ConvertToOrderRequest request = newRequest("5", null);
        assertThrows(NopException.class, () -> reqBiz.convertToOrder(req.getId(), request),
                "行供应商缺失应抛 ERR_REQ_MIXED_OR_MISSING_SUPPLIER");
    }

    @Test
    public void testConvertIdempotentRejected() {
        ErpPurRequisition req = newApprovedRequisition("PR-IDEM-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10")));
        ConvertToOrderRequest request = newRequest("5", null);

        ErpPurOrder first = reqBiz.convertToOrder(req.getId(), request);
        assertNotNull(first);

        // 重复转化 → 拒绝（已存在未作废订单）
        assertThrows(NopException.class, () -> reqBiz.convertToOrder(req.getId(), request),
                "已转化请购重复转化应抛 ERR_REQ_ALREADY_CONVERTED");

        // 作废原订单后可重新转化
        orderBiz.cancel(first.getId());
        ErpPurOrder second = reqBiz.convertToOrder(req.getId(), request);
        assertNotNull(second);
        assertEquals(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED, second.getApproveStatus());
    }

    @Test
    public void testConvertedOrderThenApprove() {
        // 证明两阶段衔接：转化产物 UNSUBMITTED → submit → approve → APPROVED
        ErpPurRequisition req = newApprovedRequisition("PR-LINK-001", SUPPLIER_ID);
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req, 1, SUPPLIER_ID, new BigDecimal("10")));
        ormTemplate.runInSession(() -> seedActiveSupplier(SUPPLIER_ID));

        ConvertToOrderRequest request = newRequest("5", null);
        ErpPurOrder order = reqBiz.convertToOrder(req.getId(), request);

        ErpPurOrder submitted = orderBiz.submit(order.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus());

        ErpPurOrder approved = orderBiz.approve(order.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "转化产物订单可走 Phase 1 状态机推进到 APPROVED");
    }

    // ---------- helpers ----------

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

    private ConvertToOrderRequest newRequest(String unitPrice, String taxRate) {
        ConvertToOrderRequest request = new ConvertToOrderRequest();
        request.setWarehouseId(WAREHOUSE_ID);
        request.setCurrencyId(CURRENCY_ID);
        Map<Integer, String> prices = new HashMap<>();
        prices.put(1, unitPrice);
        prices.put(2, unitPrice);
        request.setLineUnitPrices(prices);
        if (taxRate != null) {
            Map<Integer, String> rates = new HashMap<>();
            rates.put(1, taxRate);
            rates.put(2, taxRate);
            request.setLineTaxRates(rates);
        }
        return request;
    }

    private void seedActiveSupplier(Long id) {
        app.erp.md.dao.entity.ErpMdPartner partner = new app.erp.md.dao.entity.ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType(10);
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        daoProvider.daoFor(app.erp.md.dao.entity.ErpMdPartner.class).saveEntity(partner);
    }

    private List<ErpPurOrderLine> loadOrderLines(Long orderId) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(eq("orderId", orderId));
        return dao.findAllByQuery(q);
    }
}
