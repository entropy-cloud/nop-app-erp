package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSalarySimulation;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;

import java.util.List;
import java.util.Map;

/**
 * ErpHrSalarySimulation createSimulation per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含模拟创建（源期薪酬查询 + Simulation 头落库），模拟语义不变（payroll-simulation.md §一）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrSalarySimulationProcessor}。
 */
public class ErpHrSalarySimulationCreateSimulationProcessor extends AbstractErpHrSalarySimulationProcessor {

    public ErpHrSalarySimulation createSimulation(int sourceYear,
                                                  int sourceMonth,
                                                  int simulationPeriodYear,
                                                  int simulationPeriodMonth,
                                                  String simulationName,
                                                  Map<String, Object> employeeScope,
                                                  IServiceContext context) {
        List<ErpHrSalary> sourceSalaries = findSourceSalaries(sourceYear, sourceMonth, employeeScope, context);
        if (sourceSalaries.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_SOURCE_NOT_FOUND)
                    .param(ErpHrErrors.ARG_SOURCE_PERIOD, sourceYear + "-" + sourceMonth);
        }

        ErpHrSalarySimulation simulation = simulationDao().newEntity();

        simulation.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        simulation.setCode(buildSimulationCode(simulationPeriodYear, simulationPeriodMonth));
        simulation.setSourceSalaryId(sourceSalaries.get(0).getId());
        simulation.setSimulationPeriodYear(simulationPeriodYear);
        simulation.setSimulationPeriodMonth(simulationPeriodMonth);
        simulation.setSimulationName(simulationName);
        simulation.setStatus(ErpHrConstants.SIMULATION_STATUS_DRAFT);
        simulationDao().saveEntity(simulation);
        return simulation;
    }
}
