package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinEmployeeAdvanceBiz;
import app.erp.fin.biz.IErpFinExpenseClaimBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.dao.entity.ErpFinExpenseClaimLine;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdSubject;
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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 报销抵扣借款端到端单测（Phase 3）。验证：借款审核→员工预支应收辅助账；报销审核（own_account）→
 * 报销应付辅助账 + 自动抵扣（净额=min(借款未还, 报销应付)）+ EMPLOYEE_ADVANCE_SETTLE 清算凭证 +
 * 借款 outstanding 下降 + 报销应付下降；报销反审核→反向抵扣（借款应收恢复）+ 红冲报销应付。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinExpenseOffsetAdvance extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinExpenseClaimBiz claimBiz;
    @Inject
    IErpFinEmployeeAdvanceBiz advanceBiz;

    @Test
    public void testOffsetThenReverse() {
        long partnerId = 7701L;
        long[] ids = ormTemplate.runInSession(session -> {
            seedOpenPeriod();
            seedAcctSchema(1L);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税额");
            seedSubject("2241", "其他应付款-员工");
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId);
            Long advanceId = seedAdvance("ADV-OFF-001", empId, new BigDecimal("500"));
            Long claimId = seedClaim("EC-OFF-001", empId, new BigDecimal("100"),
                    new BigDecimal("13"), new BigDecimal("113"));
            return new long[]{advanceId, claimId};
        });
        long advanceId = ids[0];
        long claimId = ids[1];

        // 1. 借款审核 → 员工预支应收辅助账 open=500
        advanceBiz.approve(advanceId, CTX);
        ErpFinArApItem advanceItem = findItem("EMPLOYEE_ADVANCE", "ADV-OFF-001");
        assertNotNull(advanceItem);
        assertEquals(0, advanceItem.getOpenAmountFunctional().compareTo(new BigDecimal("500")));

        // 2. 报销审核 → 报销应付辅助账 + 抵扣（净额=min(500,113)=113）+ SETTLE 凭证
        claimBiz.approve(claimId, CTX);

        ErpFinArApItem payableItem = findItem("EXPENSE_CLAIM", "EC-OFF-001");
        assertEquals(0, payableItem.getOpenAmountFunctional().compareTo(BigDecimal.ZERO), "报销应付被全额抵扣 open=0");
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, payableItem.getStatus());

        ErpFinArApItem advanceItemAfter = findItem("EMPLOYEE_ADVANCE", "ADV-OFF-001");
        assertEquals(0, advanceItemAfter.getOpenAmountFunctional().compareTo(new BigDecimal("387")), "借款应收 open=500-113=387");
        assertEquals(ErpFinConstants.AR_AP_STATUS_PARTIAL, advanceItemAfter.getStatus());

        // SETTLE 清算凭证落库
        assertTrue(!findBillLinks("EC-OFF-001", 180).isEmpty(), "EMPLOYEE_ADVANCE_SETTLE 凭证已落库");

        // 借款单 settled/outstanding 回写
        ErpFinEmployeeAdvance advance = daoProvider.daoFor(ErpFinEmployeeAdvance.class).getEntityById(advanceId);
        assertEquals(0, advance.getSettledAmount().compareTo(new BigDecimal("113")));
        assertEquals(0, advance.getOutstandingAmount().compareTo(new BigDecimal("387")));

        // 报销单记录抵扣的借款
        ErpFinExpenseClaim claim = daoProvider.daoFor(ErpFinExpenseClaim.class).getEntityById(claimId);
        assertEquals(advanceId, claim.getSettleAdvanceId());

        // 3. 报销反审核 → 反向抵扣（借款应收恢复 open=500）+ 红冲报销应付
        claimBiz.reverseApprove(claimId, CTX);

        ErpFinArApItem advanceItemRev = findItem("EMPLOYEE_ADVANCE", "ADV-OFF-001");
        assertEquals(0, advanceItemRev.getOpenAmountFunctional().compareTo(new BigDecimal("500")), "反向抵扣后借款应收恢复 open=500");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, advanceItemRev.getStatus());

        ErpFinArApItem payableItemRev = findItem("EXPENSE_CLAIM", "EC-OFF-001");
        assertEquals(ErpFinConstants.AR_AP_STATUS_CANCELLED, payableItemRev.getStatus(), "报销应付随红冲 CANCELLED");

        ErpFinEmployeeAdvance advanceRev = daoProvider.daoFor(ErpFinEmployeeAdvance.class).getEntityById(advanceId);
        assertEquals(0, advanceRev.getOutstandingAmount().compareTo(new BigDecimal("500")), "借款 outstanding 恢复 500");
    }

    @Test
    public void testOffsetPartialExpenseLargerThanAdvance() {
        // 报销应付(300) > 借款未还(200)：抵扣 200，报销剩余 100 留作应付-员工
        long partnerId = 7702L;
        long[] ids = ormTemplate.runInSession(session -> {
            seedOpenPeriod();
            seedAcctSchema(1L);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税额");
            seedSubject("2241", "其他应付款-员工");
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId);
            Long advanceId = seedAdvance("ADV-OFF-002", empId, new BigDecimal("200"));
            Long claimId = seedClaim("EC-OFF-002", empId, new BigDecimal("300"),
                    BigDecimal.ZERO, new BigDecimal("300"));
            return new long[]{advanceId, claimId};
        });
        advanceBiz.approve(ids[0], CTX);
        claimBiz.approve(ids[1], CTX);

        ErpFinArApItem advanceItem = findItem("EMPLOYEE_ADVANCE", "ADV-OFF-002");
        assertEquals(0, advanceItem.getOpenAmountFunctional().compareTo(BigDecimal.ZERO), "借款被全额抵扣 open=0");
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, advanceItem.getStatus());

        ErpFinArApItem payableItem = findItem("EXPENSE_CLAIM", "EC-OFF-002");
        assertEquals(0, payableItem.getOpenAmountFunctional().compareTo(new BigDecimal("100")), "报销剩余 100 留作应付-员工");
        assertEquals(ErpFinConstants.AR_AP_STATUS_PARTIAL, payableItem.getStatus());
    }

    // ---------- seed helpers ----------

    private void seedOpenPeriod() {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode("2026-06");
        period.setName("2026-06");
        period.setOrgId(1L);
        period.setYear(2026);
        period.setMonth(6);
        period.setStartDate(LocalDate.of(2026, 6, 1));
        period.setEndDate(LocalDate.of(2026, 6, 30));
        period.setStatus(10);
        dao.saveEntity(period);
    }

    private Long seedEmployee(long partnerId) {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee emp = new ErpMdEmployee();
        emp.setCode("E-" + partnerId);
        emp.setName("员工-" + partnerId);
        emp.setOrgId(1L);
        emp.setPartnerId(partnerId);
        emp.setStatus(10);
        dao.saveEntity(emp);
        return emp.getId();
    }

    private Long seedAdvance(String code, Long employeeId, BigDecimal amount) {
        IEntityDao<ErpFinEmployeeAdvance> dao = daoProvider.daoFor(ErpFinEmployeeAdvance.class);
        ErpFinEmployeeAdvance advance = new ErpFinEmployeeAdvance();
        advance.setCode(code);
        advance.setOrgId(1L);
        advance.setEmployeeId(employeeId);
        advance.setAdvanceType(10);
        advance.setBusinessDate(LocalDate.of(2026, 6, 5));
        advance.setCurrencyId(1L);
        advance.setExchangeRate(BigDecimal.ONE);
        advance.setAmountFunctional(amount);
        advance.setAmountSource(amount);
        advance.setSettledAmount(BigDecimal.ZERO);
        advance.setOutstandingAmount(amount);
        advance.setDocStatus(ErpFinConstants.DOC_STATUS_DRAFT);
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        dao.saveEntity(advance);
        return advance.getId();
    }

    private Long seedClaim(String code, Long claimantId, BigDecimal amountWithoutTax,
                           BigDecimal tax, BigDecimal withTax) {
        IEntityDao<ErpFinExpenseClaim> dao = daoProvider.daoFor(ErpFinExpenseClaim.class);
        ErpFinExpenseClaim claim = new ErpFinExpenseClaim();
        claim.setCode(code);
        claim.setOrgId(1L);
        claim.setClaimantId(claimantId);
        claim.setBusinessDate(LocalDate.of(2026, 6, 15));
        claim.setPaymentMode(ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
        claim.setCurrencyId(1L);
        claim.setExchangeRate(BigDecimal.ONE);
        claim.setAmountWithoutTax(amountWithoutTax);
        claim.setTaxAmount(tax);
        claim.setAmountWithTax(withTax);
        claim.setReason("差旅");
        claim.setDocStatus(ErpFinConstants.DOC_STATUS_DRAFT);
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        dao.saveEntity(claim);

        IEntityDao<ErpFinExpenseClaimLine> lineDao = daoProvider.daoFor(ErpFinExpenseClaimLine.class);
        ErpFinExpenseClaimLine line = new ErpFinExpenseClaimLine();
        line.setClaimId(claim.getId());
        line.setLineNo(1);
        line.setExpenseType(10);
        line.setAmountWithoutTax(amountWithoutTax);
        line.setTaxAmount(tax);
        line.setAmountWithTax(withTax);
        lineDao.saveEntity(line);
        return claim.getId();
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(10);
        subject.setDirection(10);
        subject.setStatus(10);
        dao.saveEntity(subject);
    }

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature(10);
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus(10);
        dao.saveEntity(schema);
    }

    private ErpFinArApItem findItem(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        List<ErpFinArApItem> items = dao.findAllByQuery(q);
        return items.isEmpty() ? null : items.get(0);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, int businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }
}
