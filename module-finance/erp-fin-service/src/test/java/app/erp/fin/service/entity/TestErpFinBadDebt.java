package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.biz.IErpFinBadDebtBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PeriodPreCheckReport;
import app.erp.fin.dao.dto.BadDebtProvisionResult;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.baddebt.BadDebtProvisionCalculator;
import app.erp.fin.service.baddebt.BadDebtProvisionService;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.Arrays;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 坏账准备五步分录行为测试（Phase 3，{@code bad-debt.md}）。覆盖：
 * <ul>
 *   <li>账龄分桶计提（5 区间 + 排除负余额/已核销）+ 补提 BAD_DEBT_RESERVE 凭证（借信用减值损失/贷坏账准备）</li>
 *   <li>坏账核销（status→WRITTEN_OFF + openAmount→0 + BAD_DEBT_WRITE_OFF 凭证 借Allowance/贷AR，不进 P&L）</li>
 *   <li>坏账收回恢复（回退正常态 + BAD_DEBT_RECOVERY 凭证 借AR/贷Allowance）</li>
 *   <li>准备释放（超额 → BAD_DEBT_RELEASE 凭证 借Allowance/贷信用减值损失）</li>
 *   <li>期末 allowance 充足性门控（必需&gt;账面阻止结账 / 相等通过）+ NRV 正值</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:bad-debt-test.yaml")
