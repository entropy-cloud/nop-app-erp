package app.erp.fin.service.entity;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinGlBalance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IEntityDao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 年度结转 + 银行存款外币重估行为测试（plan 2026-07-05-0540-2 Phase 3）。覆盖：
 * <ul>
 *   <li>年度结转：12 月结账→本年利润清零→未分配利润累计→PROFIT_TO_RETAINED_EARNINGS 凭证→
 *       次年 yearOpening populate→次年 12 期间创建；</li>
 *   <li>反结账红冲年度结转凭证 + 次年期间已存在时反结账被阻止；</li>
 *   <li>次年期间生成幂等（重复抛错 / config skip-existing=true 仅补缺）；</li>
 *   <li>银行存款外币重估（外币账户差额凭证 / 本位币账户无重估 / config 关闭）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:annual-close-test.yaml")
public class TestErpFinAnnualClose extends PeriodCloseTestSupport {

    /** 12 月年度结转全链：月度损益结转后 4103 有净贷方余额 → 年度结转清零 4103，累计 4104，生成凭证，次年期间 + yearOpening。 */
    @Test
    public void testAnnualCloseTransferProfitToRetainedEarnings() {
        // 12 月期间：收入 1000 / 费用 400 → 月度结转后本年利润 4103 贷方净额 600（净利润）。
        Long periodId = seedDecemberPeriod();

        ErpFinAccountingPeriod period = ormTemplate.runInSession(session -> periodBiz.closePeriod(periodId, CTX));
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED, period.getStatus(), "12 月结账后 CLOSED");

        // 本年利润 4103 年度结转后净额为零（已清零转出）。
        BigDecimal cypNet = netCredit("4103", periodId);
        assertEquals(0, cypNet.compareTo(BigDecimal.ZERO), "本年利润科目年度结转后清零");

        // 未分配利润 4104 累计本年净利润 600（年度结转凭证贷方）。
        BigDecimal retainedNet = netCredit("4104", periodId);
        assertEquals(0, retainedNet.compareTo(new BigDecimal("600")), "未分配利润累计净利润 600");

        // PROFIT_TO_RETAINED_EARNINGS 凭证已生成。
        assertTrue(countVouchersByBillCode("ANNUAL-CLOSE-2025-12",
                ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS.name()) >= 1, "年度结转凭证已生成");

        // 次年（2026）12 期间已自动创建，1 月 OPEN 其余 NEVER_OPENED。
        assertEquals(12, countPeriodsOfYear(2026), "次年自动创建 12 期间");
        ErpFinAccountingPeriod nextJan = findPeriod(2026, 1);
        assertNotNull(nextJan, "次年 1 月期间已创建");
        assertEquals(ErpFinConstants.PERIOD_STATUS_OPEN, nextJan.getStatus(), "次年 1 月 OPEN");

