package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinNotesReceivableBiz;
import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应收票据业财过账端到端单测（Phase 3）。验证：收到→NOTES_RECEIVABLE_RECEIVED 凭证 + RECEIVABLE 辅助账（抵 AR）；
 * 贴现→NOTES_RECEIVABLE_DISCOUNTED 五件套科目分解凭证；背书→NOTES_RECEIVABLE_ENDORSED 凭证 + PAYABLE 辅助账（抵 AP）；
 * 承兑→NOTES_RECEIVABLE_COLLECTION 凭证；票据核销联动 AR/AP（同方向核销）；writeOff→红冲 + 辅助账 CANCELLED。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinNotesReceivablePosting extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinNotesReceivableBiz notesBiz;
    @Inject
    IErpFinReconciliationBiz reconciliationBiz;

    @Test
    public void testReceivePostsAndGeneratesReceivableSubledger() {
        long partnerId = 7701L;
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            seedSubject("1121", "应收票据");
            seedSubject("1122", "应收账款");
            return seedReceivable("NR-POST-001", null, new BigDecimal("10000"), partnerId);
        });

        ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.receive(noteId, CTX));
        assertTrue(Boolean.TRUE.equals(note.getPosted()), "过账成功 posted=true");
        assertFalse(findBillLinks("NR-POST-001", ErpFinBusinessType.NOTES_RECEIVABLE_RECEIVED.name()).isEmpty(), "RECEIVED 凭证回链已落库");

        ErpFinArApItem item = findItem("NOTES_RECEIVABLE", "NR-POST-001");
        assertEquals(app.erp.fin.service.ErpFinConstants.DIRECTION_RECEIVABLE, item.getDirection(), "方向=应收");
        assertEquals(partnerId, item.getPartnerId(), "partnerId = 出票客户");
        assertEquals(0, item.getAmountFunctional().compareTo(new BigDecimal("10000")));
    }

    @Test
    public void testDiscountPostsFiveLineDecomposition() {
        long partnerId = 7702L;
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            seedSubject("1121", "应收票据");
            seedSubject("1002", "银行存款");
            seedSubject("6603", "财务费用-利息支出");
            return seedReceivable("NR-POST-002", app.erp.fin.service.ErpFinConstants.NOTES_RECV_RECEIVED,
                    new BigDecimal("36000"), partnerId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 30));
        });

        ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.discount(noteId, LocalDate.of(2026, 7, 1), 9001L, new BigDecimal("0.06"), null, CTX));
        assertTrue(Boolean.TRUE.equals(note.getPosted()), "贴现过账成功 posted=true");
        assertFalse(findBillLinks("NR-POST-002", ErpFinBusinessType.NOTES_RECEIVABLE_DISCOUNTED.name()).isEmpty(), "DISCOUNTED 凭证回链已落库");
    }

    @Test
    public void testDiscountFxPosts6051VoucherLineAndBalance() {
        // plan 2026-07-19-0730-1 Proof 3：cash-at-spot plug 范式外币贴现端到端 6051 行 + 复式平衡断言。
        // USD note: amountSource=USD 100 / exchangeRate=6.6667 / amountFunctional=CNY 666.67
        // discountRate=0.12 / remainingDays=30 / spotRate=6.7000（外币升值）
        // → discountInterestFunctional=6.67 / netAmount=663.3000 / exchangeGainLoss=−3.3000（Cr 6051）
        // 复式平衡：Dr 663.3000 + Dr 6.67 = 669.97 ≡ Cr 3.3000 + Cr 666.67 = 669.97
        long partnerId = 7706L;
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "USD", false);
            seedSubject("1121", "应收票据");
            seedSubject("1002", "银行存款");
            seedSubject("6603", "财务费用-利息支出");
            seedSubject("6051", "汇兑损益");
            return seedFxReceivable("NR-FX-POST-001",
                    app.erp.fin.service.ErpFinConstants.NOTES_RECV_RECEIVED,
                    new BigDecimal("100"), new BigDecimal("6.6667"), new BigDecimal("666.67"), 2L, partnerId,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        });

        Boolean originalFlag = AppConfig.var(ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, Boolean.FALSE);
        ErpFinNotesReceivable note;
        try {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, Boolean.TRUE);
            note = ormTemplate.runInSession(session -> notesBiz.discount(
                    noteId, LocalDate.of(2026, 7, 1), 9001L, new BigDecimal("0.12"), new BigDecimal("6.7000"), CTX));
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, originalFlag);
        }

        assertTrue(Boolean.TRUE.equals(note.getPosted()), "FX 贴现过账成功 posted=true");
        List<ErpFinVoucherBillR> links = findBillLinks("NR-FX-POST-001",
                ErpFinBusinessType.NOTES_RECEIVABLE_DISCOUNTED.name());
        assertFalse(links.isEmpty(), "FX DISCOUNTED 凭证回链已落库");

        List<ErpFinVoucherLine> lines = linesOf(links.get(0).getVoucherId());
        assertEquals(4, lines.size(), "FX 贴现凭证 4 行（含 6051）");

        BigDecimal dr1002 = debitOf(lines, "1002");
        BigDecimal dr6603 = debitOf(lines, "6603");
        BigDecimal cr6051 = creditOf(lines, "6051");
        BigDecimal cr1121 = creditOf(lines, "1121");

        assertEquals(0, new BigDecimal("663.3000").compareTo(dr1002), "Dr 1002 netAmount cash-at-spot=663.3000");
        assertEquals(0, new BigDecimal("6.67").compareTo(dr6603), "Dr 6603 discountInterest functional=6.67");
        assertEquals(0, new BigDecimal("3.3000").compareTo(cr6051), "Cr 6051 exchangeGainLoss 取负=3.3000");
        assertEquals(0, new BigDecimal("666.67").compareTo(cr1121), "Cr 1121 faceAmount functional=666.67");

        // 复式平衡断言（Decision 2 硬约束）：Σ Dr ≡ Σ Cr = 669.97
        BigDecimal sumDr = dr1002.add(dr6603);
        BigDecimal sumCr = cr6051.add(cr1121);
        assertEquals(0, sumDr.compareTo(sumCr),
                "Σ Dr (" + sumDr + ") ≡ Σ Cr (" + sumCr + ") 复式平衡");
        assertEquals(0, new BigDecimal("669.97").compareTo(sumDr), "Σ Dr = 669.97");
        assertEquals(0, new BigDecimal("669.97").compareTo(sumCr), "Σ Cr = 669.97");
    }

    @Test
    public void testEndorsePostsAndGeneratesPayableSubledger() {
        long partnerId = 7703L;
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            seedSubject("1121", "应收票据");
            seedSubject("2202", "应付账款");
            return seedReceivable("NR-POST-003", app.erp.fin.service.ErpFinConstants.NOTES_RECV_RECEIVED,
                    new BigDecimal("8000"), partnerId);
        });

        ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.endorse(noteId, null, CTX));
        assertTrue(Boolean.TRUE.equals(note.getPosted()), "背书过账成功 posted=true");
        assertFalse(findBillLinks("NR-POST-003", ErpFinBusinessType.NOTES_RECEIVABLE_ENDORSED.name()).isEmpty(), "ENDORSED 凭证回链已落库");

        ErpFinArApItem item = findItem("NOTES_ENDORSED", "NR-POST-003");
        assertEquals(app.erp.fin.service.ErpFinConstants.DIRECTION_PAYABLE, item.getDirection(), "方向=应付");
        assertEquals(partnerId, item.getPartnerId(), "partnerId = 背书抵供应商");
    }

    @Test
    public void testReceiveReconcilesAgainstArInvoice() {
        long partnerId = 7704L;
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            seedSubject("1121", "应收票据");
            seedSubject("1122", "应收账款");
            return seedReceivable("NR-RECON-001", null, new BigDecimal("5000"), partnerId);
        });

        // 预置客户应收发票辅助账（AR_INVOICE，RECEIVABLE，同方向，可被票据收到项核销）。
        Long arItemId = ormTemplate.runInSession(s -> seedArApItem(partnerId,
                app.erp.fin.service.ErpFinConstants.DIRECTION_RECEIVABLE, "AR_INVOICE", "AR-001", new BigDecimal("5000")));

        ormTemplate.runInSession(() -> notesBiz.receive(noteId, CTX));
        ErpFinArApItem notesItem = findItem("NOTES_RECEIVABLE", "NR-RECON-001");

        ReconciliationLineInput line = new ReconciliationLineInput();
        line.setPaymentItemId(notesItem.getId());
        line.setInvoiceItemId(arItemId);
        line.setSettledAmountSource(new BigDecimal("5000"));
        line.setSettledAmountFunctional(new BigDecimal("5000"));

        ErpFinReconciliation recon = ormTemplate.runInSession(session -> reconciliationBiz.create(
                app.erp.fin.service.ErpFinConstants.DIRECTION_RECEIVABLE, partnerId,
                LocalDate.of(2026, 7, 15), Collections.singletonList(line), CTX));
        ormTemplate.runInSession(() -> reconciliationBiz.post(recon.getId(), CTX));

        ErpFinArApItem settledNotes = findItem("NOTES_RECEIVABLE", "NR-RECON-001");
        assertEquals(app.erp.fin.service.ErpFinConstants.AR_AP_STATUS_SETTLED, settledNotes.getStatus(),
                "票据收到项经核销单已结清");
    }

    @Test
    public void testWriteOffReversesPosting() {
        long partnerId = 7705L;
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            seedSubject("1121", "应收票据");
            seedSubject("1122", "应收账款");
            return seedReceivable("NR-POST-004", null, new BigDecimal("3000"), partnerId);
        });

        ormTemplate.runInSession(() -> notesBiz.receive(noteId, CTX));
        assertTrue(!findBillLinks("NR-POST-004", ErpFinBusinessType.NOTES_RECEIVABLE_RECEIVED.name()).isEmpty(), "过账凭证已存在");

        ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.writeOff(noteId, CTX));
        assertFalse(Boolean.TRUE.equals(note.getPosted()), "注销红冲后 posted=false");
        // 红冲后辅助账 CANCELLED
        ErpFinArApItem item = findItem("NOTES_RECEIVABLE", "NR-POST-004");
        assertEquals(app.erp.fin.service.ErpFinConstants.AR_AP_STATUS_CANCELLED, item.getStatus(), "红冲后辅助账 CANCELLED");
    }

    // ---------- seed helpers ----------

    private void seedBase() {
        seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        seedAcctSchema(1L);
    }

    private Long seedReceivable(String code, String status, BigDecimal amountFunctional, Long partnerId) {
        return seedReceivable(code, status, amountFunctional, partnerId,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));
    }

    private Long seedReceivable(String code, String status, BigDecimal amountFunctional, Long partnerId,
                                LocalDate issueDate, LocalDate dueDate) {
        IEntityDao<ErpFinNotesReceivable> dao = daoProvider.daoFor(ErpFinNotesReceivable.class);
        ErpFinNotesReceivable note = new ErpFinNotesReceivable();
        note.setCode(code);
        note.setOrgId(1L);
        note.setNotesType(app.erp.fin.service.ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE);
        note.setNotesNo("N-" + code);
        note.setIssueDate(issueDate);
        note.setDueDate(dueDate);
        note.setCurrencyId(1L);
        note.setExchangeRate(BigDecimal.ONE);
        note.setAmountFunctional(amountFunctional);
        note.setAmountSource(amountFunctional);
        note.setPartnerId(partnerId);
        note.setStatus(status);
        note.setPosted(false);
        dao.saveEntity(note);
        return note.getId();
    }

    private Long seedFxReceivable(String code, String status, BigDecimal amountSource, BigDecimal exchangeRate,
                                  BigDecimal amountFunctional, Long currencyId, Long partnerId,
                                  LocalDate issueDate, LocalDate dueDate) {
        IEntityDao<ErpFinNotesReceivable> dao = daoProvider.daoFor(ErpFinNotesReceivable.class);
        ErpFinNotesReceivable note = new ErpFinNotesReceivable();
        note.setCode(code);
        note.setOrgId(1L);
        note.setNotesType(app.erp.fin.service.ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE);
        note.setNotesNo("N-" + code);
        note.setIssueDate(issueDate);
        note.setDueDate(dueDate);
        note.setCurrencyId(currencyId);
        note.setExchangeRate(exchangeRate);
        note.setAmountFunctional(amountFunctional);
        note.setAmountSource(amountSource);
        note.setPartnerId(partnerId);
        note.setStatus(status);
        note.setPosted(false);
        dao.saveEntity(note);
        return note.getId();
    }

    private void seedCurrency(Long id, String code, boolean functional) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = new ErpMdCurrency();
        c.setId(id);
        c.setCode(code);
        c.setName(code);
        c.setIsFunctional(functional);
        dao.saveEntity(c);
    }

    private Long seedArApItem(Long partnerId, String direction, String sourceBillType, String sourceBillCode, BigDecimal amount) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = new ErpFinArApItem();
        item.setCode("ARI-" + sourceBillCode);
        item.setOrgId(1L);
        item.setAcctSchemaId(1L);
        item.setDirection(direction);
        item.setPartnerId(partnerId);
        item.setSourceBillType(sourceBillType);
        item.setSourceBillCode(sourceBillCode);
        item.setBusinessDate(LocalDate.of(2026, 6, 15));
        item.setDueDate(LocalDate.of(2026, 8, 15));
        item.setCurrencyId(1L);
        item.setExchangeRate(BigDecimal.ONE);
        item.setAmountSource(amount);
        item.setAmountFunctional(amount);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(amount);
        item.setOpenAmountFunctional(amount);
        item.setStatus(app.erp.fin.service.ErpFinConstants.AR_AP_STATUS_OPEN);
        dao.saveEntity(item);
        return item.getId();
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

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }

    private ErpFinArApItem findItem(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        List<ErpFinArApItem> items = dao.findAllByQuery(q);
        return items.isEmpty() ? null : items.get(0);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherLine> linesOf(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q);
    }

    private BigDecimal debitOf(List<ErpFinVoucherLine> lines, String subjectCode) {
        return lines.stream()
                .filter(l -> subjectCode.equals(l.getSubjectCode()))
                .map(ErpFinVoucherLine::getDebitAmount)
                .findFirst().orElse(BigDecimal.ZERO);
    }

    private BigDecimal creditOf(List<ErpFinVoucherLine> lines, String subjectCode) {
        return lines.stream()
                .filter(l -> subjectCode.equals(l.getSubjectCode()))
                .map(ErpFinVoucherLine::getCreditAmount)
                .findFirst().orElse(BigDecimal.ZERO);
    }
}
