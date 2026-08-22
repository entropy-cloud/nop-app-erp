package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSalarySimulation;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ErpHrSalarySimulation adjustItem per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含单项即时调整（DRAFT 守卫 + 源薪酬字段读取 + ItemAdjustment upsert + 覆盖合入即时重算），模拟语义不变（payroll-simulation.md §二）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrSalarySimulationProcessor}。
 */
public class ErpHrSalarySimulationAdjustItemProcessor extends AbstractErpHrSalarySimulationProcessor {

    public ErpHrSalary adjustItem(String simulationId,
                                  String employeeId,
                                  String salaryItemCode,
                                  BigDecimal adjustedAmount,
                                  String reason,
                                  IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        if (!ErpHrConstants.SIMULATION_STATUS_DRAFT.equals(simulation.getStatus())) {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_SIMULATION_ID, simulationId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, simulation.getStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, ErpHrConstants.SIMULATION_STATUS_DRAFT);
        }

        ErpHrSalary base = requireSourceSalary(employeeId, simulation);
        BigDecimal originalAmount = readSalaryField(base, salaryItemCode);

        // 先收集既有 overrides（避免 save 后未 flush 导致查询不可见）
        Map<String, BigDecimal> overrides = collectOverrides(simulationId, employeeId);

        ErpHrSalarySimulationItemAdjustment adj = findAdjustment(simulationId, employeeId, salaryItemCode);
        boolean isNew = adj == null;
        if (isNew) {
            adj = daoProvider.daoFor(ErpHrSalarySimulationItemAdjustment.class).newEntity();
            adj.setSimulationId(simulationId);
            adj.setEmployeeId(employeeId);
            adj.setSalaryItemCode(salaryItemCode);
        }
        adj.setOriginalAmount(originalAmount);
        adj.setAdjustedAmount(adjustedAmount != null ? adjustedAmount : BigDecimal.ZERO);
        adj.setAdjustmentReason(reason);
        adj.setAdjustedBy(context.getUserId());
        adj.setAdjustedAt(CoreMetrics.currentTimestamp());

        IEntityDao<ErpHrSalarySimulationItemAdjustment> adjDao = daoProvider.daoFor(ErpHrSalarySimulationItemAdjustment.class);
        if (isNew) {
            adjDao.saveEntity(adj);
        } else {
            adjDao.updateEntity(adj);
        }

        // 合入本次调整后即时应变（不重查 DB——save 可能未 flush）
        overrides.put(salaryItemCode, adj.getAdjustedAmount());
        int targetYear = simulation.getSimulationPeriodYear() != null
                ? simulation.getSimulationPeriodYear() : base.getYear();
        int targetMonth = simulation.getSimulationPeriodMonth() != null
                ? simulation.getSimulationPeriodMonth() : base.getMonth();
        return payrollCalculator.recalculateWithOverrides(base, overrides, targetYear, targetMonth);
    }
}
