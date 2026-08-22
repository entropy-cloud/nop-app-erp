package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrDepartmentBiz;
import app.erp.hr.biz.IErpHrEmploymentContractBiz;
import app.erp.hr.biz.IErpHrLeaveRequestBiz;
import app.erp.hr.biz.IErpHrPositionBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrPosition;
import app.erp.hr.service.ErpHrConfigs;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.statemachine.ErpHrEmployeeStateMachine;
import app.erp.hr.service.statemachine.ErpHrEmploymentContractStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpHrEmployee transferEmployee per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含员工调动编排（雇佣状态守卫 + 目标部门/职位校验 + 休假冲突告警 + 部门/职位/上级更新 + 合同处理）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpHrEmployeeTransferEmployeeProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpHrEmployeeTransferEmployeeProcessor.class);

    /**
     * 调动后新合同 code 总长上限，对齐 {@code module-hr/model/app-erp-hr.orm.xml} domain
     * {@code code} precision=50（{@link ErpHrEmploymentContract#getCode() code} 列绑该 domain，
     * 与 voucherCode 同精度，同 1430-1/1600-1 类 buildXxxCode overflow 缺陷）。Processor 不反向依赖 ORM domain
     * 元数据 API，故在此持有常量而非运行时读取 precision。
     */
    private static final int SUCCESSOR_CONTRACT_CODE_MAX_LENGTH = 50;

    /**
     * 超限路径追加的 MD5 摘要长度（hex 字符）。保留全长 active.code 指纹，使两条不同长 active.code 即使头部相同
     * 也不退化为同一新合同 code（避免唯一性冲突）。
     */
    private static final int SUCCESSOR_HASH_SUFFIX_LENGTH = 4;

    static final LocalDate MIN_QUERY_DATE = LocalDate.of(1970, 1, 1);
    static final LocalDate MAX_QUERY_DATE = LocalDate.of(2999, 12, 31);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpHrDepartmentBiz departmentBiz;
    @Inject
    IErpHrPositionBiz positionBiz;
    @Inject
    IErpHrEmploymentContractBiz employmentContractBiz;
    @Inject
    IErpHrLeaveRequestBiz leaveRequestBiz;
    @Inject
    ErpHrEmploymentContractStateMachine contractStateMachine;
    @Inject
    ErpHrEmployeeStateMachine employeeStateMachine;

    public ErpHrEmployee transferEmployee(String employeeId,
                                          String targetDepartmentId,
                                          String targetPositionId,
                                          String targetSuperiorId,
                                          LocalDate effectiveDate,
                                          String handleContract,
                                          IServiceContext context) {
        ErpHrEmployee employee = requireTransferableEmployee(employeeId, context);
        ErpHrDepartment targetDept = requireTargetDepartment(targetDepartmentId, context);
        if (targetPositionId != null) {
            requireTargetPosition(targetPositionId, targetDept.getId(), context);
        }

        warnIfLeaveConflict(employee.getId(), effectiveDate, context);

        employee.setDepartmentId(targetDept.getId());
        if (targetPositionId != null) {
            employee.setPositionId(targetPositionId);
        }
        if (targetSuperiorId != null) {
            employee.setSuperiorId(targetSuperiorId);
        }
        employeeDao().updateEntity(employee);

        resolveHandleContract(handleContract, employee, effectiveDate, context);

        return employee;
    }

    // ---------- validation gates ----------

    protected ErpHrEmployee requireTransferableEmployee(String employeeId, IServiceContext context) {
        ErpHrEmployee employee = employeeDao().getEntityById(employeeId);
        if (employee == null) {
            throw new NopException(ErpHrErrors.ERR_EMPLOYEE_NOT_TRANSFERABLE)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, null);
        }
        String status = employee.getEmploymentStatus();
        // 只读调动守卫委托 ErpHrEmployeeStateMachine（Bean 分类权威，契约 §4）：仅 ACTIVE/PROBATION 可调动。
        // ERR_EMPLOYEE_NOT_TRANSFERABLE 领域码对外不变（调动守卫是只读判断而非状态迁移，领域有专属错误码）。
        if (!isTransferable(status)) {
            throw new NopException(ErpHrErrors.ERR_EMPLOYEE_NOT_TRANSFERABLE)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, status);
        }
        return employee;
    }

    boolean isTransferable(String employmentStatus) {
        return employeeStateMachine.isTransferable(employmentStatus);
    }

    protected ErpHrDepartment requireTargetDepartment(String targetDepartmentId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("id", targetDepartmentId));
        q.setLimit(1);
        ErpHrDepartment dept = departmentBiz.findFirst(q, null, context);
        if (dept == null) {
            throw new NopException(ErpHrErrors.ERR_TRANSFER_TARGET_DEPT_NOT_FOUND)
                    .param(ErpHrErrors.ARG_TARGET_DEPARTMENT_ID, targetDepartmentId);
        }
        return dept;
    }

    protected ErpHrPosition requireTargetPosition(String targetPositionId, String expectedDepartmentId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("id", targetPositionId));
        q.setLimit(1);
        ErpHrPosition position = positionBiz.findFirst(q, null, context);
        if (position == null
                || (expectedDepartmentId != null
                    && position.getDepartmentId() != null
                    && !expectedDepartmentId.equals(position.getDepartmentId()))) {
            throw new NopException(ErpHrErrors.ERR_TRANSFER_TARGET_POSITION_NOT_FOUND)
                    .param(ErpHrErrors.ARG_TARGET_POSITION_ID, targetPositionId)
                    .param(ErpHrErrors.ARG_TARGET_DEPARTMENT_ID, expectedDepartmentId);
        }
        return position;
    }

    // ---------- leave conflict warn (config-gated, non-blocking) ----------

    protected void warnIfLeaveConflict(String employeeId, LocalDate effectiveDate, IServiceContext context) {
        if (!ErpHrConfigs.transferLeaveConflictWarn()) {
            return;
        }
        if (employeeId == null || effectiveDate == null) {
            return;
        }
        // xmeta 仅允许 dateBetween（非 le/ge），故用宽界 dateBetween 表达
        // startDate <= effectiveDate（下界放宽到 1970）且 endDate >= effectiveDate（上界放宽到 2999）
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("status", ErpHrConstants.LEAVE_STATUS_APPROVED),
                dateBetween("startDate", MIN_QUERY_DATE, effectiveDate),
                dateBetween("endDate", effectiveDate, MAX_QUERY_DATE)));
        q.setLimit(1);
        long count = leaveRequestBiz.findCount(q, context);
        if (count > 0) {
            LOG.warn("员工[{}]调动生效日期[{}]与已批准休假冲突，已告警不阻塞（UC-HR-08）", employeeId, effectiveDate);
        }
    }

    // ---------- contract handling ----------

    protected void resolveHandleContract(String handleContract, ErpHrEmployee employee, LocalDate effectiveDate,
                                         IServiceContext context) {
        String mode = normalizeHandleContract(handleContract);
        boolean shouldHandle;
        if (ErpHrConstants.TRANSFER_HANDLE_CONTRACT_YES.equals(mode)) {
            shouldHandle = true;
        } else if (ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO.equals(mode)) {
            shouldHandle = false;
        } else {
            shouldHandle = ErpHrConfigs.transferAutoHandleContract();
        }
        if (!shouldHandle) {
            return;
        }
        ErpHrEmploymentContract active = findActiveContract(employee.getId(), context);
        if (active != null) {
            // 固定来源态/目标态判断委托 ErpHrEmploymentContractStateMachine（Bean 矩阵权威，契约 §4/§7）：
            // terminate 仅 ACTIVE 合法。findActiveContract 已限定 status=ACTIVE，常规流程必过；非法边 Bean 抛
            // common 层码，此处映射领域 ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION（common 码作 cause）。
            try {
                contractStateMachine.assertCanTerminate(active.getStatus());
            } catch (NopException e) {
                throw new NopException(ErpHrErrors.ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION, e)
                        .param(ErpHrErrors.ARG_CONTRACT_ID, active.getId())
                        .param(ErpHrErrors.ARG_CURRENT_STATUS, active.getStatus());
            }
            active.setStatus(contractStateMachine.terminateTargetStatus());
            employmentContractBiz.updateEntity(active, null, context);
        }
        ErpHrEmploymentContract successor = newContractFrom(active, employee, effectiveDate);
        employmentContractBiz.saveEntity(successor, null, context);
    }

    static String normalizeHandleContract(String handleContract) {
        if (handleContract == null || handleContract.trim().isEmpty()) {
            return ErpHrConstants.TRANSFER_HANDLE_CONTRACT_AUTO;
        }
        String upper = handleContract.trim().toUpperCase();
        if (ErpHrConstants.TRANSFER_HANDLE_CONTRACT_YES.equals(upper)
                || ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO.equals(upper)) {
            return upper;
        }
        return ErpHrConstants.TRANSFER_HANDLE_CONTRACT_AUTO;
    }

    protected ErpHrEmploymentContract findActiveContract(String employeeId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("status", ErpHrConstants.CONTRACT_STATUS_ACTIVE)));
        q.setLimit(1);
        return employmentContractBiz.findFirst(q, null, context);
    }

    protected ErpHrEmploymentContract newContractFrom(ErpHrEmploymentContract active, ErpHrEmployee employee,
                                                      LocalDate effectiveDate) {
        ErpHrEmploymentContract c = employmentContractBiz.newEntity();
        c.setBusinessDate(CoreMetrics.today());
        c.setCode(buildSuccessorCode(employee, active, effectiveDate));
        c.setEmployeeId(employee.getId());
        c.setContractType(active != null && active.getContractType() != null
                ? active.getContractType()
                : "FIXED_TERM");
        c.setSignDate(effectiveDate);
        c.setStartDate(effectiveDate);
        if (active != null) {
            c.setEndDate(active.getEndDate());
            c.setProbationMonths(active.getProbationMonths());
            c.setWorkingHoursPerWeek(active.getWorkingHoursPerWeek());
            c.setAnnualSalary(active.getAnnualSalary());
            c.setMonthlySalary(active.getMonthlySalary());
            c.setSalaryCurrencyId(active.getSalaryCurrencyId());
            c.setSalaryPayMethod(active.getSalaryPayMethod());
            c.setSocialInsuranceBase(active.getSocialInsuranceBase());
            c.setHousingFundBase(active.getHousingFundBase());
        }
        c.setStatus(ErpHrConstants.CONTRACT_STATUS_ACTIVE);
        c.setOrgId(employee.getOrgId());
        return c;
    }

    /**
     * 生成调动后新劳动合同 code。短码（拼接结果 ≤ {@link #SUCCESSOR_CONTRACT_CODE_MAX_LENGTH}）路径
     * 保持原拼接 {@code "TRF-" + employeeId + "-" + effectiveDate + "-" + active.code} 逐字符不变；
     * 超限时进入截断 + 哈希摘要分支：优先保留 {@code "TRF-" + employeeId + "-" + effectiveDate} 固定段
     * 不截断（最坏 4 + 19(Long.MAX) + 1 + 10 = 34 &lt; 50，留 ≥16 给 active.code 压缩段），对 active.code
     * 段截取头部并追加 MD5 前 4 hex 摘要。
     *
     * <p>覆盖任意长度 employeeId + 任意 ISO effectiveDate + 任意长度 active.code 组合，均返回
     * ≤ {@link #SUCCESSOR_CONTRACT_CODE_MAX_LENGTH}，消除 code precision 50 的字符串右截断
     * （sqlState=22001）latent defect（0100-2 Follow-up 显式 successor）。
     */
    static String buildSuccessorCode(ErpHrEmployee employee, ErpHrEmploymentContract active,
                                     LocalDate effectiveDate) {
        String employeeIdStr = (employee != null && employee.getId() != null)
                ? String.valueOf(employee.getId()) : "0";
        String base = "TRF-" + employeeIdStr + "-" + effectiveDate.toString();
        String activeCode = (active != null && active.getCode() != null && !active.getCode().isEmpty())
                ? active.getCode() : null;
        if (activeCode == null) {
            return base;
        }
        String code = base + "-" + activeCode;
        if (code.length() <= SUCCESSOR_CONTRACT_CODE_MAX_LENGTH) {
            return code;
        }
        // 固定段 = "TRF-"(4) + employeeIdStr + "-"(1) + effectiveDate(10) + "-"(1,分隔符)；剩余预算给 active.code 压缩段。
        int budget = SUCCESSOR_CONTRACT_CODE_MAX_LENGTH
                - "TRF-".length() - employeeIdStr.length() - 1 - effectiveDate.toString().length() - 1;
        int headLen = Math.max(0, budget - SUCCESSOR_HASH_SUFFIX_LENGTH);
        String head = headLen > 0 ? activeCode.substring(0, Math.min(headLen, activeCode.length())) : "";
        String hash = StringHelper.md5Hash(activeCode).substring(0, SUCCESSOR_HASH_SUFFIX_LENGTH);
        return base + "-" + head + hash;
    }

    private IEntityDao<ErpHrEmployee> employeeDao() {
        return daoProvider.daoFor(ErpHrEmployee.class);
    }
}
