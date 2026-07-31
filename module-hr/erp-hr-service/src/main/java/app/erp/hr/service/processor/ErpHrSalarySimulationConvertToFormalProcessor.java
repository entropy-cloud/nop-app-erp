package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSalarySimulation;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ErpHrSalarySimulation convertToFormal per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含模拟转正式（APPROVED 守卫 + 逐人 PAID/重复冲突检测 + 模拟结果写正式薪酬 + Simulation 转 CONVERTED），
 * 触及薪酬保护区域，过账接线语义不变（payroll-simulation.md §五 / payroll.md）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrSalarySimulationProcessor}。
 */
public class ErpHrSalarySimulationConvertToFormalProcessor extends AbstractErpHrSalarySimulationProcessor {

    public ErpHrSalarySimulation convertToFormal(Long simulationId, IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        if (!ErpHrConstants.SIMULATION_STATUS_APPROVED.equals(simulation.getStatus())) {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_SIMULATION_ID, simulationId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, simulation.getStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, ErpHrConstants.SIMULATION_STATUS_APPROVED);
        }

        Map<Long, EmployeeSimResult> sims = computeAllEmployeeSims(simulation, context);
        int targetYear = simulation.getSimulationPeriodYear();
        int targetMonth = simulation.getSimulationPeriodMonth();
        String targetPeriod = periodLabel(targetYear, targetMonth);

        Long firstConvertedId = null;
        int convertedCount = 0;
        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (Map.Entry<Long, EmployeeSimResult> e : sims.entrySet()) {
            Long empId = e.getKey();
            ErpHrSalary simulated = e.getValue().simulated;

            if (hasPaidSalary(empId, targetYear, targetMonth)) {
                conflicts.add(conflictEntry(empId, "PAID_CONFLICT",
                        "目标期间 " + targetPeriod + " 已存在 PAID 正式薪酬"));
                continue;
            }
            if (hasNonVoidSalary(empId, targetYear, targetMonth)) {
                conflicts.add(conflictEntry(empId, "DUPLICATE",
                        "员工 " + empId + " 在目标期间 " + targetPeriod + " 已存在正式薪酬"));
                continue;
            }

            ErpHrSalary formal = salaryBiz.newEntity();

            formal.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
            formal.setEmployeeId(simulated.getEmployeeId());
            formal.setYear(targetYear);
            formal.setMonth(targetMonth);
            formal.setBasicSalary(simulated.getBasicSalary());
            formal.setPositionAllowance(simulated.getPositionAllowance());
            formal.setPerformanceBonus(simulated.getPerformanceBonus());
            formal.setOvertimePay(simulated.getOvertimePay());
            formal.setMealAllowance(simulated.getMealAllowance());
            formal.setTransportAllowance(simulated.getTransportAllowance());
            formal.setOtherAllowance(simulated.getOtherAllowance());
            formal.setGrossSalary(simulated.getGrossSalary());
            formal.setSocialInsurance(simulated.getSocialInsurance());
            formal.setHousingFund(simulated.getHousingFund());
            formal.setTaxAmount(simulated.getTaxAmount());
            formal.setOtherDeductions(simulated.getOtherDeductions());
            formal.setNetSalary(simulated.getNetSalary());
            formal.setActualWorkDays(simulated.getActualWorkDays());
            formal.setRequiredWorkDays(simulated.getRequiredWorkDays());
            formal.setTotalOvertimeHours(simulated.getTotalOvertimeHours());
            formal.setUnpaidLeaveDays(simulated.getUnpaidLeaveDays());
            formal.setCumulativeData(simulated.getCumulativeData());
            formal.setApproveStatus(ErpHrConstants.APPROVE_STATUS_UNSUBMITTED);
            formal.setPaymentStatus(ErpHrConstants.PAYMENT_PENDING);
            salaryBiz.saveEntity(formal, null, context);

            if (firstConvertedId == null) {
                firstConvertedId = formal.getId();
            }
            convertedCount++;
        }

        if (convertedCount == 0) {
            // 全员冲突——按最严重错误抛出
            boolean hasPaidConflict = false;
            for (Map<String, Object> c : conflicts) {
                if ("PAID_CONFLICT".equals(c.get("conflictType"))) {
                    hasPaidConflict = true;
                    break;
                }
            }
            if (hasPaidConflict) {
                throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_TARGET_PERIOD_CONFLICT)
                        .param(ErpHrErrors.ARG_TARGET_PERIOD, targetPeriod);
            }
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_EMPLOYEE_DUPLICATE)
                    .param(ErpHrErrors.ARG_TARGET_PERIOD, targetPeriod);
        }

        simulation.setStatus(ErpHrConstants.SIMULATION_STATUS_CONVERTED);
        simulation.setConvertedSalaryId(firstConvertedId);
        simulation.setConvertedAt(CoreMetrics.currentTimestamp());
        simulationDao().updateEntity(simulation);
        return simulation;
    }
}
