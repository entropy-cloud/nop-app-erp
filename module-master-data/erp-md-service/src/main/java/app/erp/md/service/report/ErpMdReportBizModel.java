package app.erp.md.service.report;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.service.ErpMdErrors;
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
 * 主数据域报表渲染入口。注入平台 {@link IReportEngine}，按报表名解析 VFS 模板路径
 * （{@code /nop/main/report/md/<name>.xpt.xml}）并渲染 html/xlsx/pdf。
 *
 * <p>镜像 {@code ErpMfgReportBizModel}/{@code ErpHrReportBizModel} 域隔离范式（…quality→{@code /qa/}，
 * master-data→{@code /md/}）：不自建报表引擎，渲染逻辑全部委托给 {@code nop-report}；报表名经
 * {@link StringHelper#isValidVPath} 校验防路径注入。
 *
 * <p>两张种子报表：物料价格清单（{@link ErpMdMaterial} ⨝ {@link ErpMdMaterialSku} 四档价格，对齐
 * {@code master-data/README.md}）+ 往来单位清单（{@link ErpMdPartner} 客户/供应商分类，对齐
 * {@code master-data/README.md}）。
 */
@BizModel("ErpMdReport")
public class ErpMdReportBizModel {

    /** 主数据报表模板 VFS 根路径（与其他域隔离）。 */
    static final String REPORT_PATH_PREFIX = "/nop/main/report/md/";

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
            throw new NopException(ErpMdErrors.ERR_REPORT_RENDER_TYPE_INVALID)
                    .param(ErpMdErrors.ARG_RENDER_TYPE, renderType);
        }
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);

        ITemplateOutput output = reportEngine.getRenderer(path, renderType);
        IResource resource = ResourceHelper.getTempResource("md-rpt");
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
            throw new NopException(ErpMdErrors.ERR_REPORT_NAME_INVALID)
                    .param(ErpMdErrors.ARG_REPORT_NAME, reportName);
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
            case "material-price-list":
                data.put(DS_VAR, buildMaterialPriceListDataset(asString(data, "materialCode")));
                break;
            case "partner-list":
                data.put(DS_VAR, buildPartnerListDataset(asString(data, "partnerType")));
                break;
            default:
                break;
        }
    }

    private static String asString(Map<String, Object> data, String k) {
        if (data == null) return null;
        Object v = data.get(k);
        return v == null ? null : v.toString();
    }

    // ===================== 数据集构造（也作 @BizQuery 供前端取原始数据） =====================

    /** 物料价格清单数据集：物料×默认SKU 四档价格，对齐 {@code master-data/README.md}。 */
    @BizQuery
    public List<Map<String, Object>> materialPriceListData(@Optional @Name("materialCode") String materialCode,
                                                            IServiceContext context) {
        return buildMaterialPriceListDataset(materialCode);
    }

    /** 往来单位清单数据集：客户/供应商分类清单，对齐 {@code master-data/README.md}。 */
    @BizQuery
    public List<Map<String, Object>> partnerListData(@Optional @Name("partnerType") String partnerType,
                                                      IServiceContext context) {
        return buildPartnerListDataset(partnerType);
    }

    // ===================== 数据集聚合实现 =====================

    /**
     * 物料价格清单数据集。从 {@link ErpMdMaterial}（可选 code 模糊过滤，这里精确过滤简化）+ 关联默认
     * {@link ErpMdMaterialSku}（{@code isDefault=true}）四档价格合成，对齐 {@code master-data/README.md}。
     * 无默认 SKU 时价格列留空。
     */
    List<Map<String, Object>> buildMaterialPriceListDataset(String materialCode) {
        return ormTemplate.runInSession(session -> {
            List<ErpMdMaterial> materials = loadMaterials(materialCode);
            if (materials.isEmpty()) {
                return Collections.emptyList();
            }
            Set<Long> materialIds = new HashSet<>();
            for (ErpMdMaterial m : materials) {
                if (m.getId() != null) materialIds.add(m.getId());
            }
            Map<Long, ErpMdMaterialSku> defaultSkuByMaterial = loadDefaultSkus(materialIds);
            List<Map<String, Object>> rows = new ArrayList<>(materials.size());
            for (ErpMdMaterial m : materials) {
                ErpMdMaterialSku sku = defaultSkuByMaterial.get(m.getId());
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("materialId", m.getId());
                r.put("materialCode", m.getCode());
                r.put("materialName", m.getName());
                r.put("materialType", m.orm_propValueByName("materialType"));
                r.put("status", m.getStatus());
                r.put("skuCode", sku != null ? sku.getSkuCode() : null);
                r.put("purchasePrice", sku != null ? nz(sku.getPurchasePrice()) : null);
                r.put("salePrice", sku != null ? nz(sku.getSalePrice()) : null);
                r.put("wholesalePrice", sku != null ? nz(sku.getWholesalePrice()) : null);
                r.put("retailPrice", sku != null ? nz(sku.getRetailPrice()) : null);
                rows.add(r);
            }
            return rows;
        });
    }

    /**
     * 往来单位清单数据集。从 {@link ErpMdPartner}（可选 partnerType 过滤：CUSTOMER/SUPPLIER）按 code 排序，
     * 对齐 {@code master-data/README.md}。
     */
    List<Map<String, Object>> buildPartnerListDataset(String partnerType) {
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            if (partnerType != null) q.addFilter(eq("partnerType", partnerType));
            q.addOrderField("code", false);
            List<ErpMdPartner> partners = daoProvider.daoFor(ErpMdPartner.class).findAllByQuery(q);
            if (partners.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> rows = new ArrayList<>(partners.size());
            for (ErpMdPartner p : partners) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("partnerId", p.getId());
                r.put("partnerCode", p.getCode());
                r.put("partnerName", p.getName());
                r.put("partnerType", p.orm_propValueByName("partnerType"));
                r.put("status", p.getStatus());
                r.put("contactPerson", p.getContactPerson());
                r.put("phone", p.getPhone());
                r.put("email", p.getEmail());
                r.put("creditLimit", nz(p.getCreditLimit()));
                r.put("creditPeriodDays", p.getCreditPeriodDays());
                rows.add(r);
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private List<ErpMdMaterial> loadMaterials(String materialCode) {
        QueryBean q = new QueryBean();
        if (materialCode != null) q.addFilter(eq("code", materialCode));
        q.addOrderField("code", false);
        return daoProvider.daoFor(ErpMdMaterial.class).findAllByQuery(q);
    }

    private Map<Long, ErpMdMaterialSku> loadDefaultSkus(Set<Long> materialIds) {
        if (materialIds.isEmpty()) return Collections.emptyMap();
        QueryBean q = new QueryBean();
        q.addFilter(in("materialId", materialIds));
        q.addFilter(eq("isDefault", Boolean.TRUE));
        List<ErpMdMaterialSku> skus = daoProvider.daoFor(ErpMdMaterialSku.class).findAllByQuery(q);
        Map<Long, ErpMdMaterialSku> map = new HashMap<>();
        for (ErpMdMaterialSku s : skus) {
            map.put(s.getMaterialId(), s);
        }
        return map;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
