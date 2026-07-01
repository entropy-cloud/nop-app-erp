package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 3 端到端集成测试：sales→inventory→finance 三域经 {@code generateMove}(OUTGOING) 打通，含销售独有可用量门控。
 *
 * <p>完整链路：建订单+出库单(UNSUBMITTED)→提交→审核→出库移动单 DONE+库存余额扣减+存货凭证(SALES_OUTPUT:借主营业务成本/贷存货)
 * +{@code posted=true}+订单 {@code deliveryStatus} 回写→反审核（内部冲销）→余额恢复。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalOrderToDeliveryEnd extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();


    static final Long ORG_ID = 1301L;
    static final Long CUSTOMER_ID = 2301L;
    static final Long WAREHOUSE_ID = 3301L;
    static final Long MATERIAL_ID = 4301L;
    static final Long UOM_ID = 5301L;
    static final Long CURRENCY_ID = 6301L;
    static final Long ACCT_SCHEMA_ID = 7301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpSalDeliveryBiz deliveryBiz;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Test
    public void testOrderToDeliveryToEnd() {
        seedPrereqs();
        long orderLine = 8301L;
        long deliveryId = 8302L;
        long deliveryLineId = 8303L;
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            // 预置库存 20 @ 5 = 100（出库类须校验可用量，且 SALES_OUTPUT 成本取自 avgCost 快照）
            seedStock("SEED-E2E-001", new BigDecimal("20"), new BigDecimal("5"));
            Long orderId = newOrder("SO-E2E-001");
            newOrderLine(orderId, orderLine, 1, new BigDecimal("10"));
            newDelivery("SD-E2E-001", deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLine, new BigDecimal("10"));
            return null;
        });

        // 1. UNSUBMITTED → 提交
        ErpSalDelivery submitted = deliveryBiz.submit(deliveryId, CTX);
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus());

        // 2. 审核 → 出库移动单 DONE + 余额扣减 + 存货凭证 + posted + 发货状态回写
        ErpSalDelivery approved = deliveryBiz.approve(deliveryId, CTX);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());
        assertEquals(true, approved.getPosted(), "出库审核 posted=true");

        ErpInvStockMove move = findMove("SD-E2E-001");
        assertNotNull(move);
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus());
        assertEquals(ErpInvConstants.MOVE_TYPE_OUTGOING, move.getMoveType(), "出库类型");
        assertEquals(0, findBalance().getTotalQuantity().compareTo(new BigDecimal("10")),
                "库存余额 20(预置) - 10(出库) = 10");

        ErpFinVoucherBillR link = findVoucherLink(move.getCode());
        assertNotNull(link, "存货估值凭证(SALES_OUTPUT)落地");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        // SALES_OUTPUT：借 6401 主营业务成本 10×5=50 / 贷 1401 库存商品 50
        assertEquals(0, voucher.getTotalDebit().compareTo(new BigDecimal("50")),
                "借主营业务成本 50");
        assertEquals(0, voucher.getTotalCredit().compareTo(new BigDecimal("50")),
                "贷库存商品 50");

        ErpSalOrder order = findOrder("SO-E2E-001");
        assertEquals(ErpSalConstants.DELIVERY_STATUS_DELIVERED, order.getDeliveryStatus(),
                "订单发货状态回写 DELIVERED（单行全发清）");

        // 3. 反审核 → 内部冲销，余额恢复为预置 20，APPROVED→REJECTED
        ErpSalDelivery reversed = deliveryBiz.reverseApprove(deliveryId, CTX);
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus());
        assertEquals(0, findBalance().getTotalQuantity().compareTo(new BigDecimal("20")),
                "冲销后库存余额恢复为预置 20");
        ErpInvStockMove reversal = findReversal(move.getCode());
        assertNotNull(reversal, "应生成反向冲销移动单");
        assertEquals(ErpInvConstants.MOVE_TYPE_INCOMING, reversal.getMoveType(), "出库的反向=入库");
    }

    // ---------- seed helpers ----------

    private void seedPrereqs() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 10);
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
            seedSubject("6401", "主营业务成本");
            seedAcctSchema();
            return null;
        });
    }

    private void seedAcctSchema() {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setId(ACCT_SCHEMA_ID);
        schema.setCode("AS-" + ORG_ID);
        schema.setName("账套" + ORG_ID);
        schema.setOrgId(ORG_ID);
        schema.setNature(10);
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus(10);
        dao.saveEntity(schema);
    }

    private void seedActiveCustomer() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(CUSTOMER_ID);
        partner.setCode("CUS-" + CUSTOMER_ID);
        partner.setName("客户" + CUSTOMER_ID);
        partner.setPartnerType(10);
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, int status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(10);
        subject.setDirection(10);
        subject.setStatus(10);
        dao.saveEntity(subject);
    }

    private ErpInvStockMove seedStock(String billCode, BigDecimal qty, BigDecimal unitCost) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpInvConstants.MOVE_TYPE_INCOMING);
        request.setOrgId(ORG_ID);
        request.setBusinessDate(LocalDate.of(2026, 7, 1));
        request.setDestWarehouseId(WAREHOUSE_ID);
        request.setAcctSchemaId(ACCT_SCHEMA_ID);
        request.setCurrencyId(CURRENCY_ID);
        request.setRelatedBillType("SEED_STOCK");
        request.setRelatedBillCode(billCode);
        StockMoveLineRequest line = new StockMoveLineRequest();
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitCost(unitCost);
        line.setCurrencyId(CURRENCY_ID);
        request.setLines(Collections.singletonList(line));
        return stockMoveBiz.generateMove(request, CTX);
    }

    private Long newOrder(String code) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        dao.saveEntity(order);
        return order.getId();
    }

    private void newOrderLine(Long orderId, long lineId, int lineNo, BigDecimal qty) {
        IEntityDao<ErpSalOrderLine> dao = daoProvider.daoFor(ErpSalOrderLine.class);
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setId(lineId);
        line.setOrderId(orderId);
        line.setLineNo(lineNo);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("5"));
        line.setAmount(qty.multiply(new BigDecimal("5")));
        dao.saveEntity(line);
    }

    private void newDelivery(String code, long deliveryId, Long orderId) {
        IEntityDao<ErpSalDelivery> dao = daoProvider.daoFor(ErpSalDelivery.class);
        ErpSalDelivery delivery = new ErpSalDelivery();
        delivery.setId(deliveryId);
        delivery.setCode(code);
        delivery.setOrgId(ORG_ID);
        delivery.setOrderId(orderId);
        delivery.setCustomerId(CUSTOMER_ID);
        delivery.setWarehouseId(WAREHOUSE_ID);
        delivery.setBusinessDate(LocalDate.of(2026, 7, 1));
        delivery.setCurrencyId(CURRENCY_ID);
        delivery.setExchangeRate(new BigDecimal("1"));
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        delivery.setPosted(false);
        dao.saveEntity(delivery);
    }

    private void newDeliveryLine(long lineId, long deliveryId, long orderLineId, BigDecimal qty) {
        IEntityDao<ErpSalDeliveryLine> dao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        ErpSalDeliveryLine line = new ErpSalDeliveryLine();
        line.setId(lineId);
        line.setDeliveryId(deliveryId);
        line.setLineNo(1);
        line.setOrderLineId(orderLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("5"));
        dao.saveEntity(line);
    }

    // ---------- query helpers ----------

    private ErpSalOrder findOrder(String code) {
        return daoProvider.daoFor(ErpSalOrder.class).findAllByQuery(new QueryBean()).stream()
                .filter(o -> code.equals(o.getCode())).findFirst().orElse(null);
    }

    private ErpInvStockMove findMove(String deliveryCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY));
        q.addFilter(eq("relatedBillCode", deliveryCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpInvStockMove findReversal(String originalMoveCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpSalConstants.RELATED_BILL_TYPE_REVERSAL));
        q.addFilter(eq("relatedBillCode", originalMoveCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpFinVoucherBillR findVoucherLink(String moveCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean()).stream()
                .filter(l -> moveCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
