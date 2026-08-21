package app.erp.fin.service;

import app.erp.common.test.ThreadLocalFrozenClock;
import app.erp.fin.biz.IErpFinNotesPayableBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinCreditFacility;
import app.erp.fin.dao.entity.ErpFinNotesPayable;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitBaseTestCase;
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

/**
 * 跨月模拟运行时证据（plan 2026-08-01-1357-1 Phase 4 / 设计文档 §4.2 step 5 方式 a + §5 验收 2）。
 *
 * <p>在冻结时钟下模拟月初翻车税的两个边界系统日：月末当日（2026-07-31）与次月 1 日（2026-08-01），
 * 各自 seed 由 {@code CoreMetrics.today()} 派生的 OPEN 期间后跑过账链路（notesPayableBiz.issue →
 * NotesPostingDispatcher → resolveOpenPeriod(voucherDate=today)），断言两边界日均 POSTED 成功。
 * 证明路径 C 冻结时钟下过账期间解析跨月确定，不再月初翻车。
 *
 * <p>不使用 {@code @RegisterExtension FinFrozenClockExtension}——本测试在方法内手动
 * {@link ThreadLocalFrozenClock#install(LocalDate)}/{@link ThreadLocalFrozenClock#clear()} 切换两个边界日，
 * 故继承 {@link JunitBaseTestCase}（容器+DB，无快照机制），仅断言运行时跨月行为。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestClockRolloverFinance extends JunitBaseTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinNotesPayableBiz notesPayableBiz;

    @Test
    public void testPostingResolvesPeriodAtMonthEndAndNextMonthStart() {
        ThreadLocalFrozenClock.ensureRegistered();
        try {
            // 边界日 1：月末当日（原月初税起点的前一日）
            assertIssuePostsAtFrozenDate(LocalDate.of(2026, 7, 31));
            // 边界日 2：次月 1 日（原月初税的活跃红源日）
            assertIssuePostsAtFrozenDate(LocalDate.of(2026, 8, 1));
        } finally {
            ThreadLocalFrozenClock.clear();
        }
    }

    /**
     * 在指定冻结日下：seed 由 today() 派生的 OPEN 期间 → 开商业承兑（issue 过账 voucherDate=today）
     * → 断言 ISSUED（过账 resolveOpenPeriod 成功）。两边界日共用，证明跨月确定。
     */
    private void assertIssuePostsAtFrozenDate(LocalDate frozenDate) {
        ThreadLocalFrozenClock.install(frozenDate);
        LocalDate today = io.nop.api.core.time.CoreMetrics.today();
        assertEquals(frozenDate, today, "CoreMetrics.today() 应返回冻结日");

        String noteId = ormTemplate.runInSession(s -> {
            seedBase(today);
            return seedPayable("NP-ROLLOVER-" + today.getMonthValue(),
                    ErpFinConstants.NOTES_TYPE_COMMERCIAL_ACCEPTANCE, null, new BigDecimal("5000"));
        });

        ErpFinNotesPayable note = ormTemplate.runInSession(session -> notesPayableBiz.issue(noteId, CTX));
        assertEquals(ErpFinConstants.NOTES_PAY_ISSUED, note.getStatus(),
                "冻结日 " + frozenDate + "：issue 过账 resolveOpenPeriod 应成功（POSTED→ISSUED）");
    }

    private void seedBase(LocalDate today) {
        int year = today.getYear();
        int month = today.getMonthValue();
        String code = year + "-" + String.format("%02d", month);
        seedOpenPeriod(code, year, month, today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
        seedAcctSchema("1");
        seedSubject("2202", "应付账款");
        seedSubject("2203", "应付票据");
        seedSubject("1002", "银行存款");
    }

    private String seedPayable(String code, String notesType, String creditFacilityId, BigDecimal amountFunctional) {
        IEntityDao<ErpFinNotesPayable> dao = daoProvider.daoFor(ErpFinNotesPayable.class);
        ErpFinNotesPayable note = new ErpFinNotesPayable();
        note.setCode(code);
        note.setOrgId("1");
        note.setNotesType(notesType);
        note.setNotesNo("N-" + code);
        note.setCurrencyId("1");
        note.setExchangeRate(BigDecimal.ONE);
        note.setAmountFunctional(amountFunctional);
        note.setAmountSource(amountFunctional);
        note.setCreditFacilityId(creditFacilityId);
        note.setPosted(false);
        dao.saveEntity(note);
        return note.getId();
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId("1");
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
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

    private void seedAcctSchema(String orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId("1");
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }
}
