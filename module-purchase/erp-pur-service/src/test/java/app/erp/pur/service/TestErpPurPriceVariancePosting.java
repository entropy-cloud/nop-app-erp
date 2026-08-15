package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.50 / P1-RC-018 价格差异过账（plan 2026-08-16-0424-1）。
 *
 * <p>验证 UC-PUR-05「让步接收时存在过账行: 科目 == 价格差异科目 且 金额 == 差异 * 数量」落地：
 * <ul>
 *   <li>策略「接收并过账差异」（erp-pur.price-diff-strategy=POST_DIFFERENCE）下 AP_INVOICE 增 PPV 行
 *       （1404 材料成本差异，金额=|差异| 量值，涨价借/降价贷）+ 1403 在途物资按差异拆分
 *       （1403 = TOTAL_AMOUNT − 差异，借贷恒等 Dr Σ = Cr Σ 保持）。</li>
 *   <li>容差内零差异 / 无 receiveLineId 行跳过 / 订单价 0 跳过 → 差异 0 → 既有三行零变化。</li>
 *   <li>默认配置（拒绝族）差异键不写 → 既有三行零 PPV 回归；strict 抛 ERR_INVOICE_PRICE_MISMATCH 不变；
 *       POST_DIFFERENCE 覆盖 strict 放行（策略路径）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurPriceVariancePosting extends JunitAutoTestCase {

    static final Long ORG_ID = 1303L;
    static final Long SUPPLIER_ID = 2301L;
    static final Long MATERIAL_ID = 4301L;
    static final Long UOM_ID = 5301L;
    static final Long CURRENCY_ID = 6301L;
    static final Long ACCT_SCHEMA_ID = 7303L;
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- 策略「接收并过账差异」：涨价 → PPV 借 1404 + 1403 拆分 ----------

    @Test
    public void testPostDifferencePriceUpSplitsPpv() {
        // 订单价 5 / 发票价 10（差异 100% > 5% 容差），数量 10：差异 = (10−5)×10 = 50
        // TOTAL_AMOUNT=100 → 1403 = 100−50 = 50 借 + 1404 = 50 借 + 2221 = 13 借 / 2202 = 113 贷，Dr Σ = Cr Σ = 113
        Chain chain = seedChain("PO-PPV-UP-001", "PR-PPV-UP-001", "PI-PPV-UP-001",
                new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));

        withPriceDiffStrategy("POST_DIFFERENCE", () -> {
            assertEquals(0, approve(chain.invoiceId).getStatus(), "POST_DIFFERENCE 涨价超容差 → 放行 APPROVED");
        });

        ErpPurInvoice reloaded = reload(chain.invoiceId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reloaded.getApproveStatus());
        assertTrue(Boolean.TRUE.equals(reloaded.getPosted()), "过账成功 posted=true");

        List<ErpFinVoucherLine> lines = voucherLines(chain.invoiceCode);
        assertEquals(4, lines.size(), "AP_INVOICE 含 PPV 行 → 4 行（1403/2221/2202/1404）");
        assertAmount("50", debitAmount(findLine(lines, "1403")), "1403 = TOTAL_AMOUNT − 差异 = 50 借");
        assertAmount("50", debitAmount(findLine(lines, "1404")), "PPV = 差异 × 数量 = 50 借（涨价）");
        assertAmount("13", debitAmount(findLine(lines, "2221")), "进项税不变 13 借");
        assertAmount("113", creditAmount(findLine(lines, "2202")), "应付 113 贷");
        assertEquals(0, sumDebit(lines).compareTo(sumCredit(lines)), "借贷恒等 Dr Σ = Cr Σ");
    }

    // ---------- 策略「接收并过账差异」：降价 → PPV 贷 1404 + 1403 拆分 ----------

    @Test
    public void testPostDifferencePriceDownCreditsPpv() {
        // 订单价 10 / 发票价 5（差异 −100% 超容差），数量 10：差异 = (5−10)×10 = −50
        // TOTAL_AMOUNT=50 → 1403 = 50−(−50) = 100 借 + 1404 = 50 贷 + 2221 = 6.5 借 / 2202 = 56.5 贷，Dr Σ = Cr Σ = 106.5
        Chain chain = seedChain("PO-PPV-DN-001", "PR-PPV-DN-001", "PI-PPV-DN-001",
                new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("10"),
                new BigDecimal("50"), new BigDecimal("6.5"), new BigDecimal("56.5"));

        withPriceDiffStrategy("POST_DIFFERENCE", () -> {
            assertEquals(0, approve(chain.invoiceId).getStatus(), "POST_DIFFERENCE 降价超容差 → 放行 APPROVED");
        });

        List<ErpFinVoucherLine> lines = voucherLines(chain.invoiceCode);
        assertEquals(4, lines.size(), "AP_INVOICE 含 PPV 行 → 4 行");
        assertAmount("100", debitAmount(findLine(lines, "1403")), "1403 = TOTAL_AMOUNT − 差异 = 100 借");
        assertAmount("50", creditAmount(findLine(lines, "1404")), "PPV = |差异| = 50 贷（降价）");
        assertEquals(0, sumDebit(lines).compareTo(sumCredit(lines)), "借贷恒等 Dr Σ = Cr Σ");
    }

    // ---------- 容差内零差异：既有三行零变化 ----------

    @Test
    public void testWithinToleranceNoPpv() {
        // 订单价 10 / 发票价 10.2（2% ≤ 5% 容差），数量 10：差异不超容差不计入 → 差异 0
        Chain chain = seedChain("PO-PPV-IN-001", "PR-PPV-IN-001", "PI-PPV-IN-001",
                new BigDecimal("10"), new BigDecimal("10.2"), new BigDecimal("10"),
                new BigDecimal("102"), new BigDecimal("13.26"), new BigDecimal("115.26"));

        withPriceDiffStrategy("POST_DIFFERENCE", () -> {
            assertEquals(0, approve(chain.invoiceId).getStatus(), "容差内 → 放行");
        });

        List<ErpFinVoucherLine> lines = voucherLines(chain.invoiceCode);
        assertEquals(3, lines.size(), "容差内零差异 → 既有三行零变化");
        assertAmount("102", debitAmount(findLine(lines, "1403")), "1403 保持 TOTAL_AMOUNT 不变");
        assertEquals(0, sumDebit(lines).compareTo(sumCredit(lines)), "借贷恒等");
    }

    // ---------- 无 receiveLineId 行跳过：既有三行零变化 ----------

    @Test
    public void testNoReceiveLineIdSkipped() {
        // 发票行无 receiveLineId（无订单/直接凭发票场景）→ 跳过差异计算 → 差异 0
        Chain chain = seedChain("PO-PPV-NRL-001", "PR-PPV-NRL-001", "PI-PPV-NRL-001",
                new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpPurInvoiceLine> lineDao = daoProvider.daoFor(ErpPurInvoiceLine.class);
            ErpPurInvoiceLine line = lineDao.getEntityById(chain.invoiceLineId);
            line.setReceiveLineId(null);
            lineDao.updateEntity(line);
        });

        withPriceDiffStrategy("POST_DIFFERENCE", () -> {
            assertEquals(0, approve(chain.invoiceId).getStatus(), "无回链行跳过 → 放行");
        });

        List<ErpFinVoucherLine> lines = voucherLines(chain.invoiceCode);
        assertEquals(3, lines.size(), "无 receiveLineId 行跳过 → 既有三行零变化");
    }

    // ---------- 订单价 0 跳过：既有三行零变化 ----------

    @Test
    public void testOrderPriceZeroSkipped() {
        // 订单行单价 0（orderPrice.signum() == 0）→ 跳过价格比对 → 差异 0
        Chain chain = seedChain("PO-PPV-ZP-001", "PR-PPV-ZP-001", "PI-PPV-ZP-001",
                new BigDecimal("0"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));

        withPriceDiffStrategy("POST_DIFFERENCE", () -> {
            assertEquals(0, approve(chain.invoiceId).getStatus(), "订单价 0 跳过 → 放行");
        });

        List<ErpFinVoucherLine> lines = voucherLines(chain.invoiceCode);
        assertEquals(3, lines.size(), "订单价 0 行跳过 → 既有三行零变化");
    }

    // ---------- 差异 0（超容差但无回链）：既有三行零变化 ----------

    @Test
    public void testVarianceZeroNoPpv() {
        // POST_DIFFERENCE 下无回链订单 → computeOverTolerancePriceVariance 返回 0 → 差异键不写/0 → 既有三行
        Chain chain = seedChain("PO-PPV-V0-001", "PR-PPV-V0-001", "PI-PPV-V0-001",
                new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpPurReceiveLine> rlDao = daoProvider.daoFor(ErpPurReceiveLine.class);
            ErpPurReceiveLine rl = rlDao.getEntityById(chain.receiveLineId);
            rl.setOrderLineId(null);
            rlDao.updateEntity(rl);
        });

        withPriceDiffStrategy("POST_DIFFERENCE", () -> {
            assertEquals(0, approve(chain.invoiceId).getStatus(), "无订单回链 → 差异 0 → 放行");
        });

        List<ErpFinVoucherLine> lines = voucherLines(chain.invoiceCode);
        assertEquals(3, lines.size(), "差异 0 → 无 PPV 行，1403 不变");
        assertAmount("100", debitAmount(findLine(lines, "1403")), "1403 保持原金额零变化");
    }

    // ---------- 默认配置（拒绝族）：差异键不写 → 既有三行零 PPV 回归 ----------

    @Test
    public void testDefaultRejectFamilyNoPpv() {
        // 默认 config（非 POST_DIFFERENCE = 拒绝族）+ 非 strict：超容差 warn 放行，但不写差异键 → 零 PPV
        Chain chain = seedChain("PO-PPV-DEF-001", "PR-PPV-DEF-001", "PI-PPV-DEF-001",
                new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));

        assertEquals(0, approve(chain.invoiceId).getStatus(), "默认非 strict 超容差 → warn 放行（既有行为）");

        List<ErpFinVoucherLine> lines = voucherLines(chain.invoiceCode);
        assertEquals(3, lines.size(), "默认拒绝族差异键不写 → 既有三行零 PPV");
        assertAmount("100", debitAmount(findLine(lines, "1403")), "1403 = TOTAL_AMOUNT 不拆分");
        assertEquals(0, sumDebit(lines).compareTo(sumCredit(lines)), "借贷恒等");
    }

    // ---------- strict 抛错不变（拒绝族既有行为零回归） ----------

    @Test
    public void testStrictRejectsUnchanged() {
        Chain chain = seedChain("PO-PPV-STR-001", "PR-PPV-STR-001", "PI-PPV-STR-001",
                new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));

        withStrictMode(true, () -> {
            ApiResponse<?> bad = approve(chain.invoiceId);
            assertEquals(ErpPurErrors.ERR_INVOICE_PRICE_MISMATCH.getErrorCode(), bad.getCode(),
                    "默认拒绝族 strict 超容差 → ERR_INVOICE_PRICE_MISMATCH 拒绝（既有行为不变）");
        });
        ErpPurInvoice reloaded = reload(chain.invoiceId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, reloaded.getApproveStatus(),
                "拒绝后审核状态保持 SUBMITTED");
    }

    // ---------- POST_DIFFERENCE 覆盖 strict：策略路径放行 + PPV 行 ----------

    @Test
    public void testPostDifferenceOverridesStrict() {
        // POST_DIFFERENCE + strict=true：价格超容差 warn 放行（策略优先于 strict 价格拒绝），PPV 行过账
        Chain chain = seedChain("PO-PPV-OVS-001", "PR-PPV-OVS-001", "PI-PPV-OVS-001",
                new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));

        withStrictMode(true, () -> withPriceDiffStrategy("POST_DIFFERENCE", () -> {
            assertEquals(0, approve(chain.invoiceId).getStatus(), "POST_DIFFERENCE 覆盖 strict → 放行");
        }));

        List<ErpFinVoucherLine> lines = voucherLines(chain.invoiceCode);
        assertEquals(4, lines.size(), "策略路径 → PPV 行过账");
        assertAmount("50", debitAmount(findLine(lines, "1404")), "PPV 借 1404 = 50");
        assertEquals(0, sumDebit(lines).compareTo(sumCredit(lines)), "借贷恒等");
    }

    // ---------- config helpers ----------

    private void withPriceDiffStrategy(String strategy, Runnable body) {
        AppConfig.getConfigProvider().assignConfigValue(ErpPurConstants.CONFIG_PRICE_DIFF_STRATEGY, strategy);
        try {
            body.run();
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(ErpPurConstants.CONFIG_PRICE_DIFF_STRATEGY, "");
        }
    }

    private void withStrictMode(boolean strict, Runnable body) {
        AppConfig.getConfigProvider().assignConfigValue(ErpPurConstants.CONFIG_MATCH_STRICT_MODE, strict);
        try {
            body.run();
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(ErpPurConstants.CONFIG_MATCH_STRICT_MODE, false);
        }
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> approve(Long invoiceId) {
        return executeRpc(mutation, "ErpPurInvoice__approve",
                ApiRequest.build(Map.of("id", String.valueOf(invoiceId))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurInvoice reload(Long invoiceId) {
        return daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
    }

    // ---------- voucher assertion helpers ----------

    private List<ErpFinVoucherLine> voucherLines(String invoiceCode) {
        ErpFinVoucherBillR link = findBillLink(invoiceCode);
        assertNotNull(link, "应生成业财回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertNotNull(voucher, "凭证应落库");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证 docStatus=已过账");
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucher.getId()));
        return lineDao.findAllByQuery(q);
    }

    private ErpFinVoucherLine findLine(List<ErpFinVoucherLine> lines, String subjectCode) {
        return lines.stream().filter(l -> subjectCode.equals(l.getSubjectCode())).findFirst()
                .orElseThrow(() -> new AssertionError("凭证行缺失科目: " + subjectCode));
    }

    private BigDecimal debitAmount(ErpFinVoucherLine line) {
        return line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO;
    }

    private BigDecimal creditAmount(ErpFinVoucherLine line) {
        return line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO;
    }

    private BigDecimal sumDebit(List<ErpFinVoucherLine> lines) {
        return lines.stream().map(this::debitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumCredit(List<ErpFinVoucherLine> lines) {
        return lines.stream().map(this::creditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertAmount(String expected, BigDecimal actual, String msg) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), msg);
    }

    private ErpFinVoucherBillR findBillLink(String invoiceCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean())
                .stream().filter(l -> invoiceCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    // ---------- seed helpers ----------

    private Chain seedChain(String orderCode, String receiveCode, String invoiceCode,
                            BigDecimal orderPrice, BigDecimal invoicePrice, BigDecimal qty,
                            BigDecimal totalAmount, BigDecimal tax, BigDecimal withTax) {
        Long orderId;
        Long orderLineId;
        Long receiveId;
        Long receiveLineId;
        Long invoiceId;
        Long invoiceLineId;
        synchronized (this) {
            orderId = nextId();
            orderLineId = nextId();
            receiveId = nextId();
            receiveLineId = nextId();
            invoiceId = nextId();
            invoiceLineId = nextId();
        }
        ormTemplate.runInSession(session -> {
            seedFinanceAndSupplier();
            newOrder(orderCode, orderId);
            newOrderLine(orderId, orderLineId, orderPrice, qty);
            newReceive(receiveCode, receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, invoicePrice, qty);
            newInvoice(invoiceCode, invoiceId, totalAmount, tax, withTax);
            newInvoiceLine(invoiceLineId, invoiceId, receiveLineId, invoicePrice, qty);
            return null;
        });
        return new Chain(invoiceId, invoiceLineId, receiveLineId, invoiceCode);
    }

    /** seed 链返回的引用（id 为 seed 内部分配，测试直接消费）。 */
    private static final class Chain {
        final Long invoiceId;
        final Long invoiceLineId;
        final Long receiveLineId;
        final String invoiceCode;

        Chain(Long invoiceId, Long invoiceLineId, Long receiveLineId, String invoiceCode) {
            this.invoiceId = invoiceId;
            this.invoiceLineId = invoiceLineId;
            this.receiveLineId = receiveLineId;
            this.invoiceCode = invoiceCode;
        }
    }

    private void seedFinanceAndSupplier() {
        seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
        seedSubject("1403", "在途物资");
        seedSubject("2221", "应交税费-进项税额");
        seedSubject("2202", "应付账款");
        seedSubject("1404", "材料成本差异");
        seedAcctSchema();
        seedActiveSupplier();
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
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedActiveSupplier() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(SUPPLIER_ID);
        partner.setCode("SUP-" + SUPPLIER_ID);
        partner.setName("供应商" + SUPPLIER_ID);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
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
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private Long newOrder(String code, Long orderId) {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        ErpPurOrder order = new ErpPurOrder();
        order.setId(orderId);
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setSupplierId(SUPPLIER_ID);
        order.setWarehouseId(9301L);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        order.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        dao.saveEntity(order);
        return order.getId();
    }

    private void newOrderLine(Long orderId, Long lineId, BigDecimal unitPrice, BigDecimal qty) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine line = new ErpPurOrderLine();
        line.setId(lineId);
        line.setOrderId(orderId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        line.setAmount(unitPrice.multiply(qty));
        dao.saveEntity(line);
    }

    private void newReceive(String code, Long receiveId, Long orderId) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        ErpPurReceive receive = new ErpPurReceive();
        receive.setId(receiveId);
        receive.setCode(code);
        receive.setOrgId(ORG_ID);
        receive.setOrderId(orderId);
        receive.setSupplierId(SUPPLIER_ID);
        receive.setWarehouseId(9301L);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(CURRENCY_ID);
        receive.setExchangeRate(new BigDecimal("1"));
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        receive.setPosted(false);
        dao.saveEntity(receive);
    }

    private void newReceiveLine(Long lineId, Long receiveId, Long orderLineId, BigDecimal unitPrice, BigDecimal qty) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setId(lineId);
        line.setReceiveId(receiveId);
        line.setLineNo(1);
        line.setOrderLineId(orderLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        dao.saveEntity(line);
    }

    private void newInvoice(String code, Long invoiceId, BigDecimal amount, BigDecimal tax, BigDecimal withTax) {
        IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
        ErpPurInvoice invoice = new ErpPurInvoice();
        invoice.setId(invoiceId);
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setSupplierId(SUPPLIER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        invoice.setPaidStatus(ErpPurConstants.PAID_STATUS_UNPAID);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(amount);
        invoice.setTotalTaxAmount(tax);
        invoice.setTotalAmountWithTax(withTax);
        invoice.setPosted(false);
        dao.saveEntity(invoice);
    }

    private void newInvoiceLine(Long lineId, Long invoiceId, Long receiveLineId, BigDecimal unitPrice, BigDecimal qty) {
        IEntityDao<ErpPurInvoiceLine> dao = daoProvider.daoFor(ErpPurInvoiceLine.class);
        ErpPurInvoiceLine line = new ErpPurInvoiceLine();
        line.setId(lineId);
        line.setInvoiceId(invoiceId);
        line.setReceiveLineId(receiveLineId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        line.setTaxRate(new BigDecimal("13"));
        dao.saveEntity(line);
    }

    private final AtomicLong idSeq = new AtomicLong(100000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
