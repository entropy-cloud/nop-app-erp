package app.erp.prj.service.dashboard;

import app.erp.prj.dao.entity.ErpPrjBudget;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.api.core.time.CoreMetrics;
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
 * <p>项目毛利率指标（{@link #getProjectGrossMargin}）：聚合 {@link ErpPrjProjectPnl}
 * （由 ProjectPnlCalculator 周期物化），整体毛利率 = Σ grossProfit / Σ revenueAmount
 * （两列均为 DECIMAL 直接可加，避免 grossMarginPct 字符串加权语义歧义）。
 */
@BizModel("ErpPrjDashboard")
public class ErpPrjDashboardBizModel {

    /** 预警扫描的服务端硬上限：项目行数封顶，防止企业级数据量 OOM（类 D 裁决保留）。 */
    private static final int ALERT_MAX_ROWS = 5000;

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
            // DB 级 GROUP BY status + COUNT，避免全表物化
            QueryBean q = new QueryBean();
            q.setSourceName(ErpPrjProject.class.getName());
            QueryFieldBean dim = QueryFieldBean.mainField("status");
            QueryFieldBean cnt = QueryFieldBean.mainField("status").count().alias("cnt");
            q.setFields(java.util.Arrays.asList(dim, cnt));
            List<Map<String, Object>> rows = ormTemplate.findListByQuery(q);
            List<Map<String, Object>> result = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                String s = row.get("status") == null ? "UNKNOWN" : String.valueOf(row.get("status"));
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("status", s);
                r.put("count", ((Number) row.get("cnt")).longValue());
                result.add(r);
            }
            result.sort(Comparator.<Map<String, Object>, Long>comparing(
                    r -> (Long) r.get("count"), Comparator.reverseOrder()));
            return result;
        });
    }

    /** 成本超支预警（已发生成本 > 预算 的项目）。 */
    @BizQuery
    public List<Map<String, Object>> findCostOverrunAlert(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            // 类 D 裁决：逐行比对预算 vs 成本需项目明细，带硬上限的受限扫描
            QueryBean pq = new QueryBean();
            pq.setLimit(ALERT_MAX_ROWS);
            List<ErpPrjProject> projects = daoProvider.daoFor(ErpPrjProject.class).findAllByQuery(pq);
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
        LocalDate today = CoreMetrics.currentDate();
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

    /**
     * 项目毛利率 KPI 摘要（{@code dashboards.md §6}）。聚合 {@link ErpPrjProjectPnl}：
     * Σ revenueAmount、Σ totalCost、Σ grossProfit；整体毛利率 = Σ grossProfit / Σ revenueAmount
     * （两列均 DECIMAL 直接可加，避免 grossMarginPct 字符串列加权歧义）。
     *
     * @param projectId 可选项目过滤；为空时聚合全部 PnL 汇总记录
     */
    @BizQuery
    public Map<String, Object> getProjectGrossMargin(@Optional @Name("projectId") Long projectId,
                                                      IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpPrjProjectPnl> dao = daoProvider.daoFor(ErpPrjProjectPnl.class);
            QueryBean q = new QueryBean();
            if (projectId != null) {
                q.addFilter(eq("projectId", projectId));
            }
            List<ErpPrjProjectPnl> rows = dao.findAllByQuery(q);

            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            BigDecimal totalGrossProfit = BigDecimal.ZERO;
            Set<Long> projectIds = new HashSet<>();
            for (ErpPrjProjectPnl p : rows) {
                totalRevenue = totalRevenue.add(nz(p.getRevenueAmount()));
                totalCost = totalCost.add(nz(p.getTotalCost()));
                totalGrossProfit = totalGrossProfit.add(nz(p.getGrossProfit()));
                if (p.getProjectId() != null) projectIds.add(p.getProjectId());
            }
            BigDecimal grossMarginPct = totalRevenue.signum() > 0
                    ? totalGrossProfit.divide(totalRevenue, 4, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("projectCount", (long) projectIds.size());
            kpi.put("totalRevenue", totalRevenue);
            kpi.put("totalCost", totalCost);
            kpi.put("totalGrossProfit", totalGrossProfit);
            kpi.put("grossMarginPct", grossMarginPct);
            return kpi;
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

    /** DB 级 GROUP BY projectId + SUM(totalAmount)，返回 projectId → 总预算。 */
    private Map<Long, BigDecimal> loadBudgetByProject() {
        return sumByProject(ErpPrjBudget.class.getName(), "totalAmount");
    }

    /** DB 级 GROUP BY projectId + SUM(totalAmount)，返回 projectId → 已发生成本。 */
    private Map<Long, BigDecimal> loadCostByProject() {
        return sumByProject(ErpPrjCostCollection.class.getName(), "totalAmount");
    }

    private Map<Long, BigDecimal> sumByProject(String entityName, String amountField) {
        QueryBean q = new QueryBean();
        q.setSourceName(entityName);
        QueryFieldBean dim = QueryFieldBean.mainField("projectId");
        QueryFieldBean sumAmt = QueryFieldBean.mainField(amountField).sum().alias("total");
        q.setFields(java.util.Arrays.asList(dim, sumAmt));
        List<Map<String, Object>> rows = ormTemplate.findListByQuery(q);
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object pid = row.get("projectId");
            if (pid == null) continue;
            map.put(((Number) pid).longValue(), toBigDecimal(row.get("total")));
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

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(v.toString());
    }
}