public class TestErpFinBadDebt extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBadDebtBiz badDebtBiz;
    @Inject
    IErpFinAccountingPeriodBiz periodCloseBiz;
    @Inject
    BadDebtProvisionService provisionService;
    @Inject
    BadDebtProvisionCalculator calculator;

    @Test
    public void testAgingBucketProvisionAndReserve() {
        LocalDate asOf = LocalDate.of(2024, 6, 30);
        Long[] holder = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2024-06", 2024, 6, LocalDate.of(2024, 6, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("6701", "信用减值损失", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            // 5 区间各 1000：0-30(10d)/31-60(40d)/61-90(75d)/91-180(120d)/180+(200d)
            seedReceivable("AR-BD-1", pid, asOf.minusDays(10), "1000");
            seedReceivable("AR-BD-2", pid, asOf.minusDays(40), "1000");
            seedReceivable("AR-BD-3", pid, asOf.minusDays(75), "1000");
            seedReceivable("AR-BD-4", pid, asOf.minusDays(120), "1000");
            seedReceivable("AR-BD-5", pid, asOf.minusDays(200), "1000");
            holder[0] = pid;
        });

        BadDebtProvisionResult result = ormTemplate.runInSession(session -> badDebtBiz.runBadDebtProvision(holder[0], CTX));

        // 必需 = 1000*(0.005+0.02+0.05+0.15+0.40) = 625
        assertEquals(0, result.getRequiredProvision().compareTo(new BigDecimal("625.000")), "必需准备 625");
        assertEquals(0, result.getAllowanceBalance().compareTo(BigDecimal.ZERO), "初始 Allowance 0");
        assertEquals("RESERVE", result.getAction(), "不足→补提");
        assertNotNull(result.getVoucherId(), "生成补提凭证");
        output("1_provision_result.json5", provisionResultState(result));

        // 凭证：借信用减值损失 625 / 贷坏账准备 625（进 P&L）
        List<ErpFinVoucherLine> lines = linesOf(result.getVoucherId());
        ErpFinVoucherLine expense = lineOfSubject(lines, "6701");
        assertEquals(ErpFinConstants.DC_DEBIT, expense.getDcDirection(), "借信用减值损失");
        ErpFinVoucherLine allowance = lineOfSubject(lines, "1231");
        assertEquals(ErpFinConstants.DC_CREDIT, allowance.getDcDirection(), "贷坏账准备");
        assertEquals(0, expense.getDebitAmount().compareTo(new BigDecimal("625.000")), "金额 625");
        output("2_voucher_lines.json5", lines.stream().map(this::voucherLineState).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testProvisionExcludesNegativeAndWrittenOff() {
        LocalDate asOf = LocalDate.of(2024, 7, 31);
        Long[] holder = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2024-07", 2024, 7, LocalDate.of(2024, 7, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("6701", "信用减值损失", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedReceivable("AR-INC-1", pid, asOf.minusDays(10), "1000");
            // 负余额项应排除
            seedReceivableOpen("AR-NEG", pid, asOf.minusDays(10), new BigDecimal("-200"));
            // 已核销项应排除
            seedReceivableStatus("AR-WO", pid, asOf.minusDays(10), "500", ErpFinConstants.AR_AP_STATUS_WRITTEN_OFF);
            holder[0] = pid;
        });

        List<ErpFinArApItem> open = provisionService.getReceivableOpenItems();
        BadDebtProvisionResult result = calculator.calculate(open, asOf);

        // 仅 AR-INC-1 (1000 × 0.005 = 5) 计入；负余额/已核销排除
        assertEquals(0, result.getTotalConsidered().compareTo(new BigDecimal("1000")), "仅正未核销项计入");
        assertEquals(0, result.getRequiredProvision().compareTo(new BigDecimal("5.000")), "必需 5");
    }

    @Test
    public void testWriteOffSetsStatusAndVoucherNoPL() {
        LocalDate asOf = LocalDate.of(2024, 8, 31);
        Long[] holder = new Long[1];
        Long[] itemId = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2024-08", 2024, 8, LocalDate.of(2024, 8, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            ErpFinArApItem item = seedReceivable("AR-WO-1", pid, asOf.minusDays(10), "500");
            itemId[0] = item.getId();
            holder[0] = pid;
        });

        // write-off-require-approval=false → 创建即自动审批执行
        ErpFinBadDebt debt = ormTemplate.runInSession(session -> badDebtBiz.writeOff(itemId[0], "客户破产", CTX));

        assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, debt.getApprovalStatus(), "自动审批通过");
        assertNotNull(debt.getVoucherId(), "生成核销凭证");
        output("1_writeoff_debt.json5", badDebtState(debt));

        ErpFinArApItem after = daoProvider.daoFor(ErpFinArApItem.class).getEntityById(itemId[0]);
        assertEquals(ErpFinConstants.AR_AP_STATUS_WRITTEN_OFF, after.getStatus(), "status→WRITTEN_OFF");
        assertEquals(0, after.getOpenAmountFunctional().compareTo(BigDecimal.ZERO), "openAmount→0");
        assertEquals(0, after.getSettledAmountFunctional().compareTo(new BigDecimal("500")), "settledAmount+=500");
        output("2_ar_ap_item_state.json5", arApItemState(after));

        // 凭证：借 Allowance 500 / 贷 AR 500（不进 P&L — 无 expense 科目）
        List<ErpFinVoucherLine> lines = linesOf(debt.getVoucherId());
        ErpFinVoucherLine allowance = lineOfSubject(lines, "1231");
        assertEquals(ErpFinConstants.DC_DEBIT, allowance.getDcDirection(), "核销借 Allowance");
        ErpFinVoucherLine ar = lineOfSubject(lines, "1122");
        assertEquals(ErpFinConstants.DC_CREDIT, ar.getDcDirection(), "核销贷 AR");
        // 确认无信用减值损失科目（不进 P&L）
        assertTrue(lines.stream().noneMatch(l -> "6701".equals(l.getSubjectCode())), "核销不进 P&L");
        output("3_voucher_lines.json5", lines.stream().map(this::voucherLineState).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testRecoveryRestoresArApItem() {
        LocalDate asOf = LocalDate.of(2024, 9, 30);
        Long[] holder = new Long[1];
        Long[] itemId = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2024-09", 2024, 9, LocalDate.of(2024, 9, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            ErpFinArApItem item = seedReceivable("AR-RC-1", pid, asOf.minusDays(10), "300");
            itemId[0] = item.getId();
            holder[0] = pid;
        });

        ormTemplate.runInSession(() -> badDebtBiz.writeOff(itemId[0], "核销", CTX));
        ErpFinBadDebt recovery = ormTemplate.runInSession(session -> badDebtBiz.recover(itemId[0], "事后回款", CTX));

        assertEquals(ErpFinConstants.BAD_DEBT_TYPE_RECOVERY, recovery.getDocType(), "恢复单类型");
        assertNotNull(recovery.getVoucherId(), "生成恢复凭证");
        output("1_recovery_debt.json5", badDebtState(recovery));

        ErpFinArApItem after = daoProvider.daoFor(ErpFinArApItem.class).getEntityById(itemId[0]);
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, after.getStatus(), "回退正常态 OPEN");
        assertEquals(0, after.getOpenAmountFunctional().compareTo(new BigDecimal("300")), "openAmount 恢复");
        output("2_ar_ap_item_state.json5", arApItemState(after));

        // 凭证：借 AR 300 / 贷 Allowance 300
        List<ErpFinVoucherLine> lines = linesOf(recovery.getVoucherId());
        assertEquals(ErpFinConstants.DC_DEBIT, lineOfSubject(lines, "1122").getDcDirection(), "恢复借 AR");
        assertEquals(ErpFinConstants.DC_CREDIT, lineOfSubject(lines, "1231").getDcDirection(), "恢复贷 Allowance");
        output("3_voucher_lines.json5", lines.stream().map(this::voucherLineState).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testReleaseWhenAllowanceExceedsRequired() {
        LocalDate asOf = LocalDate.of(2024, 10, 31);
        Long[] holder = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2024-10", 2024, 10, LocalDate.of(2024, 10, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("6701", "信用减值损失", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedReceivable("AR-RL-1", pid, asOf.minusDays(10), "1000");
            // 预置 Allowance 账面 500（贷方 500）→ 必需 5 → 释放 495
            seedAllowanceVoucher(pid, "500", asOf);
            holder[0] = pid;
        });

        BadDebtProvisionResult result = ormTemplate.runInSession(session -> badDebtBiz.runBadDebtProvision(holder[0], CTX));

        assertEquals("RELEASE", result.getAction(), "超额→释放");
        output("1_provision_result.json5", provisionResultState(result));
        // 释放 = 500 − 5 = 495
        List<ErpFinVoucherLine> lines = linesOf(result.getVoucherId());
        // 借 Allowance / 贷 信用减值损失（释放是唯一贷记 Bad Debt Expense 的场景）
        assertEquals(ErpFinConstants.DC_DEBIT, lineOfSubject(lines, "1231").getDcDirection(), "释放借 Allowance");
        ErpFinVoucherLine expense = lineOfSubject(lines, "6701");
        assertEquals(ErpFinConstants.DC_CREDIT, expense.getDcDirection(), "释放贷信用减值损失");
        assertEquals(0, expense.getCreditAmount().compareTo(new BigDecimal("495.000")), "释放金额 495");
        output("2_voucher_lines.json5", lines.stream().map(this::voucherLineState).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testNoActionWhenAllowanceSufficient() {
        LocalDate asOf = LocalDate.of(2024, 11, 30);
        Long[] holder = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2024-11", 2024, 11, LocalDate.of(2024, 11, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("6701", "信用减值损失", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedReceivable("AR-OK-1", pid, asOf.minusDays(10), "1000");
            // 预置 Allowance = 5（精确等于必需 1000×0.005=5）
            seedAllowanceVoucher(pid, "5", asOf);
            holder[0] = pid;
        });

        BadDebtProvisionResult result = ormTemplate.runInSession(session -> badDebtBiz.runBadDebtProvision(holder[0], CTX));

        assertEquals("NONE", result.getAction(), "精度内相等→无动作");
        assertNull(result.getVoucherId(), "无凭证生成");
        output("1_provision_result.json5", provisionResultState(result));
    }

    @Test
    public void testPeriodCloseAllowanceGateBlocksWhenShortfall() {
        LocalDate asOf = LocalDate.of(2024, 12, 31);
        Long[] holder = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2024-12", 2024, 12, LocalDate.of(2024, 12, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("6701", "信用减值损失", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            // 180+ 区间 1000 → 必需 400，Allowance 0 → 缺口 400（阻断）
            seedReceivable("AR-GATE-1", pid, asOf.minusDays(200), "1000");
            holder[0] = pid;
        });

        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(holder[0]);
        app.erp.fin.dao.dto.BadDebtProvisionResult calc = provisionService.calculateRequiredProvision(period);

        // NRV 正值校验：gross 1000 − allowance 0 = 1000 > 0
        assertTrue(provisionService.getReceivableTotal().compareTo(BigDecimal.ZERO) > 0, "NRV 正值");
        assertEquals(0, calc.getRequiredProvision().compareTo(new BigDecimal("400.000")), "必需 400");

        // 期末门控经 preCheck 报告 shortfall
        PeriodPreCheckReport report = ormTemplate.runInSession(session -> periodCloseBiz.preCheck(holder[0], CTX));
        assertTrue(report.getAllowanceShortfall().compareTo(BigDecimal.ZERO) > 0, "Allowance 缺口阻断");
        assertTrue(report.hasIssues(), "前置检查未通过");
        java.util.Map<String, Object> preCheckState = new java.util.LinkedHashMap<>();
        preCheckState.put("allowanceShortfall", report.getAllowanceShortfall());
        preCheckState.put("hasIssues", report.hasIssues());
        output("1_pre_check_report.json5", preCheckState);
    }

    // ---------- helpers ----------

    private java.util.Map<String, Object> provisionResultState(BadDebtProvisionResult r) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("action", r.getAction());
        m.put("requiredProvision", r.getRequiredProvision());
        m.put("allowanceBalance", r.getAllowanceBalance());
        m.put("voucherId", r.getVoucherId());
        m.put("totalConsidered", r.getTotalConsidered());
        return m;
    }

    private java.util.Map<String, Object> badDebtState(ErpFinBadDebt d) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("docType", d.getDocType());
        m.put("approvalStatus", d.getApprovalStatus());
        m.put("voucherId", d.getVoucherId());
        return m;
    }

    private java.util.Map<String, Object> arApItemState(ErpFinArApItem it) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", it.getId());
        m.put("status", it.getStatus());
        m.put("openAmountFunctional", it.getOpenAmountFunctional());
        m.put("settledAmountFunctional", it.getSettledAmountFunctional());
        return m;
    }

    private java.util.Map<String, Object> voucherLineState(ErpFinVoucherLine l) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("subjectCode", l.getSubjectCode());
        m.put("dcDirection", l.getDcDirection());
        m.put("debitAmount", l.getDebitAmount());
        m.put("creditAmount", l.getCreditAmount());
        return m;
    }

    private Long seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(1L);
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
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

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(subjectClass);
        s.setDirection(direction);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }

    private ErpFinArApItem seedReceivable(String code, Long periodId, LocalDate businessDate, String amount) {
        return seedReceivableStatus(code, periodId, businessDate, amount, ErpFinConstants.AR_AP_STATUS_OPEN);
    }

    private ErpFinArApItem seedReceivableStatus(String code, Long periodId, LocalDate businessDate,
                                                String amount, String status) {
        BigDecimal amt = new BigDecimal(amount);
        return seedReceivableRaw(code, periodId, businessDate, amt, amt, status);
    }

    private ErpFinArApItem seedReceivableOpen(String code, Long periodId, LocalDate businessDate, BigDecimal open) {
        return seedReceivableRaw(code, periodId, businessDate, open, open, ErpFinConstants.AR_AP_STATUS_OPEN);
    }

    private ErpFinArApItem seedReceivableRaw(String code, Long periodId, LocalDate businessDate,
                                             BigDecimal openFunctional, BigDecimal amountFunctional, String status) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = new ErpFinArApItem();
        item.setCode(code);
        item.setOrgId(1L);
        item.setAcctSchemaId(1L);
        item.setDirection(ErpFinConstants.DIRECTION_RECEIVABLE);
        item.setPartnerId(1L);
        item.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        item.setSourceBillCode(code);
        item.setBusinessDate(businessDate);
        item.setCurrencyId(1L);
        item.setExchangeRate(BigDecimal.ONE);
        item.setAmountSource(amountFunctional);
        item.setAmountFunctional(amountFunctional);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(openFunctional);
        item.setOpenAmountFunctional(openFunctional);
        item.setStatus(status);
        item.setPeriodId(periodId);
        dao.saveEntity(item);
        return item;
    }

    /** 预置 Allowance 账面：直接写入一张已过账凭证，贷 Allowance 科目（模拟前期计提累积）。 */
    private void seedAllowanceVoucher(Long periodId, String creditAmount, LocalDate voucherDate) {
        ormTemplate.flushSession();
        BigDecimal amt = new BigDecimal(creditAmount);
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        ErpMdSubject allowance = findSubject("1231");
        ErpMdSubject expense = findSubject("6701");
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode("SEED-ALLOW-" + System.nanoTime());
        v.setVoucherType("TRANSFER");
        v.setPostingType("NORMAL");
        v.setVoucherDate(voucherDate);
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(amt);
        v.setTotalCredit(amt);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);
        ErpFinVoucherLine dr = new ErpFinVoucherLine();
        dr.setVoucherId(v.getId());
        dr.setLineNo(1);
        dr.setSubjectId(expense.getId());
        dr.setSubjectCode(expense.getCode());
        dr.setSubjectName(expense.getName());
        dr.setDcDirection(ErpFinConstants.DC_DEBIT);
        dr.setDebitAmount(amt);
        dr.setCreditAmount(BigDecimal.ZERO);
        dr.setCurrencyId(1L);
        dr.setExchangeRate(BigDecimal.ONE);
        dr.setAmountSource(amt);
        dr.setAmountFunctional(amt);
        dr.setAcctSchemaId(1L);
        lDao.saveEntity(dr);
        ErpFinVoucherLine cr = new ErpFinVoucherLine();
        cr.setVoucherId(v.getId());
        cr.setLineNo(2);
        cr.setSubjectId(allowance.getId());
        cr.setSubjectCode(allowance.getCode());
        cr.setSubjectName(allowance.getName());
        cr.setDcDirection(ErpFinConstants.DC_CREDIT);
        cr.setDebitAmount(BigDecimal.ZERO);
        cr.setCreditAmount(amt);
        cr.setCurrencyId(1L);
        cr.setExchangeRate(BigDecimal.ONE);
        cr.setAmountSource(amt);
        cr.setAmountFunctional(amt);
        cr.setAcctSchemaId(1L);
        lDao.saveEntity(cr);
    }

    private ErpMdSubject findSubject(String code) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpFinVoucherLine> linesOf(Long voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    private ErpFinVoucherLine lineOfSubject(List<ErpFinVoucherLine> lines, String subjectCode) {
        return lines.stream().filter(l -> subjectCode.equals(l.getSubjectCode())).findFirst().orElseThrow();
    }
}
