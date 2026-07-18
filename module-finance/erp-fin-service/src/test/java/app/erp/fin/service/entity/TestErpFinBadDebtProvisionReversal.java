package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBadDebtBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.dto.BadDebtProvisionReversalResult;
import app.erp.fin.dao.dto.BadDebtProvisionResult;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.baddebt.BadDebtProvisionService;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 坏账准备反向红冲闭环集成测试（plan 2026-07-18-2251-2 Phase 3）。
 *
 * <p>验证 {@link IErpFinBadDebtBiz#reverseBadDebtProvision} 反向指定期间全部 BAD_DEBT_RESERVE/RELEASE 凭证：
 * <ul>
 *   <li>正路径单凭证：{@code runBadDebtProvision} 产 1 张 BAD_DEBT_RESERVE → {@code reverseBadDebtProvision}
 *       → 原凭证 {@code isReversed=true} + 红字凭证行同向取负（Dr 6701=-X / Cr 1231=-X）+
 *       Allowance 余额回退至反向前 + reversedReserveCount=1</li>
 *   <li>多凭证累积反向：{@code runBadDebtProvision} 两次产 2 张 BDR 凭证 → {@code reverseBadDebtProvision}
 *       → 两张全 {@code isReversed=true} + 红字凭证 2 张 + reversedReserveCount=2（参 Phase 1 Decision (a) 反向全部）</li>
 *   <li>守卫未找到凭证：无计提记录的期间调用抛 {@code ERR_BAD_DEBT_PROVISION_NOT_FOUND}</li>
 *   <li>守卫 CLOSED_FINAL 期间：置 period.status=CLOSED_FINAL 调用抛
 *       {@code ERR_BAD_DEBT_PROVISION_PERIOD_FINAL_CLOSED}</li>
 *   <li>混合反向：同期间产 1 BDR + 1 BDL → {@code reverseBadDebtProvision} → 两张全红冲</li>
 * </ul>
 *
 * <p>对称 {@code TestErpFinBadDebt} 正向计提，本测试覆盖反向闭环（{@code bad-debt.md §步骤2b 反向红冲}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:bad-debt-test.yaml")
public class TestErpFinBadDebtProvisionReversal extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBadDebtBiz badDebtBiz;
    @Inject
    BadDebtProvisionService provisionService;

    @Test
    public void testReverseSingleReserveVoucherReversesAndRollsBackAllowance() {
        LocalDate asOf = LocalDate.of(2025, 1, 31);
        Long[] holder = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2025-01", 2025, 1, LocalDate.of(2025, 1, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("6701", "信用减值损失", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedReceivable("AR-REV-1", pid, asOf.minusDays(10), "1000");
            holder[0] = pid;
        });

        BigDecimal allowanceBefore = ormTemplate.runInSession(session -> provisionService.getAllowanceBalance());
        assertEquals(0, allowanceBefore.compareTo(BigDecimal.ZERO), "前置：Allowance=0");

        BadDebtProvisionResult provision = ormTemplate.runInSession(session ->
                badDebtBiz.runBadDebtProvision(holder[0], CTX));
        assertEquals("RESERVE", provision.getAction(), "前置：补提");
        BigDecimal provisionAmount = provision.getRequiredProvision();
        Long originalVoucherId = provision.getVoucherId();
        assertNotNull(originalVoucherId, "前置：BDR 凭证已生成");

        BigDecimal allowanceAfterProvision = ormTemplate.runInSession(session -> provisionService.getAllowanceBalance());
        assertEquals(0, allowanceAfterProvision.compareTo(provisionAmount),
                "前置：Allowance=provisionAmount=" + provisionAmount);

        BadDebtProvisionReversalResult result = ormTemplate.runInSession(session ->
                badDebtBiz.reverseBadDebtProvision(holder[0], CTX));

        assertEquals(1, result.getReversedReserveCount(), "reversedReserveCount=1");
        assertEquals(0, result.getReversedReleaseCount(), "reversedReleaseCount=0");
        assertEquals(0, provisionAmount.compareTo(result.getReversedReserveAmount()),
                "reversedReserveAmount=provisionAmount");
        assertEquals("2025-01", result.getPeriodCode(), "periodCode 一致");

        ErpFinVoucher original = daoProvider.daoFor(ErpFinVoucher.class).requireEntityById(originalVoucherId);
        assertEquals(Boolean.TRUE, original.getIsReversed(), "原 BDR 凭证 isReversed=true");

        List<ErpFinVoucherLine> reversalLines = findReversalVoucherLines(
                ErpFinConstants.BAD_DEBT_RESERVE_BILL_CODE_PREFIX + "2025-01",
                ErpFinBusinessType.BAD_DEBT_RESERVE);
        assertEquals(2, reversalLines.size(), "红字凭证行数=2");
        ErpFinVoucherLine dr = lineOfSubject(reversalLines, "6701");
        assertEquals(ErpFinConstants.DC_DEBIT, dr.getDcDirection(), "红字 Dr 6701 方向不变 DEBIT");
        assertTrue(dr.getDebitAmount().signum() < 0, "红字 Dr 6701 金额取负");
        ErpFinVoucherLine cr = lineOfSubject(reversalLines, "1231");
        assertEquals(ErpFinConstants.DC_CREDIT, cr.getDcDirection(), "红字 Cr 1231 方向不变 CREDIT");
        assertTrue(cr.getCreditAmount().signum() < 0, "红字 Cr 1231 金额取负");

        BigDecimal allowanceAfterReverse = ormTemplate.runInSession(session -> provisionService.getAllowanceBalance());
        assertEquals(0, allowanceAfterReverse.compareTo(BigDecimal.ZERO),
                "Allowance 回退至反向前（0）");

        output("1_reversal_result.json5", reversalResultState(result));
        output("2_reversal_voucher_lines.json5",
                reversalLines.stream().map(this::voucherLineState).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testReverseAllWhenMultipleVouchersAccumulated() {
        LocalDate asOf = LocalDate.of(2025, 2, 28);
        Long[] holder = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2025-02", 2025, 2, LocalDate.of(2025, 2, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("6701", "信用减值损失", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedReceivable("AR-MULTI-1", pid, asOf.minusDays(10), "1000");
            holder[0] = pid;
        });

        ormTemplate.runInSession(session -> badDebtBiz.runBadDebtProvision(holder[0], CTX));
        // 第二次 runBadDebtProvision：CloseVoucherWriter 无幂等，累积第二张 BDR 凭证（必需 = Allowance，理论上应 action=NONE，
        // 但实测若两次期间状态变更（首次写凭证后 Allowance 升至 required），第二次会 action=NONE；故通过直接 seed 一张额外的
        // NORMAL BDR 凭证模拟"多次累积"场景，对齐 CloseVoucherWriter 无幂等的缺陷面）。
        seedExtraReserveVoucher(holder[0], asOf, new BigDecimal("100"));

        List<ErpFinVoucher> bdrVouchers = ormTemplate.runInSession(session ->
                provisionService.findUnreversedProvisionVouchers(
                        ErpFinConstants.BAD_DEBT_RESERVE_BILL_CODE_PREFIX + "2025-02",
                        ErpFinBusinessType.BAD_DEBT_RESERVE));
        assertEquals(2, bdrVouchers.size(), "前置：2 张 BDR 凭证累积");

        BadDebtProvisionReversalResult result = ormTemplate.runInSession(session ->
                badDebtBiz.reverseBadDebtProvision(holder[0], CTX));

        assertEquals(2, result.getReversedReserveCount(), "reversedReserveCount=2（反向全部累积）");
        assertEquals(0, new BigDecimal("105").compareTo(result.getReversedReserveAmount()),
                "reversedReserveAmount=两次 BDR 金额合计 5+100=105");

        for (ErpFinVoucher v : bdrVouchers) {
            ErpFinVoucher refreshed = daoProvider.daoFor(ErpFinVoucher.class).requireEntityById(v.getId());
            assertEquals(Boolean.TRUE, refreshed.getIsReversed(),
                    "累积的每张 BDR 凭证 isReversed=true: " + v.getCode());
        }
        output("1_multi_reversal_result.json5", reversalResultState(result));
    }

    @Test
    public void testGuardNoProvisionVoucherRejects() {
        LocalDate asOf = LocalDate.of(2025, 3, 31);
        Long[] holder = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2025-03", 2025, 3, LocalDate.of(2025, 3, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("6701", "信用减值损失", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            holder[0] = pid;
        });

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> badDebtBiz.reverseBadDebtProvision(holder[0], CTX)));
        assertEquals(ErpFinErrors.ERR_BAD_DEBT_PROVISION_NOT_FOUND.getErrorCode(), ex.getErrorCode(),
                "未找到 BDR/BDL 凭证：ERR_BAD_DEBT_PROVISION_NOT_FOUND");
    }

    @Test
    public void testGuardPeriodFinalClosedRejects() {
        LocalDate asOf = LocalDate.of(2025, 4, 30);
        Long[] holder = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2025-04", 2025, 4, LocalDate.of(2025, 4, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("6701", "信用减值损失", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedReceivable("AR-GATE-1", pid, asOf.minusDays(10), "1000");
            holder[0] = pid;
        });

        ormTemplate.runInSession(session -> badDebtBiz.runBadDebtProvision(holder[0], CTX));

        // 置 period.status=CLOSED_FINAL
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
            ErpFinAccountingPeriod p = dao.getEntityById(holder[0]);
            p.setStatus(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL);
            dao.updateEntity(p);
        });

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> badDebtBiz.reverseBadDebtProvision(holder[0], CTX)));
        assertEquals(ErpFinErrors.ERR_BAD_DEBT_PROVISION_PERIOD_FINAL_CLOSED.getErrorCode(), ex.getErrorCode(),
                "CLOSED_FINAL 期间：ERR_BAD_DEBT_PROVISION_PERIOD_FINAL_CLOSED");
    }

    @Test
    public void testReverseMixedReserveAndRelease() {
        LocalDate asOf = LocalDate.of(2025, 5, 31);
        Long[] holder = new Long[1];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriod("2025-05", 2025, 5, LocalDate.of(2025, 5, 1), asOf);
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("6701", "信用减值损失", ErpFinConstants.SUBJECT_CLASS_EXPENSE, ErpFinConstants.DC_DEBIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            holder[0] = pid;
        });

        // 直接 seed 1 张 BDR + 1 张 BDL 模拟混合反向场景
        seedExtraReserveVoucher(holder[0], asOf, new BigDecimal("200"));
        seedExtraReleaseVoucher(holder[0], asOf, new BigDecimal("150"));

        BadDebtProvisionReversalResult result = ormTemplate.runInSession(session ->
                badDebtBiz.reverseBadDebtProvision(holder[0], CTX));

        assertEquals(1, result.getReversedReserveCount(), "reversedReserveCount=1");
        assertEquals(1, result.getReversedReleaseCount(), "reversedReleaseCount=1");
        assertEquals(2, result.getTotalReversedCount(), "totalReversedCount=2");
        assertEquals(0, new BigDecimal("200").compareTo(result.getReversedReserveAmount()),
                "reversedReserveAmount=200");
        assertEquals(0, new BigDecimal("150").compareTo(result.getReversedReleaseAmount()),
                "reversedReleaseAmount=150");
        output("1_mixed_reversal_result.json5", reversalResultState(result));
    }

    // ---------- helpers ----------

    private java.util.Map<String, Object> reversalResultState(BadDebtProvisionReversalResult r) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("periodId", r.getPeriodId());
        m.put("periodCode", r.getPeriodCode());
        m.put("reversedReserveCount", r.getReversedReserveCount());
        m.put("reversedReleaseCount", r.getReversedReleaseCount());
        m.put("reversedReserveAmount", r.getReversedReserveAmount());
        m.put("reversedReleaseAmount", r.getReversedReleaseAmount());
        m.put("totalReversedCount", r.getTotalReversedCount());
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

    private void seedReceivable(String code, Long periodId, LocalDate businessDate, String amount) {
        BigDecimal amt = new BigDecimal(amount);
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
        item.setAmountSource(amt);
        item.setAmountFunctional(amt);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(amt);
        item.setOpenAmountFunctional(amt);
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        item.setPeriodId(periodId);
        dao.saveEntity(item);
    }

    /**
     * 直接持久化一张额外的 BAD_DEBT_RESERVE 凭证 + 业财回链（模拟 {@code CloseVoucherWriter} 无幂等检查的多次累积缺陷面，
     * 参 Phase 1 Decision (a) 残留风险注释）。billHeadCode = {@code BAD_DEBT_RESERVE_BILL_CODE_PREFIX + period.code}
     * 完全匹配（与 {@code BadDebtProvisionService.runBadDebtProvisionForSchema:98-102} 写入逻辑一致）。
     */
    private void seedExtraReserveVoucher(Long periodId, LocalDate voucherDate, BigDecimal amount) {
        ormTemplate.flushSession();
        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        ErpMdSubject expense = findSubject("6701");
        ErpMdSubject allowance = findSubject("1231");
        List<LineSeed> lines = java.util.Arrays.asList(
                new LineSeed(expense.getId(), expense.getCode(), expense.getName(),
                        ErpFinConstants.DC_DEBIT, amount),
                new LineSeed(allowance.getId(), allowance.getCode(), allowance.getName(),
                        ErpFinConstants.DC_CREDIT, amount));
        seedProvisionVoucher(periodId, period.getCode(), voucherDate, lines, amount,
                ErpFinConstants.BAD_DEBT_RESERVE_BILL_CODE_PREFIX + period.getCode(),
                ErpFinBusinessType.BAD_DEBT_RESERVE);
    }

    private void seedExtraReleaseVoucher(Long periodId, LocalDate voucherDate, BigDecimal amount) {
        ormTemplate.flushSession();
        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        ErpMdSubject expense = findSubject("6701");
        ErpMdSubject allowance = findSubject("1231");
        List<LineSeed> lines = java.util.Arrays.asList(
                new LineSeed(allowance.getId(), allowance.getCode(), allowance.getName(),
                        ErpFinConstants.DC_DEBIT, amount),
                new LineSeed(expense.getId(), expense.getCode(), expense.getName(),
                        ErpFinConstants.DC_CREDIT, amount));
        seedProvisionVoucher(periodId, period.getCode(), voucherDate, lines, amount,
                ErpFinConstants.BAD_DEBT_RELEASE_BILL_CODE_PREFIX + period.getCode(),
                ErpFinBusinessType.BAD_DEBT_RELEASE);
    }

    private void seedProvisionVoucher(Long periodId, String periodCode, LocalDate voucherDate,
                                       List<LineSeed> lines, BigDecimal amount,
                                       String billHeadCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        IEntityDao<ErpFinVoucherBillR> rDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode("SEED-" + businessType.name() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        v.setVoucherType("TRANSFER");
        v.setPostingType(ErpFinConstants.POSTING_TYPE_NORMAL);
        v.setVoucherDate(voucherDate);
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(amount);
        v.setTotalCredit(amount);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);
        int lineNo = 1;
        for (LineSeed ls : lines) {
            ErpFinVoucherLine line = new ErpFinVoucherLine();
            line.setVoucherId(v.getId());
            line.setLineNo(lineNo++);
            line.setSubjectId(ls.subjectId);
            line.setSubjectCode(ls.subjectCode);
            line.setSubjectName(ls.subjectName);
            line.setDcDirection(ls.dcDirection);
            boolean isCredit = ErpFinConstants.DC_CREDIT.equals(ls.dcDirection);
            line.setDebitAmount(isCredit ? BigDecimal.ZERO : ls.amount);
            line.setCreditAmount(isCredit ? ls.amount : BigDecimal.ZERO);
            line.setCurrencyId(1L);
            line.setExchangeRate(BigDecimal.ONE);
            line.setAmountSource(ls.amount);
            line.setAmountFunctional(ls.amount);
            line.setAcctSchemaId(1L);
            line.setBusinessType(businessType.name());
            lDao.saveEntity(line);
        }
        ErpFinVoucherBillR billR = new ErpFinVoucherBillR();
        billR.setVoucherId(v.getId());
        billR.setBillType(businessType.name());
        billR.setBillCode(billHeadCode);
        billR.setBusinessType(businessType.name());
        rDao.saveEntity(billR);
    }

    private ErpMdSubject findSubject(String code) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpFinVoucherLine> findReversalVoucherLines(String billHeadCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> linkDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType.name())));
        List<ErpFinVoucherBillR> links = linkDao.findAllByQuery(q);
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = voucherDao.getEntityById(link.getVoucherId());
            if (v != null && ErpFinConstants.POSTING_TYPE_REVERSAL.equals(v.getPostingType())) {
                QueryBean lq = new QueryBean();
                lq.addFilter(eq("voucherId", v.getId()));
                return lineDao.findAllByQuery(lq);
            }
        }
        return java.util.Collections.emptyList();
    }

    private ErpFinVoucherLine lineOfSubject(List<ErpFinVoucherLine> lines, String subjectCode) {
        return lines.stream()
                .filter(l -> subjectCode.equals(l.getSubjectCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("预期科目行不存在: " + subjectCode));
    }

    private static final class LineSeed {
        final Long subjectId;
        final String subjectCode;
        final String subjectName;
        final String dcDirection;
        final BigDecimal amount;

        LineSeed(Long subjectId, String subjectCode, String subjectName, String dcDirection, BigDecimal amount) {
            this.subjectId = subjectId;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.dcDirection = dcDirection;
            this.amount = amount;
        }
    }
}
