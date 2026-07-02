package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinEmployeeAdvanceBiz;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdEmployee;
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

/**
 * 员工借款单三轴审批状态机 + 前置校验单测（Phase 2）。验证 UNSUBMITTED↔SUBMITTED→APPROVED/REJECTED 迁移、
 * 非法迁移拒绝、前置校验（员工启用/partnerId/金额>0）拒绝、settled/outstanding 派生。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinEmployeeAdvanceApproval extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinEmployeeAdvanceBiz advanceBiz;

    @Test
    public void testSubmitApproveReverse() {
        Long advanceId = seedValidAdvance("ADV-001", ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
                new BigDecimal("500"));
        advanceBiz.submit(advanceId, CTX);
        ErpFinEmployeeAdvance advance = advanceBiz.approve(advanceId, CTX);
        assertEquals(ErpFinConstants.APPROVE_STATUS_APPROVED, advance.getApproveStatus());
        // outstanding 派生 = amount - settled(0) = 500
        assertEquals(0, advance.getOutstandingAmount().compareTo(new BigDecimal("500")));
        advance = advanceBiz.reverseApprove(advanceId, CTX);
        assertEquals(ErpFinConstants.APPROVE_STATUS_REJECTED, advance.getApproveStatus());
    }

    @Test
    public void testRejectAndResubmit() {
        Long advanceId = seedValidAdvance("ADV-002", ErpFinConstants.APPROVE_STATUS_SUBMITTED,
                new BigDecimal("300"));
        advanceBiz.reject(advanceId, CTX);
        ErpFinEmployeeAdvance advance = advanceBiz.submit(advanceId, CTX);
        assertEquals(ErpFinConstants.APPROVE_STATUS_SUBMITTED, advance.getApproveStatus());
    }

    @Test
    public void testCancel() {
        Long advanceId = seedValidAdvance("ADV-003", ErpFinConstants.APPROVE_STATUS_SUBMITTED,
                new BigDecimal("200"));
        ErpFinEmployeeAdvance advance = advanceBiz.cancel(advanceId, CTX);
        assertEquals(ErpFinConstants.DOC_STATUS_CANCELLED, advance.getDocStatus());
    }

    @Test
    public void testIllegalApproveFromUnsubmitted() {
        Long advanceId = seedValidAdvance("ADV-004", ErpFinConstants.APPROVE_STATUS_UNSUBMITTED,
                new BigDecimal("100"));
        assertThrows(NopException.class, () -> advanceBiz.approve(advanceId, CTX));
    }

    @Test
    public void testRejectEmployeePartnerMissing() {
        Long advanceId = seedAdvance("ADV-005", null, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE,
                ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, new BigDecimal("100"));
        assertThrows(NopException.class, () -> advanceBiz.submit(advanceId, CTX));
    }

    @Test
    public void testRejectEmployeeInactive() {
        Long advanceId = seedAdvance("ADV-006", 9902L, 20,
                ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, new BigDecimal("100"));
        assertThrows(NopException.class, () -> advanceBiz.submit(advanceId, CTX));
    }

    @Test
    public void testRejectAmountNotPositive() {
        Long advanceId = seedAdvance("ADV-007", 9902L, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE,
                ErpFinConstants.APPROVE_STATUS_UNSUBMITTED, BigDecimal.ZERO);
        assertThrows(NopException.class, () -> advanceBiz.submit(advanceId, CTX));
    }

    // ---------- seed helpers ----------

    private Long seedValidAdvance(String code, int approveStatus, BigDecimal amount) {
        return seedAdvance(code, 9902L, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE, approveStatus, amount);
    }

    private Long seedAdvance(String code, Long partnerId, int employeeStatus, int approveStatus, BigDecimal amount) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpMdEmployee> empDao = daoProvider.daoFor(ErpMdEmployee.class);
            ErpMdEmployee emp = new ErpMdEmployee();
            emp.setCode("E-" + code);
            emp.setName("员工-" + code);
            emp.setOrgId(1L);
            emp.setPartnerId(partnerId);
            emp.setStatus(employeeStatus);
            empDao.saveEntity(emp);

            IEntityDao<ErpFinEmployeeAdvance> dao = daoProvider.daoFor(ErpFinEmployeeAdvance.class);
            ErpFinEmployeeAdvance advance = new ErpFinEmployeeAdvance();
            advance.setCode(code);
            advance.setOrgId(1L);
            advance.setEmployeeId(emp.getId());
            advance.setAdvanceType(10);
            advance.setBusinessDate(java.time.LocalDate.of(2026, 6, 10));
            advance.setCurrencyId(1L);
            advance.setExchangeRate(BigDecimal.ONE);
            advance.setAmountFunctional(amount);
            advance.setAmountSource(amount);
            advance.setSettledAmount(BigDecimal.ZERO);
            advance.setOutstandingAmount(amount);
            advance.setDocStatus(ErpFinConstants.DOC_STATUS_DRAFT);
            advance.setApproveStatus(approveStatus);
            dao.saveEntity(advance);
            return advance.getId();
        });
    }
}
