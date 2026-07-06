package app.erp.qa.service.report;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.qa.dao.entity.ErpQaAction;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
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
import java.time.LocalDate;
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
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 质量域报表渲染入口。注入平台 {@link IReportEngine}，按报表名解析 VFS 模板路径
 * （{@code /nop/main/report/qa/<name>.xpt.xml}）并渲染 html/xlsx/pdf。
 *
 * <p>镜像 {@code ErpMfgReportBizModel}/{@code ErpHrReportBizModel} 域隔离范式（…maintenance→{@code /mnt/}，
 * quality→{@code /qa/}）：不自建报表引擎，渲染逻辑全部委托给 {@code nop-report}；报表名经
 * {@link StringHelper#isValidVPath} 校验防路径注入。
 *
 * <p>两张种子报表：质检合格率统计表（{@link ErpQaInspection}/{@link ErpQaInspectionLine} 按物料/模板聚合合格率，
 * 对齐 {@code quality/state-machine.md}）+ NCR-CAPA 统计表（{@link ErpQaNonConformance}/{@link ErpQaAction}
 * 按严重度/状态聚合，对齐 {@code quality/state-machine.md}）。
 */
@BizModel("ErpQaReport")
public class ErpQaReportBizModel {

    /** 质量报表模板 VFS 根路径（与其他域隔离）。 */
    static final String REPORT_PATH_PREFIX = "/nop/main/report/qa/";

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
            throw new NopException(ErpQaErrors.ERR_REPORT_RENDER_TYPE_INVALID)
                    .param(ErpQaErrors.ARG_RENDER_TYPE, renderType);
        }
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);

        ITemplateOutput output = reportEngine.getRenderer(path, renderType);
        IResource resource = ResourceHelper.getTempResource("qa-rpt");
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
            throw new NopException(ErpQaErrors.ERR_REPORT_NAME_INVALID)
                    .param(ErpQaErrors.ARG_REPORT_NAME, reportName);
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
            case "inspection-summary":
                data.put(DS_VAR, buildInspectionSummaryDataset(
                        asLong(data, "materialId"),
                        asDate(data, "startDate"), asDate(data, "endDate")));
                break;
            case "ncr-capa-summary":
                data.put(DS_VAR, buildNcrCapaSummaryDataset(
                        asDate(data, "startDate"), asDate(data, "endDate")));
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

    private static LocalDate asDate(Map<String, Object> data, String k) {
        if (data == null) return null;
        Object v = data.get(k);
        if (v == null) return null;
        return v instanceof LocalDate ? (LocalDate) v : LocalDate.parse(v.toString());
    }

    // ===================== 数据集构造（也作 @BizQuery 供前端取原始数据） =====================

    /** 质检合格率统计数据集：按物料聚合合格率，对齐 {@code quality/state-machine.md}。 */
    @BizQuery
    public List<Map<String, Object>> inspectionSummaryData(@Optional @Name("materialId") Long materialId,
                                                            @Optional @Name("startDate") LocalDate startDate,
                                                            @Optional @Name("endDate") LocalDate endDate,
                                                            IServiceContext context) {
        return buildInspectionSummaryDataset(materialId, startDate, endDate);
    }

    /** NCR-CAPA 统计数据集：按严重度聚合 NCR 数 + CAPA 动作数，对齐 {@code quality/state-machine.md}。 */
    @BizQuery
    public List<Map<String, Object>> ncrCapaSummaryData(@Optional @Name("startDate") LocalDate startDate,
                                                         @Optional @Name("endDate") LocalDate endDate,
                                                         IServiceContext context) {
        return buildNcrCapaSummaryDataset(startDate, endDate);
    }

    // ===================== 数据集聚合实现 =====================

    /**
     * 质检合格率统计数据集。从 {@link ErpQaInspection}（inspectionDate 区间过滤 + 可选 materialId）按
     * materialId 聚合：totalInspections / acceptedCount（ACCEPTED+CONDITIONAL）/ rejectedCount（REJECTED）/
     * pendingCount（PENDING）/ passRate = acceptedCount / totalInspections，
     * 对齐 {@code quality/state-machine.md}。
     */
    List<Map<String, Object>> buildInspectionSummaryDataset(Long materialId, LocalDate startDate, LocalDate endDate) {
        return ormTemplate.runInSession(session -> {
            List<ErpQaInspection> inspections = loadInspections(materialId, startDate, endDate);
            if (inspections.isEmpty()) {
                return Collections.emptyList();
            }
            Map<Long, InspectionAggregator> agg = new LinkedHashMap<>();
            for (ErpQaInspection i : inspections) {
                Long mid = i.getMaterialId();
                if (mid == null) continue;
                InspectionAggregator a = agg.computeIfAbsent(mid, InspectionAggregator::new);
                a.total++;
                String result = i.getResult();
                if (ErpQaConstants.INSPECTION_RESULT_ACCEPTED.equals(result)
                        || ErpQaConstants.INSPECTION_RESULT_CONDITIONAL.equals(result)) {
                    a.accepted++;
                } else if (ErpQaConstants.INSPECTION_RESULT_REJECTED.equals(result)) {
                    a.rejected++;
                } else {
                    a.pending++;
                }
            }
            Map<Long, String> materialNames = resolveMaterialNames(agg.keySet());
            List<Map<String, Object>> rows = new ArrayList<>(agg.size());
            for (InspectionAggregator a : agg.values()) {
                BigDecimal passRate = a.total > 0
                        ? new BigDecimal(a.accepted).divide(new BigDecimal(a.total), 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("materialId", a.materialId);
                r.put("materialName", materialNames.getOrDefault(a.materialId, ""));
                r.put("totalInspections", a.total);
                r.put("acceptedCount", a.accepted);
                r.put("rejectedCount", a.rejected);
                r.put("pendingCount", a.pending);
                r.put("passRate", passRate);
                rows.add(r);
            }
            return rows;
        });
    }

    /**
     * NCR-CAPA 统计数据集。从 {@link ErpQaNonConformance}（ncrDate 区间过滤）按 severity 聚合：
     * ncrCount / resolvedNcrCount（status=RESOLVED）/ capaActionCount（关联 {@link ErpQaAction} 总数）/
     * completedActionCount（action.status=COMPLETED），对齐 {@code quality/state-machine.md}。
     */
    List<Map<String, Object>> buildNcrCapaSummaryDataset(LocalDate startDate, LocalDate endDate) {
        return ormTemplate.runInSession(session -> {
            List<ErpQaNonConformance> ncrs = loadNcrs(startDate, endDate);
            if (ncrs.isEmpty()) {
                return Collections.emptyList();
            }
            Set<Long> ncrIds = new HashSet<>();
            for (ErpQaNonConformance n : ncrs) {
                if (n.getId() != null) ncrIds.add(n.getId());
            }
            Map<Long, Integer> actionCountByNcr = countActionsByNcr(ncrIds);
            Map<Long, Integer> completedActionCountByNcr = countActionsByNcr(ncrIds, ErpQaConstants.ACTION_STATUS_COMPLETED);
            Map<String, NcrAggregator> agg = new LinkedHashMap<>();
            for (ErpQaNonConformance n : ncrs) {
                String severity = n.getSeverity() != null ? n.getSeverity() : "(未指定)";
                NcrAggregator a = agg.computeIfAbsent(severity, NcrAggregator::new);
                a.ncrCount++;
                if (ErpQaConstants.NCR_STATUS_RESOLVED.equals(n.getStatus())
                        || ErpQaConstants.NCR_STATUS_CANCELLED.equals(n.getStatus())) {
                    a.resolvedNcrCount++;
                }
                a.capaActionCount += actionCountByNcr.getOrDefault(n.getId(), 0);
                a.completedActionCount += completedActionCountByNcr.getOrDefault(n.getId(), 0);
            }
            List<String> orderedSeverities = Arrays.asList(
                    ErpQaConstants.RECALL_SEVERITY_CRITICAL,
                    ErpQaConstants.RECALL_SEVERITY_HIGH,
                    ErpQaConstants.RECALL_SEVERITY_MEDIUM,
                    ErpQaConstants.RECALL_SEVERITY_LOW);
            List<Map<String, Object>> rows = new ArrayList<>(agg.size());
            for (String sev : orderedSeverities) {
                NcrAggregator a = agg.get(sev);
                if (a == null) continue;
                rows.add(toNcrRow(sev, a));
            }
            for (Map.Entry<String, NcrAggregator> e : agg.entrySet()) {
                if (orderedSeverities.contains(e.getKey())) continue;
                rows.add(toNcrRow(e.getKey(), e.getValue()));
            }
            return rows;
        });
    }

    private static Map<String, Object> toNcrRow(String severity, NcrAggregator a) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("severity", severity);
        r.put("ncrCount", a.ncrCount);
        r.put("resolvedNcrCount", a.resolvedNcrCount);
        r.put("capaActionCount", a.capaActionCount);
        r.put("completedActionCount", a.completedActionCount);
        return r;
    }

    // ===================== helpers =====================

    private List<ErpQaInspection> loadInspections(Long materialId, LocalDate startDate, LocalDate endDate) {
        QueryBean q = new QueryBean();
        if (materialId != null) q.addFilter(eq("materialId", materialId));
        if (startDate != null) q.addFilter(ge("inspectionDate", startDate));
        if (endDate != null) q.addFilter(le("inspectionDate", endDate));
        return daoProvider.daoFor(ErpQaInspection.class).findAllByQuery(q);
    }

    private List<ErpQaNonConformance> loadNcrs(LocalDate startDate, LocalDate endDate) {
        QueryBean q = new QueryBean();
        if (startDate != null) q.addFilter(ge("ncrDate", startDate));
        if (endDate != null) q.addFilter(le("ncrDate", endDate));
        return daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q);
    }

    private Map<Long, Integer> countActionsByNcr(Set<Long> ncrIds) {
        return countActionsByNcr(ncrIds, null);
    }

    private Map<Long, Integer> countActionsByNcr(Set<Long> ncrIds, String status) {
        if (ncrIds.isEmpty()) return Collections.emptyMap();
        QueryBean q = new QueryBean();
        q.addFilter(in("ncrId", ncrIds));
        if (status != null) q.addFilter(eq("status", status));
        List<ErpQaAction> actions = daoProvider.daoFor(ErpQaAction.class).findAllByQuery(q);
        Map<Long, Integer> counts = new HashMap<>();
        for (ErpQaAction a : actions) {
            counts.merge(a.getNcrId(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<Long, String> resolveMaterialNames(Set<Long> materialIds) {
        if (materialIds.isEmpty()) return Collections.emptyMap();
        QueryBean q = new QueryBean();
        q.addFilter(in("id", materialIds));
        List<ErpMdMaterial> materials = daoProvider.daoFor(ErpMdMaterial.class).findAllByQuery(q);
        Map<Long, String> names = new HashMap<>();
        for (ErpMdMaterial m : materials) {
            names.put(m.getId(), m.getName());
        }
        return names;
    }

    private static class InspectionAggregator {
        final Long materialId;
        int total = 0;
        int accepted = 0;
        int rejected = 0;
        int pending = 0;

        InspectionAggregator(Long materialId) {
            this.materialId = materialId;
        }
    }

    private static class NcrAggregator {
        final String severity;
        int ncrCount = 0;
        int resolvedNcrCount = 0;
        int capaActionCount = 0;
        int completedActionCount = 0;

        NcrAggregator(String severity) {
            this.severity = severity;
        }
    }
}
