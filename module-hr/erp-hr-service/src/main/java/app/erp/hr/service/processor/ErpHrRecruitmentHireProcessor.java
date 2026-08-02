package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.biz.IErpHrEmploymentContractBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrRecruitment;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.exceptions.UnknownEntityException;
import jakarta.inject.Inject;

import java.time.LocalDate;

/**
 * ErpHrRecruitment hire per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含入职编排（OFFERED→HIRED 状态守卫 + 联动创建员工 + 创建 ACTIVE 合同 + employeeId 回写）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>假设：ErpHrErrors 未定义招聘记录 not-found 专用错误码，故 {@link #requireRecruitment} 复刻
 * {@code CrudBizModel.requireEntity} 的语义，不存在时抛平台 {@link UnknownEntityException}（与原 BizModel 行为一致）。
 */
public class ErpHrRecruitmentHireProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpHrEmployeeBiz employeeBiz;
    @Inject
    IErpHrEmploymentContractBiz employmentContractBiz;

    public ErpHrRecruitment hire(String id, LocalDate hiredDate, IServiceContext context) {
        ErpHrRecruitment rec = requireRecruitment(id, context);
        requireStatus(rec, ErpHrConstants.RECRUITMENT_STATUS_OFFERED, ErpHrConstants.RECRUITMENT_STATUS_HIRED);
        rec.setHiredDate(hiredDate);
        rec.setStatus(ErpHrConstants.RECRUITMENT_STATUS_HIRED);

        ErpHrEmployee newEmployee = createEmployeeFromRecruitment(rec, hiredDate, context);
        rec.setEmployeeId(newEmployee.getId());
        recruitmentDao().updateEntity(rec);

        createContractForNewEmployee(rec, newEmployee, hiredDate, context);
        return rec;
    }

    // ---------- hire linkage ----------

    protected ErpHrEmployee createEmployeeFromRecruitment(ErpHrRecruitment rec, LocalDate hiredDate,
                                                           IServiceContext context) {
        try {
            ErpHrEmployee employee = employeeBiz.newEntity();
            employee.setCode(generateEmployeeCode(rec));
            employee.setFirstName(extractFirstName(rec.getCandidateName()));
            employee.setLastName(extractLastName(rec.getCandidateName()));
            employee.setFullName(rec.getCandidateName());
            employee.setGender("MALE");
            employee.setHireDate(hiredDate);
            employee.setEmploymentStatus(ErpHrConstants.EMPLOYMENT_ACTIVE);
            employee.setEmployeeType("FULL_TIME");
            employee.setDepartmentId(rec.getDepartmentId());
            employee.setPositionId(rec.getPositionId());
            if (rec.getCandidateEmail() != null) {
                employee.setEmail(rec.getCandidateEmail());
            }
            if (rec.getCandidatePhone() != null) {
                employee.setMobilePhone(rec.getCandidatePhone());
            }
            employee.setOrgId(rec.getOrgId());
            employeeBiz.saveEntity(employee, null, context);
            return employee;
        } catch (Exception e) {
            throw new NopException(ErpHrErrors.ERR_RECRUITMENT_EMPLOYEE_CREATE_FAILED, e)
                    .param(ErpHrErrors.ARG_RECRUITMENT_ID, rec.getId());
        }
    }

    protected void createContractForNewEmployee(ErpHrRecruitment rec, ErpHrEmployee employee,
                                                 LocalDate hiredDate, IServiceContext context) {
        ErpHrEmploymentContract contract = employmentContractBiz.newEntity();
        contract.setBusinessDate(CoreMetrics.today());
        contract.setCode("HIRE-" + rec.getId() + "-" + hiredDate.toString());
        contract.setEmployeeId(employee.getId());
        contract.setContractType(ErpHrConstants.CONTRACT_TYPE_FIXED_TERM);
        contract.setSignDate(hiredDate);
        contract.setStartDate(hiredDate);
        if (rec.getOfferSalary() != null) {
            contract.setMonthlySalary(rec.getOfferSalary());
        }
        contract.setStatus(ErpHrConstants.CONTRACT_STATUS_ACTIVE);
        contract.setOrgId(rec.getOrgId());
        employmentContractBiz.saveEntity(contract, null, context);
    }

    static String generateEmployeeCode(ErpHrRecruitment rec) {
        return "EMP-" + rec.getId() + "-" + CoreMetrics.currentTimeMillis() % 100000;
    }

    static String extractFirstName(String candidateName) {
        if (candidateName == null || candidateName.isEmpty()) {
            return "新";
        }
        return candidateName.substring(0, 1);
    }

    static String extractLastName(String candidateName) {
        if (candidateName == null || candidateName.length() <= 1) {
            return "员工";
        }
        return candidateName.substring(1);
    }

    // ---------- validation ----------

    protected ErpHrRecruitment requireRecruitment(String id, IServiceContext context) {
        ErpHrRecruitment rec = recruitmentDao().getEntityById(Long.valueOf(id));
        if (rec == null) {
            throw new UnknownEntityException(recruitmentDao().getEntityName(), id);
        }
        return rec;
    }

    protected void requireStatus(ErpHrRecruitment rec, String expected, String target) {
        if (!expected.equals(rec.getStatus())) {
            throw illegalTransition(rec, target);
        }
    }

    protected NopException illegalTransition(ErpHrRecruitment rec, String target) {
        return new NopException(ErpHrErrors.ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpHrErrors.ARG_RECRUITMENT_ID, rec.getId())
                .param(ErpHrErrors.ARG_CURRENT_STATUS, rec.getStatus())
                .param(ErpHrErrors.ARG_EXPECTED_STATUS, target);
    }

    private IEntityDao<ErpHrRecruitment> recruitmentDao() {
        return daoProvider.daoFor(ErpHrRecruitment.class);
    }
}
