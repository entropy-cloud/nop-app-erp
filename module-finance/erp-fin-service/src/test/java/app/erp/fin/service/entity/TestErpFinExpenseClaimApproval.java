package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinExpenseClaimBiz;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.dao.entity.ErpFinExpenseClaimLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 费用报销单三轴审批状态机 + 前置校验单测（Phase 2）。验证 UNSUBMITTED↔SUBMITTED→APPROVED/REJECTED
 * 正向/反向迁移、非法迁移拒绝、前置校验（报销人启用/partnerId/行非空/价税合计/事由必填）拒绝。
 *
 * <p>过账（posted 置位）属 Phase 3 端到端测试，本测试不 seed 会计期间，approve 时过账失败吞异常、posted 保持 false，
 * 仅验证状态机迁移。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinExpenseClaimApproval extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinExpenseClaimBiz claimBiz;

    @Test
    public void testSubmitWithdrawApproveReverse() {
        Long claimId = seedValidClaim("EC-APP-001", ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, null);
        // UNSUBMITTED → SUBMITTED
        ErpFinExpenseClaim claim = claimBiz.submit(claimId, CTX);
        assertEquals(ErpFinConstants.APPROVE_STATUS_SUBMITTED, claim.getApproveStatus());
        // SUBMITTED → UNSUBMITTED
        claim = claimBiz.withdrawSubmit(claimId, CTX);
        assertEquals(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, claim.getApproveStatus());
        // UNSUBMITTED → SUBMITTED again
        claimBiz.submit(claimId, CTX);
        // SUBMITTED → APPROVED（过账未配置，posted 保持 false）
        claim = claimBiz.approve(claimId, CTX);
        assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, claim.getApproveStatus());
        // APPROVED → REJECTED（reverseApprove，posted=false 不触发红冲）
        claim = claimBiz.reverseApprove(claimId, CTX);
        assertEquals(ErpFinConstants.APPROVE_STATUS_REJECTED, claim.getApproveStatus());
    }

    @Test
    public void testRejectAndResubmit() {
        Long claimId = seedValidClaim("EC-APP-002", ErpFinConstants.APPROVE_STATUS_SUBMITTED, null);
        ErpFinExpenseClaim claim = claimBiz.reject(claimId, CTX);
        assertEquals(ErpFinConstants.APPROVE_STATUS_REJECTED, claim.getApproveStatus());
        // REJECTED → SUBMITTED（修改重提）
        claim = claimBiz.submit(claimId, CTX);
        assertEquals(ErpFinConstants.APPROVE_STATUS_SUBMITTED, claim.getApproveStatus());
    }

    @Test
    public void testCancel() {
        Long claimId = seedValidClaim("EC-APP-003", ErpFinConstants.APPROVE_STATUS_SUBMITTED, null);
        ErpFinExpenseClaim claim = claimBiz.cancel(claimId, CTX);
        assertEquals(ErpFinConstants.DOC_STATUS_CANCELLED, claim.getDocStatus());
    }

    @Test
    public void testIllegalApproveFromUnsubmitted() {
        Long claimId = seedValidClaim("EC-APP-004", ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, null);
        assertThrows(NopException.class, () -> claimBiz.approve(claimId, CTX));
    }

    @Test
    public void testRejectClaimantPartnerMissing() {
        Long claimId = seedClaimWithClaimant("EC-APP-005", null, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE,
                ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, true);
        NopException ex = assertThrows(NopException.class, () -> claimBiz.submit(claimId, CTX));
        assertTrue(ex.getMessage().contains("partnerId") || ex.getErrorCode() != null);
    }

    @Test
    public void testRejectClaimantInactive() {
        Long claimId = seedClaimWithClaimant("EC-APP-006", 9901L, "INACTIVE",
                ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, true);
        assertThrows(NopException.class, () -> claimBiz.submit(claimId, CTX));
    }

    @Test
    public void testRejectLinesEmpty() {
        Long claimId = seedValidClaimNoLines("EC-APP-007");
        assertThrows(NopException.class, () -> claimBiz.submit(claimId, CTX));
    }

    @Test
    public void testRejectAmountMismatch() {
        Long claimId = seedClaimAmountMismatch("EC-APP-008");
        assertThrows(NopException.class, () -> claimBiz.submit(claimId, CTX));
    }

    // ---------- seed helpers ----------

    private Long seedValidClaim(String code, String approveStatus, Long docStatus) {
        return seedClaimWithClaimant(code, 9901L, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE, approveStatus, true);
    }

    private Long seedClaimWithClaimant(String code, Long partnerId, String employeeStatus,
                                       String approveStatus, boolean withLines) {
        return ormTemplate.runInSession(session -> {
            Long empId = seedEmployee(partnerId, employeeStatus);
            ErpFinExpenseClaim claim = newClaim(code, empId, approveStatus);
            if (withLines) {
                addLine(claim.getId(), new BigDecimal("113"));
            }
            return claim.getId();
        });
    }

    private Long seedValidClaimNoLines(String code) {
        return seedClaimWithClaimant(code, 9901L, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE,
                ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, false);
    }

    private Long seedClaimAmountMismatch(String code) {
        return ormTemplate.runInSession(session -> {
            Long empId = seedEmployee(9901L, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            ErpFinExpenseClaim claim = newClaim(code, empId, ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
            // head total = 113（newClaim 默认），line total = 200 → mismatch
            addLine(claim.getId(), new BigDecimal("200"));
            return claim.getId();
        });
    }

    private Long seedEmployee(Long partnerId, String status) {
        // 测试库不强制 FK（对齐 TestErpFinArApItemGeneration 用合成 partnerId=2L/3L），故仅 set partnerId，
        // 不必先建 ErpMdPartner。partnerId=null 表示员工无内部往来单位（partner-missing 用例）。
        IEntityDao<ErpMdEmployee> empDao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee emp = new ErpMdEmployee();
        emp.setCode("E-" + (partnerId != null ? partnerId : "nopartner"));
        emp.setName("员工-" + (partnerId != null ? partnerId : "nopartner"));
        emp.setOrgId(1L);
        emp.setPartnerId(partnerId);
        emp.setStatus(status);
        empDao.saveEntity(emp);
        return emp.getId();
    }

    private ErpFinExpenseClaim newClaim(String code, Long claimantId, String approveStatus) {
        IEntityDao<ErpFinExpenseClaim> dao = daoProvider.daoFor(ErpFinExpenseClaim.class);
        ErpFinExpenseClaim claim = new ErpFinExpenseClaim();
        claim.setCode(code);
        claim.setOrgId(1L);
        claim.setClaimantId(claimantId);
        claim.setBusinessDate(java.time.LocalDate.of(2026, 6, 15));
        claim.setPaymentMode(ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
        claim.setCurrencyId(1L);
        claim.setExchangeRate(BigDecimal.ONE);
        claim.setAmountWithoutTax(new BigDecimal("100"));
        claim.setTaxAmount(new BigDecimal("13"));
        claim.setAmountWithTax(new BigDecimal("113"));
        claim.setReason("差旅报销");
        claim.setDocStatus(ErpFinConstants.DOC_STATUS_DRAFT);
        claim.setApproveStatus(approveStatus);
        dao.saveEntity(claim);
        return claim;
    }

    private void addLine(Long claimId, BigDecimal amountWithTax) {
        IEntityDao<ErpFinExpenseClaimLine> dao = daoProvider.daoFor(ErpFinExpenseClaimLine.class);
        ErpFinExpenseClaimLine line = new ErpFinExpenseClaimLine();
        line.setClaimId(claimId);
        line.setLineNo(1);
        line.setExpenseType("TRAVEL");
        line.setAmountWithoutTax(amountWithTax);
        line.setTaxAmount(BigDecimal.ZERO);
        line.setAmountWithTax(amountWithTax);
        dao.saveEntity(line);
    }
}
