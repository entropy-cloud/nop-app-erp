package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrSalaryBiz;
import app.erp.hr.biz.IErpHrSalarySimulationBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSalarySimulation;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;
import app.erp.hr.service.ErpHrConfigs;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.payroll.PayrollCalculator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.TreeBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 薪酬模拟聚合根 BizModel（payroll-simulation.md §一/§二/§三/§四/§五）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展模拟生命周期：创建模拟、调整即时应变、对比视图、批量调薪、异常告警、审批状态机、转正式。
 *
 * <p>核算复用 0831-2 {@link PayrollCalculator#recalculateWithOverrides}（覆盖重算入口；计算规则零修改）。
 * 跨实体访问经 {@link IErpHrSalaryBiz}（同模块）+ {@link IDaoProvider}（ItemAdjustment/Employee 只读查询）。
 */
@BizModel("ErpHrSalarySimulation")
public class ErpHrSalarySimulationBizModel extends CrudBizModel<ErpHrSalarySimulation>
        implements IErpHrSalarySimulationBiz {

    @Inject
    PayrollCalculator payrollCalculator;
    @Inject
    IErpHrSalaryBiz salaryBiz;

    public ErpHrSalarySimulationBizModel() {
        setEntityName(ErpHrSalarySimulation.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalarySimulation createSimulation(@Name("sourceYear") int sourceYear,
                                                   @Name("sourceMonth") int sourceMonth,
                                                   @Name("simulationPeriodYear") int simulationPeriodYear,
                                                   @Name("simulationPeriodMonth") int simulationPeriodMonth,
                                                   @Name("simulationName") String simulationName,
                                                   @Name("employeeScope") Map<String, Object> employeeScope,
                                                   IServiceContext context) {
        List<ErpHrSalary> sourceSalaries = findSourceSalaries(sourceYear, sourceMonth, employeeScope, context);
        if (sourceSalaries.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_SOURCE_NOT_FOUND)
                    .param(ErpHrErrors.ARG_SOURCE_PERIOD, sourceYear + "-" + sourceMonth);
        }

        ErpHrSalarySimulation simulation = newEntity();
        simulation.setCode(buildSimulationCode(simulationPeriodYear, simulationPeriodMonth));
        simulation.setSourceSalaryId(sourceSalaries.get(0).getId());
        simulation.setSimulationPeriodYear(simulationPeriodYear);
        simulation.setSimulationPeriodMonth(simulationPeriodMonth);
        simulation.setSimulationName(simulationName);
        simulation.setStatus(ErpHrConstants.SIMULATION_STATUS_DRAFT);
        saveEntity(simulation, null, context);
        return simulation;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalary adjustItem(@Name("simulationId") Long simulationId,
                                  @Name("employeeId") Long employeeId,
                                  @Name("salaryItemCode") String salaryItemCode,
                                  @Name("adjustedAmount") BigDecimal adjustedAmount,
                                  @Name("reason") String reason,
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
            adj = new ErpHrSalarySimulationItemAdjustment();
            adj.setSimulationId(simulationId);
            adj.setEmployeeId(employeeId);
            adj.setSalaryItemCode(salaryItemCode);
        }
        adj.setOriginalAmount(originalAmount);
        adj.setAdjustedAmount(adjustedAmount != null ? adjustedAmount : BigDecimal.ZERO);
        adj.setAdjustmentReason(reason);
        adj.setAdjustedBy(context.getUserId());
        adj.setAdjustedAt(LocalDateTime.now());

        IEntityDao<ErpHrSalarySimulationItemAdjustment> adjDao = daoProvider().daoFor(ErpHrSalarySimulationItemAdjustment.class);
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

    @Override
    @BizQuery
    public ErpHrSalary getSimulatedSalary(@Name("simulationId") Long simulationId,
                                          @Name("employeeId") Long employeeId,
                                          IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        ErpHrSalary base = requireSourceSalary(employeeId, simulation);
        Map<String, BigDecimal> overrides = collectOverrides(simulationId, employeeId);
        int targetYear = simulation.getSimulationPeriodYear() != null
                ? simulation.getSimulationPeriodYear() : base.getYear();
        int targetMonth = simulation.getSimulationPeriodMonth() != null
                ? simulation.getSimulationPeriodMonth() : base.getMonth();
        return payrollCalculator.recalculateWithOverrides(base, overrides, targetYear, targetMonth);
    }

    @Override
    @BizQuery
    public List<ErpHrSalarySimulationItemAdjustment> listAdjustments(@Name("simulationId") Long simulationId,
                                                                     @Name("employeeId") Long employeeId,
                                                                     IServiceContext context) {
        return findAdjustmentsByEmployee(simulationId, employeeId);
    }

    @Override
    @BizQuery
    public Map<String, Object> getComparison(@Name("simulationId") Long simulationId,
                                             @Name("employeeId") Long employeeId,
                                             IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        ErpHrSalary source = requireSourceSalary(employeeId, simulation);
        ErpHrSalary simulated = getSimulatedSalary(simulationId, employeeId, context);
        ErpHrSalary current = findCurrentPeriodSalary(employeeId,
                simulation.getSimulationPeriodYear(), simulation.getSimulationPeriodMonth());

        int sourceYear = source.getYear();
        int sourceMonth = source.getMonth();
        // 上期：源期间前一月（用于跨期对比；若不可得则用源期间）
        ErpHrSalary previous = findPreviousPeriodSalary(employeeId, sourceYear, sourceMonth);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String itemCode : SALARY_ITEM_CODES) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemCode", itemCode);
            row.put("previousAmount", readSalaryField(previous != null ? previous : source, itemCode));
            row.put("currentAmount", current != null ? readSalaryField(current, itemCode) : null);
            row.put("simulatedAmount", readSalaryField(simulated, itemCode));
            BigDecimal baseline = current != null ? readSalaryField(current, itemCode)
                    : readSalaryField(source, itemCode);
            row.put("diff", readSalaryField(simulated, itemCode).subtract(nz(baseline)));
            rows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeeId", employeeId);
        result.put("simulationPeriod", periodLabel(simulation.getSimulationPeriodYear(),
                simulation.getSimulationPeriodMonth()));
        result.put("sourcePeriod", periodLabel(sourceYear, sourceMonth));
        result.put("rows", rows);
        return result;
    }

    @Override
    @BizQuery
    public List<Map<String, Object>> getDepartmentSummary(@Name("simulationId") Long simulationId,
                                                           IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        Map<Long, EmployeeSimResult> sims = computeAllEmployeeSims(simulation, context);
        Map<Long, Long> empToDept = loadEmployeeDepartments(sims.keySet());

        Map<Long, SummaryAccumulator> byDept = new LinkedHashMap<>();
        for (Map.Entry<Long, EmployeeSimResult> e : sims.entrySet()) {
            Long deptId = empToDept.get(e.getKey());
            if (deptId == null) {
                continue;
            }
            SummaryAccumulator acc = byDept.computeIfAbsent(deptId, k -> new SummaryAccumulator());
            acc.accumulate(e.getValue());
        }
        Map<Long, String> deptNames = loadDepartmentNames(byDept.keySet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, SummaryAccumulator> e : byDept.entrySet()) {
            result.add(e.getValue().toMap("departmentId", e.getKey(),
                    "departmentName", deptNames.get(e.getKey())));
        }
        return result;
    }

    @Override
    @BizQuery
    public List<Map<String, Object>> getProjectSummary(@Name("simulationId") Long simulationId,
                                                        IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        Map<Long, EmployeeSimResult> sims = computeAllEmployeeSims(simulation, context);

        Map<String, SummaryAccumulator> byItem = new LinkedHashMap<>();
        for (String itemCode : SALARY_ITEM_CODES) {
            byItem.put(itemCode, new SummaryAccumulator());
        }
        for (EmployeeSimResult r : sims.values()) {
            for (String itemCode : SALARY_ITEM_CODES) {
                BigDecimal src = readSalaryField(r.source, itemCode);
                BigDecimal sim = readSalaryField(r.simulated, itemCode);
                SummaryAccumulator acc = byItem.get(itemCode);
                acc.sourceTotal = acc.sourceTotal.add(src);
                acc.simulatedTotal = acc.simulatedTotal.add(sim);
                acc.count++;
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, SummaryAccumulator> e : byItem.entrySet()) {
            result.add(e.getValue().toMap("salaryItemCode", e.getKey(), null, null));
        }
        return result;
    }

    @Override
    @BizQuery
    public Map<String, Object> getCompanySummary(@Name("simulationId") Long simulationId,
                                                  IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        Map<Long, EmployeeSimResult> sims = computeAllEmployeeSims(simulation, context);
        SummaryAccumulator acc = new SummaryAccumulator();
        for (EmployeeSimResult r : sims.values()) {
            acc.accumulate(r);
        }
        return acc.toMap(null, null, null, null);
    }

    @Override
    @BizMutation
    @SingleSession
    public Map<String, Object> applyBatchAdjustment(@Name("simulationId") Long simulationId,
                                                     @Name("scope") Map<String, Object> scope,
                                                     @Name("adjustType") String adjustType,
                                                     @Name("value") Object value,
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

        List<Long> targetEmployeeIds = filterByScope(new ArrayList<>(sims.keySet()), scope);

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

    @Override
    @BizQuery
    public List<Map<String, Object>> findAnomalies(@Name("simulationId") Long simulationId,
                                                    IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        Map<Long, EmployeeSimResult> sims = computeAllEmployeeSims(simulation, context);
        BigDecimal netThreshold = ErpHrConfigs.simulationNetPayChangeThreshold();
        BigDecimal totalThreshold = ErpHrConfigs.simulationTotalChangeThreshold();
        boolean taxJumpAlert = ErpHrConfigs.simulationTaxBracketJumpAlert();

        List<Map<String, Object>> anomalies = new ArrayList<>();
        for (Map.Entry<Long, EmployeeSimResult> e : sims.entrySet()) {
            Long empId = e.getKey();
            EmployeeSimResult r = e.getValue();
            BigDecimal srcNet = nz(r.source.getNetSalary());
            BigDecimal simNet = nz(r.simulated.getNetSalary());
            BigDecimal srcGross = nz(r.source.getGrossSalary());
            BigDecimal simGross = nz(r.simulated.getGrossSalary());

            BigDecimal netChange = changeRatio(srcNet, simNet);
            if (netChange != null && netChange.abs().compareTo(netThreshold) > 0) {
                anomalies.add(anomalyEntry(empId, ErpHrConstants.ANOMALY_NET_PAY_CHANGE,
                        "实发变化 " + pct(netChange) + " 超阈值 " + pct(netThreshold),
                        "netChangeRatio", netChange));
            }
            BigDecimal grossChange = changeRatio(srcGross, simGross);
            if (grossChange != null && grossChange.abs().compareTo(totalThreshold) > 0) {
                anomalies.add(anomalyEntry(empId, ErpHrConstants.ANOMALY_TOTAL_CHANGE,
                        "应发变化 " + pct(grossChange) + " 超阈值 " + pct(totalThreshold),
                        "grossChangeRatio", grossChange));
            }
            if (taxJumpAlert && taxBracketJumped(r.source, r.simulated)) {
                anomalies.add(anomalyEntry(empId, ErpHrConstants.ANOMALY_TAX_BRACKET_JUMP,
                        "个税有效税率跳档（来源有效税率→模拟有效税率跨档）",
                        "sourceEffectiveRate", effectiveRate(r.source),
                        "simulatedEffectiveRate", effectiveRate(r.simulated)));
            }
        }
        return anomalies;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalarySimulation submitForReview(@Name("simulationId") Long simulationId,
                                                 IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        if (!ErpHrConstants.SIMULATION_STATUS_DRAFT.equals(simulation.getStatus())) {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_SIMULATION_ID, simulationId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, simulation.getStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, ErpHrConstants.SIMULATION_STATUS_DRAFT);
        }
        if (!hasAnyAdjustment(simulationId)) {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_NO_ADJUSTMENT)
                    .param(ErpHrErrors.ARG_SIMULATION_ID, simulationId);
        }
        simulation.setStatus(ErpHrConstants.SIMULATION_STATUS_IN_REVIEW);
        updateEntity(simulation, null, context);
        return simulation;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalarySimulation approve(@Name("simulationId") Long simulationId,
                                         @Name("reviewerId") Long reviewerId,
                                         IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        if (!ErpHrConstants.SIMULATION_STATUS_IN_REVIEW.equals(simulation.getStatus())) {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_SIMULATION_ID, simulationId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, simulation.getStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, ErpHrConstants.SIMULATION_STATUS_IN_REVIEW);
        }
        simulation.setStatus(ErpHrConstants.SIMULATION_STATUS_APPROVED);
        simulation.setReviewerId(reviewerId);
        simulation.setReviewedAt(LocalDateTime.now());
        updateEntity(simulation, null, context);
        return simulation;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalarySimulation reject(@Name("simulationId") Long simulationId,
                                        @Name("reason") String reason,
                                        IServiceContext context) {
        ErpHrSalarySimulation simulation = requireSimulation(simulationId, context);
        if (!ErpHrConstants.SIMULATION_STATUS_IN_REVIEW.equals(simulation.getStatus())) {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_ILLEGAL_TRANSITION)
                    .param(ErpHrErrors.ARG_SIMULATION_ID, simulationId)
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, simulation.getStatus())
                    .param(ErpHrErrors.ARG_EXPECTED_STATUS, ErpHrConstants.SIMULATION_STATUS_IN_REVIEW);
        }
        simulation.setStatus(ErpHrConstants.SIMULATION_STATUS_REJECTED);
        simulation.setNotes(reason);
        updateEntity(simulation, null, context);
        return simulation;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpHrSalarySimulation convertToFormal(@Name("simulationId") Long simulationId,
                                                 IServiceContext context) {
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
        simulation.setConvertedAt(LocalDateTime.now());
        updateEntity(simulation, null, context);
        return simulation;
    }

    @Override
    @BizQuery
    public List<ErpHrSalarySimulation> findSimulationsByConvertedSalary(@Name("salaryId") Long salaryId,
                                                                        IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("convertedSalaryId", salaryId));
        return findList(q, null, context);
    }

    List<ErpHrSalarySimulationItemAdjustment> findAdjustmentsByEmployee(Long simulationId, Long employeeId) {
        IEntityDao<ErpHrSalarySimulationItemAdjustment> dao = daoProvider().daoFor(ErpHrSalarySimulationItemAdjustment.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("simulationId", simulationId),
                eq("employeeId", employeeId)));
        return dao.findAllByQuery(q);
    }

    // ---------- helpers ----------

    ErpHrSalarySimulation requireSimulation(Long simulationId, IServiceContext context) {
        return requireEntity(String.valueOf(simulationId), null, context);
    }

    List<ErpHrSalary> findSourceSalaries(int year, int month, Map<String, Object> scope, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("year", year),
                eq("month", month),
                in("paymentStatus", Arrays.asList(
                        ErpHrConstants.PAYMENT_PENDING,
                        ErpHrConstants.PAYMENT_PAID))));
        if (scope != null) {
            applyEmployeeScope(q, scope);
        }
        return salaryBiz.findList(q, null, context);
    }

    void applyEmployeeScope(QueryBean salaryQuery, Map<String, Object> scope) {
        Object departmentId = scope.get("departmentId");
        Object positionId = scope.get("positionId");
        Object employeeIds = scope.get("employeeIds");
        if (employeeIds != null) {
            List<Object> ids = toLongList(employeeIds);
            if (!ids.isEmpty()) {
                salaryQuery.addFilter(in("employeeId", ids));
            }
            return;
        }
        List<Long> matchedEmployeeIds = findEmployeeIdsByScope(departmentId, positionId);
        if (!matchedEmployeeIds.isEmpty()) {
            salaryQuery.addFilter(in("employeeId", matchedEmployeeIds));
        } else if (departmentId != null || positionId != null) {
            salaryQuery.addFilter(in("employeeId", Collections.singletonList(-1L)));
        }
    }

    List<Long> findEmployeeIdsByScope(Object departmentId, Object positionId) {
        if (departmentId == null && positionId == null) {
            return Collections.emptyList();
        }
        IEntityDao<ErpHrEmployee> dao = daoProvider().daoFor(ErpHrEmployee.class);
        QueryBean q = new QueryBean();
        List<TreeBean> filters = new ArrayList<>();
        if (departmentId != null) {
            filters.add(eq("departmentId", toLong(departmentId)));
        }
        if (positionId != null) {
            filters.add(eq("positionId", toLong(positionId)));
        }
        if (!filters.isEmpty()) {
            q.addFilter(and(filters.toArray(new TreeBean[0])));
        }
        q.setLimit(10000);
        List<ErpHrEmployee> employees = dao.findAllByQuery(q);
        List<Long> ids = new ArrayList<>(employees.size());
        for (ErpHrEmployee e : employees) {
            ids.add(e.getId());
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    List<Object> toLongList(Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        List<Object> result = new ArrayList<>();
        if (raw instanceof List) {
            for (Object o : (List<Object>) raw) {
                result.add(toLong(o));
            }
        } else if (raw.getClass().isArray()) {
            for (Object o : (Object[]) raw) {
                result.add(toLong(o));
            }
        } else {
            result.add(toLong(raw));
        }
        return result;
    }

    Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return Long.parseLong(o.toString());
    }

    ErpHrSalary requireSourceSalary(Long employeeId, ErpHrSalarySimulation simulation) {
        int sourceYear;
        int sourceMonth;
        if (simulation.getSourceSalaryId() != null) {
            // 内部只读访问：经 daoProvider 直接读，不走代理管道（context 不可得；对齐 PayrollCalculator 读 master 模式）
            ErpHrSalary source = daoProvider().daoFor(ErpHrSalary.class)
                    .getEntityById(simulation.getSourceSalaryId());
            if (source != null) {
                sourceYear = source.getYear();
                sourceMonth = source.getMonth();
            } else {
                throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_SOURCE_NOT_FOUND)
                        .param(ErpHrErrors.ARG_SOURCE_PERIOD, "salaryId=" + simulation.getSourceSalaryId());
            }
        } else {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_SOURCE_NOT_FOUND)
                    .param(ErpHrErrors.ARG_SOURCE_PERIOD, "no sourceSalaryId");
        }
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("year", sourceYear),
                eq("month", sourceMonth)));
        q.setLimit(1);
        IEntityDao<ErpHrSalary> dao = daoProvider().daoFor(ErpHrSalary.class);
        List<ErpHrSalary> list = dao.findAllByQuery(q);
        if (list.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_SOURCE_NOT_FOUND)
                    .param(ErpHrErrors.ARG_SOURCE_PERIOD, sourceYear + "-" + sourceMonth)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId);
        }
        return list.get(0);
    }

    ErpHrSalarySimulationItemAdjustment findAdjustment(Long simulationId, Long employeeId, String salaryItemCode) {
        IEntityDao<ErpHrSalarySimulationItemAdjustment> dao = daoProvider().daoFor(ErpHrSalarySimulationItemAdjustment.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("simulationId", simulationId),
                eq("employeeId", employeeId),
                eq("salaryItemCode", salaryItemCode)));
        q.setLimit(1);
        List<ErpHrSalarySimulationItemAdjustment> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    Map<String, BigDecimal> collectOverrides(Long simulationId, Long employeeId) {
        List<ErpHrSalarySimulationItemAdjustment> adjList = findAdjustmentsByEmployee(simulationId, employeeId);
        Map<String, BigDecimal> overrides = new LinkedHashMap<>();
        for (ErpHrSalarySimulationItemAdjustment a : adjList) {
            overrides.put(a.getSalaryItemCode(), a.getAdjustedAmount());
        }
        return overrides;
    }

    BigDecimal readSalaryField(ErpHrSalary salary, String fieldName) {
        if (fieldName == null) {
            return BigDecimal.ZERO;
        }
        switch (fieldName) {
            case "basicSalary":
                return nz(salary.getBasicSalary());
            case "positionAllowance":
                return nz(salary.getPositionAllowance());
            case "performanceBonus":
                return nz(salary.getPerformanceBonus());
            case "overtimePay":
                return nz(salary.getOvertimePay());
            case "mealAllowance":
                return nz(salary.getMealAllowance());
            case "transportAllowance":
                return nz(salary.getTransportAllowance());
            case "otherAllowance":
                return nz(salary.getOtherAllowance());
            case "otherDeductions":
                return nz(salary.getOtherDeductions());
            default:
                return BigDecimal.ZERO;
        }
    }

    String buildSimulationCode(int year, int month) {
        return "SIM-" + year + String.format("%02d", month) + "-" + System.nanoTime();
    }

    static final List<String> SALARY_ITEM_CODES = Arrays.asList(
            "basicSalary", "positionAllowance", "performanceBonus", "overtimePay",
            "mealAllowance", "transportAllowance", "otherAllowance",
            "grossSalary", "socialInsurance", "housingFund", "taxAmount",
            "otherDeductions", "netSalary");

    ErpHrSalary findCurrentPeriodSalary(Long employeeId, Integer year, Integer month) {
        if (year == null || month == null) {
            return null;
        }
        return findOneSalary(employeeId, year, month);
    }

    ErpHrSalary findPreviousPeriodSalary(Long employeeId, int year, int month) {
        int prevYear = year;
        int prevMonth = month - 1;
        if (prevMonth < 1) {
            prevMonth = 12;
            prevYear = year - 1;
        }
        return findOneSalary(employeeId, prevYear, prevMonth);
    }

    ErpHrSalary findOneSalary(Long employeeId, int year, int month) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("year", year),
                eq("month", month)));
        q.setLimit(1);
        IEntityDao<ErpHrSalary> dao = daoProvider().daoFor(ErpHrSalary.class);
        List<ErpHrSalary> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    Map<Long, EmployeeSimResult> computeAllEmployeeSims(ErpHrSalarySimulation simulation, IServiceContext context) {
        int sourceYear;
        int sourceMonth;
        // 内部只读访问：经 daoProvider 直接读代表行（context 不可得；对齐 PayrollCalculator 读 master 模式）
        ErpHrSalary representative = simulation.getSourceSalaryId() != null
                ? daoProvider().daoFor(ErpHrSalary.class).getEntityById(simulation.getSourceSalaryId()) : null;
        if (representative == null) {
            return Collections.emptyMap();
        }
        sourceYear = representative.getYear();
        sourceMonth = representative.getMonth();

        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("year", sourceYear),
                eq("month", sourceMonth),
                in("paymentStatus", Arrays.asList(
                        ErpHrConstants.PAYMENT_PENDING,
                        ErpHrConstants.PAYMENT_PAID))));
        q.setLimit(10000);
        IEntityDao<ErpHrSalary> dao = daoProvider().daoFor(ErpHrSalary.class);
        List<ErpHrSalary> sources = dao.findAllByQuery(q);

        Map<Long, EmployeeSimResult> result = new LinkedHashMap<>();
        int targetYear = simulation.getSimulationPeriodYear() != null
                ? simulation.getSimulationPeriodYear() : sourceYear;
        int targetMonth = simulation.getSimulationPeriodMonth() != null
                ? simulation.getSimulationPeriodMonth() : sourceMonth;
        for (ErpHrSalary src : sources) {
            Long empId = src.getEmployeeId();
            Map<String, BigDecimal> overrides = collectOverrides(simulation.getId(), empId);
            ErpHrSalary simulated = payrollCalculator.recalculateWithOverrides(src, overrides, targetYear, targetMonth);
            result.put(empId, new EmployeeSimResult(src, simulated));
        }
        return result;
    }

    List<Long> filterByScope(List<Long> employeeIds, Map<String, Object> scope) {
        if (scope == null || scope.isEmpty()) {
            return employeeIds;
        }
        Object employeeIdsRaw = scope.get("employeeIds");
        if (employeeIdsRaw != null) {
            List<Object> ids = toLongList(employeeIdsRaw);
            List<Long> result = new ArrayList<>();
            for (Object o : ids) {
                if (o instanceof Long && employeeIds.contains(o)) {
                    result.add((Long) o);
                }
            }
            return result;
        }
        Object departmentId = scope.get("departmentId");
        Object positionId = scope.get("positionId");
        if (departmentId == null && positionId == null) {
            return employeeIds;
        }
        List<Long> matched = findEmployeeIdsByScope(departmentId, positionId);
        List<Long> result = new ArrayList<>();
        for (Long id : employeeIds) {
            if (matched.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    BigDecimal resolveBatchAdjustment(String adjustType, ErpHrSalary source,
                                      BigDecimal numericValue, Map<String, BigDecimal> levelMap,
                                      String jobGrade) {
        if (adjustType == null) {
            return null;
        }
        switch (adjustType) {
            case ErpHrConstants.BATCH_ADJUST_TYPE_FIXED:
                return numericValue != null ? numericValue : BigDecimal.ZERO;
            case ErpHrConstants.BATCH_ADJUST_TYPE_RATIO:
                return nz(source.getBasicSalary())
                        .multiply(numericValue != null ? numericValue : BigDecimal.ZERO);
            case ErpHrConstants.BATCH_ADJUST_TYPE_ALLOWANCE:
                return numericValue != null ? numericValue : BigDecimal.ZERO;
            case ErpHrConstants.BATCH_ADJUST_TYPE_LEVEL_MAP:
                if (levelMap == null || jobGrade == null) {
                    return null;
                }
                BigDecimal v = levelMap.get(jobGrade);
                return v != null ? v : BigDecimal.ZERO;
            default:
                return null;
        }
    }

    void recordAdjustment(Long simulationId, Long employeeId, String salaryItemCode,
                          BigDecimal originalAmount, BigDecimal adjustedAmount,
                          String reason, IServiceContext context) {
        ErpHrSalarySimulationItemAdjustment adj = findAdjustment(simulationId, employeeId, salaryItemCode);
        boolean isNew = adj == null;
        if (isNew) {
            adj = new ErpHrSalarySimulationItemAdjustment();
            adj.setSimulationId(simulationId);
            adj.setEmployeeId(employeeId);
            adj.setSalaryItemCode(salaryItemCode);
        }
        adj.setOriginalAmount(originalAmount);
        adj.setAdjustedAmount(adjustedAmount);
        adj.setAdjustmentReason(reason);
        adj.setAdjustedBy(context.getUserId());
        adj.setAdjustedAt(LocalDateTime.now());
        IEntityDao<ErpHrSalarySimulationItemAdjustment> dao = daoProvider().daoFor(ErpHrSalarySimulationItemAdjustment.class);
        if (isNew) {
            dao.saveEntity(adj);
        } else {
            dao.updateEntity(adj);
        }
    }

    Map<Long, Long> loadEmployeeDepartments(java.util.Set<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        IEntityDao<ErpHrEmployee> dao = daoProvider().daoFor(ErpHrEmployee.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", new ArrayList<>(employeeIds)));
        q.setLimit(10000);
        List<ErpHrEmployee> employees = dao.findAllByQuery(q);
        Map<Long, Long> result = new LinkedHashMap<>();
        for (ErpHrEmployee e : employees) {
            if (e.getDepartmentId() != null) {
                result.put(e.getId(), e.getDepartmentId());
            }
        }
        return result;
    }

    Map<Long, String> loadEmployeeJobGrades(java.util.Set<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        IEntityDao<ErpHrEmployee> dao = daoProvider().daoFor(ErpHrEmployee.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", new ArrayList<>(employeeIds)));
        q.setLimit(10000);
        List<ErpHrEmployee> employees = dao.findAllByQuery(q);
        Map<Long, Long> empToPosition = new LinkedHashMap<>();
        for (ErpHrEmployee e : employees) {
            if (e.getPositionId() != null) {
                empToPosition.put(e.getId(), e.getPositionId());
            }
        }
        if (empToPosition.isEmpty()) {
            return Collections.emptyMap();
        }
        IEntityDao<app.erp.hr.dao.entity.ErpHrPosition> posDao = daoProvider().daoFor(app.erp.hr.dao.entity.ErpHrPosition.class);
        QueryBean pq = new QueryBean();
        pq.addFilter(in("id", new ArrayList<>(empToPosition.values())));
        pq.setLimit(10000);
        List<app.erp.hr.dao.entity.ErpHrPosition> positions = posDao.findAllByQuery(pq);
        Map<Long, String> posToGrade = new LinkedHashMap<>();
        for (app.erp.hr.dao.entity.ErpHrPosition p : positions) {
            posToGrade.put(p.getId(), p.getJobGrade());
        }
        Map<Long, String> result = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> e : empToPosition.entrySet()) {
            result.put(e.getKey(), posToGrade.get(e.getValue()));
        }
        return result;
    }

    Map<Long, String> loadDepartmentNames(java.util.Set<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        IEntityDao<app.erp.hr.dao.entity.ErpHrDepartment> dao = daoProvider().daoFor(app.erp.hr.dao.entity.ErpHrDepartment.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", new ArrayList<>(departmentIds)));
        q.setLimit(10000);
        List<app.erp.hr.dao.entity.ErpHrDepartment> depts = dao.findAllByQuery(q);
        Map<Long, String> result = new LinkedHashMap<>();
        for (app.erp.hr.dao.entity.ErpHrDepartment d : depts) {
            result.put(d.getId(), d.getName());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    Map<String, BigDecimal> toStringKeyedMap(Object raw) {
        if (raw == null) {
            return Collections.emptyMap();
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        if (raw instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) raw).entrySet()) {
                result.put(e.getKey(), toBigDecimal(e.getValue()));
            }
        }
        return result;
    }

    BigDecimal toBigDecimal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        }
        if (o instanceof Number) {
            return new BigDecimal(o.toString());
        }
        return new BigDecimal(o.toString());
    }

    BigDecimal changeRatio(BigDecimal source, BigDecimal simulated) {
        if (source == null || source.signum() == 0) {
            return null;
        }
        return simulated.subtract(source).divide(source, 6, RoundingMode.HALF_UP);
    }

    BigDecimal effectiveRate(ErpHrSalary salary) {
        BigDecimal gross = nz(salary.getGrossSalary());
        if (gross.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return nz(salary.getTaxAmount()).divide(gross, 6, RoundingMode.HALF_UP);
    }

    boolean taxBracketJumped(ErpHrSalary source, ErpHrSalary simulated) {
        BigDecimal srcRate = effectiveRate(source);
        BigDecimal simRate = effectiveRate(simulated);
        // 有效税率跨 5 个百分点视为跳档（个税税率表档位间距约 5-10%）
        return simRate.subtract(srcRate).compareTo(new BigDecimal("0.05")) > 0;
    }

    Map<String, Object> anomalyEntry(Long employeeId, String anomalyType, String message,
                                     String detailKey, Object detailValue) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("employeeId", employeeId);
        entry.put("anomalyType", anomalyType);
        entry.put("message", message);
        if (detailKey != null) {
            entry.put(detailKey, detailValue);
        }
        return entry;
    }

    Map<String, Object> anomalyEntry(Long employeeId, String anomalyType, String message,
                                     String k1, Object v1, String k2, Object v2) {
        Map<String, Object> entry = anomalyEntry(employeeId, anomalyType, message, k1, v1);
        if (k2 != null) {
            entry.put(k2, v2);
        }
        return entry;
    }

    boolean hasAnyAdjustment(Long simulationId) {
        IEntityDao<ErpHrSalarySimulationItemAdjustment> dao = daoProvider().daoFor(ErpHrSalarySimulationItemAdjustment.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("simulationId", simulationId));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    boolean hasPaidSalary(Long employeeId, int year, int month) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("year", year),
                eq("month", month),
                eq("paymentStatus", ErpHrConstants.PAYMENT_PAID)));
        q.setLimit(1);
        IEntityDao<ErpHrSalary> dao = daoProvider().daoFor(ErpHrSalary.class);
        return !dao.findAllByQuery(q).isEmpty();
    }

    boolean hasNonVoidSalary(Long employeeId, int year, int month) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("year", year),
                eq("month", month),
                in("paymentStatus", Arrays.asList(
                        ErpHrConstants.PAYMENT_PENDING,
                        ErpHrConstants.PAYMENT_PAID))));
        q.setLimit(1);
        IEntityDao<ErpHrSalary> dao = daoProvider().daoFor(ErpHrSalary.class);
        return !dao.findAllByQuery(q).isEmpty();
    }

    Map<String, Object> conflictEntry(Long employeeId, String conflictType, String message) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("employeeId", employeeId);
        entry.put("conflictType", conflictType);
        entry.put("message", message);
        return entry;
    }

    String periodLabel(Integer year, Integer month) {
        if (year == null || month == null) {
            return "";
        }
        return year + "-" + String.format("%02d", month);
    }

    String pct(BigDecimal ratio) {
        if (ratio == null) {
            return "N/A";
        }
        return ratio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%";
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    static class EmployeeSimResult {
        final ErpHrSalary source;
        final ErpHrSalary simulated;

        EmployeeSimResult(ErpHrSalary source, ErpHrSalary simulated) {
            this.source = source;
            this.simulated = simulated;
        }
    }

    static class SummaryAccumulator {
        BigDecimal sourceGrossTotal = BigDecimal.ZERO;
        BigDecimal simulatedGrossTotal = BigDecimal.ZERO;
        BigDecimal sourceNetTotal = BigDecimal.ZERO;
        BigDecimal simulatedNetTotal = BigDecimal.ZERO;
        BigDecimal sourceTotal = BigDecimal.ZERO;
        BigDecimal simulatedTotal = BigDecimal.ZERO;
        int count = 0;

        void accumulate(EmployeeSimResult r) {
            sourceGrossTotal = sourceGrossTotal.add(nz(r.source.getGrossSalary()));
            simulatedGrossTotal = simulatedGrossTotal.add(nz(r.simulated.getGrossSalary()));
            sourceNetTotal = sourceNetTotal.add(nz(r.source.getNetSalary()));
            simulatedNetTotal = simulatedNetTotal.add(nz(r.simulated.getNetSalary()));
            sourceTotal = sourceTotal.add(nz(r.source.getGrossSalary()));
            simulatedTotal = simulatedTotal.add(nz(r.simulated.getGrossSalary()));
            count++;
        }

        Map<String, Object> toMap(Object groupKey, Object groupValue, Object nameKey, Object nameValue) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (groupKey != null) {
                m.put(groupKey.toString(), groupValue);
            }
            if (nameKey != null) {
                m.put(nameKey.toString(), nameValue);
            }
            m.put("employeeCount", count);
            m.put("sourceGrossTotal", sourceGrossTotal);
            m.put("simulatedGrossTotal", simulatedGrossTotal);
            m.put("grossDiff", simulatedGrossTotal.subtract(sourceGrossTotal));
            m.put("sourceNetTotal", sourceNetTotal);
            m.put("simulatedNetTotal", simulatedNetTotal);
            m.put("netDiff", simulatedNetTotal.subtract(sourceNetTotal));
            m.put("sourceTotal", sourceTotal);
            m.put("simulatedTotal", simulatedTotal);
            m.put("totalDiff", simulatedTotal.subtract(sourceTotal));
            return m;
        }
    }
}
