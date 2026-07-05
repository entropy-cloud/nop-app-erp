package app.erp.sal.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.notIn;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：销售退货 SALES_RETURN 过账端到端——反向 SALES_OUTPUT 凭证（借库存商品/贷主营业务成本）
 * + DIRECTION_RECEIVABLE 负 openAmount 辅助账（credit memo）+ 应收余额回减（经 {@code sumOpen} 语义）
 * + 反审核红字冲销（辅助账 CANCELLED + 余额恢复）。
 *
 * <p>余额证明：客户应收 = Σ RECEIVABLE 方向辅助账 openAmountFunctional（排除 SETTLED/CANCELLED），
 * 即 {@code PartnerBalanceUpdater.sumOpen} 的同口径计算。退货负项使该和自然减计退货 totalAmountWithTax。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReturnPosting extends JunitAutoTestCase {

    static final Long ORG_ID = 3804L;
    static final Long CUSTOMER_ID = 4804L;
    static final Long WAREHOUSE_ID = 5804L;
    static final Long MATERIAL_ID = 6804L;
    static final Long UOM_ID = 7804L;
    static final Long CURRENCY_ID = 8804L;
    static final Long ACCT_SCHEMA_ID = 9804L;
    static final BigDecimal RETURN_COST = new BigDecimal("20");        // 4 × 5（凭证 TOTAL_COST）
    static final BigDecimal RETURN_WITH_TAX = new BigDecimal("24");    // 退货含税售价（辅助账口径）

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(730000L);

    @Test
    public void testApproveGeneratesSalesReturnVoucherAndNegativeArItem() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-POST-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-POST-001", returnId, deliveryCtx[0]);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });

        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");
        ErpSalReturn approved = reload(returnId);
        assertTrue(Boolean.TRUE.equals(approved.getPosted()), "审核过账 posted=true");

        // SALES_RETURN 凭证（billHeadCode=return.code，businessType=150）
        ErpFinVoucherBillR link = findBillLink("RT-POST-001", ErpFinBusinessType.SALES_RETURN.name());
        assertNotNull(link, "应生成 SALES_RETURN 业财回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证 docStatus=已过账");
        // 借 1401 库存商品 20 / 贷 6401 主营业务成本 20
        assertEquals(0, voucher.getTotalDebit().compareTo(RETURN_COST), "借方合计=20");
        assertEquals(0, voucher.getTotalCredit().compareTo(RETURN_COST), "贷方合计=20");
        assertEquals(2, countVoucherLines(voucher.getId()), "SALES_RETURN 凭证 2 行");

        // DIRECTION_RECEIVABLE 负 openAmount 辅助账（credit memo）
        ErpFinArApItem item = findArItem("RT-POST-001");
        assertNotNull(item, "应生成退货辅助账项");
        assertEquals(ErpFinConstants.DIRECTION_RECEIVABLE, item.getDirection(), "方向=应收");
        assertEquals(ErpFinConstants.SOURCE_BILL_SAL_RETURN, item.getSourceBillType(), "sourceBillType=SAL_RETURN");
        assertEquals(0, item.getOpenAmountFunctional().compareTo(RETURN_WITH_TAX.negate()),
                "openAmountFunctional = 负 totalAmountWithTax（credit memo）");

        // 客户应收余额（sumOpen 口径）= 负 totalAmountWithTax（退货回减）
        assertEquals(0, sumReceivableOpen().compareTo(RETURN_WITH_TAX.negate()),
                "应收余额 sumOpen = -24（退货回减 totalAmountWithTax）");
    }

    @Test
    public void testReverseApproveCancelsArItemAndRestoresBalance() {
        seedPeriodAndSubjects();
        Long[] deliveryCtx = seedApprovedDelivery("SD-REV-001", new BigDecimal("10"));
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-REV-001", returnId, deliveryCtx[0]);
            newReturnLine(nextId(), returnId, deliveryCtx[1], new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus());
        assertTrue(Boolean.TRUE.equals(reload(returnId).getPosted()), "先过账 posted=true");
        assertEquals(0, sumReceivableOpen().compareTo(RETURN_WITH_TAX.negate()), "退货后应收=-24");

        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "反审核应成功（先红字冲销）");
        ErpSalReturn reversed = reload(returnId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus());
        assertFalse(Boolean.TRUE.equals(reversed.getPosted()), "反审核后 posted 反转为 false");

        // 退货辅助账项被取消（status=CANCELLED、openAmount=0）
        ErpFinArApItem item = findArItem("RT-REV-001");
        assertEquals(ErpFinConstants.AR_AP_STATUS_CANCELLED, item.getStatus(), "辅助账 CANCELLED");
        assertEquals(0, item.getOpenAmountFunctional().compareTo(BigDecimal.ZERO), "openAmount=0");

        // 应收余额恢复为 0（负项被取消）
        assertEquals(0, sumReceivableOpen().compareTo(BigDecimal.ZERO), "反审核后应收余额恢复=0");
    }

    // ---------- seed ----------

    private Long[] seedApprovedDelivery(String deliveryCode, BigDecimal deliveryQty) {
        Long orderId = nextId();
        Long deliveryId = nextId();
        Long orderLineId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            seedActiveCustomer();
            newOrderWithId("SO-" + deliveryCode, orderId);
            newOrderLine(orderId, orderLineId, 1, deliveryQty);
            newDeliveryApproved(deliveryCode, deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, deliveryQty);
            return null;
        });
        return new Long[]{deliveryId, deliveryLineId};
    }

    private ErpSalReturn reload(Long returnId) {
        return daoProvider.daoFor(ErpSalReturn.class).getEntityById(returnId);
    }

    // ---------- sumOpen 口径（同 PartnerBalanceUpdater.sumOpen）----------

    private BigDecimal sumReceivableOpen() {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("partnerId", CUSTOMER_ID));
        q.addFilter(eq("direction", ErpFinConstants.DIRECTION_RECEIVABLE));
        q.addFilter(notIn("status", List.of(ErpFinConstants.AR_AP_STATUS_SETTLED,
                ErpFinConstants.AR_AP_STATUS_CANCELLED)));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem it : dao.findAllByQuery(q)) {
            if (it.getOpenAmountFunctional() != null) {
                sum = sum.add(it.getOpenAmountFunctional());
            }
        }
        return sum;
    }

    // ---------- rpc ----------

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApproveReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- queries ----------

    private ErpFinVoucherBillR findBillLink(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private long countVoucherLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }

    private ErpFinArApItem findArItem(String returnCode) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", ErpFinConstants.SOURCE_BILL_SAL_RETURN),
                eq("sourceBillCode", returnCode)));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    // ---------- seed helpers ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1401", "库存商品");
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
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(schema);
    }

    private void seedActiveCustomer() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(CUSTOMER_ID);
        partner.setCode("CUS-" + CUSTOMER_ID);
        partner.setName("客户" + CUSTOMER_ID);
        partner.setPartnerType("SUPPLIER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
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
        subject.setSubjectClass("ASSET");
        subject.setDirection("DEBIT");
        subject.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(subject);
    }

    private void newOrderWithId(String code, Long orderId) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder order = new ErpSalOrder();
        order.setId(orderId);
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
    }

    private void newOrderLine(Long orderId, Long lineId, int lineNo, BigDecimal qty) {
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

    private void newDeliveryApproved(String code, Long deliveryId, Long orderId) {
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
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        delivery.setPosted(false);
        dao.saveEntity(delivery);
    }

    private void newDeliveryLine(Long lineId, Long deliveryId, Long orderLineId, BigDecimal qty) {
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

    private void newReturn(String code, Long returnId, Long deliveryId) {
        IEntityDao<ErpSalReturn> dao = daoProvider.daoFor(ErpSalReturn.class);
        ErpSalReturn returnOrder = new ErpSalReturn();
        returnOrder.setId(returnId);
        returnOrder.setCode(code);
        returnOrder.setOrgId(ORG_ID);
        returnOrder.setDeliveryId(deliveryId);
        returnOrder.setCustomerId(CUSTOMER_ID);
        returnOrder.setWarehouseId(WAREHOUSE_ID);
        returnOrder.setBusinessDate(LocalDate.of(2026, 7, 2));
        returnOrder.setCurrencyId(CURRENCY_ID);
        returnOrder.setExchangeRate(new BigDecimal("1"));
        returnOrder.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        returnOrder.setTotalAmount(RETURN_COST);
        returnOrder.setTotalTaxAmount(RETURN_WITH_TAX.subtract(RETURN_COST));
        returnOrder.setTotalAmountWithTax(RETURN_WITH_TAX);
        returnOrder.setPosted(false);
        dao.saveEntity(returnOrder);
    }

    private void newReturnLine(Long lineId, Long returnId, Long deliveryLineId, BigDecimal qty, BigDecimal unitPrice) {
        IEntityDao<ErpSalReturnLine> dao = daoProvider.daoFor(ErpSalReturnLine.class);
        ErpSalReturnLine line = new ErpSalReturnLine();
        line.setId(lineId);
        line.setReturnId(returnId);
        line.setLineNo(1);
        line.setDeliveryLineId(deliveryLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        line.setAmount(qty.multiply(unitPrice));
        line.setReason("质量不合格");
        dao.saveEntity(line);
    }

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
