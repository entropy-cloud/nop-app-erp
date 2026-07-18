package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinEmployeeAdvanceBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdSubject;
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
import static io.nop.api.core.beans.FilterBeans.like;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 员工借款 reverseCashRepay 红冲闭环集成测试（plan 2026-07-18-1745-3 Phase 3）。
 *
 * <p>验证 {@link IErpFinEmployeeAdvanceBiz#reverseCashRepay} 反向现金还款：
 * <ul>
 *   <li>正路径：cashRepay(500) → reverseCashRepay → 原 EMPLOYEE_ADVANCE_SETTLE(CASH) 凭证 isReversed=true +
 *       红字凭证行同向取负（Dr 1002=-500/Cr 1221=-500）+ advance 字段回退（settled=0, outstanding=500）</li>
 *   <li>多次 cashRepay 后 reverseCashRepay 仅撤销最近一笔（不影响其他笔）</li>
 *   <li>守卫：无 cashRepay NORMAL 凭证可红冲抛 ERR_EMPLOYEE_ADVANCE_CASH_REPAY_VOUCHER_NOT_FOUND</li>
 * </ul>
 *
 * <p>兑现 owner doc {@code expense-claim.md §红冲联动} cashRepay 路径承诺（plan 1745-3 修复 owner-doc 漂移）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinEmployeeAdvanceCashRepayReversal extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinEmployeeAdvanceBiz advanceBiz;

    @Test
    public void testReverseCashRepayRedReversesVoucherAndRollsBackFields() {
        long partnerId = 8820L;
        Long advanceId = ormTemplate.runInSession(session -> {
            seedCurrentMonthOpenPeriod();
            seedAcctSchema(1L);
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedPostedAdvance("ADV-REV-001", empId, new BigDecimal("500"));
        });

        // 前置：cashRepay 500
        ormTemplate.runInSession(session -> advanceBiz.cashRepay(advanceId, new BigDecimal("500"), CTX));

        ErpFinEmployeeAdvance before = fetchAdvance(advanceId);
        assertEquals(0, new BigDecimal("500").compareTo(before.getSettledAmount()), "前置：settled=500");
        assertEquals(0, BigDecimal.ZERO.compareTo(before.getOutstandingAmount()), "前置：outstanding=0");

        Long originalVoucherId = findCashRepayVoucherId("ADV-REV-001", false);
        assertNotNull(originalVoucherId, "前置：cashRepay 凭证已生成");

        output("1_before_reverse_advance.json5", advanceState(before));

        // 反向现金还款
        ErpFinEmployeeAdvance reversed = ormTemplate.runInSession(session ->
                advanceBiz.reverseCashRepay(advanceId, CTX));

        // 字段回退：settled=0, outstanding=500
        assertEquals(0, BigDecimal.ZERO.compareTo(reversed.getSettledAmount()), "reverseCashRepay 后 settled=0");
        assertEquals(0, new BigDecimal("500").compareTo(reversed.getOutstandingAmount()),
                "reverseCashRepay 后 outstanding=500");

        ErpFinEmployeeAdvance after = fetchAdvance(advanceId);
        assertEquals(0, BigDecimal.ZERO.compareTo(after.getSettledAmount()), "DB settled=0");
        assertEquals(0, new BigDecimal("500").compareTo(after.getOutstandingAmount()), "DB outstanding=500");
        output("2_after_reverse_advance.json5", advanceState(after));

        // 原凭证 isReversed=true
        ErpFinVoucher originalVoucher = daoProvider.daoFor(ErpFinVoucher.class).requireEntityById(originalVoucherId);
        assertEquals(Boolean.TRUE, originalVoucher.getIsReversed(), "原 cashRepay 凭证 isReversed=true");

        // 红字凭证存在 + 同向取负（Dr 1002=-500 / Cr 1221=-500）
        Long reversalVoucherId = findCashRepayVoucherId("ADV-REV-001", true);
        assertNotNull(reversalVoucherId, "红字凭证应生成");
        assertNotEquals(originalVoucherId, reversalVoucherId, "红字凭证 id ≠ 原凭证 id");

        List<ErpFinVoucherLine> reversalLines = linesOf(reversalVoucherId);
        ErpFinVoucherLine dr = lineOfSubject(reversalLines, "1002");
        assertEquals(ErpFinConstants.DC_DEBIT, dr.getDcDirection(), "红字 Dr 1002 方向不变 DEBIT");
        assertEquals(0, new BigDecimal("-500").compareTo(dr.getDebitAmount()), "红字 Dr 1002 金额取负 -500");
        ErpFinVoucherLine cr = lineOfSubject(reversalLines, "1221");
        assertEquals(ErpFinConstants.DC_CREDIT, cr.getDcDirection(), "红字 Cr 1221 方向不变 CREDIT");
        assertEquals(0, new BigDecimal("-500").compareTo(cr.getCreditAmount()), "红字 Cr 1221 金额取负 -500");
        output("3_reversal_voucher_lines.json5",
                reversalLines.stream().map(this::voucherLineState).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testReverseCashRepayOnlyReversesLatestWhenMultiple() {
        long partnerId = 8821L;
        Long advanceId = ormTemplate.runInSession(session -> {
            seedCurrentMonthOpenPeriod();
            seedAcctSchema(1L);
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedPostedAdvance("ADV-REV-002", empId, new BigDecimal("500"));
        });

        // 两次 cashRepay：200 + 300（outstanding 从 500→300→0）
        ormTemplate.runInSession(session -> advanceBiz.cashRepay(advanceId, new BigDecimal("200"), CTX));
        ormTemplate.runInSession(session -> advanceBiz.cashRepay(advanceId, new BigDecimal("300"), CTX));

        ErpFinEmployeeAdvance before = fetchAdvance(advanceId);
        assertEquals(0, new BigDecimal("500").compareTo(before.getSettledAmount()), "前置：settled=500");
        assertEquals(0, BigDecimal.ZERO.compareTo(before.getOutstandingAmount()), "前置：outstanding=0");

        // 反向应仅撤销最近一笔 300，使 settled=200, outstanding=300
        ErpFinEmployeeAdvance reversed = ormTemplate.runInSession(session ->
                advanceBiz.reverseCashRepay(advanceId, CTX));

        assertEquals(0, new BigDecimal("200").compareTo(reversed.getSettledAmount()),
                "reverseCashRepay 后 settled=200（仅撤销最近 300）");
        assertEquals(0, new BigDecimal("300").compareTo(reversed.getOutstandingAmount()),
                "reverseCashRepay 后 outstanding=300");
        output("1_after_reverse_latest.json5", advanceState(reversed));
    }

    @Test
    public void testGuardNoCashRepayVoucherRejects() {
        long partnerId = 8822L;
        Long advanceId = ormTemplate.runInSession(session -> {
            seedCurrentMonthOpenPeriod();
            seedAcctSchema(1L);
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedPostedAdvance("ADV-REV-003", empId, new BigDecimal("500"));
        });

        // 已过账 advance 但无 cashRepay 凭证 → 守卫拒绝
        NopException ex = assertThrows(NopException.class, () -> ormTemplate.runInSession(session ->
                        advanceBiz.reverseCashRepay(advanceId, CTX)),
                "无 cashRepay 凭证可红冲：ERR_EMPLOYEE_ADVANCE_CASH_REPAY_VOUCHER_NOT_FOUND");
        assertEquals(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_CASH_REPAY_VOUCHER_NOT_FOUND.getErrorCode(),
                ex.getErrorCode(), "错误码匹配");

        // advance 字段不变
        ErpFinEmployeeAdvance after = fetchAdvance(advanceId);
        assertEquals(0, BigDecimal.ZERO.compareTo(after.getSettledAmount()), "settled 不变");
        assertEquals(0, new BigDecimal("500").compareTo(after.getOutstandingAmount()), "outstanding 不变");
    }

    // ---------- seed helpers ----------

    private Long seedPostedAdvance(String code, Long employeeId, BigDecimal amount) {
        return seedAdvanceWithState(code, employeeId, amount,
                ErpFinConstants.APPROVE_STATUS_APPROVED, true,
                BigDecimal.ZERO, amount);
    }

    private Long seedAdvanceWithState(String code, Long employeeId, BigDecimal amount,
                                       String approveStatus, boolean posted,
                                       BigDecimal settledAmount, BigDecimal outstandingAmount) {
        IEntityDao<ErpFinEmployeeAdvance> dao = daoProvider.daoFor(ErpFinEmployeeAdvance.class);
        ErpFinEmployeeAdvance advance = new ErpFinEmployeeAdvance();
        advance.setCode(code);
        advance.setOrgId(1L);
        advance.setEmployeeId(employeeId);
        advance.setAdvanceType("EXPENSE_ADVANCE");
        advance.setBusinessDate(LocalDate.of(2026, 8, 10));
        advance.setCurrencyId(1L);
        advance.setExchangeRate(BigDecimal.ONE);
        advance.setAmountFunctional(amount);
        advance.setAmountSource(amount);
        advance.setSettledAmount(settledAmount);
        advance.setOutstandingAmount(outstandingAmount);
        advance.setDocStatus(ErpFinConstants.DOC_STATUS_DRAFT);
        advance.setApproveStatus(approveStatus);
        advance.setPosted(posted);
        dao.saveEntity(advance);
        return advance.getId();
    }

    private Long seedEmployee(long partnerId, String status) {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee emp = new ErpMdEmployee();
        emp.setCode("E-" + partnerId);
        emp.setName("员工-" + partnerId);
        emp.setOrgId(1L);
        emp.setPartnerId(partnerId);
        emp.setStatus(status);
        dao.saveEntity(emp);
        return emp.getId();
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

    private void seedCurrentMonthOpenPeriod() {
        LocalDate today = io.nop.api.core.time.CoreMetrics.today();
        int year = today.getYear();
        int month = today.getMonthValue();
        String code = String.format("%04d-%02d", year, month);
        seedOpenPeriod(code, year, month, today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
    }

    // ---------- query helpers ----------

    private ErpFinEmployeeAdvance fetchAdvance(Long id) {
        return daoProvider.daoFor(ErpFinEmployeeAdvance.class).getEntityById(id);
    }

    /**
     * 经 ErpFinVoucherBillR 反查 cashRepay 凭证 id（按 advanceCode 前缀 + postingType 过滤）。
     * @param reversal true=查红字凭证；false=查原 NORMAL 凭证
     */
    private Long findCashRepayVoucherId(String advanceCode, boolean reversal) {
        IEntityDao<ErpFinVoucherBillR> linkDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                like("billCode", "EA-CASH-REPAY-" + advanceCode + "-%"),
                eq("businessType", ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE.name())
        ));
        List<ErpFinVoucherBillR> links = linkDao.findAllByQuery(q);
        String expectedPostingType = reversal
                ? ErpFinConstants.POSTING_TYPE_REVERSAL
                : ErpFinConstants.POSTING_TYPE_NORMAL;
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
            if (v != null && expectedPostingType.equals(v.getPostingType())) {
                return v.getId();
            }
        }
        return null;
    }

    private List<ErpFinVoucherLine> linesOf(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q);
    }

    private ErpFinVoucherLine lineOfSubject(List<ErpFinVoucherLine> lines, String subjectCode) {
        return lines.stream()
                .filter(l -> subjectCode.equals(l.getSubjectCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("预期科目行不存在: " + subjectCode));
    }

    private java.util.Map<String, Object> advanceState(ErpFinEmployeeAdvance a) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("code", a.getCode());
        m.put("settledAmount", a.getSettledAmount());
        m.put("outstandingAmount", a.getOutstandingAmount());
        m.put("approveStatus", a.getApproveStatus());
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
}
