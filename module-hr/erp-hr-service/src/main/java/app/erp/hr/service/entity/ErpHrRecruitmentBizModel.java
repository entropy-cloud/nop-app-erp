
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.biz.IErpHrEmploymentContractBiz;
import app.erp.hr.biz.IErpHrRecruitmentBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrRecruitment;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 招聘记录 BizModel（use-cases.md UC-HR-05）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展招聘状态机推进 OPEN→SCREENING→INTERVIEW→OFFERED→HIRED +
 * HIRED 自动创建员工 + 合同 + employeeId 回写。
 *
 * <p>跨实体访问：
 * <ul>
 *   <li>{@link IErpHrEmployeeBiz}：HIRED 时创建新员工；</li>
 *   <li>{@link IErpHrEmploymentContractBiz}：HIRED 时创建 ACTIVE 合同。</li>
 * </ul>
 *
 * <p>本期在扁平 {@link ErpHrRecruitment} 上实现状态机，不创建多实体拆分（Candidate/Interview/Scorecard/Offer）——
 * 归 successor（见 plan Deferred But Adjudicated）。
 */
@BizModel("ErpHrRecruitment")
public class ErpHrRecruitmentBizModel extends CrudBizModel<ErpHrRecruitment> implements IErpHrRecruitmentBiz {

    @Inject
    IErpHrEmployeeBiz employeeBiz;
    @Inject
    IErpHrEmploymentContractBiz employmentContractBiz;

    public ErpHrRecruitmentBizModel() {
        setEntityName(ErpHrRecruitment.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrRecruitment> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrRecruitment entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(CoreMetrics.today());
        }
        if (entity.getStatus() == null) {
            entity.setStatus(ErpHrConstants.RECRUITMENT_STATUS_OPEN);
        }
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrRecruitment moveToScreening(@Name("id") String id, IServiceContext context) {
        ErpHrRecruitment rec = requireEntity(id, null, context);
        requireStatus(rec, ErpHrConstants.RECRUITMENT_STATUS_OPEN, ErpHrConstants.RECRUITMENT_STATUS_SCREENING);
        rec.setStatus(ErpHrConstants.RECRUITMENT_STATUS_SCREENING);
        updateEntity(rec, null, context);
        return rec;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrRecruitment scheduleInterview(@Name("id") String id,
                                              @Name("interviewerId") Long interviewerId,
                                              @Name("interviewDate") LocalDate interviewDate,
                                              IServiceContext context) {
        ErpHrRecruitment rec = requireEntity(id, null, context);
        requireStatus(rec, ErpHrConstants.RECRUITMENT_STATUS_SCREENING, ErpHrConstants.RECRUITMENT_STATUS_INTERVIEW);
        rec.setInterviewerId(interviewerId);
        rec.setInterviewDate(interviewDate);
        rec.setStatus(ErpHrConstants.RECRUITMENT_STATUS_INTERVIEW);
        updateEntity(rec, null, context);
        return rec;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrRecruitment makeOffer(@Name("id") String id,
                                      @Name("offerSalary") BigDecimal offerSalary,
                                      IServiceContext context) {
        ErpHrRecruitment rec = requireEntity(id, null, context);
        requireStatus(rec, ErpHrConstants.RECRUITMENT_STATUS_INTERVIEW, ErpHrConstants.RECRUITMENT_STATUS_OFFERED);
        rec.setOfferSalary(offerSalary);
        rec.setStatus(ErpHrConstants.RECRUITMENT_STATUS_OFFERED);
        updateEntity(rec, null, context);
        return rec;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrRecruitment hire(@Name("id") String id,
                                 @Name("hiredDate") LocalDate hiredDate,
                                 IServiceContext context) {
        ErpHrRecruitment rec = requireEntity(id, null, context);
        requireStatus(rec, ErpHrConstants.RECRUITMENT_STATUS_OFFERED, ErpHrConstants.RECRUITMENT_STATUS_HIRED);
        rec.setHiredDate(hiredDate);
        rec.setStatus(ErpHrConstants.RECRUITMENT_STATUS_HIRED);

        ErpHrEmployee newEmployee = createEmployeeFromRecruitment(rec, hiredDate, context);
        rec.setEmployeeId(newEmployee.getId());
        updateEntity(rec, null, context);

        createContractForNewEmployee(rec, newEmployee, hiredDate, context);
        return rec;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrRecruitment reject(@Name("id") String id, IServiceContext context) {
        ErpHrRecruitment rec = requireEntity(id, null, context);
        if (ErpHrConstants.RECRUITMENT_STATUS_HIRED.equals(rec.getStatus())
                || ErpHrConstants.RECRUITMENT_STATUS_CLOSED.equals(rec.getStatus())
                || ErpHrConstants.RECRUITMENT_STATUS_REJECTED.equals(rec.getStatus())) {
            throw illegalTransition(rec, ErpHrConstants.RECRUITMENT_STATUS_REJECTED);
        }
        rec.setStatus(ErpHrConstants.RECRUITMENT_STATUS_REJECTED);
        updateEntity(rec, null, context);
        return rec;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrRecruitment close(@Name("id") String id, IServiceContext context) {
        ErpHrRecruitment rec = requireEntity(id, null, context);
        rec.setStatus(ErpHrConstants.RECRUITMENT_STATUS_CLOSED);
        updateEntity(rec, null, context);
        return rec;
    }

    // ---------- hire linkage ----------

    ErpHrEmployee createEmployeeFromRecruitment(ErpHrRecruitment rec, LocalDate hiredDate,
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

    void createContractForNewEmployee(ErpHrRecruitment rec, ErpHrEmployee employee,
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

    void requireStatus(ErpHrRecruitment rec, String expected, String target) {
        if (!expected.equals(rec.getStatus())) {
            throw illegalTransition(rec, target);
        }
    }

    NopException illegalTransition(ErpHrRecruitment rec, String target) {
        return new NopException(ErpHrErrors.ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpHrErrors.ARG_RECRUITMENT_ID, rec.getId())
                .param(ErpHrErrors.ARG_CURRENT_STATUS, rec.getStatus())
                .param(ErpHrErrors.ARG_EXPECTED_STATUS, target);
    }
}
