package app.erp.prj.service.dashboard;

import app.erp.prj.dao.entity.ErpPrjBudget;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 项目看板聚合入口（{@code dashboards.md §6}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合，
 * 镜像 {@code ErpFinDashboardBizModel} 范式。
 *
 * <p>KPI 口径：在手项目数取自 {@link ErpPrjProject}（status=OPEN count）；
 * 项目总预算取自 {@link ErpPrjBudget}（OPEN 项目 Σ totalAmount）；
 * 已发生成本取自 {@link ErpPrjCostCollection}（OPEN 项目 Σ totalAmount）；
 * 预算执行率 = 已发生成本 / 项目总预算。
 *
 * <p>项目毛利率指标 Non-Goal（{@code ErpPrjProjectPnl} 未物化，见 plan 2026-07-06-1606-1 Phase 1 Decision）。
 */
@BizModel("ErpPrjDashboard")
public class ErpPrjDashboardBizModel {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @BizQuery
    public Map<String, Object> getDashboardKpi(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpPrjProject> openProjects = loadOpenProjects();
            Set<Long> openProjectIds = collectIds(openProjects);

            BigDecimal totalBudget = sumBudgetForProjects(openProjectIds);
            BigDecimal incurredCost = sumCostForProjects(openProjectIds);
            BigDecimal executionRate = (totalBudget.signum() > 0)
                    ? incurredCost.divide(totalBudget, 4, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("openProjectCount", (long) openProjects.size());
            kpi.put("totalBudget", totalBudget);
            kpi.put("incurredCost", incurredCost);
            kpi.put("executionRate", executionRate);
            return kpi;
        });
    }

    /** 项目状态分布（按 status 聚合）。 */
    @BizQuery
    public List<Map<String, Object>> getProjectStatusDistribution(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpPrjProject> projects = daoProvider.daoFor(ErpPrjProject.class).findAll();
            Map<String, Long> countByStatus = new LinkedHashMap<>();
            for (ErpPrjProject p : projects) {
                String s = p.getStatus();
                if (s == null) s = "UNKNOWN";
                countByStatus.merge(s, 1L, Long::sum);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            countByStatus.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                    .forEach(e -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("status", e.getKey());
                        row.put("count", e.getValue());
                        rows.add(row);
                    });
            return rows;
        });
    }

    /** 成本超支预警（已发生成本 > 预算 的项目）。 */
    @BizQuery
    public List<Map<String, Object>> findCostOverrunAlert(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpPrjProject> projects = daoProvider.daoFor(ErpPrjProject.class).findAll();
            Map<Long, BigDecimal> budgetByProject = loadBudgetByProject();
            Map<Long, BigDecimal> costByProject = loadCostByProject();
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpPrjProject p : projects) {
                BigDecimal budget = budgetByProject.getOrDefault(p.getId(), BigDecimal.ZERO);
                BigDecimal cost = costByProject.getOrDefault(p.getId(), BigDecimal.ZERO);
                if (cost.signum() > 0 && cost.compareTo(budget) > 0) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("projectId", p.getId());
                    row.put("projectCode", p.getCode());
                    row.put("projectName", p.getName());
                    row.put("budget", budget);
                    row.put("incurredCost", cost);
                    row.put("overrunAmount", cost.subtract(budget));
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    /** 项目延期预警（endDate < today 且 status != COMPLETED）。 */
    @BizQuery
    public List<Map<String, Object>> findDelayedProjectAlert(IServiceContext context) {
        LocalDate today = LocalDate.now();
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
            QueryBean q = new QueryBean();
            q.addFilter(ne("status", ErpPrjConstants.PROJECT_STATUS_COMPLETED));
            List<ErpPrjProject> projects = dao.findAllByQuery(q);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpPrjProject p : projects) {
                LocalDate end = p.getEndDate();
                if (end != null && end.isBefore(today)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("projectId", p.getId());
                    row.put("projectCode", p.getCode());
                    row.put("projectName", p.getName());
                    row.put("endDate", end);
                    row.put("status", p.getStatus());
                    long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(end, today);
                    row.put("overdueDays", overdueDays);
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private List<ErpPrjProject> loadOpenProjects() {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpPrjConstants.PROJECT_STATUS_OPEN));
        return dao.findAllByQuery(q);
    }

    private BigDecimal sumBudgetForProjects(Set<Long> projectIds) {
        if (projectIds.isEmpty()) return BigDecimal.ZERO;
        IEntityDao<ErpPrjBudget> dao = daoProvider.daoFor(ErpPrjBudget.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("projectId", projectIds));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpPrjBudget b : dao.findAllByQuery(q)) {
            sum = sum.add(nz(b.getTotalAmount()));
        }
        return sum;
    }

    private BigDecimal sumCostForProjects(Set<Long> projectIds) {
        if (projectIds.isEmpty()) return BigDecimal.ZERO;
        IEntityDao<ErpPrjCostCollection> dao = daoProvider.daoFor(ErpPrjCostCollection.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("projectId", projectIds));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpPrjCostCollection c : dao.findAllByQuery(q)) {
            sum = sum.add(nz(c.getTotalAmount()));
        }
        return sum;
    }

    private Map<Long, BigDecimal> loadBudgetByProject() {
        IEntityDao<ErpPrjBudget> dao = daoProvider.daoFor(ErpPrjBudget.class);
        Map<Long, BigDecimal> map = new HashMap<>();
        for (ErpPrjBudget b : dao.findAll()) {
            if (b.getProjectId() == null) continue;
            map.merge(b.getProjectId(), nz(b.getTotalAmount()), BigDecimal::add);
        }
        return map;
    }

    private Map<Long, BigDecimal> loadCostByProject() {
        IEntityDao<ErpPrjCostCollection> dao = daoProvider.daoFor(ErpPrjCostCollection.class);
        Map<Long, BigDecimal> map = new HashMap<>();
        for (ErpPrjCostCollection c : dao.findAll()) {
            if (c.getProjectId() == null) continue;
            map.merge(c.getProjectId(), nz(c.getTotalAmount()), BigDecimal::add);
        }
        return map;
    }

    private static Set<Long> collectIds(List<ErpPrjProject> projects) {
        Set<Long> ids = new HashSet<>();
        for (ErpPrjProject p : projects) ids.add(p.getId());
        return ids;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
