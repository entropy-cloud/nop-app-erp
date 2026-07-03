package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinExpenseClaimBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 报销单业财过账端到端单测（Phase 3）。验证 APPROVED→EXPENSE_CLAIM 凭证落库（借费用/进项税/贷方按 paymentMode）
 * + DIRECTION_PAYABLE 辅助账（partnerId=claimant.partnerId）+ posted=true；reverseApprove→红冲 + 辅助账 CANCELLED。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinExpenseClaimPosting extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinExpenseClaimBiz claimBiz;

    @Test
    public void testApprovePostsAndGeneratesPayableSubledger() {
        long partnerId = 9901L;
        Long claimId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedAcctSchema(1L);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税额");
            seedSubject("2241", "其他应付款-员工");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedClaim("EC-POST-001", empId, ErpFinConstants.APPROVE_STATUS_SUBMITTED,
                    new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"),
                    ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
        });

        ErpFinExpenseClaim claim = claimBiz.approve(claimId, CTX);
        assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, claim.getApproveStatus());
        assertTrue(Boolean.TRUE.equals(claim.getPosted()), "过账成功 posted=true");

        // EXPENSE_CLAIM 凭证经业财回链可查
        assertTrue(!findBillLinks("EC-POST-001", ErpFinBusinessType.EXPENSE_CLAIM.name()).isEmpty(), "EXPENSE_CLAIM 凭证回链已落库");

        // DIRECTION_PAYABLE 辅助账，partnerId = claimant.partnerId（非 employee.id）
        ErpFinArApItem item = findItem("EXPENSE_CLAIM", "EC-POST-001");
        assertEquals(ErpFinConstants.DIRECTION_PAYABLE, item.getDirection(), "方向=应付");
        assertEquals(partnerId, item.getPartnerId(), "partnerId = claimant.partnerId");
        assertEquals(0, item.getAmountFunctional().compareTo(new BigDecimal("113")), "金额=价税合计 113");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, item.getStatus(), "状态=未核销");
    }

    @Test
    public void testReverseApproveCancelsSubledger() {
        long partnerId = 9902L;
        Long claimId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedAcctSchema(1L);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税额");
            seedSubject("2241", "其他应付款-员工");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedClaim("EC-POST-002", empId, ErpFinConstants.APPROVE_STATUS_SUBMITTED,
                    new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"),
                    ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
        });

        claimBiz.approve(claimId, CTX);
        ErpFinArApItem before = findItem("EXPENSE_CLAIM", "EC-POST-002");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, before.getStatus());

        ErpFinExpenseClaim claim = claimBiz.reverseApprove(claimId, CTX);
        assertTrue(!Boolean.TRUE.equals(claim.getPosted()), "反审核后 posted=false");

        ErpFinArApItem after = findItem("EXPENSE_CLAIM", "EC-POST-002");
        assertEquals(ErpFinConstants.AR_AP_STATUS_CANCELLED, after.getStatus(), "红冲后辅助账 CANCELLED");
        assertEquals(0, after.getOpenAmountFunctional().compareTo(BigDecimal.ZERO), "红冲后 openAmount=0");
    }

    @Test
    public void testCompanyAccountPaymentModeCreditBank() {
        long partnerId = 9903L;
        Long claimId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedAcctSchema(1L);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税额");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedClaim("EC-POST-003", empId, ErpFinConstants.APPROVE_STATUS_SUBMITTED,
                    new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"),
                    ErpFinConstants.PAYMENT_MODE_COMPANY_ACCOUNT);
        });

        ErpFinExpenseClaim claim = claimBiz.approve(claimId, CTX);
        assertTrue(Boolean.TRUE.equals(claim.getPosted()), "公司直付过账成功");
        // 公司直付贷银行存款，不生成应付-员工辅助账（PAYABLE 余额无新增）
        assertTrue(findItems("EXPENSE_CLAIM", "EC-POST-003").isEmpty(),
                "company_account 贷银行存款，不生成员工应付辅助账");
    }

    // ---------- seed helpers ----------

    private Long seedClaim(String code, Long claimantId, String approveStatus,
                           BigDecimal amountWithoutTax, BigDecimal tax, BigDecimal withTax, String paymentMode) {
        IEntityDao<ErpFinExpenseClaim> dao = daoProvider.daoFor(ErpFinExpenseClaim.class);
        ErpFinExpenseClaim claim = new ErpFinExpenseClaim();
        claim.setCode(code);
        claim.setOrgId(1L);
        claim.setClaimantId(claimantId);
        claim.setBusinessDate(LocalDate.of(2026, 6, 15));
        claim.setPaymentMode(paymentMode);
        claim.setCurrencyId(1L);
        claim.setExchangeRate(BigDecimal.ONE);
        claim.setAmountWithoutTax(amountWithoutTax);
        claim.setTaxAmount(tax);
        claim.setAmountWithTax(withTax);
        claim.setReason("差旅");
        claim.setDocStatus(ErpFinConstants.DOC_STATUS_DRAFT);
        claim.setApproveStatus(approveStatus);
        dao.saveEntity(claim);

        IEntityDao<ErpFinExpenseClaimLine> lineDao = daoProvider.daoFor(ErpFinExpenseClaimLine.class);
        ErpFinExpenseClaimLine line = new ErpFinExpenseClaimLine();
        line.setClaimId(claim.getId());
        line.setLineNo(1);
        line.setExpenseType("TRAVEL");
        line.setAmountWithoutTax(amountWithoutTax);
        line.setTaxAmount(tax);
        line.setAmountWithTax(withTax);
        lineDao.saveEntity(line);
        return claim.getId();
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

    private ErpFinArApItem findItem(String sourceBillType, String sourceBillCode) {
        List<ErpFinArApItem> items = findItems(sourceBillType, sourceBillCode);
        return items.isEmpty() ? null : items.get(0);
    }

    private List<ErpFinArApItem> findItems(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }
}
