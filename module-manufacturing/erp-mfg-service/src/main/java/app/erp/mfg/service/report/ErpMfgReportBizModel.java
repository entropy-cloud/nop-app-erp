package app.erp.mfg.service.report;

import app.erp.mfg.biz.CrpLoadReportItem;
import app.erp.mfg.biz.IErpMfgCrpLoadBiz;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgCrpLoad;
import app.erp.mfg.dao.entity.ErpMfgForecast;
import app.erp.mfg.dao.entity.ErpMfgForecastLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
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
import io.nop.dao.api.IEntityDao;
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
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 制造运营报表渲染入口。注入平台 {@link IReportEngine}，按报表名解析 VFS 模板路径
 * （{@code /nop/main/report/mfg/<name>.xpt.xml}）并渲染 html/xlsx/pdf。
 *
 * <p>镜像 {@code ErpFinReportBizModel} 域隔离范式（finance→{@code /fin/}，manufacturing→{@code /mfg/}）：
 * 不自建报表引擎，渲染逻辑全部委托给 {@code nop-report}；报表名经 {@link StringHelper#isValidVPath} 校验
 * 防路径注入。数据集由本类的 {@code buildXxxDataset} 方法从既有制造域 ORM 实体/CRP 引擎产物聚合。
 *
 * <p>三张种子报表：CRP 负荷报表 / 生产差异报表 / 预测差异报表（口径对齐各 owner doc）。
 */
@BizModel("ErpMfgReport")
public class ErpMfgReportBizModel {

    /** 制造报表模板 VFS 根路径（与 finance {@code /fin/} 隔离）。 */
    static final String REPORT_PATH_PREFIX = "/nop/main/report/mfg/";

    /** 模板内统一引用的数据集变量名（单元格 {@code *=^ds!<field>}）。 */
    static final String DS_VAR = "ds";

    private static final Set<String> ALLOWED_RENDER_TYPES = new HashSet<>(Arrays.asList("html", "xlsx", "pdf"));

    @Inject
    IReportEngine reportEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpMfgCrpLoadBiz crpLoadBiz;

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
            throw new NopException(ErpMfgErrors.ERR_REPORT_RENDER_TYPE_INVALID)
                    .param(ErpMfgErrors.ARG_RENDER_TYPE, renderType);
        }
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);

        ITemplateOutput output = reportEngine.getRenderer(path, renderType);
        IResource resource = ResourceHelper.getTempResource("mfg-rpt");
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
     * <p>接受 {@code crp-load-report}、{@code crp-load-report.xpt.xml} 两种形式，统一补全到
     * {@code /nop/main/report/mfg/crp-load-report.xpt.xml}。
     */
    String resolveReportPath(String reportName) {
        if (StringHelper.isEmpty(reportName) || !StringHelper.isValidVPath(reportName)) {
            throw new NopException(ErpMfgErrors.ERR_REPORT_NAME_INVALID)
                    .param(ErpMfgErrors.ARG_REPORT_NAME, reportName);
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
            case "crp-load-report":
                data.put(DS_VAR, buildCrpLoadDataset(asLong(data, "workcenterId"),
                        asDate(data, "startDate"), asDate(data, "endDate"), context));
                break;
            case "production-variance-report":
                data.put(DS_VAR, buildProductionVarianceDataset(asLong(data, "workOrderId"),
                        asDate(data, "startDate"), asDate(data, "endDate")));
                break;
            case "forecast-variance-report":
                data.put(DS_VAR, buildForecastVarianceDataset(asLong(data, "materialId"),
                        asDate(data, "periodStart"), asDate(data, "periodEnd")));
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

    /** CRP 负荷数据集：工作中心×日期负荷/产能/负荷率/超负荷。口径对齐 {@code manufacturing/crp.md §负载报表}。 */
    @BizQuery
    public List<Map<String, Object>> crpLoadData(@Optional @Name("workcenterId") Long workcenterId,
                                                  @Optional @Name("startDate") LocalDate startDate,
                                                  @Optional @Name("endDate") LocalDate endDate,
                                                  IServiceContext context) {
        return buildCrpLoadDataset(workcenterId, startDate, endDate, context);
    }

    /** 生产差异数据集：工单×差异类型/金额/标准成本/实际成本，对齐 1838-2 差异引擎输出。 */
    @BizQuery
    public List<Map<String, Object>> productionVarianceData(@Optional @Name("workOrderId") Long workOrderId,
                                                             @Optional @Name("startDate") LocalDate startDate,
                                                             @Optional @Name("endDate") LocalDate endDate,
                                                             IServiceContext context) {
        return buildProductionVarianceDataset(workOrderId, startDate, endDate);
    }

    /** 预测差异数据集：预测 vs 实际消耗对比，按物料聚合，对齐 {@code manufacturing/mrp.md} + 0427-1。 */
    @BizQuery
    public List<Map<String, Object>> forecastVarianceData(@Optional @Name("materialId") Long materialId,
                                                           @Optional @Name("periodStart") LocalDate periodStart,
                                                           @Optional @Name("periodEnd") LocalDate periodEnd,
                                                           IServiceContext context) {
        return buildForecastVarianceDataset(materialId, periodStart, periodEnd);
    }

    // ===================== 数据集聚合实现 =====================

    /**
     * CRP 负荷数据集：委托 {@link IErpMfgCrpLoadBiz#getLoadReport}（复用 1707-1 已审计的负荷/产能/超负荷计算），
     * 区间未指定时从既有 CrpLoad 快照推导 [min,max] loadDate。返回行字段对齐 {@code crp.md §负载报表}。
     */
    List<Map<String, Object>> buildCrpLoadDataset(Long workcenterId, LocalDate startDate, LocalDate endDate,
                                                  IServiceContext context) {
        List<Long> wcIds = workcenterId != null ? Collections.singletonList(workcenterId) : null;
        LocalDate from = startDate;
        LocalDate to = endDate;
        if (from == null || to == null) {
            LocalDate[] window = deriveCrpWindow(workcenterId);
            if (window == null) return Collections.emptyList();
            if (from == null) from = window[0];
            if (to == null) to = window[1];
        }
        List<?> items = crpLoadBiz.getLoadReport(from, to, wcIds, context);
        List<Map<String, Object>> rows = new ArrayList<>(items.size());
        for (Object o : items) {
            if (o instanceof CrpLoadReportItem) {
                rows.add(crpItemToMap((CrpLoadReportItem) o));
            } else if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) o;
                rows.add(m);
            }
        }
        return rows;
    }

    private static Map<String, Object> crpItemToMap(CrpLoadReportItem item) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("workcenterId", item.getWorkcenterId());
        r.put("workcenterCode", item.getWorkcenterCode());
        r.put("loadDate", item.getLoadDate());
        r.put("loadHours", item.getLoadHours());
        r.put("setupHours", item.getSetupHours());
        r.put("capacityHours", item.getCapacityHours());
        r.put("loadRate", item.getLoadRate());
        r.put("overloaded", item.getOverloaded());
        return r;
    }

    /**
     * 生产差异数据集：从 {@link ErpMfgCostVariance}（1838-2 产物）按 工单×差异类型 聚合
     * standardAmount/actualAmount/varianceAmount，对齐 {@code manufacturing/state-machine.md}。
     */
    List<Map<String, Object>> buildProductionVarianceDataset(Long workOrderId, LocalDate startDate, LocalDate endDate) {
        return ormTemplate.runInSession(session -> {
            List<ErpMfgCostVariance> lines = loadVarianceLines(workOrderId, startDate, endDate);
            List<Map<String, Object>> rows = new ArrayList<>(lines.size());
            for (ErpMfgCostVariance v : lines) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("workOrderId", v.getWorkOrderId());
                r.put("workOrderCode", workOrderCodeOf(v));
                r.put("varianceType", v.getVarianceType());
                r.put("costElement", v.getCostElement());
                r.put("standardAmount", nz(v.getStandardAmount()));
                r.put("actualAmount", nz(v.getActualAmount()));
                r.put("varianceAmount", nz(v.getVarianceAmount()));
                r.put("businessDate", v.getBusinessDate());
                rows.add(r);
            }
            return rows;
        });
    }

    /**
     * 预测差异数据集：预测（{@link ErpMfgForecast}/{@link ErpMfgForecastLine}，status=APPROVED）
     * vs 实际消耗（{@link ErpMfgWorkOrder} 完工数量），按物料聚合 forecastQty/actualQty/variance/varianceRatio，
     * 对齐 {@code manufacturing/mrp.md} + 0427-1。
     */
    List<Map<String, Object>> buildForecastVarianceDataset(Long materialId, LocalDate periodStart, LocalDate periodEnd) {
        return ormTemplate.runInSession(session -> {
            Map<Long, BigDecimal> forecastByMat = aggregateForecastQty(materialId, periodStart, periodEnd);
            Map<Long, BigDecimal> actualByMat = aggregateActualQty(materialId, periodStart, periodEnd);
            Set<Long> materials = new HashSet<>(forecastByMat.keySet());
            materials.addAll(actualByMat.keySet());
            List<Long> sorted = new ArrayList<>(materials);
            Collections.sort(sorted);
            List<Map<String, Object>> rows = new ArrayList<>(sorted.size());
            for (Long matId : sorted) {
                BigDecimal forecastQty = nz(forecastByMat.get(matId));
                BigDecimal actualQty = nz(actualByMat.get(matId));
                BigDecimal variance = actualQty.subtract(forecastQty);
                BigDecimal ratio = forecastQty.signum() == 0
                        ? (actualQty.signum() == 0 ? BigDecimal.ZERO : new BigDecimal("9999"))
                        : variance.divide(forecastQty, 4, RoundingMode.HALF_UP);
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("materialId", matId);
                r.put("forecastQty", forecastQty);
                r.put("actualQty", actualQty);
                r.put("variance", variance);
                r.put("varianceRatio", ratio);
                rows.add(r);
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private LocalDate[] deriveCrpWindow(Long workcenterId) {
        // 类 D 裁决：CRP 负荷报表窗口推导需明细 min/max(loadDate)，带硬上限的受限扫描
        IEntityDao<ErpMfgCrpLoad> dao = daoProvider.daoFor(ErpMfgCrpLoad.class);
        QueryBean q = new QueryBean();
        if (workcenterId != null) {
            q.addFilter(eq("workcenterId", workcenterId));
        }
        q.setLimit(5000);
        List<ErpMfgCrpLoad> all = dao.findAllByQuery(q);
        if (all.isEmpty()) return null;
        LocalDate min = null;
        LocalDate max = null;
        for (ErpMfgCrpLoad l : all) {
            LocalDate d = l.getLoadDate();
            if (d == null) continue;
            if (min == null || d.isBefore(min)) min = d;
            if (max == null || d.isAfter(max)) max = d;
        }
        if (min == null) return null;
        return new LocalDate[]{min, max};
    }

    private List<ErpMfgCostVariance> loadVarianceLines(Long workOrderId, LocalDate startDate, LocalDate endDate) {
        IEntityDao<ErpMfgCostVariance> dao = daoProvider.daoFor(ErpMfgCostVariance.class);
        QueryBean q = new QueryBean();
        if (workOrderId != null) q.addFilter(eq("workOrderId", workOrderId));
        if (startDate != null) q.addFilter(ge("businessDate", startDate));
        if (endDate != null) q.addFilter(le("businessDate", endDate));
        q.addOrderField("workOrderId", false);
        q.addOrderField("varianceType", false);
        return dao.findAllByQuery(q);
    }

    private String workOrderCodeOf(ErpMfgCostVariance v) {
        ErpMfgWorkOrder wo = v.getWorkOrder();
        return wo != null ? wo.getCode() : null;
    }

    private Map<Long, BigDecimal> aggregateForecastQty(Long materialId, LocalDate periodStart, LocalDate periodEnd) {
        List<ErpMfgForecast> forecasts = loadApprovedForecasts();
        if (forecasts.isEmpty()) return Collections.emptyMap();
        Set<Long> headIds = new HashSet<>();
        for (ErpMfgForecast f : forecasts) headIds.add(f.getId());
        QueryBean q = new QueryBean();
        q.addFilter(in("forecastId", headIds));
        if (materialId != null) q.addFilter(eq("materialId", materialId));
        List<ErpMfgForecastLine> lines = daoProvider.daoFor(ErpMfgForecastLine.class).findAllByQuery(q);
        Map<Long, BigDecimal> map = new HashMap<>();
        for (ErpMfgForecastLine l : lines) {
            if (!periodOverlaps(l.getPeriodStart(), l.getPeriodEnd(), periodStart, periodEnd)) continue;
            BigDecimal qty = nz(l.getForecastQty());
            if (qty.signum() == 0) continue;
            map.merge(l.getMaterialId(), qty, BigDecimal::add);
        }
        return map;
    }

    private List<ErpMfgForecast> loadApprovedForecasts() {
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpMfgConstants.FORECAST_STATUS_APPROVED));
        return daoProvider.daoFor(ErpMfgForecast.class).findAllByQuery(q);
    }

    private Map<Long, BigDecimal> aggregateActualQty(Long materialId, LocalDate periodStart, LocalDate periodEnd) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED));
        if (materialId != null) q.addFilter(eq("productId", materialId));
        List<ErpMfgWorkOrder> orders = daoProvider.daoFor(ErpMfgWorkOrder.class).findAllByQuery(q);
        Map<Long, BigDecimal> map = new HashMap<>();
        for (ErpMfgWorkOrder wo : orders) {
            if (!periodOverlaps(wo.getPlannedStartDate(), wo.getPlannedEndDate(), periodStart, periodEnd)) continue;
            BigDecimal qty = nz(wo.getCompletedQuantity());
            if (qty.signum() == 0) continue;
            Long prodId = wo.getProductId();
            if (prodId == null) continue;
            map.merge(prodId, qty, BigDecimal::add);
        }
        return map;
    }

    /** 区间相交判断：[aStart,aEnd] ∩ [bStart,bEnd] 非空（null 视为无界）。 */
    private static boolean periodOverlaps(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        if (bStart == null && bEnd == null) return true;
        LocalDate s = aStart != null ? aStart : LocalDate.MIN;
        LocalDate e = aEnd != null ? aEnd : LocalDate.MAX;
        LocalDate bs = bStart != null ? bStart : LocalDate.MIN;
        LocalDate be = bEnd != null ? bEnd : LocalDate.MAX;
        return !s.isAfter(be) && !e.isBefore(bs);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
