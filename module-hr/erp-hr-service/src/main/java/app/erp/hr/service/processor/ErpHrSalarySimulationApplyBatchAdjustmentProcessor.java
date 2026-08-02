package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSalarySimulation;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ErpHrSalarySimulation applyBatchAdjustment per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含批量调薪（DRAFT 守卫 + 范围过滤 + 按调薪类型解析 + 逐人 basicSalary 调整记录 + 即时重算汇总），模拟语义不变（payroll-simulation.md §四）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrSalarySimulationProcessor}。
 */
public class ErpHrSalarySimulationApplyBatchAdjustmentProcessor extends AbstractErpHrSalarySimulationProcessor {

    public Map<String, Object> applyBatchAdjustment(Long simulationId,
                                                    Map<String, Object> scope,
                                                    String adjustType,
                                                    Object value,
                                                    IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        if (!ErpHrConstants.SIMULATION_STATUS_DRAFT.equals(simulation.getStatus())) {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_SIMULATION_ID, simulationId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, simulation.getStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, ErpHrConstants.SIMULATION_STATUS_DRAFT);
        }

        Map<String, BigDecimal> levelMap = ErpHrConstants.BATCH_ADJUST_TYPE_LEVEL_MAP.equals(adjustType)
                ? toStringKeyedMap(value) : null;
        BigDecimal numericValue = levelMap == null ? toBigDecimal(value) : null;

        Map<Long, EmployeeSimResult> sims = computeAllEmployeeSims(simulation, context);
        Map<Long, String> empGrades = loadEmployeeJobGrades(sims.keySet());

        java.util.List<Long> targetEmployeeIds = filterByScope(new ArrayList<>(sims.keySet()), scope);

        int targetYear = simulation.getSimulationPeriodYear() != null
                ? simulation.getSimulationPeriodYear() : 0;
        int targetMonth = simulation.getSimulationPeriodMonth() != null
                ? simulation.getSimulationPeriodMonth() : 0;

        BigDecimal totalGrossIncrease = BigDecimal.ZERO;
        int affectedCount = 0;
        for (Long empId : targetEmployeeIds) {
            EmployeeSimResult r = sims.get(empId);
            BigDecimal adjustment = resolveBatchAdjustment(adjustType, r.source, numericValue,
                    levelMap, empGrades.get(empId));
            if (adjustment == null) {
                continue;
            }
            BigDecimal newBasic = nz(r.source.getBasicSalary()).add(adjustment);
            if (newBasic.signum() < 0) {
                newBasic = BigDecimal.ZERO;
            }
            recordAdjustment(simulationId, empId, "basicSalary",
                    nz(r.source.getBasicSalary()), newBasic,
                    ErpHrConstants.ADJUSTMENT_REASON_SALARY_CHANGE, context);

            // 即时应变：合入本次调整后内存重算（避免 save 未 flush 导致重查不可见）
            Map<String, BigDecimal> overrides = collectOverrides(simulationId, empId);
            overrides.put("basicSalary", newBasic);
            int ty = targetYear != 0 ? targetYear : r.source.getYear();
            int tm = targetMonth != 0 ? targetMonth : (r.source.getMonth() != null ? r.source.getMonth() : 0);
            ErpHrSalary newSim = payrollCalculator.recalculateWithOverrides(r.source, overrides, ty, tm);
            totalGrossIncrease = totalGrossIncrease.add(nz(newSim.getGrossSalary()).subtract(nz(r.source.getGrossSalary())));
            affectedCount++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("affectedCount", affectedCount);
        result.put("totalGrossIncrease", totalGrossIncrease);
        result.put("avgIncrease", affectedCount > 0
                ? totalGrossIncrease.divide(new BigDecimal(affectedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        return result;
    }
}
