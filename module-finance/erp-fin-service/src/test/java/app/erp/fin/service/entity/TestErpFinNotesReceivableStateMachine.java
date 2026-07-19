package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinNotesReceivableBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinNotesDiscount;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应收票据 7 态状态机单测（Phase 2）。验证 receive/discount/endorse/collect/honor/dishonor/writeOff
 * 正向迁移、贴现计算（discountInterest=票面×贴现率×剩余天数/360、netAmount=票面−贴现息）、非法迁移抛异常。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinNotesReceivableStateMachine extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinNotesReceivableBiz notesBiz;

    @Test
    public void testReceive() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-001", null, new BigDecimal("10000")); });
        ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.receive(noteId, CTX));
        assertEquals(ErpFinConstants.NOTES_RECV_RECEIVED, note.getStatus());
    }

    @Test
    public void testDiscountComputesInterestAndNetAmount() {
        // faceAmount=36000, rate=6%, remainingDays=60 → discountInterest=36000×0.06×60/360=360.00, net=35640.00
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            return seedReceivable("NR-002", ErpFinConstants.NOTES_RECV_RECEIVED, new BigDecimal("36000"),
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 30));
        });

        ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.discount(noteId, LocalDate.of(2026, 7, 1), 9001L, new BigDecimal("0.06"), null, CTX));
        assertEquals(ErpFinConstants.NOTES_RECV_DISCOUNTED, note.getStatus());

        ErpFinNotesDiscount discount = findDiscount(note.getId());
        assertEquals(0, new BigDecimal("360.00").compareTo(discount.getDiscountInterest()), "贴现息=360.00");
        assertEquals(0, new BigDecimal("35640.00").compareTo(discount.getNetAmount()), "实得=35640.00");
        assertEquals(note.getDiscountId(), discount.getId());
    }

    @Test
    public void testDiscountFxWithSpotRateDerivesExchangeGainLossCashAtSpot() {
        // USD note: amountSource=USD 100, exchangeRate=6.6667, amountFunctional=CNY 666.67,
        // discountRate=0.12, remainingDays=30, spotRate=6.7000（外币升值）
        // → discountInterestFunctional=6.67, discountInterestSource=1.00 USD, netAmountSource=99 USD,
        //   netAmount=99×6.7000=663.3000 (cash-at-spot), exchangeGainLoss=666.67−6.67−663.3000=−3.3000（Cr 6051）
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "USD", false);
            return seedFxReceivable("NR-FX-001", ErpFinConstants.NOTES_RECV_RECEIVED,
                    new BigDecimal("100"), new BigDecimal("6.6667"), new BigDecimal("666.67"), 2L,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        });

        Boolean originalFlag = AppConfig.var(ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, Boolean.FALSE);
        try {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, Boolean.TRUE);
            ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.discount(
                    noteId, LocalDate.of(2026, 7, 1), 9001L, new BigDecimal("0.12"), new BigDecimal("6.7000"), CTX));
            assertEquals(ErpFinConstants.NOTES_RECV_DISCOUNTED, note.getStatus());

            ErpFinNotesDiscount discount = findDiscount(note.getId());
            assertEquals(0, new BigDecimal("6.67").compareTo(discount.getDiscountInterest()),
                    "discountInterest(functional 口径）=6.67");
            assertEquals(0, new BigDecimal("663.3000").compareTo(discount.getNetAmount()),
                    "netAmount(cash-at-spot）=663.3000");
            assertEquals(0, new BigDecimal("-3.3000").compareTo(discount.getExchangeGainLoss()),
                    "exchangeGainLoss(plug）=−3.3000（负数 → Cr 6051 汇兑收益）");
            assertEquals(0, new BigDecimal("6.7000").compareTo(discount.getExchangeRate()),
                    "ErpFinNotesDiscount.exchangeRate=6.7000（spotRate 覆盖 note.exchangeRate）");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, originalFlag);
        }
    }

    @Test
    public void testDiscountFxFallbackWhenSpotRateNull() {
        // USD note + config 启用 + exchangeRate=null（旧签名委派）→ 走 ZERO 兜底路径（向后兼容）。
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "USD", false);
            return seedFxReceivable("NR-FX-002", ErpFinConstants.NOTES_RECV_RECEIVED,
                    new BigDecimal("100"), new BigDecimal("6.6667"), new BigDecimal("666.67"), 2L,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        });

        Boolean originalFlag = AppConfig.var(ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, Boolean.FALSE);
        try {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, Boolean.TRUE);
            // exchangeRate=null（5 参数签名显式传 null，对齐旧 4 参数签名行为）
            ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.discount(
                    noteId, LocalDate.of(2026, 7, 1), 9001L, new BigDecimal("0.12"), null, CTX));
            assertEquals(ErpFinConstants.NOTES_RECV_DISCOUNTED, note.getStatus());

            ErpFinNotesDiscount discount = findDiscount(note.getId());
            assertEquals(0, BigDecimal.ZERO.compareTo(discount.getExchangeGainLoss()),
                    "exchangeRate=null 走 ZERO 兜底路径");
            // netAmount=functional 兜底口径：666.67 − 6.67 = 660.00
            assertEquals(0, new BigDecimal("660.00").compareTo(discount.getNetAmount()),
                    "netAmount（functional 兜底口径）=660.00");
            // exchangeRate 兜底为 note.exchangeRate=6.6667
            assertEquals(0, new BigDecimal("6.6667").compareTo(discount.getExchangeRate()),
                    "ErpFinNotesDiscount.exchangeRate 兜底=note.exchangeRate=6.6667");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, originalFlag);
        }
    }

    @Test
    public void testDiscountFxSuppressedByConfigGate() {
        // USD note + config 关闭 + exchangeRate=6.7000 → config 关闭抑制派生（向后兼容）。
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "USD", false);
            return seedFxReceivable("NR-FX-003", ErpFinConstants.NOTES_RECV_RECEIVED,
                    new BigDecimal("100"), new BigDecimal("6.6667"), new BigDecimal("666.67"), 2L,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        });

        Boolean originalFlag = AppConfig.var(ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, Boolean.FALSE);
        try {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, Boolean.FALSE);
            ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.discount(
                    noteId, LocalDate.of(2026, 7, 1), 9001L, new BigDecimal("0.12"), new BigDecimal("6.7000"), CTX));
            assertEquals(ErpFinConstants.NOTES_RECV_DISCOUNTED, note.getStatus());

            ErpFinNotesDiscount discount = findDiscount(note.getId());
            assertEquals(0, BigDecimal.ZERO.compareTo(discount.getExchangeGainLoss()),
                    "config 关闭时 exchangeGainLoss 抑制=ZERO");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, originalFlag);
        }
    }

    @Test
    public void testEndorse() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-003", ErpFinConstants.NOTES_RECV_RECEIVED, new BigDecimal("5000")); });
        ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.endorse(noteId, null, CTX));
        assertEquals(ErpFinConstants.NOTES_RECV_ENDORSED, note.getStatus());
    }

    @Test
    public void testCollectFromReceived() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-004", ErpFinConstants.NOTES_RECV_RECEIVED, new BigDecimal("5000")); });
        ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.collect(noteId, CTX));
        assertEquals(ErpFinConstants.NOTES_RECV_COLLECTION_PENDING, note.getStatus());
    }

    @Test
    public void testCollectFromDiscounted() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-005", ErpFinConstants.NOTES_RECV_DISCOUNTED, new BigDecimal("5000")); });
        ErpFinNotesReceivable note = ormTemplate.runInSession(session -> notesBiz.collect(noteId, CTX));
        assertEquals(ErpFinConstants.NOTES_RECV_COLLECTION_PENDING, note.getStatus());
    }

    @Test
    public void testHonorAndDishonor() {
        Long honoredId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-006", ErpFinConstants.NOTES_RECV_COLLECTION_PENDING, new BigDecimal("5000")); });
        assertEquals(ErpFinConstants.NOTES_RECV_HONORED, ormTemplate.runInSession(session -> notesBiz.honor(honoredId, CTX)).getStatus());

        Long dishonoredId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-007", ErpFinConstants.NOTES_RECV_COLLECTION_PENDING, new BigDecimal("5000")); });
        assertEquals(ErpFinConstants.NOTES_RECV_DISHONORED, ormTemplate.runInSession(session -> notesBiz.dishonor(dishonoredId, CTX)).getStatus());
    }

    @Test
    public void testWriteOffFromReceived() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-008", ErpFinConstants.NOTES_RECV_RECEIVED, new BigDecimal("5000")); });
        assertEquals(ErpFinConstants.NOTES_RECV_WRITE_OFF, ormTemplate.runInSession(session -> notesBiz.writeOff(noteId, CTX)).getStatus());
    }

    @Test
    public void testIllegalTransitionHonorFromReceived() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-009", ErpFinConstants.NOTES_RECV_RECEIVED, new BigDecimal("5000")); });
        // 承兑须从托收中迁入，从 RECEIVED 直接承兑为非法迁移。
        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> notesBiz.honor(noteId, CTX)));
    }

    @Test
    public void testIllegalTransitionWriteOffFromTerminal() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-010", ErpFinConstants.NOTES_RECV_HONORED, new BigDecimal("5000")); });
        // HONORED 为终态，不可再注销。
        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> notesBiz.writeOff(noteId, CTX)));
    }

    // ---------- seed helpers ----------

    private void seedBase() {
        seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        seedAcctSchema(1L);
        // 状态机迁移会触发业财过账（receive/discount/endorse/honor），预置全部相关科目使过账成功。
        seedSubject("1121", "应收票据");
        seedSubject("1122", "应收账款");
        seedSubject("2202", "应付账款");
        seedSubject("1002", "银行存款");
        seedSubject("6603", "财务费用-利息支出");
    }

    private Long seedReceivable(String code, String status, BigDecimal amountFunctional) {
        return seedReceivable(code, status, amountFunctional, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));
    }

    private Long seedReceivable(String code, String status, BigDecimal amountFunctional,
                                LocalDate issueDate, LocalDate dueDate) {
        IEntityDao<ErpFinNotesReceivable> dao = daoProvider.daoFor(ErpFinNotesReceivable.class);
        ErpFinNotesReceivable note = new ErpFinNotesReceivable();
        note.setCode(code);
        note.setOrgId(1L);
        note.setNotesType(ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE);
        note.setNotesNo("N-" + code);
        note.setIssueDate(issueDate);
        note.setDueDate(dueDate);
        note.setCurrencyId(1L);
        note.setExchangeRate(BigDecimal.ONE);
        note.setAmountFunctional(amountFunctional);
        note.setAmountSource(amountFunctional);
        note.setPartnerId(7000L);
        note.setStatus(status);
        note.setPosted(false);
        dao.saveEntity(note);
        return note.getId();
    }

    private Long seedFxReceivable(String code, String status, BigDecimal amountSource, BigDecimal exchangeRate,
                                  BigDecimal amountFunctional, Long currencyId,
                                  LocalDate issueDate, LocalDate dueDate) {
        IEntityDao<ErpFinNotesReceivable> dao = daoProvider.daoFor(ErpFinNotesReceivable.class);
        ErpFinNotesReceivable note = new ErpFinNotesReceivable();
        note.setCode(code);
        note.setOrgId(1L);
        note.setNotesType(ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE);
        note.setNotesNo("N-" + code);
        note.setIssueDate(issueDate);
        note.setDueDate(dueDate);
        note.setCurrencyId(currencyId);
        note.setExchangeRate(exchangeRate);
        note.setAmountFunctional(amountFunctional);
        note.setAmountSource(amountSource);
        note.setPartnerId(7000L);
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

    private ErpFinNotesDiscount findDiscount(Long notesReceivableId) {
        IEntityDao<ErpFinNotesDiscount> dao = daoProvider.daoFor(ErpFinNotesDiscount.class);
        return dao.findAllByQuery(new io.nop.api.core.beans.query.QueryBean().addFilter(
                io.nop.api.core.beans.FilterBeans.eq("notesReceivableId", notesReceivableId))).get(0);
    }
}
