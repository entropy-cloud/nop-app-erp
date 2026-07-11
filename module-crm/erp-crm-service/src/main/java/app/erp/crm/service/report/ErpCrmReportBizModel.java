package app.erp.crm.service.report;

import app.erp.crm.dao.entity.ErpCrmForecast;
import app.erp.crm.dao.entity.ErpCrmForecastLine;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.service.ErpCrmErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.WebContentBean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.concurrent.executor.GlobalExecutors;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.eval.IEvalScope;
import io.nop.core.resource.IResource;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.resource.tpl.ITemplateOutput;
import io.nop.core.resource.tpl.ITextTemplateOutput;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import io.nop.report.core.engine.IReportEngine;
import io.nop.xlang.api.XLang;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * CRM 域报表渲染入口。注入平台 {@link IReportEngine}，按报表名解析 VFS 模板路径
 * （{@code /nop/main/report/crm/<name>.xpt.xml}）并渲染 html/xlsx/pdf。
 *
 * <p>镜像 {@code ErpMfgReportBizModel}/{@code ErpHrReportBizModel} 域隔离范式（…master-data→{@code /md/}，
 * CRM→{@code /crm/}）：不自建报表引擎，渲染逻辑全部委托给 {@code nop-report}；报表名经
 * {@link StringHelper#isValidVPath} 校验防路径注入。
 *
 * <p>两张种子报表：线索转化漏斗表（{@link ErpCrmLead} 按 stage 聚合 + 金额，对齐 {@code crm/README.md}）
 * + 销售预测准确率表（{@link ErpCrmForecast}/{@link ErpCrmForecastLine} 预测 commit/weighted vs 实际，
 * 对齐 {@code crm/sales-forecast.md}）。
 */
@BizModel("ErpCrmReport")
public class ErpCrmReportBizModel {

    /** CRM 报表模板 VFS 根路径（与其他域隔离）。 */
    static final String REPORT_PATH_PREFIX = "/nop/main/report/crm/";

    /** 模板内统一引用的数据集变量名（单元格 {@code *=^ds!<field>}）。 */
    static final String DS_VAR = "ds";

    private static final Set<String> ALLOWED_RENDER_TYPES = new HashSet<>(Arrays.asList("html", "xlsx", "pdf"));

    @Inject
    IReportEngine reportEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ===================== 渲染入口 =====================

    @BizQuery
    public String renderHtml(@Name("reportName") String reportName,
                             @Optional @Name("data") Map<String, Object> data,
                             IServiceContext context) {
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);
        ITextTemplateOutput output = reportEngine.getHtmlRenderer(path);
        return output.generateText(scope);
    }

    @BizQuery
    public WebContentBean download(@Name("reportName") String reportName,
                                   @Name("renderType") String renderType,
                                   @Optional @Name("data") Map<String, Object> data,
                                   IServiceContext context) {
        if (!ALLOWED_RENDER_TYPES.contains(renderType)) {
            throw new NopException(ErpCrmErrors.ERR_REPORT_RENDER_TYPE_INVALID)
                    .param(ErpCrmErrors.ARG_RENDER_TYPE, renderType);
        }
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);

        ITemplateOutput output = reportEngine.getRenderer(path, renderType);
        IResource resource = ResourceHelper.getTempResource("crm-rpt");
        try {
            output.generateToResource(resource, scope);
            String fileName = baseName(reportName) + "." + renderType;
            WebContentBean content = new WebContentBean("application/octet-stream", resource.toFile(), fileName);
            GlobalExecutors.globalTimer().schedule(() -> {
                resource.delete();
                return null;
            }, 5, TimeUnit.MINUTES);
            return content;
        } catch (Exception e) {
            resource.delete();
            throw NopException.adapt(e);
        }
    }

    // ===================== 路径解析与防注入 =====================

    String resolveReportPath(String reportName) {
        if (StringHelper.isEmpty(reportName) || !StringHelper.isValidVPath(reportName)) {
            throw new NopException(ErpCrmErrors.ERR_REPORT_NAME_INVALID)
                    .param(ErpCrmErrors.ARG_REPORT_NAME, reportName);
        }
        String name = reportName;
        if (!name.endsWith(".xpt.xml") && !name.endsWith(".xpt.xlsx")) {
            name = name + ".xpt.xml";
        }
        return REPORT_PATH_PREFIX + name;
    }

    private String baseName(String reportName) {
        String n = reportName;
        int slash = n.lastIndexOf('/');
        if (slash >= 0) n = n.substring(slash + 1);
        if (n.endsWith(".xpt.xml")) n = StringHelper.removeTail(n, ".xpt.xml");
        else if (n.endsWith(".xpt.xlsx")) n = StringHelper.removeTail(n, ".xpt.xlsx");
        return n;
    }

    private Map<String, Object> mergeData(Map<String, Object> data) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (data != null) merged.putAll(data);
        return merged;
    }

    private void prepareDataset(String reportName, Map<String, Object> data, IServiceContext context) {
        String key = baseName(reportName);
        if (data.containsKey(DS_VAR)) return;
        switch (key) {
            case "lead-conversion-funnel":
                data.put(DS_VAR, buildLeadConversionFunnelDataset());
                break;
            case "forecast-accuracy":
                data.put(DS_VAR, buildForecastAccuracyDataset(asLong(data, "forecastId")));
                break;
            default:
                break;
        }
    }

    private static Long asLong(Map<String, Object> data, String k) {
        if (data == null) return null;
        Object v = data.get(k);
        return v == null ? null : Long.valueOf(v.toString());
    }

    // ===================== 数据集构造（也作 @BizQuery 供前端取原始数据） =====================

    /** 线索转化漏斗数据集：按 stage 聚合线索数 + 金额，对齐 {@code crm/README.md}。 */
    @BizQuery
    public List<Map<String, Object>> leadConversionFunnelData(IServiceContext context) {
        return buildLeadConversionFunnelDataset();
    }

    /** 销售预测准确率数据集：预测 commit/weighted/bestCase + 期间汇总，对齐 {@code crm/sales-forecast.md}。 */
    @BizQuery
    public List<Map<String, Object>> forecastAccuracyData(@Optional @Name("forecastId") Long forecastId,
                                                           IServiceContext context) {
        return buildForecastAccuracyDataset(forecastId);
    }

    // ===================== 数据集聚合实现 =====================

    /**
     * 线索转化漏斗数据集。从 {@link ErpCrmLead} 按 stageId 聚合：leadCount + expectedRevenue 合计，
     * 对齐 {@code crm/README.md}。阶段名称经 {@link ErpCrmStage} 关系解析。
     * <p>DB 级 GROUP BY stageId + COUNT + SUM(expectedRevenue)（类 D 裁决：报表渲染数据集，DB 级聚合避免全表物化）。
     */
    List<Map<String, Object>> buildLeadConversionFunnelDataset() {
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.setSourceName(ErpCrmLead.class.getName());
            QueryFieldBean dim = QueryFieldBean.mainField("stageId");
            QueryFieldBean cnt = QueryFieldBean.mainField("stageId").count().alias("leadCount");
            QueryFieldBean sumRev = QueryFieldBean.mainField("expectedRevenue").sum().alias("expectedRevenue");
            q.setFields(Arrays.asList(dim, cnt, sumRev));
            List<Map<String, Object>> rows = ormTemplate.findListByQuery(q);
            if (rows.isEmpty()) {
                return Collections.emptyList();
            }
            Map<Long, Object[]> agg = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Object sid = row.get("stageId");
                if (sid == null) continue;
                long stageId = ((Number) sid).longValue();
                long leadCount = row.get("leadCount") == null ? 0L : ((Number) row.get("leadCount")).longValue();
                BigDecimal expectedRevenue = toBigDecimal(row.get("expectedRevenue"));
                agg.put(stageId, new Object[]{leadCount, expectedRevenue});
            }
            Map<Long, String> stageNames = resolveStageNames(agg.keySet());
            List<Map<String, Object>> result = new ArrayList<>(agg.size());
            for (Map.Entry<Long, Object[]> e : agg.entrySet()) {
                long stageId = e.getKey();
                Object[] v = e.getValue();
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("stageId", stageId);
                r.put("stageName", stageNames.getOrDefault(stageId, ""));
                r.put("leadCount", v[0]);
                r.put("expectedRevenue", v[1]);
                result.add(r);
            }
            return result;
        });
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(v.toString());
    }

    /**
     * 销售预测准确率数据集。从 {@link ErpCrmForecast}（可选 forecastId 过滤）+ 关联
     * {@link ErpCrmForecastLine}（行数/加权收入合计）合成，对齐 {@code crm/sales-forecast.md}：
     * commitAmount / weightedAmount / bestCaseAmount + 实际行数与加权收入合计。
     */
    List<Map<String, Object>> buildForecastAccuracyDataset(Long forecastId) {
        return ormTemplate.runInSession(session -> {
            List<ErpCrmForecast> forecasts = loadForecasts(forecastId);
            if (forecasts.isEmpty()) {
                return Collections.emptyList();
            }
            Set<Long> forecastIds = new HashSet<>();
            for (ErpCrmForecast f : forecasts) {
                if (f.getId() != null) forecastIds.add(f.getId());
            }
            Map<Long, ForecastLineAggregator> lineAgg = aggregateForecastLines(forecastIds);
            List<Map<String, Object>> rows = new ArrayList<>(forecasts.size());
            for (ErpCrmForecast f : forecasts) {
                ForecastLineAggregator la = lineAgg.getOrDefault(f.getId(), new ForecastLineAggregator());
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("forecastId", f.getId());
                r.put("periodId", f.getPeriodId());
                r.put("commitAmount", nz(f.getCommitAmount()));
                r.put("weightedAmount", nz(f.getWeightedAmount()));
                r.put("bestCaseAmount", nz(f.getBestCaseAmount()));
                r.put("expectedClosedRevenue", nz(f.getExpectedClosedRevenue()));
                r.put("opportunityCount", f.getOpportunityCount());
                r.put("lineCount", la.lineCount);
                r.put("lineWeightedRevenue", la.lineWeightedRevenue);
                rows.add(r);
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private List<ErpCrmForecast> loadForecasts(Long forecastId) {
        QueryBean q = new QueryBean();
        if (forecastId != null) q.addFilter(eq("id", forecastId));
        return daoProvider.daoFor(ErpCrmForecast.class).findAllByQuery(q);
    }

    private Map<Long, ForecastLineAggregator> aggregateForecastLines(Set<Long> forecastIds) {
        if (forecastIds.isEmpty()) return Collections.emptyMap();
        QueryBean q = new QueryBean();
        q.addFilter(in("forecastId", forecastIds));
        List<ErpCrmForecastLine> lines = daoProvider.daoFor(ErpCrmForecastLine.class).findAllByQuery(q);
        Map<Long, ForecastLineAggregator> map = new HashMap<>();
        for (ErpCrmForecastLine l : lines) {
            ForecastLineAggregator a = map.computeIfAbsent(l.getForecastId(), k -> new ForecastLineAggregator());
            a.lineCount++;
            a.lineWeightedRevenue = a.lineWeightedRevenue.add(nz(l.getWeightedRevenue()));
        }
        return map;
    }

    private Map<Long, String> resolveStageNames(Set<Long> stageIds) {
        if (stageIds.isEmpty()) return Collections.emptyMap();
        QueryBean q = new QueryBean();
        q.addFilter(in("id", stageIds));
        List<ErpCrmStage> stages = daoProvider.daoFor(ErpCrmStage.class).findAllByQuery(q);
        Map<Long, String> names = new HashMap<>();
        for (ErpCrmStage s : stages) {
            names.put(s.getId(), s.getStageName());
        }
        return names;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static class ForecastLineAggregator {
        int lineCount = 0;
        BigDecimal lineWeightedRevenue = BigDecimal.ZERO;
    }
}
