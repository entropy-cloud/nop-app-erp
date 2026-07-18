package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBadDebtBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
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

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 坏账 reverseApprove 红冲闭环集成测试（plan 2026-07-18-1745-3 Phase 3）。
 *
 * <p>验证 {@link IErpFinBadDebtBiz#reverseApprove} 反审核已审批通过的坏账单：
 * <ul>
 *   <li>writeOff 反向：原 BAD_DEBT_WRITE_OFF 凭证 isReversed=true + 红字凭证行同向取负 +
 *       ArApItem WRITTEN_OFF→OPEN + settled/open 回退对称 + 坏账单 APPROVED→REJECTED</li>
 *   <li>recovery 反向：原 BAD_DEBT_RECOVERY 凭证 isReversed=true + ArApItem OPEN→WRITTEN_OFF 回退对称</li>
 *   <li>守卫：未过账（voucherId=null）抛 ERR_BAD_DEBT_NOT_APPROVED_OR_NOT_POSTED</li>
 * </ul>
 *
 * <p>对齐 {@code ErpFinBadDebtProcessor.executeWriteOff}/{@code executeRecovery} 的反向语义（plan 1745-3）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:bad-debt-test.yaml")
public class TestErpFinBadDebtReversal extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBadDebtBiz badDebtBiz;

    @Test
    public void testWriteOffReverseApproveRedReversesVoucherAndArApItem() {
        // 反审核触发红冲凭证 voucherDate=今天，故 period 须覆盖今天（参 TestErpFinEmployeeAdvanceCashRepay.seedCurrentMonthOpenPeriod 范式）
        LocalDate today = io.nop.api.core.time.CoreMetrics.today();
        LocalDate asOf = today.minusDays(10);
        Long[] holder = new Long[2];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriodCurrentMonth("2026-07");
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            ErpFinArApItem item = seedReceivable("AR-WO-REV-1", pid, asOf, "500");
            holder[0] = pid;
            holder[1] = item.getId();
        });

        // 前置：writeOff approve 生成 BAD_DEBT_WRITE_OFF 凭证 + ArApItem WRITTEN_OFF（write-off-require-approval=false 自动审批）
        ErpFinBadDebt debt = ormTemplate.runInSession(session -> badDebtBiz.writeOff(holder[1], "客户破产", CTX));
        Long originalVoucherId = debt.getVoucherId();
        assertNotNull(originalVoucherId, "前置：writeOff 生成原凭证");

        ErpFinVoucher originalVoucher = daoProvider.daoFor(ErpFinVoucher.class).requireEntityById(originalVoucherId);
        assertEquals(Boolean.FALSE, originalVoucher.getIsReversed(), "前置：原凭证未红冲");

        ErpFinArApItem afterApprove = daoProvider.daoFor(ErpFinArApItem.class).getEntityById(holder[1]);
        assertEquals(ErpFinConstants.AR_AP_STATUS_WRITTEN_OFF, afterApprove.getStatus(), "前置：ArApItem WRITTEN_OFF");
        assertEquals(0, BigDecimal.ZERO.compareTo(afterApprove.getOpenAmountFunctional()), "前置：openAmount=0");
        output("1_before_reverse_state.json5", beforeReverseState(debt, afterApprove, originalVoucher));

        // 反审核
        ErpFinBadDebt reversed = ormTemplate.runInSession(session -> badDebtBiz.reverseApprove(debt.getId(), CTX));

        assertEquals(ErpFinConstants.APPROVE_STATUS_REJECTED, reversed.getApprovalStatus(),
                "reverseApprove 后 approvalStatus=REJECTED");
        output("2_after_reverse_debt.json5", badDebtState(reversed));

        // 原凭证 isReversed=true
        ErpFinVoucher originalAfter = daoProvider.daoFor(ErpFinVoucher.class).requireEntityById(originalVoucherId);
        assertEquals(Boolean.TRUE, originalAfter.getIsReversed(), "原凭证 isReversed=true");

        // 红字凭证存在 + 同向取负（Dr 1231=-500 / Cr 1122=-500）
        Long reversalVoucherId = findReversalVoucherId(debt.getCode());
        assertNotNull(reversalVoucherId, "红字凭证应生成");
        assertNotEquals(originalVoucherId, reversalVoucherId, "红字凭证 id ≠ 原凭证 id");

        List<ErpFinVoucherLine> reversalLines = linesOf(reversalVoucherId);
        ErpFinVoucherLine dr = lineOfSubject(reversalLines, "1231");
        assertEquals(ErpFinConstants.DC_DEBIT, dr.getDcDirection(), "红字 Dr 1231 方向不变 DEBIT");
        assertEquals(0, new BigDecimal("-500").compareTo(dr.getDebitAmount()), "红字 Dr 1231 金额取负 -500");
        ErpFinVoucherLine cr = lineOfSubject(reversalLines, "1122");
        assertEquals(ErpFinConstants.DC_CREDIT, cr.getDcDirection(), "红字 Cr 1122 方向不变 CREDIT");
        assertEquals(0, new BigDecimal("-500").compareTo(cr.getCreditAmount()), "红字 Cr 1122 金额取负 -500");
        output("3_reversal_voucher_lines.json5",
                reversalLines.stream().map(this::voucherLineState).collect(java.util.stream.Collectors.toList()));

        // ArApItem 回退对称：WRITTEN_OFF → OPEN；settled-=500，open+=500
        ErpFinArApItem afterReverse = daoProvider.daoFor(ErpFinArApItem.class).getEntityById(holder[1]);
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, afterReverse.getStatus(), "ArApItem 回退 OPEN");
        assertEquals(0, new BigDecimal("500").compareTo(afterReverse.getOpenAmountFunctional()),
                "openAmount 恢复 500");
        assertEquals(0, BigDecimal.ZERO.compareTo(afterReverse.getSettledAmountFunctional()),
                "settledAmount 回退 0");
        output("4_after_reverse_ar_ap_item.json5", arApItemState(afterReverse));
    }

    @Test
    public void testRecoveryReverseApproveRedReversesVoucherAndArApItem() {
        LocalDate today = io.nop.api.core.time.CoreMetrics.today();
        LocalDate asOf = today.minusDays(10);
        Long[] holder = new Long[2];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriodCurrentMonth("2026-07-B");
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            ErpFinArApItem item = seedReceivable("AR-RC-REV-1", pid, asOf, "300");
            holder[0] = pid;
            holder[1] = item.getId();
        });

        // 前置：writeOff + recover approve，ArApItem 现为 OPEN（recovery 反向应回到 WRITTEN_OFF）
        ormTemplate.runInSession(session -> badDebtBiz.writeOff(holder[1], "核销", CTX));
        ErpFinBadDebt recovery = ormTemplate.runInSession(session -> badDebtBiz.recover(holder[1], "事后回款", CTX));
        Long originalVoucherId = recovery.getVoucherId();
        assertNotNull(originalVoucherId, "前置：recovery 生成原凭证");

        ErpFinArApItem afterRecover = daoProvider.daoFor(ErpFinArApItem.class).getEntityById(holder[1]);
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, afterRecover.getStatus(), "前置：ArApItem 已恢复 OPEN");
        assertEquals(0, new BigDecimal("300").compareTo(afterRecover.getOpenAmountFunctional()),
                "前置：openAmount=300");

        // 反审核 recovery
        ErpFinBadDebt reversed = ormTemplate.runInSession(session ->
                badDebtBiz.reverseApprove(recovery.getId(), CTX));

        assertEquals(ErpFinConstants.APPROVE_STATUS_REJECTED, reversed.getApprovalStatus(),
                "recovery reverseApprove 后 approvalStatus=REJECTED");

        ErpFinVoucher originalAfter = daoProvider.daoFor(ErpFinVoucher.class).requireEntityById(originalVoucherId);
        assertEquals(Boolean.TRUE, originalAfter.getIsReversed(), "recovery 原凭证 isReversed=true");

        // ArApItem 回退对称：OPEN → WRITTEN_OFF；settled+=300，open-=300
        ErpFinArApItem afterReverse = daoProvider.daoFor(ErpFinArApItem.class).getEntityById(holder[1]);
        assertEquals(ErpFinConstants.AR_AP_STATUS_WRITTEN_OFF, afterReverse.getStatus(),
                "recovery 反向：ArApItem 回退 WRITTEN_OFF");
        assertEquals(0, BigDecimal.ZERO.compareTo(afterReverse.getOpenAmountFunctional()),
                "openAmount 回退 0");
        output("1_after_recovery_reverse_ar_ap_item.json5", arApItemState(afterReverse));
    }

    @Test
    public void testGuardNotPostedRejects() {
        LocalDate today = io.nop.api.core.time.CoreMetrics.today();
        LocalDate asOf = today.minusDays(10);
        Long[] holder = new Long[2];
        ormTemplate.runInSession(() -> {
            Long pid = seedOpenPeriodCurrentMonth("2026-07-C");
            seedCurrency(1L, "CNY", true);
            seedSubject("1231", "坏账准备", "ASSET", ErpFinConstants.DC_CREDIT);
            seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT);
            ErpFinArApItem item = seedReceivable("AR-NOPOST-1", pid, asOf, "200");
            holder[0] = pid;
            holder[1] = item.getId();
        });

        // 创建 UNSUBMITTED 坏账单（write-off-require-approval=true 默认），未 approve 故未过账 voucherId=null
        ErpFinBadDebt unsubmitted = ormTemplate.runInSession(session -> {
            ErpFinBadDebt d = daoProvider.daoFor(ErpFinBadDebt.class).newEntity();
            d.setCode("BD-NOPOST-" + System.nanoTime());
            d.setOrgId(1L);
            d.setAcctSchemaId(1L);
            d.setDocType(ErpFinConstants.BAD_DEBT_TYPE_WRITE_OFF);
            d.setPartnerId(1L);
            d.setSourceArApItemId(holder[1]);
            d.setAmount(new BigDecimal("200"));
            d.setCurrencyId(1L);
            d.setExchangeRate(BigDecimal.ONE);
            d.setBusinessDate(asOf);
            d.setApprovalStatus(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
            d.setPeriodId(holder[0]);
            daoProvider.daoFor(ErpFinBadDebt.class).saveEntity(d);
            return d;
        });

        ErpFinBadDebt finalDebt = unsubmitted;
        NopException ex = assertThrows(NopException.class, () -> ormTemplate.runInSession(session ->
                        badDebtBiz.reverseApprove(finalDebt.getId(), CTX)),
                "未过账坏账单不可反审核：ERR_BAD_DEBT_NOT_APPROVED_OR_NOT_POSTED");
        assertEquals(ErpFinErrors.ERR_BAD_DEBT_NOT_APPROVED_OR_NOT_POSTED.getErrorCode(), ex.getErrorCode(),
                "错误码匹配");
    }

    // ---------- helpers ----------

    private java.util.Map<String, Object> badDebtState(ErpFinBadDebt d) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("code", d.getCode());
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

    private java.util.Map<String, Object> beforeReverseState(ErpFinBadDebt d, ErpFinArApItem it, ErpFinVoucher v) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("badDebt", badDebtState(d));
        m.put("arApItem", arApItemState(it));
        java.util.Map<String, Object> vs = new java.util.LinkedHashMap<>();
        vs.put("id", v.getId());
        vs.put("isReversed", v.getIsReversed());
        vs.put("postingType", v.getPostingType());
        m.put("originalVoucher", vs);
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

    /**
     * Seed 当前月份的 OPEN 期间——反审核触发红冲凭证 voucherDate=today，过账引擎 resolveOpenPeriod 按
     * voucherDate 查找期间；须覆盖今天否则红冲失败（参 TestErpFinEmployeeAdvanceCashRepay 范式）。
     */
    private Long seedOpenPeriodCurrentMonth(String code) {
        LocalDate today = io.nop.api.core.time.CoreMetrics.today();
        return seedOpenPeriod(code, today.getYear(), today.getMonthValue(),
                today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
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
        return item;
    }

    private Long findReversalVoucherId(String billCode) {
        // 经 ErpFinVoucherBillR 反查同 billCode 的所有 voucherId，取 postingType=REVERSAL 那张
        QueryBean linkQ = new QueryBean();
        linkQ.addFilter(eq("billCode", billCode));
        List<app.erp.fin.dao.entity.ErpFinVoucherBillR> links =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucherBillR.class).findAllByQuery(linkQ);
        for (app.erp.fin.dao.entity.ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
            if (v != null && ErpFinConstants.POSTING_TYPE_REVERSAL.equals(v.getPostingType())) {
                return v.getId();
            }
        }
        return null;
    }

    private List<ErpFinVoucherLine> linesOf(Long voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    private ErpFinVoucherLine lineOfSubject(List<ErpFinVoucherLine> lines, String subjectCode) {
        return lines.stream()
                .filter(l -> subjectCode.equals(l.getSubjectCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("预期科目行不存在: " + subjectCode));
    }
}
