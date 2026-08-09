package app.erp.sal.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import io.nop.api.core.annotations.autotest.EnableSnapshot;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 服务层集成测试矩阵（RC-R1.18 + RC-R1.19）：
 * <ul>
 *   <li>P1-RC-026 退货成本策略 config 化（original 默认回归 / current=库存 avgCost / agreement=协议价 / 非法值回退）；</li>
 *   <li>P1-RC-027 已核销发票 pre-approve 守卫（SETTLED 拒绝 / PARTIAL+OPEN 放行 / 无关联发票跳过）；</li>
 *   <li>P1-RC-028 期间 CLOSED pre-approve 守卫（CLOSED/CLOSING/无期间 拒绝 / OPEN 放行）。</li>
 * </ul>
 *
 * <p>验证点：守卫先于 doApprove 触发（拒绝时 approveStatus 保持 SUBMITTED）+ computeTotalCost 与 buildLines
 * 同源消费策略（current 下 GL 凭证 totalDebit = Σ qty×avgCost）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReturnCostAndGuards extends JunitAutoTestCase {

    static final Long ORG_ID = 3718L;
    static final Long CUSTOMER_ID = 4718L;
    static final Long WAREHOUSE_ID = 5718L;
    static final Long MATERIAL_ID = 6718L;
    static final Long UOM_ID = 7718L;
    static final Long CURRENCY_ID = 8718L;
    static final Long ACCT_SCHEMA_ID = 9718L;

    static final BigDecimal LINE_QTY = new BigDecimal("4");
    static final BigDecimal LINE_UNIT_PRICE = new BigDecimal("5");
    static final BigDecimal SEEDED_AVG_COST = new BigDecimal("8");

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(718000L);

    @AfterEach
    void resetCostConfig() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpSalConstants.CONFIG_RETURN_COST_METHOD, ErpSalConstants.RETURN_COST_METHOD_ORIGINAL);
    }

    // ==================== P1-RC-026 退货成本策略 ====================

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testDefaultOriginalRegression() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDelivery("SD-COST-ORIG");
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-COST-ORIG", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "original 默认审核成功");
        assertEquals(0, new BigDecimal("20").compareTo(voucherTotalDebit("RT-COST-ORIG")),
                "original 策略 TOTAL_COST = 4×5 = 20");
    }

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testCurrentStrategyUsesAvgCost() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpSalConstants.CONFIG_RETURN_COST_METHOD, ErpSalConstants.RETURN_COST_METHOD_CURRENT);
        seedPeriodAndSubjects();
        seedStockBalance(MATERIAL_ID, WAREHOUSE_ID, SEEDED_AVG_COST, new BigDecimal("100"));
        Long[] ctx = seedApprovedDelivery("SD-COST-CUR");
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-COST-CUR", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "current 策略审核成功");
        // current 下 buildLines unitCost=avgCost=8 + computeTotalCost 同源 → GL 凭证 totalDebit = 4×8 = 32
        assertEquals(0, new BigDecimal("32").compareTo(voucherTotalDebit("RT-COST-CUR")),
                "current 策略 TOTAL_COST = 4×avgCost(8) = 32（buildLines 与 computeTotalCost 同源）");
    }

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testAgreementStrategyUsesUnitPrice() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpSalConstants.CONFIG_RETURN_COST_METHOD, ErpSalConstants.RETURN_COST_METHOD_AGREEMENT);
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDelivery("SD-COST-AGR");
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-COST-AGR", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "agreement 策略审核成功");
        assertEquals(0, new BigDecimal("20").compareTo(voucherTotalDebit("RT-COST-AGR")),
                "agreement 策略 TOTAL_COST = 4×协议价(5) = 20");
    }

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testIllegalConfigFallsBackToOriginal() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpSalConstants.CONFIG_RETURN_COST_METHOD, "bogus-value");
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDelivery("SD-COST-BAD");
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-COST-BAD", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "非法 config 回退 original 审核成功");
        assertEquals(0, new BigDecimal("20").compareTo(voucherTotalDebit("RT-COST-BAD")),
                "非法 config 回退 original → TOTAL_COST = 20");
    }

    // ==================== P1-RC-027 已核销发票守卫 ====================

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testSettledInvoiceRejected() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDelivery("SD-INV-SET");
        Long invoiceId = nextId();
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newSettledInvoice("SI-INV-SET", invoiceId, ErpSalConstants.RECEIVED_STATUS_RECEIVED);
            newInvoiceLine(nextId(), invoiceId, ctx[1]);
            newReturn("RT-INV-SET", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        ApiResponse<?> resp = approveReturn(returnId);
        assertNotEquals(0, resp.getStatus(), "已核销发票退货审核应被拒");
        assertEquals(ErpSalErrors.ERR_RETURN_INVOICE_SETTLED.getErrorCode(), resp.getCode(),
                "拒绝错误码 = ERR_RETURN_INVOICE_SETTLED");
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reload(returnId).getApproveStatus(),
                "拒绝时 approveStatus 保持 SUBMITTED");
    }

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testPartialInvoicePassed() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDelivery("SD-INV-PAR");
        Long invoiceId = nextId();
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newSettledInvoice("SI-INV-PAR", invoiceId, ErpSalConstants.RECEIVED_STATUS_PARTIAL);
            newInvoiceLine(nextId(), invoiceId, ctx[1]);
            newReturn("RT-INV-PAR", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "部分核销（PARTIAL）发票退货审核应放行");
    }

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testOpenInvoicePassed() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDelivery("SD-INV-OPEN");
        Long invoiceId = nextId();
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newSettledInvoice("SI-INV-OPEN", invoiceId, ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
            newInvoiceLine(nextId(), invoiceId, ctx[1]);
            newReturn("RT-INV-OPEN", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "未核销（OPEN）发票退货审核应放行");
    }

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testNoLinkedInvoicePassed() {
        seedPeriodAndSubjects();
        Long[] ctx = seedApprovedDelivery("SD-INV-NONE");
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-INV-NONE", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "无关联发票退货审核应放行（跳过守卫）");
    }

    // ==================== P1-RC-028 期间 CLOSED 守卫 ====================

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testClosedPeriodRejected() {
        seedPeriodAndSubjects("CLOSED");
        Long[] ctx = seedApprovedDelivery("SD-PD-CLS");
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-PD-CLS", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        ApiResponse<?> resp = approveReturn(returnId);
        assertNotEquals(0, resp.getStatus(), "CLOSED 期间退货审核应被拒");
        assertEquals(ErpSalErrors.ERR_RETURN_PERIOD_CLOSED.getErrorCode(), resp.getCode(),
                "拒绝错误码 = ERR_RETURN_PERIOD_CLOSED");
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reload(returnId).getApproveStatus(),
                "拒绝时 approveStatus 保持 SUBMITTED");
    }

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testClosingPeriodRejected() {
        seedPeriodAndSubjects(ErpSalConstants.PERIOD_STATUS_CLOSING);
        Long[] ctx = seedApprovedDelivery("SD-PD-CSG");
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-PD-CSG", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        ApiResponse<?> resp = approveReturn(returnId);
        assertNotEquals(0, resp.getStatus(), "CLOSING 期间退货审核应被拒（非 OPEN）");
        assertEquals(ErpSalErrors.ERR_RETURN_PERIOD_CLOSED.getErrorCode(), resp.getCode());
    }

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testOpenPeriodPassed() {
        seedPeriodAndSubjects(ErpSalConstants.PERIOD_STATUS_OPEN);
        Long[] ctx = seedApprovedDelivery("SD-PD-OPN");
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-PD-OPN", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        assertEquals(0, approveReturn(returnId).getStatus(), "OPEN 期间退货审核应放行");
    }

    @Test
    @EnableSnapshot(checkOutput = false, tableInit = false, sqlInput = false)
    public void testNoPeriodRejected() {
        // 不 seed 期间 → 退货 businessDate(2026-07-02) 无对应期间 → 拒绝（对齐 finance resolveOpenPeriod 严格语义）
        seedSubjectsAndAcctSchema();
        seedActiveCustomer();
        Long[] ctx = seedApprovedDelivery("SD-PD-NONE");
        Long returnId = nextId();
        ormTemplate.runInSession(session -> {
            newReturn("RT-PD-NONE", returnId, ctx[0]);
            newReturnLine(nextId(), returnId, ctx[1], LINE_QTY, LINE_UNIT_PRICE);
            return null;
        });
        ApiResponse<?> resp = approveReturn(returnId);
        assertNotEquals(0, resp.getStatus(), "无对应期间退货审核应被拒");
        assertEquals(ErpSalErrors.ERR_RETURN_PERIOD_CLOSED.getErrorCode(), resp.getCode(),
                "无期间拒绝错误码 = ERR_RETURN_PERIOD_CLOSED");
    }

    // ==================== helpers: 断言 ====================

    private BigDecimal voucherTotalDebit(String returnCode) {
        ErpFinVoucherBillR link = findBillLink(returnCode, ErpFinBusinessType.SALES_RETURN.name());
        assertTrue(link != null, "应生成 SALES_RETURN 业财回链：" + returnCode);
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        return voucher.getTotalDebit();
    }

    private ErpSalReturn reload(Long returnId) {
        return daoProvider.daoFor(ErpSalReturn.class).getEntityById(returnId);
    }

    // ==================== helpers: rpc ====================

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpSalReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ==================== helpers: query ====================

    private ErpFinVoucherBillR findBillLink(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    // ==================== helpers: seed ====================

    private void seedPeriodAndSubjects() {
        seedPeriodAndSubjects(ErpSalConstants.PERIOD_STATUS_OPEN);
    }

    private void seedPeriodAndSubjects(String periodStatus) {
        ormTemplate.runInSession(session -> {
            seedSubjectsAndAcctSchema();
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), periodStatus);
            seedActiveCustomer();
            return null;
        });
    }

    private void seedSubjectsAndAcctSchema() {
        ormTemplate.runInSession(session -> {
            seedSubject("1401", "库存商品");
            seedSubject("6401", "主营业务成本");
            seedAcctSchema();
            return null;
        });
    }

    private Long[] seedApprovedDelivery(String tag) {
        Long orderId = nextId();
        Long deliveryId = nextId();
        Long orderLineId = nextId();
        Long deliveryLineId = nextId();
        ormTemplate.runInSession(session -> {
            newOrderWithId("SO-" + tag, orderId);
            newOrderLine(orderId, orderLineId, 1, new BigDecimal("10"));
            newDeliveryApproved("SD-" + tag, deliveryId, orderId);
            newDeliveryLine(deliveryLineId, deliveryId, orderLineId, new BigDecimal("10"));
            return null;
        });
        return new Long[]{deliveryId, deliveryLineId};
    }

    private void seedStockBalance(Long materialId, Long warehouseId, BigDecimal avgCost, BigDecimal qty) {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance balance = new ErpInvStockBalance();
            balance.setId(nextId());
            balance.setOrgId(ORG_ID);
            balance.setMaterialId(materialId);
            balance.setWarehouseId(warehouseId);
            balance.setTotalQuantity(qty);
            balance.setAvailableQuantity(qty);
            balance.setAvgCost(avgCost);
            balance.setTotalCost(qty.multiply(avgCost));
            balance.setCostMethod("AVERAGE");
            balance.setCurrencyId(CURRENCY_ID);
            dao.saveEntity(balance);
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
        partner.setPartnerType("CUSTOMER");
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
        line.setUnitPrice(LINE_UNIT_PRICE);
        line.setAmount(qty.multiply(LINE_UNIT_PRICE));
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
        delivery.setExchangeRate(BigDecimal.ONE);
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        delivery.setPosted(true);
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
        line.setUnitPrice(LINE_UNIT_PRICE);
        dao.saveEntity(line);
    }

    private void newSettledInvoice(String code, Long invoiceId, String receivedStatus) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        ErpSalInvoice invoice = new ErpSalInvoice();
        invoice.setId(invoiceId);
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setCustomerId(CUSTOMER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        invoice.setReceivedStatus(receivedStatus);
        invoice.setReceivedAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("100"));
        invoice.setTotalTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmountWithTax(new BigDecimal("100"));
        invoice.setPosted(false);
        dao.saveEntity(invoice);
    }

    private void newInvoiceLine(Long lineId, Long invoiceId, Long deliveryLineId) {
        IEntityDao<ErpSalInvoiceLine> dao = daoProvider.daoFor(ErpSalInvoiceLine.class);
        ErpSalInvoiceLine line = new ErpSalInvoiceLine();
        line.setId(lineId);
        line.setInvoiceId(invoiceId);
        line.setLineNo(1);
        line.setDeliveryLineId(deliveryLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(LINE_UNIT_PRICE);
        line.setAmount(new BigDecimal("50"));
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
        returnOrder.setExchangeRate(BigDecimal.ONE);
        returnOrder.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        returnOrder.setTotalAmount(new BigDecimal("20"));
        returnOrder.setTotalAmountWithTax(new BigDecimal("24"));
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
