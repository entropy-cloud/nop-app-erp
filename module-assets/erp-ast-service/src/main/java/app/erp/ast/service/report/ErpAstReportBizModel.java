package app.erp.ast.service.report;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.service.ErpAstErrors;
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
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 资产域报表渲染入口。注入平台 {@link IReportEngine}，按报表名解析 VFS 模板路径
 * （{@code /nop/main/report/ast/<name>.xpt.xml}）并渲染 html/xlsx/pdf。
 *
 * <p>镜像 {@code ErpMfgReportBizModel}/{@code ErpHrReportBizModel} 域隔离范式（finance→{@code /fin/}，
 * manufacturing→{@code /mfg/}，inventory→{@code /inv/}，HR→{@code /hr/}，assets→{@code /ast/}）：
 * 不自建报表引擎，渲染逻辑全部委托给 {@code nop-report}；报表名经 {@link StringHelper#isValidVPath}
 * 校验防路径注入。
 *
 * <p>两张种子报表：资产折旧明细表（资产×原值/累计折旧/净值/本期折旧，对齐 {@code assets/state-machine.md}）
 * + 资产处置明细表（处置行 + 清理损益，对齐 {@code assets/state-machine.md}）。
 */
@BizModel("ErpAstReport")
public class ErpAstReportBizModel {

    /** 资产报表模板 VFS 根路径（与 finance/manufacturing/inventory/HR 域隔离）。 */
    static final String REPORT_PATH_PREFIX = "/nop/main/report/ast/";

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
            throw new NopException(ErpAstErrors.ERR_REPORT_RENDER_TYPE_INVALID)
                    .param(ErpAstErrors.ARG_RENDER_TYPE, renderType);
        }
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);

        ITemplateOutput output = reportEngine.getRenderer(path, renderType);
        IResource resource = ResourceHelper.getTempResource("ast-rpt");
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
     * <p>接受 {@code asset-depreciation-detail}、{@code asset-depreciation-detail.xpt.xml} 两种形式，
     * 统一补全到 {@code /nop/main/report/ast/asset-depreciation-detail.xpt.xml}。
     */
    String resolveReportPath(String reportName) {
        if (StringHelper.isEmpty(reportName) || !StringHelper.isValidVPath(reportName)) {
            throw new NopException(ErpAstErrors.ERR_REPORT_NAME_INVALID)
                    .param(ErpAstErrors.ARG_REPORT_NAME, reportName);
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
            case "asset-depreciation-detail":
                data.put(DS_VAR, buildAssetDepreciationDetailDataset(
                        asLong(data, "categoryId"), asDate(data, "startDate"), asDate(data, "endDate")));
                break;
            case "asset-disposal-detail":
                data.put(DS_VAR, buildAssetDisposalDetailDataset(
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

    /** 资产折旧明细数据集：按资产×类别聚合原值/累计折旧/净值/本期折旧，对齐 {@code assets/state-machine.md}。 */
    @BizQuery
    public List<Map<String, Object>> assetDepreciationDetailData(@Optional @Name("categoryId") Long categoryId,
                                                                  @Optional @Name("startDate") LocalDate startDate,
                                                                  @Optional @Name("endDate") LocalDate endDate,
                                                                  IServiceContext context) {
        return buildAssetDepreciationDetailDataset(categoryId, startDate, endDate);
    }

    /** 资产处置明细数据集：处置行 + 清理损益，对齐 {@code assets/state-machine.md}。 */
    @BizQuery
    public List<Map<String, Object>> assetDisposalDetailData(@Optional @Name("startDate") LocalDate startDate,
                                                              @Optional @Name("endDate") LocalDate endDate,
                                                              IServiceContext context) {
        return buildAssetDisposalDetailDataset(startDate, endDate);
    }

    // ===================== 数据集聚合实现 =====================

    /**
     * 资产折旧明细数据集。从 {@link ErpAstAsset}（原值/累计折旧/净值）+ {@link ErpAstDepreciationSchedule}
     * （按 businessDate 区间聚合 actualAmount 为「本期折旧」）合成，对齐 {@code assets/state-machine.md}。
     *
     * <p>类别名称经 {@link ErpAstAsset#getCategory()} 关系解析（避免 N+1：批量预取）。
     */
    List<Map<String, Object>> buildAssetDepreciationDetailDataset(Long categoryId, LocalDate startDate, LocalDate endDate) {
        return ormTemplate.runInSession(session -> {
            List<ErpAstAsset> assets = loadAssets(categoryId);
            if (assets.isEmpty()) {
                return Collections.emptyList();
            }
            Map<Long, BigDecimal> periodDepByAsset = aggregatePeriodDepreciation(assets, startDate, endDate);
            Map<Long, String> categoryNames = resolveCategoryNames(assets);
            List<Map<String, Object>> rows = new ArrayList<>(assets.size());
            for (ErpAstAsset a : assets) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("assetId", a.getId());
                r.put("assetCode", a.getCode());
                r.put("assetName", a.getName());
                r.put("categoryId", a.getCategoryId());
                r.put("categoryName", categoryNames.getOrDefault(a.getCategoryId(), ""));
                r.put("originalValue", nz(a.getOriginalValue()));
                r.put("accumulatedDepreciation", nz(a.getAccumulatedDepreciation()));
                r.put("netBookValue", nz(a.getNetBookValue()));
                r.put("periodDepreciation", nz(periodDepByAsset.get(a.getId())));
                rows.add(r);
            }
            return rows;
        });
    }

    /**
     * 资产处置明细数据集。从 {@link ErpAstDisposal}（处置类型/处置金额/清理损益/原因/单据状态）按 businessDate
     * 区间过滤，对齐 {@code assets/state-machine.md}。资产编码经 {@link ErpAstDisposal#getAsset()} 关系解析。
     */
    List<Map<String, Object>> buildAssetDisposalDetailDataset(LocalDate startDate, LocalDate endDate) {
        return ormTemplate.runInSession(session -> {
            List<ErpAstDisposal> disposals = loadDisposals(startDate, endDate);
            if (disposals.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> rows = new ArrayList<>(disposals.size());
            for (ErpAstDisposal d : disposals) {
                ErpAstAsset asset = d.getAsset();
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", d.getId());
                r.put("code", d.getCode());
                r.put("assetId", d.getAssetId());
                r.put("assetCode", asset != null ? asset.getCode() : null);
                r.put("assetName", asset != null ? asset.getName() : null);
                r.put("disposalType", d.getDisposalType());
                r.put("disposalAmount", nz(d.getDisposalAmount()));
                r.put("businessDate", d.getBusinessDate());
                r.put("gainLoss", nz(d.getGainLoss()));
                r.put("reason", d.getReason());
                r.put("docStatus", d.getDocStatus());
                rows.add(r);
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private List<ErpAstAsset> loadAssets(Long categoryId) {
        QueryBean q = new QueryBean();
        if (categoryId != null) q.addFilter(eq("categoryId", categoryId));
        q.addOrderField("code", false);
        return daoProvider.daoFor(ErpAstAsset.class).findAllByQuery(q);
    }

    private Map<Long, BigDecimal> aggregatePeriodDepreciation(List<ErpAstAsset> assets,
                                                               LocalDate startDate, LocalDate endDate) {
        Set<Long> assetIds = new HashSet<>();
        for (ErpAstAsset a : assets) {
            if (a.getId() != null) assetIds.add(a.getId());
        }
        if (assetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("assetId", assetIds));
        if (startDate != null) q.addFilter(ge("businessDate", startDate));
        if (endDate != null) q.addFilter(le("businessDate", endDate));
        List<ErpAstDepreciationSchedule> schedules = daoProvider.daoFor(ErpAstDepreciationSchedule.class).findAllByQuery(q);
        Map<Long, BigDecimal> map = new HashMap<>();
        for (ErpAstDepreciationSchedule s : schedules) {
            BigDecimal amt = nz(s.getActualAmount());
            if (amt.signum() == 0) continue;
            map.merge(s.getAssetId(), amt, BigDecimal::add);
        }
        return map;
    }

    private Map<Long, String> resolveCategoryNames(List<ErpAstAsset> assets) {
        Set<Long> catIds = new HashSet<>();
        for (ErpAstAsset a : assets) {
            if (a.getCategoryId() != null) catIds.add(a.getCategoryId());
        }
        if (catIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("id", catIds));
        List<ErpAstAssetCategory> cats = daoProvider.daoFor(ErpAstAssetCategory.class).findAllByQuery(q);
        Map<Long, String> names = new HashMap<>();
        for (ErpAstAssetCategory c : cats) {
            names.put(c.getId(), c.getName());
        }
        return names;
    }

    private List<ErpAstDisposal> loadDisposals(LocalDate startDate, LocalDate endDate) {
        QueryBean q = new QueryBean();
        if (startDate != null) q.addFilter(ge("businessDate", startDate));
        if (endDate != null) q.addFilter(le("businessDate", endDate));
        q.addOrderField("businessDate", false);
        q.addOrderField("code", false);
        return daoProvider.daoFor(ErpAstDisposal.class).findAllByQuery(q);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
