package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrSalaryBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrPosition;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSalarySimulation;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.payroll.PayrollCalculator;
import io.nop.api.core.beans.TreeBean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.exceptions.UnknownEntityException;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 薪酬模拟 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 createSimulation/adjustItem/applyBatchAdjustment/convertToFormal 四个 per-mutation Processor 共用的加载、
 * 源薪酬解析、覆盖收集、批量重算、冲突检测辅助（单一真相源）。子类只编排单 mutation 步骤顺序，模拟语义不变（payroll-simulation.md）。
 */
public abstract class AbstractErpHrSalarySimulationProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpHrSalaryBiz salaryBiz;

    @Inject
    PayrollCalculator payrollCalculator;

    static final List<String> SALARY_ITEM_CODES = Arrays.asList(
            "basicSalary", "positionAllowance", "performanceBonus", "overtimePay",
            "mealAllowance", "transportAllowance", "otherAllowance",
            "grossSalary", "socialInsurance", "housingFund", "taxAmount",
            "otherDeductions", "netSalary");

    protected IEntityDao<ErpHrSalarySimulation> simulationDao() {
        return daoProvider.daoFor(ErpHrSalarySimulation.class);
    }

    protected ErpHrSalarySimulation requireSimulation(String simulationId, IServiceContext context) {
        ErpHrSalarySimulation simulation = simulationDao().getEntityById(simulationId);
        if (simulation == null) {
            throw new UnknownEntityException(simulationDao().getEntityName(), simulationId);
        }
        return simulation;
    }

    protected List<ErpHrSalary> findSourceSalaries(int year, int month, Map<String, Object> scope, IServiceContext context) {
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

    protected String buildSimulationCode(int year, int month) {
        return "SIM-" + year + String.format("%02d", month) + "-" + CoreMetrics.nanoTime();
    }

    protected ErpHrSalary requireSourceSalary(String employeeId, ErpHrSalarySimulation simulation) {
        int sourceYear;
        int sourceMonth;
        if (simulation.getSourceSalaryId() != null) {
            ErpHrSalary source = simulation.getSourceSalary();
            if (source != null) {
                sourceYear = source.getYear();
                sourceMonth = source.getMonth();
            } else {
                throw new NopException(app.erp.hr.service.ErpHrErrors.ERR_HR_SIMULATION_SOURCE_NOT_FOUND)
                        .param(app.erp.hr.service.ErpHrErrors.ARG_SOURCE_PERIOD, "salaryId=" + simulation.getSourceSalaryId());
            }
        } else {
            throw new NopException(app.erp.hr.service.ErpHrErrors.ERR_HR_SIMULATION_SOURCE_NOT_FOUND)
                    .param(app.erp.hr.service.ErpHrErrors.ARG_SOURCE_PERIOD, "no sourceSalaryId");
        }
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("year", sourceYear),
                eq("month", sourceMonth)));
        q.setLimit(1);
        IEntityDao<ErpHrSalary> dao = daoProvider.daoFor(ErpHrSalary.class);
        List<ErpHrSalary> list = dao.findAllByQuery(q);
        if (list.isEmpty()) {
            throw new NopException(app.erp.hr.service.ErpHrErrors.ERR_HR_SIMULATION_SOURCE_NOT_FOUND)
                    .param(app.erp.hr.service.ErpHrErrors.ARG_SOURCE_PERIOD, sourceYear + "-" + sourceMonth)
                    .param(app.erp.hr.service.ErpHrErrors.ARG_EMPLOYEE_ID, employeeId);
        }
        return list.get(0);
    }

    protected List<ErpHrSalarySimulationItemAdjustment> findAdjustmentsByEmployee(String simulationId, String employeeId) {
        IEntityDao<ErpHrSalarySimulationItemAdjustment> dao = daoProvider.daoFor(ErpHrSalarySimulationItemAdjustment.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("simulationId", simulationId),
                eq("employeeId", employeeId)));
        return dao.findAllByQuery(q);
    }

    protected ErpHrSalarySimulationItemAdjustment findAdjustment(String simulationId, String employeeId, String salaryItemCode) {
        IEntityDao<ErpHrSalarySimulationItemAdjustment> dao = daoProvider.daoFor(ErpHrSalarySimulationItemAdjustment.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("simulationId", simulationId),
                eq("employeeId", employeeId),
                eq("salaryItemCode", salaryItemCode)));
        q.setLimit(1);
        List<ErpHrSalarySimulationItemAdjustment> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected Map<String, BigDecimal> collectOverrides(String simulationId, String employeeId) {
        List<ErpHrSalarySimulationItemAdjustment> adjList = findAdjustmentsByEmployee(simulationId, employeeId);
        Map<String, BigDecimal> overrides = new LinkedHashMap<>();
        for (ErpHrSalarySimulationItemAdjustment a : adjList) {
            overrides.put(a.getSalaryItemCode(), a.getAdjustedAmount());
        }
        return overrides;
    }

    protected BigDecimal readSalaryField(ErpHrSalary salary, String fieldName) {
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

    @SuppressWarnings("unchecked")
    protected Map<String, EmployeeSimResult> computeAllEmployeeSims(ErpHrSalarySimulation simulation, IServiceContext context) {
        int sourceYear;
        int sourceMonth;
        ErpHrSalary representative = simulation.getSourceSalary();
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
        IEntityDao<ErpHrSalary> dao = daoProvider.daoFor(ErpHrSalary.class);
        List<ErpHrSalary> sources = dao.findAllByQuery(q);

        Map<String, EmployeeSimResult> result = new LinkedHashMap<>();
        int targetYear = simulation.getSimulationPeriodYear() != null
                ? simulation.getSimulationPeriodYear() : sourceYear;
        int targetMonth = simulation.getSimulationPeriodMonth() != null
                ? simulation.getSimulationPeriodMonth() : sourceMonth;
        for (ErpHrSalary src : sources) {
            String empId = src.getEmployeeId();
            Map<String, BigDecimal> overrides = collectOverrides(simulation.getId(), empId);
            ErpHrSalary simulated = payrollCalculator.recalculateWithOverrides(src, overrides, targetYear, targetMonth);
            result.put(empId, new EmployeeSimResult(src, simulated));
        }
        return result;
    }

    protected List<String> filterByScope(List<String> employeeIds, Map<String, Object> scope) {
        if (scope == null || scope.isEmpty()) {
            return employeeIds;
        }
        Object employeeIdsRaw = scope.get("employeeIds");
        if (employeeIdsRaw != null) {
            List<Object> ids = toStringIdList(employeeIdsRaw);
            List<String> result = new ArrayList<>();
            for (Object o : ids) {
                if (o instanceof String && employeeIds.contains(o)) {
                    result.add((String) o);
                }
            }
            return result;
        }
        Object departmentId = scope.get("departmentId");
        Object positionId = scope.get("positionId");
        if (departmentId == null && positionId == null) {
            return employeeIds;
        }
        List<String> matched = findEmployeeIdsByScope(departmentId, positionId);
        List<String> result = new ArrayList<>();
        for (String id : employeeIds) {
            if (matched.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    protected List<String> findEmployeeIdsByScope(Object departmentId, Object positionId) {
        if (departmentId == null && positionId == null) {
            return Collections.emptyList();
        }
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        QueryBean q = new QueryBean();
        List<TreeBean> filters = new ArrayList<>();
        if (departmentId != null) {
            filters.add(eq("departmentId", toStringId(departmentId)));
        }
        if (positionId != null) {
            filters.add(eq("positionId", toStringId(positionId)));
        }
        if (!filters.isEmpty()) {
            q.addFilter(and(filters.toArray(new TreeBean[0])));
        }
        q.setLimit(10000);
        List<ErpHrEmployee> employees = dao.findAllByQuery(q);
        List<String> ids = new ArrayList<>(employees.size());
        for (ErpHrEmployee e : employees) {
            ids.add(e.getId());
        }
        return ids;
    }

    protected void applyEmployeeScope(QueryBean salaryQuery, Map<String, Object> scope) {
        Object departmentId = scope.get("departmentId");
        Object positionId = scope.get("positionId");
        Object employeeIds = scope.get("employeeIds");
        if (employeeIds != null) {
            List<Object> ids = toStringIdList(employeeIds);
            if (!ids.isEmpty()) {
                salaryQuery.addFilter(in("employeeId", ids));
            }
            return;
        }
        List<String> matchedEmployeeIds = findEmployeeIdsByScope(departmentId, positionId);
        if (!matchedEmployeeIds.isEmpty()) {
            salaryQuery.addFilter(in("employeeId", matchedEmployeeIds));
        } else if (departmentId != null || positionId != null) {
            salaryQuery.addFilter(in("employeeId", Collections.singletonList("")));
        }
    }

    @SuppressWarnings("unchecked")
    protected List<Object> toStringIdList(Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        List<Object> result = new ArrayList<>();
        if (raw instanceof List) {
            for (Object o : (List<Object>) raw) {
                result.add(toStringId(o));
            }
        } else if (raw.getClass().isArray()) {
            for (Object o : (Object[]) raw) {
                result.add(toStringId(o));
            }
        } else {
            result.add(toStringId(raw));
        }
        return result;
    }

    protected String toStringId(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return (String) o;
        }
        return o.toString();
    }

    protected Map<String, String> loadEmployeeJobGrades(Set<String> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", new ArrayList<>(employeeIds)));
        q.setLimit(10000);
        List<ErpHrEmployee> employees = dao.findAllByQuery(q);
        Map<String, String> empToPosition = new LinkedHashMap<>();
        for (ErpHrEmployee e : employees) {
            if (e.getPositionId() != null) {
                empToPosition.put(e.getId(), e.getPositionId());
            }
        }
        if (empToPosition.isEmpty()) {
            return Collections.emptyMap();
        }
        IEntityDao<ErpHrPosition> posDao = daoProvider.daoFor(ErpHrPosition.class);
        QueryBean pq = new QueryBean();
        pq.addFilter(in("id", new ArrayList<>(empToPosition.values())));
        pq.setLimit(10000);
        List<ErpHrPosition> positions = posDao.findAllByQuery(pq);
        Map<String, String> posToGrade = new LinkedHashMap<>();
        for (ErpHrPosition p : positions) {
            posToGrade.put(p.getId(), p.getJobGrade());
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : empToPosition.entrySet()) {
            result.put(e.getKey(), posToGrade.get(e.getValue()));
        }
        return result;
    }

    protected BigDecimal resolveBatchAdjustment(String adjustType, ErpHrSalary source,
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

    protected void recordAdjustment(String simulationId, String employeeId, String salaryItemCode,
                                    BigDecimal originalAmount, BigDecimal adjustedAmount,
                                    String reason, IServiceContext context) {
        ErpHrSalarySimulationItemAdjustment adj = findAdjustment(simulationId, employeeId, salaryItemCode);
        boolean isNew = adj == null;
        if (isNew) {
            adj = daoProvider.daoFor(ErpHrSalarySimulationItemAdjustment.class).newEntity();
            adj.setSimulationId(simulationId);
            adj.setEmployeeId(employeeId);
            adj.setSalaryItemCode(salaryItemCode);
        }
        adj.setOriginalAmount(originalAmount);
        adj.setAdjustedAmount(adjustedAmount);
        adj.setAdjustmentReason(reason);
        adj.setAdjustedBy(context.getUserId());
        adj.setAdjustedAt(CoreMetrics.currentTimestamp());
        IEntityDao<ErpHrSalarySimulationItemAdjustment> dao = daoProvider.daoFor(ErpHrSalarySimulationItemAdjustment.class);
        if (isNew) {
            dao.saveEntity(adj);
        } else {
            dao.updateEntity(adj);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, BigDecimal> toStringKeyedMap(Object raw) {
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

    protected BigDecimal toBigDecimal(Object o) {
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

    protected boolean hasPaidSalary(String employeeId, int year, int month) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("year", year),
                eq("month", month),
                eq("paymentStatus", ErpHrConstants.PAYMENT_PAID)));
        q.setLimit(1);
        IEntityDao<ErpHrSalary> dao = daoProvider.daoFor(ErpHrSalary.class);
        return !dao.findAllByQuery(q).isEmpty();
    }

    protected boolean hasNonVoidSalary(String employeeId, int year, int month) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("year", year),
                eq("month", month),
                in("paymentStatus", Arrays.asList(
                        ErpHrConstants.PAYMENT_PENDING,
                        ErpHrConstants.PAYMENT_PAID))));
        q.setLimit(1);
        IEntityDao<ErpHrSalary> dao = daoProvider.daoFor(ErpHrSalary.class);
        return !dao.findAllByQuery(q).isEmpty();
    }

    protected Map<String, Object> conflictEntry(String employeeId, String conflictType, String message) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("employeeId", employeeId);
        entry.put("conflictType", conflictType);
        entry.put("message", message);
        return entry;
    }

    protected String periodLabel(Integer year, Integer month) {
        if (year == null || month == null) {
            return "";
        }
        return year + "-" + String.format("%02d", month);
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    protected static class EmployeeSimResult {
        final ErpHrSalary source;
        final ErpHrSalary simulated;

        EmployeeSimResult(ErpHrSalary source, ErpHrSalary simulated) {
            this.source = source;
            this.simulated = simulated;
        }
    }
}
