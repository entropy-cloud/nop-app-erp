package app.erp.qa.service.dashboard;

import app.erp.qa.dao.entity.ErpQaAction;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaSpcCapability;
import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.ErpQaConfigs;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 质量看板聚合入口（{@code dashboards.md §9}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合，
 * 镜像 {@code ErpFinDashboardBizModel} 范式。
 *
 * <p>KPI 口径：本期质检数取自 {@link ErpQaInspection}（count, 期内 inspectionDate）；
 * 合格率 = count(result=ACCEPTED) / count(总数)；不合格数 = count(result=REJECTED)；
 * 开放 NCR 数取自 {@link ErpQaNonConformance}（count status IN [OPEN, IN_REVIEW]）。
 *
 * <p>SPC 失控预警（{@link #getSpcOutOfControlWarning}）：失控图数取自 {@link ErpQaSpcSample}
 * （distinct chartId where isOutOfControl=true，失控状态已由 SpcRuleEngine 物化为样本列）；
 * INADEQUATE 能力图数取自 {@link ErpQaSpcCapability}（distinct chartId where capabilityLevel=INADEQUATE）；
 * 待处置 SPC NCR 数取自 {@link ErpQaNonConformance}（sourceType=SPC 且 status IN [OPEN, IN_REVIEW]）。
 * 后两段纳入经 config-gated（{@code erp-dash.qa-spc-include-inadequate} / {@code erp-dash.qa-spc-include-ncr}，默认 true）。
 *
 * <p>设计文档 §9「不合格原因 TOP」字段 {@code defectType} 在 ORM 未物化，本期以 {@code dispositionType}
 * （不合格处置决定：SCRAP/RETURN/CONCESSION/DOWNGRADE）为聚合维度——语义最接近且为规范枚举。
 */
@BizModel("ErpQaDashboard")
public class ErpQaDashboardBizModel {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("startDate") LocalDate startDate,
                                                @Optional @Name("endDate") LocalDate endDate,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = CoreMetrics.currentDate();
            LocalDate from = startDate != null ? startDate : today.withDayOfMonth(1);
            LocalDate to = endDate != null ? endDate : today;

            List<ErpQaInspection> inspections = loadInspectionsInRange(from, to);
            long total = inspections.size();
            long accepted = inspections.stream()
                    .filter(i -> ErpQaConstants.INSPECTION_RESULT_ACCEPTED.equals(i.getResult()))
                    .count();
            long rejected = inspections.stream()
                    .filter(i -> ErpQaConstants.INSPECTION_RESULT_REJECTED.equals(i.getResult()))
                    .count();
            double passRate = total > 0 ? (double) accepted / (double) total : 0.0;
            long openNcrCount = countOpenNcrs();

            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("startDate", from);
            kpi.put("endDate", to);
            kpi.put("inspectionCount", total);
            kpi.put("passRate", passRate);
            kpi.put("rejectedCount", rejected);
            kpi.put("openNcrCount", openNcrCount);
            return kpi;
        });
    }

    /** 合格率趋势（近 N 月按月合格率）。 */
    @BizQuery
    public List<Map<String, Object>> getDashboardTrend(@Optional @Name("months") Integer months,
                                                        IServiceContext context) {
        int n = months == null || months <= 0 ? 12 : months;
        LocalDate today = CoreMetrics.currentDate();
        LocalDate from = today.minusMonths(n - 1L).withDayOfMonth(1);
        return ormTemplate.runInSession(session -> {
            List<ErpQaInspection> inspections = loadInspectionsInRange(from, today);
            Map<String, long[]> statsByMonth = new LinkedHashMap<>();
            for (ErpQaInspection i : inspections) {
                LocalDate d = i.getInspectionDate();
                if (d == null) continue;
                String key = d.getYear() + "-" + String.format("%02d", d.getMonthValue());
                long[] stats = statsByMonth.computeIfAbsent(key, k -> new long[2]);
                stats[1]++;
                if (ErpQaConstants.INSPECTION_RESULT_ACCEPTED.equals(i.getResult())) stats[0]++;
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                LocalDate m = from.plusMonths(i);
                String key = m.getYear() + "-" + String.format("%02d", m.getMonthValue());
                long[] stats = statsByMonth.getOrDefault(key, new long[]{0, 0});
                double rate = stats[1] > 0 ? (double) stats[0] / (double) stats[1] : 0.0;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("month", key);
                row.put("total", stats[1]);
                row.put("accepted", stats[0]);
                row.put("passRate", rate);
                rows.add(row);
            }
            return rows;
        });
    }

    /** 不合格原因 TOP（按 dispositionType 聚合降序）。 */
    @BizQuery
    public List<Map<String, Object>> findDefectTopN(@Optional @Name("limit") Integer limit,
                                                     IServiceContext context) {
        int topN = limit == null || limit <= 0 ? 10 : limit;
        return ormTemplate.runInSession(session -> {
            // DB 级 GROUP BY dispositionType + COUNT，避免全表物化
            QueryBean q = new QueryBean();
            q.setSourceName(ErpQaNonConformance.class.getName());
            QueryFieldBean dim = QueryFieldBean.mainField("dispositionType");
            QueryFieldBean cnt = QueryFieldBean.mainField("dispositionType").count().alias("cnt");
            q.setFields(Arrays.asList(dim, cnt));
            List<Map<String, Object>> rows = ormTemplate.findListByQuery(q);
            List<Map<String, Object>> grouped = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                String d = row.get("dispositionType") == null ? "UNSPECIFIED" : String.valueOf(row.get("dispositionType"));
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("dispositionType", d);
                r.put("count", ((Number) row.get("cnt")).longValue());
                grouped.add(r);
            }
            grouped.sort(Comparator.<Map<String, Object>, Long>comparing(
                    r -> (Long) r.get("count"), Comparator.reverseOrder()));
            List<Map<String, Object>> result = new ArrayList<>();
            grouped.stream().limit(topN).forEach(result::add);
            return result;
        });
    }

    /**
     * CAPA 逾期预警（Action.dueDate 早于 today-minus-overdueDays 且 status != COMPLETED）。
     * 阈值经 {@code erp-dash.qa-capa-overdue-days} 配置（默认 0=直接 < today 比对）。
     */
    @BizQuery
    public List<Map<String, Object>> findCapaOverdueAlert(IServiceContext context) {
        int overdueDays = AppConfig.var(
                ErpQaConstants.CONFIG_DASH_QA_CAPA_OVERDUE_DAYS,
                ErpQaConstants.DEFAULT_DASH_QA_CAPA_OVERDUE_DAYS);
        LocalDate today = CoreMetrics.currentDate();
        LocalDate cutoff = today.minusDays(overdueDays);
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpQaAction> dao = daoProvider.daoFor(ErpQaAction.class);
            QueryBean q = new QueryBean();
            q.addFilter(ne("status", ErpQaConstants.ACTION_STATUS_COMPLETED));
            List<ErpQaAction> actions = dao.findAllByQuery(q);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpQaAction a : actions) {
                LocalDate due = a.getDueDate();
                if (due != null && due.isBefore(cutoff)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("actionId", a.getId());
                    row.put("actionType", a.getActionType());
                    row.put("dueDate", due);
                    row.put("status", a.getStatus());
                    long overdue = java.time.temporal.ChronoUnit.DAYS.between(due, today);
                    row.put("overdueDays", overdue);
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    /**
     * SPC 失控预警摘要（{@code dashboards.md §9}）。
     *
     * <p>聚合三段：
     * <ul>
     *   <li>失控图数：distinct {@code ErpQaSpcSample.chartId} where {@code isOutOfControl=true}；</li>
     *   <li>INADEQUATE 能力图数：distinct {@code ErpQaSpcCapability.chartId} where
     *       {@code capabilityLevel=INADEQUATE}（config-gated {@code erp-dash.qa-spc-include-inadequate}）；</li>
     *   <li>待处置 SPC NCR 数：{@code ErpQaNonConformance} where {@code sourceType=SPC} 且
     *       status IN [OPEN, IN_REVIEW]（config-gated {@code erp-dash.qa-spc-include-ncr}）。</li>
     * </ul>
     */
    @BizQuery
    public Map<String, Object> getSpcOutOfControlWarning(IServiceContext context) {
        boolean includeInadequate = ErpQaConfigs.isDashQaSpcIncludeInadequate();
        boolean includeNcr = ErpQaConfigs.isDashQaSpcIncludeNcr();
        return ormTemplate.runInSession(session -> {
            long outOfControlChartCount = countOutOfControlCharts();
            long inadequateCapabilityCount = includeInadequate ? countInadequateCapabilityCharts() : 0L;
            long openSpcNcrCount = includeNcr ? countOpenSpcNcrs() : 0L;

            Map<String, Object> warning = new LinkedHashMap<>();
            warning.put("outOfControlChartCount", outOfControlChartCount);
            warning.put("inadequateCapabilityCount", inadequateCapabilityCount);
            warning.put("openSpcNcrCount", openSpcNcrCount);
            warning.put("includeInadequate", includeInadequate);
            warning.put("includeNcr", includeNcr);
            return warning;
        });
    }

    /**
     * SPC 控制图可视化数据（{@code spc.md §SPC 控制图语义}，plan 2026-07-17-2010-1）。
     *
     * <p>同域只读聚合 {@link ErpQaSpcChart}（chartType + cl/ucl/lcl 三控制限，已由
     * {@code SpcControlLimitCalculator} 持久化于 chart 实体）+ {@link ErpQaSpcSample}（subgroupNo/mean/
     * isOutOfControl/violatedRules；计数型样本额外 defectRate/defectCount/inspectedCount）。
     * 返回结构化 DTO 供看板 echarts 渲染 line（样本均值/缺陷率）+ markLine（UCL/LCL/CL）+ 违规点高亮。
     *
     * <p>chartId 解析优先级（plan 2026-07-19-0120-2 Phase 1 Decision 增 attributes-chart-id fallback）：
     * 入参 &gt; config {@code erp-dash.qa-spc-default-chart-id} &gt; config
     * {@code erp-dash.qa-spc-default-attributes-chart-id} &gt; 最近一张 {@code ErpQaSpcChart}（按 id 降序）。
     * 空数据返回零值结构（非 {@code null}）。
     */
    @BizQuery
    public Map<String, Object> getSpcControlChartData(@Optional @Name("chartId") Long chartId,
                                                       IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            Long resolvedId = chartId != null ? chartId : ErpQaConfigs.getDashQaSpcDefaultChartId();
            if (resolvedId == null) {
                resolvedId = ErpQaConfigs.getDashQaSpcDefaultAttributesChartId();
            }
            ErpQaSpcChart chart = null;
            if (resolvedId != null) {
                chart = daoProvider.daoFor(ErpQaSpcChart.class).getEntityById(resolvedId);
            }
            if (chart == null) {
                chart = findLatestSpcChart();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            if (chart == null) {
                // 空数据零值结构（非 null）
                result.put("chartId", null);
                result.put("chartCode", null);
                result.put("chartName", null);
                result.put("chartType", null);
                result.put("cl", null);
                result.put("ucl", null);
                result.put("lcl", null);
                result.put("samples", new ArrayList<Map<String, Object>>());
                return result;
            }

            List<Map<String, Object>> samples = loadSpcSamples(chart.getId());
            result.put("chartId", chart.getId());
            result.put("chartCode", chart.getCode());
            result.put("chartName", chart.getName());
            result.put("chartType", chart.getChartType());
            result.put("cl", chart.getCl());
            result.put("ucl", chart.getUcl());
            result.put("lcl", chart.getLcl());
            result.put("samples", samples);
            return result;
        });
    }

    // ===================== helpers =====================

    private long countOutOfControlCharts() {
        IEntityDao<ErpQaSpcSample> dao = daoProvider.daoFor(ErpQaSpcSample.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isOutOfControl", Boolean.TRUE));
        Set<Long> chartIds = new HashSet<>();
        for (ErpQaSpcSample s : dao.findAllByQuery(q)) {
            if (s.getChartId() != null) chartIds.add(s.getChartId());
        }
        return chartIds.size();
    }

    private ErpQaSpcChart findLatestSpcChart() {
        IEntityDao<ErpQaSpcChart> dao = daoProvider.daoFor(ErpQaSpcChart.class);
        QueryBean q = new QueryBean();
        q.addOrderField("id", true);
        q.setLimit(1);
        List<ErpQaSpcChart> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<Map<String, Object>> loadSpcSamples(Long chartId) {
        IEntityDao<ErpQaSpcSample> dao = daoProvider.daoFor(ErpQaSpcSample.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("chartId", chartId));
        q.addOrderField("subgroupNo", false);
        List<ErpQaSpcSample> raw = dao.findAllByQuery(q);
        List<Map<String, Object>> rows = new ArrayList<>(raw.size());
        for (ErpQaSpcSample s : raw) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("subgroupNo", s.getSubgroupNo());
            row.put("mean", s.getMean());
            row.put("isOutOfControl", s.getIsOutOfControl());
            row.put("violatedRules", s.getViolatedRules());
            // 计数型字段（计量型 chart 的样本此三字段为 null，向前端透明不破坏计量型渲染）
            Integer defectCount = s.getDefectCount();
            Integer inspectedCount = s.getInspectedCount();
            if (defectCount != null) row.put("defectCount", defectCount);
            if (inspectedCount != null) row.put("inspectedCount", inspectedCount);
            if (defectCount != null && inspectedCount != null && inspectedCount > 0) {
                row.put("defectRate", (double) defectCount / (double) inspectedCount);
            }
            rows.add(row);
        }
        return rows;
    }

    private long countInadequateCapabilityCharts() {
        IEntityDao<ErpQaSpcCapability> dao = daoProvider.daoFor(ErpQaSpcCapability.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("capabilityLevel", ErpQaConstants.SPC_CAPABILITY_INADEQUATE));
        Set<Long> chartIds = new HashSet<>();
        for (ErpQaSpcCapability c : dao.findAllByQuery(q)) {
            if (c.getChartId() != null) chartIds.add(c.getChartId());
        }
        return chartIds.size();
    }

    private long countOpenSpcNcrs() {
        IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceType", ErpQaConstants.NCR_SOURCE_TYPE_SPC));
        q.addFilter(in("status", Arrays.asList(
                ErpQaConstants.NCR_STATUS_OPEN,
                ErpQaConstants.NCR_STATUS_IN_REVIEW)));
        return dao.countByQuery(q);
    }

    private List<ErpQaInspection> loadInspectionsInRange(LocalDate from, LocalDate to) {
        IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
        QueryBean q = new QueryBean();
        q.addFilter(ge("inspectionDate", from));
        q.addFilter(le("inspectionDate", to));
        return dao.findAllByQuery(q);
    }

    private long countOpenNcrs() {
        IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("status", Arrays.asList(
                ErpQaConstants.NCR_STATUS_OPEN,
                ErpQaConstants.NCR_STATUS_IN_REVIEW)));
        return dao.countByQuery(q);
    }
}
