package app.erp.hr.service;

import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.dao.entity.ErpHrPosition;
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

import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 员工部门调动端到端行为测试（use-cases.md UC-HR-08）。覆盖：
 * <ul>
 *   <li>正常调动：部门/职位/上级更新（ACTIVE 状态员工）。</li>
 *   <li>不可调动状态守门（RESIGNED/TERMINATED/RETIRED 抛 ERR_EMPLOYEE_NOT_TRANSFERABLE）。</li>
 *   <li>目标部门/职位不存在守门。</li>
 *   <li>合同处理 AUTO：原 ACTIVE 合同→TERMINATED + 新建 ACTIVE 合同 startDate=effectiveDate。</li>
 *   <li>合同处理 NO：不触及合同。</li>
 *   <li>合同处理 YES：无 ACTIVE 合同时仍新建。</li>
 *   <li>休假冲突告警不阻塞：调动日期落入 APPROVED 休假区间，调动仍成功。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrEmployeeTransfer extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrEmployeeBiz employeeBiz;

    @Test
    public void testTransferUpdatesDepartmentPositionSuperior() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-NORMAL", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptFromId = seedDepartment("DEPT-FROM");
            Long deptToId = seedDepartment("DEPT-TO");
            Long posId = seedPosition("POS-DEV", deptToId);
            Long superiorId = seedEmployee("EMP-BOSS", ErpHrConstants.EMPLOYMENT_ACTIVE);
            return new Object[]{empId, deptFromId, deptToId, posId, superiorId};
        });
        Long empId = (Long) ids[0];
        Long deptToId = (Long) ids[2];
        Long posId = (Long) ids[3];
        Long superiorId = (Long) ids[4];

        ErpHrEmployee result = ormTemplate.runInSession(session -> employeeBiz.transferEmployee(
                empId, deptToId, posId, superiorId,
                LocalDate.of(2026, 7, 8), ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX));

        assertEquals(deptToId, result.getDepartmentId(), "部门应更新为目标部门");
        assertEquals(posId, result.getPositionId(), "职位应更新为目标职位");
        assertEquals(superiorId, result.getSuperiorId(), "上级应更新为目标上级");

        ErpHrEmployee refreshed = daoProvider.daoFor(ErpHrEmployee.class).getEntityById(empId);
        assertEquals(deptToId, refreshed.getDepartmentId());
        assertEquals(posId, refreshed.getPositionId());
        assertEquals(superiorId, refreshed.getSuperiorId());
    }

    @Test
    public void testTransferRejectsNonTransferableStatus() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-RESIGNED", ErpHrConstants.EMPLOYMENT_RESIGNED);
            Long deptId = seedDepartment("DEPT-TARGET-R");
            return new Object[]{empId, deptId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> employeeBiz.transferEmployee(empId, deptId, null, null,
                        LocalDate.of(2026, 7, 8),
                        ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX)));
        assertEquals(ErpHrErrors.ERR_EMPLOYEE_NOT_TRANSFERABLE.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testTransferRejectsUnknownTargetDepartment() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-DEPT-404", ErpHrConstants.EMPLOYMENT_ACTIVE);
            return new Object[]{empId};
        });
        Long empId = (Long) ids[0];

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> employeeBiz.transferEmployee(empId, 99999999L, null, null,
                        LocalDate.of(2026, 7, 8),
                        ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX)));
        assertEquals(ErpHrErrors.ERR_TRANSFER_TARGET_DEPT_NOT_FOUND.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testTransferRejectsPositionNotInTargetDepartment() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-POS-MISMATCH", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptAId = seedDepartment("DEPT-A");
            Long deptBId = seedDepartment("DEPT-B");
            Long posInBId = seedPosition("POS-IN-B", deptBId);
            return new Object[]{empId, deptAId, posInBId};
        });
        Long empId = (Long) ids[0];
        Long deptAId = (Long) ids[1];
        Long posInBId = (Long) ids[2];

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> employeeBiz.transferEmployee(empId, deptAId, posInBId, null,
                        LocalDate.of(2026, 7, 8),
                        ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX)));
        assertEquals(ErpHrErrors.ERR_TRANSFER_TARGET_POSITION_NOT_FOUND.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testTransferContractAutoTerminatesOldAndCreatesNew() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-CONTRACT-AUTO", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-AUTO");
            Long oldContractId = seedContract("CTC-OLD-AUTO", empId,
                    LocalDate.of(2024, 1, 1), LocalDate.of(2027, 12, 31),
                    "FIXED_TERM");
            return new Object[]{empId, deptId, oldContractId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];
        Long oldContractId = (Long) ids[2];
        LocalDate effective = LocalDate.of(2026, 7, 8);

        ormTemplate.runInSession(() -> employeeBiz.transferEmployee(empId, deptId, null, null, effective,
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_AUTO, CTX));

        ErpHrEmploymentContract oldContract = daoProvider.daoFor(ErpHrEmploymentContract.class)
                .getEntityById(oldContractId);
        assertEquals(ErpHrConstants.CONTRACT_STATUS_TERMINATED, oldContract.getStatus(),
                "原合同应被终止");

        ErpHrEmploymentContract newContract = findActiveContract(empId);
        assertNotNull(newContract, "应新建 ACTIVE 合同");
        assertNotEquals(oldContractId, newContract.getId());
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, newContract.getStatus());
        assertEquals(effective, newContract.getStartDate(), "新合同 startDate 应为生效日期");
        assertEquals("FIXED_TERM", newContract.getContractType(), "新合同应承袭 contractType");
    }

    @Test
    public void testTransferContractNoDoesNotTouchContracts() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-CONTRACT-NO", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-NO");
            Long oldContractId = seedContract("CTC-OLD-NO", empId,
                    LocalDate.of(2024, 1, 1), LocalDate.of(2027, 12, 31),
                    "FIXED_TERM");
            return new Object[]{empId, deptId, oldContractId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];
        Long oldContractId = (Long) ids[2];

        ormTemplate.runInSession(() -> employeeBiz.transferEmployee(empId, deptId, null, null,
                LocalDate.of(2026, 7, 8),
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX));

        ErpHrEmploymentContract oldContract = daoProvider.daoFor(ErpHrEmploymentContract.class)
                .getEntityById(oldContractId);
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, oldContract.getStatus(),
                "NO 模式不应触及原合同状态");
    }

    @Test
    public void testTransferContractYesCreatesEvenWithoutActive() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-CONTRACT-YES", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-YES");
            return new Object[]{empId, deptId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];
        LocalDate effective = LocalDate.of(2026, 7, 8);

        assertNull(findActiveContract(empId), "前置：员工无 ACTIVE 合同");

        ormTemplate.runInSession(() -> employeeBiz.transferEmployee(empId, deptId, null, null, effective,
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_YES, CTX));

        ErpHrEmploymentContract created = findActiveContract(empId);
        assertNotNull(created, "YES 模式即使无 ACTIVE 合同也应新建");
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, created.getStatus());
        assertEquals(effective, created.getStartDate());
    }

    @Test
    public void testTransferLeaveConflictWarnDoesNotBlock() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-LEAVE-CONFLICT", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-LEAVE");
            seedApprovedLeave(empId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15));
            return new Object[]{empId, deptId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];

        ErpHrEmployee result = ormTemplate.runInSession(session -> employeeBiz.transferEmployee(
                empId, deptId, null, null,
                LocalDate.of(2026, 7, 10),
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX));

        assertNotNull(result, "调动日期落入 APPROVED 休假区间应仅告警不阻塞");
        assertEquals(deptId, result.getDepartmentId());
    }

    @Test
    public void testTransferProbationEmployeeAllowed() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-PROBATION", ErpHrConstants.EMPLOYMENT_PROBATION);
            Long deptId = seedDepartment("DEPT-PROB");
            return new Object[]{empId, deptId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];

        ErpHrEmployee result = ormTemplate.runInSession(session -> employeeBiz.transferEmployee(
                empId, deptId, null, null,
                LocalDate.of(2026, 7, 8),
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX));
        assertEquals(deptId, result.getDepartmentId(), "PROBATION 状态员工应允许调动");
    }

    // ---------- helpers ----------

    private ErpHrEmploymentContract findActiveContract(Long empId) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", empId),
                eq("status", ErpHrConstants.CONTRACT_STATUS_ACTIVE)));
        q.setLimit(1);
        java.util.List<ErpHrEmploymentContract> list =
                daoProvider.daoFor(ErpHrEmploymentContract.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long seedEmployee(String code, String employmentStatus) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = new ErpHrEmployee();
        emp.setCode(code);
        emp.setFirstName("测");
        emp.setLastName("试");
        emp.setFullName(code);
        emp.setGender("MALE");
        emp.setHireDate(LocalDate.of(2025, 1, 1));
        emp.setEmploymentStatus(employmentStatus);
        emp.setEmployeeType("FULL_TIME");
        dao.saveEntity(emp);
        return emp.getId();
    }

    private Long seedDepartment(String code) {
        IEntityDao<ErpHrDepartment> dao = daoProvider.daoFor(ErpHrDepartment.class);
        ErpHrDepartment d = new ErpHrDepartment();
        d.setCode(code);
        d.setName(code);
        dao.saveEntity(d);
        return d.getId();
    }

    private Long seedPosition(String code, Long departmentId) {
        IEntityDao<ErpHrPosition> dao = daoProvider.daoFor(ErpHrPosition.class);
        ErpHrPosition p = new ErpHrPosition();
        p.setCode(code);
        p.setName(code);
        p.setDepartmentId(departmentId);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedContract(String code, Long employeeId, LocalDate startDate, LocalDate endDate,
                              String contractType) {
        IEntityDao<ErpHrEmploymentContract> dao = daoProvider.daoFor(ErpHrEmploymentContract.class);
        ErpHrEmploymentContract c = new ErpHrEmploymentContract();
        c.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        c.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        c.setCode(code);
        c.setEmployeeId(employeeId);
        c.setContractType(contractType);
        c.setSignDate(startDate);
        c.setStartDate(startDate);
        c.setEndDate(endDate);
        c.setStatus(ErpHrConstants.CONTRACT_STATUS_ACTIVE);
        dao.saveEntity(c);
        return c.getId();
    }

    private Long seedApprovedLeave(Long employeeId, LocalDate startDate, LocalDate endDate) {
        IEntityDao<ErpHrLeaveRequest> dao = daoProvider.daoFor(ErpHrLeaveRequest.class);
        ErpHrLeaveRequest l = new ErpHrLeaveRequest();
        l.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        l.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        l.setCode("LV-" + employeeId + "-" + startDate.toString());
        l.setEmployeeId(employeeId);
        l.setLeaveType("ANNUAL");
        l.setStartDate(startDate);
        l.setEndDate(endDate);
        l.setStatus(ErpHrConstants.LEAVE_STATUS_APPROVED);
        dao.saveEntity(l);
        return l.getId();
    }
}
