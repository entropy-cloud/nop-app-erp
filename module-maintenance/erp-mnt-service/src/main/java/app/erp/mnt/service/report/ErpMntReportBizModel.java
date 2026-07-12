package app.erp.mnt.service.report;

import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.dao.entity.ErpMntVisitTask;
import app.erp.mnt.service.ErpMntErrors;
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
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 维护域报表渲染入口。注入平台 {@link IReportEngine}，按报表名解析 VFS 模板路径
 * （{@code /nop/main/report/mnt/<name>.xpt.xml}）并渲染 html/xlsx/pdf。
 *
 * <p>镜像 {@code ErpMfgReportBizModel}/{@code ErpHrReportBizModel} 域隔离范式（…assets→{@code /ast/}，
 * projects→{@code /prj/}，maintenance→{@code /mnt/}）：不自建报表引擎，渲染逻辑全部委托给 {@code nop-report}；
 * 报表名经 {@link StringHelper#isValidVPath} 校验防路径注入。
 *
 * <p>两张种子报表：维护历史表（{@link ErpMntVisit} ⨝ {@link ErpMntVisitTask} 按设备/周期聚合 + 备件消耗，
 * 对齐 {@code maintenance/state-machine.md}）+ 停机统计表（{@link ErpMntDowntimeEntry} 按设备/原因聚合停机分钟，
 * 对齐 {@code maintenance/state-machine.md}）。
 */
@BizModel("ErpMntReport")
public class ErpMntReportBizModel {

    /** 维护报表模板 VFS 根路径（与其他域隔离）。 */
    static final String REPORT_PATH_PREFIX = "/nop/main/report/mnt/";

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
            throw new NopException(ErpMntErrors.ERR_REPORT_RENDER_TYPE_INVALID)
                    .param(ErpMntErrors.ARG_RENDER_TYPE, renderType);
        }
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);

        ITemplateOutput output = reportEngine.getRenderer(path, renderType);
        IResource resource = ResourceHelper.getTempResource("mnt-rpt");
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
            throw new NopException(ErpMntErrors.ERR_REPORT_NAME_INVALID)
                    .param(ErpMntErrors.ARG_REPORT_NAME, reportName);
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
            case "maintenance-history":
                data.put(DS_VAR, buildMaintenanceHistoryDataset(
                        asLong(data, "equipmentId"),
                        asDate(data, "startDate"), asDate(data, "endDate")));
                break;
            case "downtime-summary":
                data.put(DS_VAR, buildDowntimeSummaryDataset(
                        asLong(data, "equipmentId"),
                        asDate(data, "startDate"), asDate(data, "endDate")));
                break;
            default:
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

    /** 维护历史数据集：访问×设备聚合任务数 + 备件消耗单数，对齐 {@code maintenance/state-machine.md}。 */
    @BizQuery
    public List<Map<String, Object>> maintenanceHistoryData(@Optional @Name("equipmentId") Long equipmentId,
                                                             @Optional @Name("startDate") LocalDate startDate,
                                                             @Optional @Name("endDate") LocalDate endDate,
                                                             IServiceContext context) {
        return buildMaintenanceHistoryDataset(equipmentId, startDate, endDate);
    }

    /** 停机统计数据集：按设备/原因聚合停机分钟，对齐 {@code maintenance/state-machine.md}。 */
    @BizQuery
    public List<Map<String, Object>> downtimeSummaryData(@Optional @Name("equipmentId") Long equipmentId,
                                                          @Optional @Name("startDate") LocalDate startDate,
                                                          @Optional @Name("endDate") LocalDate endDate,
                                                          IServiceContext context) {
        return buildDowntimeSummaryDataset(equipmentId, startDate, endDate);
    }

    // ===================== 数据集聚合实现 =====================

    /**
     * 维护历史数据集。从 {@link ErpMntVisit}（visitDate 区间过滤 + 可选 equipmentId）逐行展开，
     * 任务数/备件消耗单数经同域聚合（{@link ErpMntVisitTask}/{@link ErpMntSparePartUsage}），对齐
     * {@code maintenance/state-machine.md}。设备编码经 {@link ErpMntVisit#getEquipment()} 关系解析。
     */
    List<Map<String, Object>> buildMaintenanceHistoryDataset(Long equipmentId, LocalDate startDate, LocalDate endDate) {
        return ormTemplate.runInSession(session -> {
            List<ErpMntVisit> visits = loadVisits(equipmentId, startDate, endDate);
            if (visits.isEmpty()) {
                return Collections.emptyList();
            }
            Set<Long> visitIds = new HashSet<>();
            for (ErpMntVisit v : visits) {
                if (v.getId() != null) visitIds.add(v.getId());
            }
            Map<Long, Integer> taskCountByVisit = countTasksByVisit(visitIds);
            Map<Long, Integer> usageCountByVisit = countUsagesByVisit(visitIds);
            Map<Long, String> equipmentNames = resolveEquipmentNames(visits);
            List<Map<String, Object>> rows = new ArrayList<>(visits.size());
            for (ErpMntVisit v : visits) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("visitId", v.getId());
                r.put("visitCode", v.getCode());
                r.put("equipmentId", v.getEquipmentId());
                r.put("equipmentName", equipmentNames.getOrDefault(v.getEquipmentId(), ""));
                r.put("visitDate", v.getVisitDate());
                r.put("visitType", v.getVisitType());
                r.put("status", v.getStatus());
                r.put("totalMinutes", nz(v.getTotalMinutes()));
                r.put("taskCount", taskCountByVisit.getOrDefault(v.getId(), 0));
                r.put("sparePartUsageCount", usageCountByVisit.getOrDefault(v.getId(), 0));
                rows.add(r);
            }
            return rows;
        });
    }

    /**
     * 停机统计数据集。从 {@link ErpMntDowntimeEntry}（startTime 日期区间过滤 + 可选 equipmentId）按
     * equipmentId × reason 聚合 totalMinutes 与 entryCount，对齐 {@code maintenance/state-machine.md}。
     */
    List<Map<String, Object>> buildDowntimeSummaryDataset(Long equipmentId, LocalDate startDate, LocalDate endDate) {
        return ormTemplate.runInSession(session -> {
            List<ErpMntDowntimeEntry> entries = loadDowntimeEntries(equipmentId, startDate, endDate);
            if (entries.isEmpty()) {
                return Collections.emptyList();
            }
            Map<String, DowntimeAggregator> agg = new LinkedHashMap<>();
            for (ErpMntDowntimeEntry e : entries) {
                Long eqId = e.getEquipmentId();
                String reason = e.getReason() != null ? e.getReason() : "(未指定)";
                String key = eqId + "|" + reason;
                DowntimeAggregator a = agg.computeIfAbsent(key, k -> new DowntimeAggregator(eqId, reason));
                a.totalMinutes = a.totalMinutes.add(nz(e.getTotalMinutes()));
                a.entryCount++;
            }
            Map<Long, String> equipmentNames = resolveEquipmentNamesByIds(collectEquipmentIds(agg));
            List<Map<String, Object>> rows = new ArrayList<>(agg.size());
            for (DowntimeAggregator a : agg.values()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("equipmentId", a.equipmentId);
                r.put("equipmentName", equipmentNames.getOrDefault(a.equipmentId, ""));
                r.put("reason", a.reason);
                r.put("downtimeMinutes", a.totalMinutes);
                r.put("entryCount", a.entryCount);
                rows.add(r);
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private List<ErpMntVisit> loadVisits(Long equipmentId, LocalDate startDate, LocalDate endDate) {
        QueryBean q = new QueryBean();
        if (equipmentId != null) q.addFilter(eq("equipmentId", equipmentId));
        if (startDate != null) q.addFilter(ge("visitDate", startDate));
        if (endDate != null) q.addFilter(le("visitDate", endDate));
        q.addOrderField("visitDate", false);
        q.addOrderField("code", false);
        return daoProvider.daoFor(ErpMntVisit.class).findAllByQuery(q);
    }

    private Map<Long, Integer> countTasksByVisit(Set<Long> visitIds) {
        if (visitIds.isEmpty()) return Collections.emptyMap();
        QueryBean q = new QueryBean();
        q.addFilter(in("visitId", visitIds));
        List<ErpMntVisitTask> tasks = daoProvider.daoFor(ErpMntVisitTask.class).findAllByQuery(q);
        Map<Long, Integer> counts = new HashMap<>();
        for (ErpMntVisitTask t : tasks) {
            counts.merge(t.getVisitId(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<Long, Integer> countUsagesByVisit(Set<Long> visitIds) {
        if (visitIds.isEmpty()) return Collections.emptyMap();
        QueryBean q = new QueryBean();
        q.addFilter(in("visitId", visitIds));
        List<ErpMntSparePartUsage> usages = daoProvider.daoFor(ErpMntSparePartUsage.class).findAllByQuery(q);
        Map<Long, Integer> counts = new HashMap<>();
        for (ErpMntSparePartUsage u : usages) {
            counts.merge(u.getVisitId(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<Long, String> resolveEquipmentNames(List<ErpMntVisit> visits) {
        Set<Long> eqIds = new HashSet<>();
        for (ErpMntVisit v : visits) {
            if (v.getEquipmentId() != null) eqIds.add(v.getEquipmentId());
        }
        return resolveEquipmentNamesByIds(eqIds);
    }

    private Set<Long> collectEquipmentIds(Map<String, DowntimeAggregator> agg) {
        Set<Long> ids = new HashSet<>();
        for (DowntimeAggregator a : agg.values()) {
            if (a.equipmentId != null) ids.add(a.equipmentId);
        }
        return ids;
    }

    private Map<Long, String> resolveEquipmentNamesByIds(Set<Long> eqIds) {
        if (eqIds.isEmpty()) return Collections.emptyMap();
        QueryBean q = new QueryBean();
        q.addFilter(in("id", eqIds));
        List<ErpMntEquipment> list = daoProvider.daoFor(ErpMntEquipment.class).findAllByQuery(q);
        Map<Long, String> names = new HashMap<>();
        for (ErpMntEquipment e : list) {
            names.put(e.getId(), e.getName());
        }
        return names;
    }

    private List<ErpMntDowntimeEntry> loadDowntimeEntries(Long equipmentId, LocalDate startDate, LocalDate endDate) {
        QueryBean q = new QueryBean();
        if (equipmentId != null) q.addFilter(eq("equipmentId", equipmentId));
        if (startDate != null) q.addFilter(ge("startTime", startDate.atStartOfDay()));
        if (endDate != null) q.addFilter(le("startTime", endDate.plusDays(1).atStartOfDay()));
        return daoProvider.daoFor(ErpMntDowntimeEntry.class).findAllByQuery(q);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static class DowntimeAggregator {
        final Long equipmentId;
        final String reason;
        BigDecimal totalMinutes = BigDecimal.ZERO;
        int entryCount = 0;

        DowntimeAggregator(Long equipmentId, String reason) {
            this.equipmentId = equipmentId;
            this.reason = reason;
        }
    }
}
