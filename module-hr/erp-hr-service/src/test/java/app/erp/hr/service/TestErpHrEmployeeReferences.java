package app.erp.hr.service;

import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * F7 §3 {@code ErpHrEmployeeBizModel.countReferences} 后端测试（plan 2026-07-23-1145-2 Phase 3）。
 *
 * <p>覆盖：HR 域内引用计数（合同/工时/薪酬/考勤/休假）返回真实数据。
 * ErpHrEmployee 与 master-data ErpMdEmployee 为不同表，countReferences 在 HR 域内直接计数。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrEmployeeReferences extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrEmployeeBiz employeeBiz;

    @Test
    public void testCountReferencesReturnsRealData() {
        Long empId = ormTemplate.runInSession(session -> {
            Long id = seedEmployee("EMP-REF-HR", ErpHrConstants.EMPLOYMENT_ACTIVE);
            seedContract("CTC-REF-HR", id);
            seedLeave("LV-REF-HR", id);
            return id;
        });

        @SuppressWarnings("unchecked")
        Map<String, Long> refs = (Map<String, Long>) employeeBiz.countReferences(empId, CTX);
        assertEquals(1L, refs.get("contract"), "应统计 1 个合同");
        assertEquals(1L, refs.get("leave"), "应统计 1 个休假");
        assertEquals(0L, refs.get("timesheet"), "无工时记录应为 0");
        assertEquals(0L, refs.get("salary"), "无薪酬记录应为 0");
        assertEquals(0L, refs.get("attendance"), "无考勤记录应为 0");
    }

    // ---------- helpers（镜像 TestErpHrEmployeeTransfer）----------

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

    private void seedContract(String code, Long employeeId) {
        IEntityDao<ErpHrEmploymentContract> dao = daoProvider.daoFor(ErpHrEmploymentContract.class);
        ErpHrEmploymentContract c = new ErpHrEmploymentContract();
        c.setBusinessDate(LocalDate.of(2026, 7, 1));
        c.setCode(code);
        c.setEmployeeId(employeeId);
        c.setContractType("FIXED_TERM");
        c.setSignDate(LocalDate.of(2025, 1, 1));
        c.setStartDate(LocalDate.of(2025, 1, 1));
        c.setEndDate(LocalDate.of(2027, 1, 1));
        c.setStatus(ErpHrConstants.CONTRACT_STATUS_ACTIVE);
        dao.saveEntity(c);
    }

    private void seedLeave(String code, Long employeeId) {
        IEntityDao<ErpHrLeaveRequest> dao = daoProvider.daoFor(ErpHrLeaveRequest.class);
        ErpHrLeaveRequest l = new ErpHrLeaveRequest();
        l.setBusinessDate(LocalDate.of(2026, 7, 1));
        l.setCode(code);
        l.setEmployeeId(employeeId);
        l.setLeaveType("ANNUAL");
        l.setStartDate(LocalDate.of(2026, 8, 1));
        l.setEndDate(LocalDate.of(2026, 8, 3));
        l.setStatus(ErpHrConstants.LEAVE_STATUS_APPROVED);
        dao.saveEntity(l);
    }
}