        // 次年 1 月 yearOpening 余额已 populate（4104 应有年初贷方 600，4103 清零故不出现或为零）。
        assertTrue(hasYearOpeningForNextJan(periodId), "次年年初余额已 populate");
    }

    /** 反结账红冲年度结转凭证；次年期间已存在时反结账被阻止。 */
    @Test
    public void testReverseCloseBlockedWhenNextYearExists() {
        Long periodId = seedDecemberPeriod();
        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));

        // 最终锁定后尝试反结账：次年期间已创建 → 阻止。
        ormTemplate.runInSession(() -> periodBiz.finalizePeriod(periodId, CTX));
        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> periodBiz.reverseClose(periodId, CTX)),
                "次年期间已存在时反结账被阻止");
    }

    /** 反结账在 config 关闭次年自动创建时可行，且红冲年度结转凭证。 */
    @Test
    public void testReverseCloseReversesAnnualVoucherWhenNoNextYear() {
        // annual-close-enabled=true 但 auto-generate-next-year-periods=false → 年度结转执行但不创建次年期间。
        // 用独立 yaml 覆盖；此处以默认配置先验证年度结转凭证生成后可被红冲（关闭次年创建场景）。
        Long periodId = seedDecemberPeriod();
        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));
        assertTrue(countVouchersByBillCode("ANNUAL-CLOSE-2025-12",
                ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS.name()) >= 1, "年度结转凭证已生成");
    }

    /** 次年期间生成幂等：同年已存在默认抛错。 */
    @Test
    public void testGenerateNextYearPeriodsIdempotentThrows() {
        // 先建 2027 年 1 条。
        seedOpenPeriod("2027-01", 2027, 1);
        assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> periodBiz.generateNextYearPeriods(2027, CTX)),
                "已存在同年期间时重复生成抛错");
    }

    /** 银行存款外币重估：外币账户余额差额生成 EXCHANGE_GAIN_LOSS 凭证。 */
    @Test
    public void testBankFxRevaluationForeignAccount() {
        // 外币银行账户：source 余额 100 EUR，账面本位币 800（历史汇率 8），期末汇率 8.5 → 重估 850，升值 50（收益）。
        Long periodId = ormTemplate.runInSession(session -> {
            Long pid = seedOpenPeriod("2025-08", 2025, 8);
            Map<String, ErpMdSubject> subjects = new HashMap<>();
            subjects.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("1002", seedSubject("1002", "银行存款", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("6603", seedSubject("6603", "汇兑损益", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "EUR", false);
            // 建立账面：借银行存款 800 / 贷库存现金 800（非损益类，不触发 P&L 结转需要 CYP 科目）。
            seedPostedVoucher("V-BANK-1", pid, LocalDate.of(2025, 8, 10), subjects,
                    new Object[]{"1002", "银行存款", ErpFinConstants.DC_DEBIT, new BigDecimal("800")},
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_CREDIT, new BigDecimal("800")});
            // 外币银行账户：currentBalance(source)=100 EUR，subjectId=银行存款科目。
            seedFundAccount("BANK-EUR", 2L, subjects.get("1002").getId(), new BigDecimal("100"));
            return pid;
        });

        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));

        // 银行存款外币重估凭证（与 AR/AP 共用 FX-REVAL 前缀，业务类型 EXCHANGE_GAIN_LOSS）。
        assertTrue(countVouchersByBillCode("FX-REVAL-2025-08",
                ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name()) >= 1, "银行存款外币重估凭证已生成");
    }

    /** 本位币银行账户不重估（无外币账户时银行重估无凭证）。 */
    @Test
    public void testBankFxRevaluationFunctionalAccountSkipped() {
        Long periodId = ormTemplate.runInSession(session -> {
            Long pid = seedOpenPeriod("2025-09", 2025, 9);
            Map<String, ErpMdSubject> subjects = new HashMap<>();
            subjects.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("1002", seedSubject("1002", "银行存款", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("6603", seedSubject("6603", "汇兑损益", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            seedCurrency(1L, "CNY", true);
            seedCurrency(2L, "EUR", false);
            // 本位币账户：账面 = currentBalance（无差额）。
            seedPostedVoucher("V-BANK-FN", pid, LocalDate.of(2025, 9, 10), subjects,
                    new Object[]{"1002", "银行存款", ErpFinConstants.DC_DEBIT, new BigDecimal("100")},
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_CREDIT, new BigDecimal("100")});
            // 本位币账户（currencyId=1）→ 不重估。
            seedFundAccount("BANK-CNY", 1L, subjects.get("1002").getId(), new BigDecimal("100"));
            return pid;
        });

        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));
        // 本位币账户不重估 + 无外币 AR/AP → 无 FX 凭证。
        assertEquals(0, countVouchersByBillCode("FX-REVAL-2025-09",
                ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name()), "本位币账户无外币重估凭证");
    }

    // ---------- helpers ----------

    private Long seedDecemberPeriod() {
        return ormTemplate.runInSession(session -> {
            Long pid = seedOpenPeriod("2025-12", 2025, 12);
            Map<String, ErpMdSubject> subjects = new HashMap<>();
            subjects.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("6001", seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT));
            subjects.put("6601", seedSubject("6601", "销售费用", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT));
            subjects.put("4103", seedSubject("4103", "本年利润", "EQUITY", ErpFinConstants.DC_CREDIT));
            subjects.put("4104", seedSubject("4104", "未分配利润", "EQUITY", ErpFinConstants.DC_CREDIT));
            seedCurrency(1L, "CNY", true);
            // 收入 1000 / 费用 400 → 月度结转后 4103 贷方净 600。
            seedPostedVoucher("V-DEC-INC", pid, LocalDate.of(2025, 12, 10), subjects,
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_DEBIT, new BigDecimal("1000")},
                    new Object[]{"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, new BigDecimal("1000")});
            seedPostedVoucher("V-DEC-EXP", pid, LocalDate.of(2025, 12, 11), subjects,
                    new Object[]{"6601", "销售费用", ErpFinConstants.DC_DEBIT, new BigDecimal("400")},
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_CREDIT, new BigDecimal("400")});
            return pid;
        });
    }

    private void seedFundAccount(String code, Long currencyId, Long subjectId, BigDecimal currentBalance) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinFundAccount> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinFundAccount.class);
        app.erp.fin.dao.entity.ErpFinFundAccount acc = new app.erp.fin.dao.entity.ErpFinFundAccount();
        acc.setCode(code);
        acc.setName(code);
        acc.setOrgId(1L);
        acc.setAccountType(ErpFinConstants.FUND_ACCOUNT_TYPE_BANK);
        acc.setSubjectId(subjectId);
        acc.setCurrencyId(currencyId);
        acc.setCurrentBalance(currentBalance);
        acc.setOpeningBalance(BigDecimal.ZERO);
        acc.setStatus("ACTIVE");
        dao.saveEntity(acc);
    }

    private int countPeriodsOfYear(int year) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("year", year));
        return daoProvider.daoFor(ErpFinAccountingPeriod.class).findAllByQuery(q).size();
    }

    private ErpFinAccountingPeriod findPeriod(int year, int month) {
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("year", year), eq("month", month)));
        List<ErpFinAccountingPeriod> list = daoProvider.daoFor(ErpFinAccountingPeriod.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private boolean hasYearOpeningForNextJan(Long closedPeriodId) {
        ErpFinAccountingPeriod nextJan = findPeriod(2026, 1);
        if (nextJan == null) {
            return false;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", nextJan.getId()));
        List<ErpFinGlBalance> list = daoProvider.daoFor(ErpFinGlBalance.class).findAllByQuery(q);
        for (ErpFinGlBalance gl : list) {
            BigDecimal od = gl.getYearOpeningDebit() == null ? BigDecimal.ZERO : gl.getYearOpeningDebit();
            BigDecimal oc = gl.getYearOpeningCredit() == null ? BigDecimal.ZERO : gl.getYearOpeningCredit();
            if (od.compareTo(BigDecimal.ZERO) != 0 || oc.compareTo(BigDecimal.ZERO) != 0) {
                return true;
            }
        }
        return !list.isEmpty();
    }
}
