package app.erp.prj.service.report;

import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.WebContentBean;
import io.nop.api.core.beans.query.QueryBean;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 项目域报表渲染入口。注入平台 {@link IReportEngine}，按报表名解析 VFS 模板路径
 * （{@code /nop/main/report/prj/<name>.xpt.xml}）并渲染 html/xlsx/pdf。
 *
 * <p>镜像 {@code ErpMfgReportBizModel}/{@code ErpHrReportBizModel} 域隔离范式（finance→{@code /fin/}，
 * manufacturing→{@code /mfg/}，inventory→{@code /inv/}，HR→{@code /hr/}，assets→{@code /ast/}，
 * projects→{@code /prj/}）：不自建报表引擎，渲染逻辑全部委托给 {@code nop-report}；报表名经
 * {@link StringHelper#isValidVPath} 校验防路径注入。
 *
 * <p>两张种子报表：项目成本汇总表（{@link ErpPrjProject} actualCost/budget + 预算执行率，
 * 对齐 {@code projects/cost-collection.md}）+ 工时明细表（{@link ErpPrjTimesheet} 按项目/员工/周期聚合，
 * 对齐 {@code projects/cost-collection.md}）。
 */
@BizModel("ErpPrjReport")
public class ErpPrjReportBizModel {

    /** 项目报表模板 VFS 根路径（与其他域隔离）。 */
    static final String REPORT_PATH_PREFIX = "/nop/main/report/prj/";

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
            throw new NopException(ErpPrjErrors.ERR_REPORT_RENDER_TYPE_INVALID)
                    .param(ErpPrjErrors.ARG_RENDER_TYPE, renderType);
        }
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);

        ITemplateOutput output = reportEngine.getRenderer(path, renderType);
        IResource resource = ResourceHelper.getTempResource("prj-rpt");
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

    /**
     * 将报表名规范化为完整 VFS 路径，并校验防路径注入。
     *
     * <p>接受 {@code project-cost-summary}、{@code project-cost-summary.xpt.xml} 两种形式，统一补全到
     * {@code /nop/main/report/prj/project-cost-summary.xpt.xml}。
     */
    String resolveReportPath(String reportName) {
        if (StringHelper.isEmpty(reportName) || !StringHelper.isValidVPath(reportName)) {
            throw new NopException(ErpPrjErrors.ERR_REPORT_NAME_INVALID)
                    .param(ErpPrjErrors.ARG_REPORT_NAME, reportName);
        }
        String name = reportName;
        if (!name.endsWith(".xpt.xml") && !name.endsWith(".xpt.xlsx")) {
            name = name + ".xpt.xml";
        }
        return REPORT_PATH_PREFIX + name;
    }

    /** 取报表名的去后缀短名（用于数据集路由与下载文件名）。 */
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

    /** 按报表短名自动装配数据集（调用方未预先提供时）。 */
    private void prepareDataset(String reportName, Map<String, Object> data, IServiceContext context) {
        String key = baseName(reportName);
        if (data.containsKey(DS_VAR)) return;
        switch (key) {
            case "project-cost-summary":
                data.put(DS_VAR, buildProjectCostSummaryDataset(
                        asLong(data, "projectId"), asDate(data, "startDate"), asDate(data, "endDate")));
                break;
            case "timesheet-detail":
                data.put(DS_VAR, buildTimesheetDetailDataset(
                        asLong(data, "projectId"),
                        asDate(data, "startDate"), asDate(data, "endDate")));
                break;
            default:
                // 未知报表：不自动装配，模板可经 beforeExpand 自行构造数据集
                break;
        }
    }

    private static Long asLong(Map<String, Object> data, String k) {
        if (data == null) return null;
        Object v = data.get(k);
        if (v == null) return null;
        String s = v.toString();
        if (s.trim().isEmpty()) return null;
        return Long.valueOf(s);
    }

    private static LocalDate asDate(Map<String, Object> data, String k) {
        if (data == null) return null;
        Object v = data.get(k);
        if (v == null) return null;
        if (v instanceof LocalDate) return (LocalDate) v;
        String s = v.toString();
        if (s.isEmpty()) return null;
        if (s.matches("\\d{13}")) return LocalDate.ofInstant(Instant.ofEpochMilli(Long.parseLong(s)), ZoneOffset.UTC);
        if (s.matches("\\d{10}")) return LocalDate.ofInstant(Instant.ofEpochSecond(Long.parseLong(s)), ZoneOffset.UTC);
        return LocalDate.parse(s);
    }

    // ===================== 数据集构造（也作 @BizQuery 供前端取原始数据） =====================

    /** 项目成本汇总数据集：actualCost/budget/committedCost/billedAmount + 预算执行率，对齐 {@code projects/cost-collection.md}。 */
    @BizQuery
    public List<Map<String, Object>> projectCostSummaryData(@Optional @Name("projectId") Long projectId,
                                                             @Optional @Name("startDate") LocalDate startDate,
                                                             @Optional @Name("endDate") LocalDate endDate,
                                                             IServiceContext context) {
        return buildProjectCostSummaryDataset(projectId, startDate, endDate);
    }

    /** 工时明细数据集：按项目/员工/周期聚合工时与工时成本，对齐 {@code projects/cost-collection.md}。 */
    @BizQuery
    public List<Map<String, Object>> timesheetDetailData(@Optional @Name("projectId") Long projectId,
                                                          @Optional @Name("startDate") LocalDate startDate,
                                                          @Optional @Name("endDate") LocalDate endDate,
                                                          IServiceContext context) {
        return buildTimesheetDetailDataset(projectId, startDate, endDate);
    }

    // ===================== 数据集聚合实现 =====================

    /**
     * 项目成本汇总数据集。从 {@link ErpPrjProject}（budget/committedCost/actualCost/billedAmount）实时聚合，
     * 计算「预算执行率 = actualCost / budget」（budget≤0 视为未设预算，执行率留空），对齐
     * {@code projects/cost-collection.md}：actualCost 由 1018-2 工时/采购/费用回写，不引入新实体。
     *
     * <p>区间过滤对齐项目 {@code startDate}/{@code endDate}：[pStart,pEnd] ∩ [startDate,endDate] 非空。
     */
    List<Map<String, Object>> buildProjectCostSummaryDataset(Long projectId, LocalDate startDate, LocalDate endDate) {
        return ormTemplate.runInSession(session -> {
            List<ErpPrjProject> projects = loadProjects(projectId, startDate, endDate);
            if (projects.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> rows = new ArrayList<>(projects.size());
            for (ErpPrjProject p : projects) {
                BigDecimal budget = nz(p.getBudget());
                BigDecimal actual = nz(p.getActualCost());
                BigDecimal executionRate = budget.signum() > 0
                        ? actual.divide(budget, 4, RoundingMode.HALF_UP)
                        : null;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("projectId", p.getId());
                r.put("projectCode", p.getCode());
                r.put("projectName", p.getName());
                r.put("status", p.getStatus());
                r.put("budget", budget);
                r.put("committedCost", nz(p.getCommittedCost()));
                r.put("actualCost", actual);
                r.put("billedAmount", nz(p.getBilledAmount()));
                r.put("executionRate", executionRate);
                rows.add(r);
            }
            return rows;
        });
    }

    /**
     * 工时明细数据集。从 {@link ErpPrjTimesheet}（workDate 区间过滤 + 可选 projectId）按
     * projectId × userId 聚合 hours/costAmount，对齐 {@code projects/cost-collection.md}。
     */
    List<Map<String, Object>> buildTimesheetDetailDataset(Long projectId, LocalDate startDate, LocalDate endDate) {
        return ormTemplate.runInSession(session -> {
            List<ErpPrjTimesheet> timesheets = loadTimesheets(projectId, startDate, endDate);
            if (timesheets.isEmpty()) {
                return Collections.emptyList();
            }
            Map<String, Aggregator> agg = new LinkedHashMap<>();
            Map<String, Long> projectIdByUser = new HashMap<>();
            for (ErpPrjTimesheet t : timesheets) {
                Long pid = t.getProjectId();
                Long uid = t.getUserId();
                String key = pid + "|" + uid;
                Aggregator a = agg.computeIfAbsent(key, k -> new Aggregator(pid, uid));
                a.hours = a.hours.add(nz(t.getHours()));
                a.costAmount = a.costAmount.add(nz(t.getCostAmount()));
            }
            List<Map<String, Object>> rows = new ArrayList<>(agg.size());
            for (Aggregator a : agg.values()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("projectId", a.projectId);
                r.put("userId", a.userId);
                r.put("hours", a.hours);
                r.put("costAmount", a.costAmount);
                rows.add(r);
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private List<ErpPrjProject> loadProjects(Long projectId, LocalDate startDate, LocalDate endDate) {
        QueryBean q = new QueryBean();
        if (projectId != null) q.addFilter(eq("id", projectId));
        if (startDate != null) q.addFilter(ge("endDate", startDate));
        if (endDate != null) q.addFilter(le("startDate", endDate));
        q.addOrderField("code", false);
        return daoProvider.daoFor(ErpPrjProject.class).findAllByQuery(q);
    }

    private List<ErpPrjTimesheet> loadTimesheets(Long projectId, LocalDate startDate, LocalDate endDate) {
        QueryBean q = new QueryBean();
        if (projectId != null) q.addFilter(eq("projectId", projectId));
        if (startDate != null) q.addFilter(ge("workDate", startDate));
        if (endDate != null) q.addFilter(le("workDate", endDate));
        q.addOrderField("projectId", false);
        q.addOrderField("userId", false);
        return daoProvider.daoFor(ErpPrjTimesheet.class).findAllByQuery(q);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static class Aggregator {
        final Long projectId;
        final Long userId;
        BigDecimal hours = BigDecimal.ZERO;
        BigDecimal costAmount = BigDecimal.ZERO;

        Aggregator(Long projectId, Long userId) {
            this.projectId = projectId;
            this.userId = userId;
        }
    }
}
