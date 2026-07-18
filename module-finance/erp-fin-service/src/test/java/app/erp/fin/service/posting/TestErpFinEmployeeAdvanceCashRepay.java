package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinEmployeeAdvanceBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.provider.EmployeeAdvanceAcctDocProvider;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.like;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 员工借款现金还款（{@code EMPLOYEE_ADVANCE_SETTLE} 现金还款路径）端到端集成测试（plan 2026-07-18-0718-2，Phase 2）。
 *
 * <p>覆盖 {@link IErpFinEmployeeAdvanceBiz#cashRepay}：
 * <ul>
 *   <li>正路径全额还款 + 字段翻转 + 「已结清」派生投影（docStatus 保持 DRAFT）+ 凭证行 Dr 1002 / Cr 1221</li>
 *   <li>部分还款 + 字段部分翻转 + docStatus 不变</li>
 *   <li>未过账守卫（ERR_EMPLOYEE_ADVANCE_NOT_REPAYABLE + 无凭证）</li>
 *   <li>超额守卫（ERR_EMPLOYEE_ADVANCE_CASH_REPAY_EXCEEDS_OUTSTANDING + 无凭证）</li>
 *   <li>金额非法守卫（ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID + 无凭证）</li>
 *   <li>Provider SETTLE_TYPE 分派单元测试（CASH/OFFSET/null 三态 + 非支持类型空列表）</li>
 * </ul>
 *
 * <p>owner doc（{@code docs/design/finance/expense-claim.md §现金还款}）：现金还款用 {@code ErpFinVoucher} 凭证承载，
 * Dr 银行存款 / Cr 其他应收款-员工预支。docStatus 不引入 SETTLED 字典值，outstandingAmount=0 由派生投影表达「已结清」。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinEmployeeAdvanceCashRepay extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;
    static final String POSTING_TYPE_NORMAL = ErpFinConstants.POSTING_TYPE_NORMAL;
    static final String VOUCHER_STATUS_POSTED = ErpFinConstants.VOUCHER_STATUS_POSTED;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinEmployeeAdvanceBiz advanceBiz;

    @Test
    public void testCashRepayFullAmountSettlesAndGeneratesVoucher() {
        long partnerId = 8810L;
        Long advanceId = ormTemplate.runInSession(session -> {
            seedCurrentMonthOpenPeriod();
            seedAcctSchema(1L);
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedPostedAdvance("ADV-CASH-001", empId, new BigDecimal("500"));
        });

        ErpFinEmployeeAdvance result = ormTemplate.runInSession(session ->
                advanceBiz.cashRepay(advanceId, new BigDecimal("500"), CTX));

        assertNotNull(result, "cashRepay 应返回更新后的 advance");

        ErpFinEmployeeAdvance advance = fetchAdvance(advanceId);
        assertEquals(0, new BigDecimal("500").compareTo(advance.getSettledAmount()),
                "全额还款后 settledAmount = 500");
        assertEquals(0, BigDecimal.ZERO.compareTo(advance.getOutstandingAmount()),
                "全额还款后 outstandingAmount = 0（已结清派生投影）");
        assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, advance.getApproveStatus(),
                "approveStatus 保持 APPROVED 不变");
        assertEquals(ErpFinConstants.DOC_STATUS_DRAFT, advance.getDocStatus(),
                "docStatus 保持 DRAFT 不变（plan Decision (c)：不引入 SETTLED 字典值，派生投影表达已结清）");

        Long voucherId = findCashRepayVoucherId("ADV-CASH-001");
        assertNotNull(voucherId, "应生成 EMPLOYEE_ADVANCE_SETTLE 现金还款凭证");

        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).requireEntityById(voucherId);
        assertEquals(POSTING_TYPE_NORMAL, voucher.getPostingType(), "凭证 postingType=NORMAL");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证 docStatus=POSTED");

        List<ErpFinVoucherLine> lines = linesOf(voucherId);
        ErpFinVoucherLine dr = lineOfSubject(lines, "1002");
        assertEquals(DC_DEBIT, dr.getDcDirection(), "Dr 1002 银行存款");
        assertEquals(0, new BigDecimal("500").compareTo(dr.getDebitAmount()), "借方金额 500");
        ErpFinVoucherLine cr = lineOfSubject(lines, "1221");
        assertEquals(DC_CREDIT, cr.getDcDirection(), "Cr 1221 其他应收款-员工预支");
        assertEquals(0, new BigDecimal("500").compareTo(cr.getCreditAmount()), "贷方金额 500");
    }

    @Test
    public void testCashRepayPartialAmountUpdatesFields() {
        long partnerId = 8811L;
        Long advanceId = ormTemplate.runInSession(session -> {
            seedCurrentMonthOpenPeriod();
            seedAcctSchema(1L);
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedPostedAdvance("ADV-CASH-002", empId, new BigDecimal("500"));
        });

        ErpFinEmployeeAdvance result = ormTemplate.runInSession(session ->
                advanceBiz.cashRepay(advanceId, new BigDecimal("200"), CTX));

        assertNotNull(result, "cashRepay 部分还款应返回更新后的 advance");

        ErpFinEmployeeAdvance advance = fetchAdvance(advanceId);
        assertEquals(0, new BigDecimal("200").compareTo(advance.getSettledAmount()),
                "部分还款后 settledAmount = 200");
        assertEquals(0, new BigDecimal("300").compareTo(advance.getOutstandingAmount()),
                "部分还款后 outstandingAmount = 300");
        assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, advance.getApproveStatus(),
                "approveStatus 保持 APPROVED 不变");

        Long voucherId = findCashRepayVoucherId("ADV-CASH-002");
        assertNotNull(voucherId, "部分还款也应生成凭证");

        List<ErpFinVoucherLine> lines = linesOf(voucherId);
        ErpFinVoucherLine dr = lineOfSubject(lines, "1002");
        assertEquals(0, new BigDecimal("200").compareTo(dr.getDebitAmount()), "借方金额 200");
        ErpFinVoucherLine cr = lineOfSubject(lines, "1221");
        assertEquals(0, new BigDecimal("200").compareTo(cr.getCreditAmount()), "贷方金额 200");
    }

    @Test
    public void testGuardNotPostedRejects() {
        long partnerId = 8812L;
        Long advanceId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-08", 2026, 8, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
            seedAcctSchema(1L);
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedAdvanceWithState("ADV-CASH-003", empId, new BigDecimal("500"),
                    ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, false,
                    BigDecimal.ZERO, new BigDecimal("500"));
        });

        NopException ex = assertThrows(NopException.class, () -> ormTemplate.runInSession(session ->
                        advanceBiz.cashRepay(advanceId, new BigDecimal("200"), CTX)),
                "未过账 advance 不可现金还款：ERR_EMPLOYEE_ADVANCE_NOT_REPAYABLE");
        assertEquals(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_NOT_REPAYABLE.getErrorCode(), ex.getErrorCode(),
                "错误码匹配");

        assertNull(findCashRepayVoucherId("ADV-CASH-003"), "守卫拒绝时无凭证生成");
        ErpFinEmployeeAdvance advance = fetchAdvance(advanceId);
        assertEquals(0, BigDecimal.ZERO.compareTo(advance.getSettledAmount()), "settledAmount 不变");
    }

    @Test
    public void testGuardExceedsOutstandingRejects() {
        long partnerId = 8813L;
        Long advanceId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-08", 2026, 8, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
            seedAcctSchema(1L);
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            // advance outstanding=300，尝试还 500 超额
            return seedAdvanceWithState("ADV-CASH-004", empId, new BigDecimal("500"),
                    ErpFinConstants.APPROVE_STATUS_APPROVED, true,
                    new BigDecimal("200"), new BigDecimal("300"));
        });

        NopException ex = assertThrows(NopException.class, () -> ormTemplate.runInSession(session ->
                        advanceBiz.cashRepay(advanceId, new BigDecimal("500"), CTX)),
                "超额还款：ERR_EMPLOYEE_ADVANCE_CASH_REPAY_EXCEEDS_OUTSTANDING");
        assertEquals(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_CASH_REPAY_EXCEEDS_OUTSTANDING.getErrorCode(), ex.getErrorCode(),
                "错误码匹配");

        assertNull(findCashRepayVoucherId("ADV-CASH-004"), "超额守卫拒绝时无凭证生成");
        ErpFinEmployeeAdvance advance = fetchAdvance(advanceId);
        assertEquals(0, new BigDecimal("200").compareTo(advance.getSettledAmount()), "settledAmount 不变");
        assertEquals(0, new BigDecimal("300").compareTo(advance.getOutstandingAmount()), "outstandingAmount 不变");
    }

    @Test
    public void testGuardAmountInvalidRejects() {
        long partnerId = 8814L;
        Long advanceId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-08", 2026, 8, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
            seedAcctSchema(1L);
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedPostedAdvance("ADV-CASH-005", empId, new BigDecimal("500"));
        });

        // amount = 0
        NopException exZero = assertThrows(NopException.class, () -> ormTemplate.runInSession(session ->
                        advanceBiz.cashRepay(advanceId, BigDecimal.ZERO, CTX)),
                "amount=0：ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID");
        assertEquals(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID.getErrorCode(), exZero.getErrorCode(),
                "错误码匹配（amount=0）");

        // amount = null
        NopException exNull = assertThrows(NopException.class, () -> ormTemplate.runInSession(session ->
                        advanceBiz.cashRepay(advanceId, null, CTX)),
                "amount=null：ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID");
        assertEquals(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID.getErrorCode(), exNull.getErrorCode(),
                "错误码匹配（amount=null）");

        // negative amount
        NopException exNeg = assertThrows(NopException.class, () -> ormTemplate.runInSession(session ->
                        advanceBiz.cashRepay(advanceId, new BigDecimal("-100"), CTX)),
                "负金额：ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID");
        assertEquals(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID.getErrorCode(), exNeg.getErrorCode(),
                "错误码匹配（负金额）");

        assertNull(findCashRepayVoucherId("ADV-CASH-005"), "金额非法守卫拒绝时无凭证生成");
    }

    @Test
    public void testProviderSettleTypeDispatch() {
        EmployeeAdvanceAcctDocProvider provider = new EmployeeAdvanceAcctDocProvider();

        assertTrue(provider.getSupportedBusinessTypes().contains(ErpFinBusinessType.EMPLOYEE_ADVANCE),
                "Provider 支持 EMPLOYEE_ADVANCE");
        assertTrue(provider.getSupportedBusinessTypes().contains(ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE),
                "Provider 支持 EMPLOYEE_ADVANCE_SETTLE");

        BigDecimal amount = new BigDecimal("500");
        Long partnerId = 8815L;

        // (a) SETTLE_TYPE=CASH → Dr 1002 / Cr 1221
        List<VoucherFact> cashFacts = provider.createFacts(buildSettleEvent(amount, partnerId,
                ErpFinConstants.SETTLE_TYPE_CASH), new AcctDocContext());
        assertEquals(2, cashFacts.size(), "CASH 路径返回 2 行分录");
        VoucherFact cashDr = cashFacts.get(0);
        assertEquals("1002", cashDr.getSubjectCode(), "CASH Dr 1002 银行存款");
        assertEquals(DC_DEBIT, cashDr.getDcDirection(), "CASH Dr 方向 DEBIT");
        assertEquals(0, amount.compareTo(cashDr.getAmount()), "CASH Dr 金额");
        VoucherFact cashCr = cashFacts.get(1);
        assertEquals("1221", cashCr.getSubjectCode(), "CASH Cr 1221 其他应收款-员工预支");
        assertEquals(DC_CREDIT, cashCr.getDcDirection(), "CASH Cr 方向 CREDIT");
        assertEquals(partnerId, cashCr.getPartnerId(), "CASH Cr 携带 partnerId");

        // (b) SETTLE_TYPE=OFFSET → Dr 2241 / Cr 1221（既有报销抵扣路径）
        List<VoucherFact> offsetFacts = provider.createFacts(buildSettleEvent(amount, partnerId,
                ErpFinConstants.SETTLE_TYPE_OFFSET), new AcctDocContext());
        assertEquals(2, offsetFacts.size(), "OFFSET 路径返回 2 行分录");
        assertEquals("2241", offsetFacts.get(0).getSubjectCode(), "OFFSET Dr 2241 应付-员工");
        assertEquals(DC_DEBIT, offsetFacts.get(0).getDcDirection(), "OFFSET Dr 方向 DEBIT");
        assertEquals(partnerId, offsetFacts.get(0).getPartnerId(), "OFFSET Dr 携带 partnerId");
        assertEquals("1221", offsetFacts.get(1).getSubjectCode(), "OFFSET Cr 1221 应收-员工预支");
        assertEquals(DC_CREDIT, offsetFacts.get(1).getDcDirection(), "OFFSET Cr 方向 CREDIT");

        // (c) SETTLE_TYPE=null（未设）→ Dr 2241 / Cr 1221（默认 OFFSET 路径，零回归验证）
        List<VoucherFact> nullFacts = provider.createFacts(buildSettleEvent(amount, partnerId, null),
                new AcctDocContext());
        assertEquals(2, nullFacts.size(), "null SETTLE_TYPE 返回 2 行分录（默认 OFFSET）");
        assertEquals("2241", nullFacts.get(0).getSubjectCode(), "null Dr 2241（默认 OFFSET 行为）");
        assertEquals("1221", nullFacts.get(1).getSubjectCode(), "null Cr 1221（默认 OFFSET 行为）");

        // (d) 非支持 businessType → 空列表
        PostingEvent other = new PostingEvent();
        other.setBusinessType(ErpFinBusinessType.AP_INVOICE);
        other.setBillData(new LinkedHashMap<>());
        assertTrue(provider.createFacts(other, new AcctDocContext()).isEmpty(),
                "非支持 businessType 返回空列表");
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
        // docStatus 保持 DRAFT（与 approve 流程一致——Processor 仅推进 approveStatus，不动 docStatus；
        // cashRepay 也不动 docStatus，对齐 plan Decision (c) 派生投影）
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

    /**
     * Seed 当月 OPEN 期间——postCashRepay 的 voucherDate=CoreMetrics.today()（实际日期），
     * 过账引擎 resolveOpenPeriod 按 voucherDate 查找期间；须覆盖今天否则过账失败。
     */
    private void seedCurrentMonthOpenPeriod() {
        LocalDate today = io.nop.api.core.time.CoreMetrics.today();
        int year = today.getYear();
        int month = today.getMonthValue();
        String code = String.format("%04d-%02d", year, month);
        seedOpenPeriod(code, year, month, today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
    }

    // ---------- query helpers ----------

    private PostingEvent buildSettleEvent(BigDecimal amount, Long partnerId, String settleType) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE);
        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put("TOTAL", amount);
        billData.put(ErpFinConstants.BILL_DATA_EMPLOYEE_ID, partnerId);
        if (settleType != null) {
            billData.put(ErpFinConstants.BILL_DATA_SETTLE_TYPE, settleType);
        }
        event.setBillData(billData);
        return event;
    }

    private ErpFinEmployeeAdvance fetchAdvance(Long id) {
        return daoProvider.daoFor(ErpFinEmployeeAdvance.class).getEntityById(id);
    }

    private Long findCashRepayVoucherId(String advanceCode) {
        IEntityDao<ErpFinVoucherBillR> linkDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                like("billCode", "EA-CASH-REPAY-" + advanceCode + "-%"),
                eq("businessType", ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE.name())
        ));
        q.setLimit(1);
        List<ErpFinVoucherBillR> links = linkDao.findAllByQuery(q);
        return links.isEmpty() ? null : links.get(0).getVoucherId();
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
}
