package app.erp.md.service.dashboard;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 主数据看板聚合入口（{@code dashboards.md 主数据看板}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经内存聚合，镜像 {@code ErpFinDashboardBizModel} 范式。
 *
 * <p>KPI 口径：物料总数取自 {@link ErpMdMaterial} count；往来单位总数取自 {@link ErpMdPartner}
 * （按 partnerType=customer/vendor 分计）；停用主数据数取各主数据 status=INACTIVE count。
 *
 * <p>预警：无 SKU 物料（无关联 MaterialSku 的物料，数据质量）；无价格物料（无任何价格档的 SKU）。
 */
@BizModel("ErpMdDashboard")
public class ErpMdDashboardBizModel {

    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String PARTNER_TYPE_CUSTOMER = "CUSTOMER";
    private static final String PARTNER_TYPE_VENDOR = "SUPPLIER";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @BizQuery
    public Map<String, Object> getDashboardKpi(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpMdMaterial> materials = daoProvider.daoFor(ErpMdMaterial.class).findAll();
            List<ErpMdPartner> partners = daoProvider.daoFor(ErpMdPartner.class).findAll();
            List<ErpMdMaterialSku> skus = daoProvider.daoFor(ErpMdMaterialSku.class).findAll();

            long materialCount = materials.size();
            long customerCount = partners.stream()
                    .filter(p -> PARTNER_TYPE_CUSTOMER.equals(p.getPartnerType()))
                    .count();
            long vendorCount = partners.stream()
                    .filter(p -> PARTNER_TYPE_VENDOR.equals(p.getPartnerType()))
                    .count();
            long inactiveMaterialCount = materials.stream()
                    .filter(m -> STATUS_INACTIVE.equals(m.getStatus()))
                    .count();
            long inactivePartnerCount = partners.stream()
                    .filter(p -> STATUS_INACTIVE.equals(p.getStatus()))
                    .count();

            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("materialCount", materialCount);
            kpi.put("customerCount", customerCount);
            kpi.put("vendorCount", vendorCount);
            kpi.put("inactiveMaterialCount", inactiveMaterialCount);
            kpi.put("inactivePartnerCount", inactivePartnerCount);
            return kpi;
        });
    }

    /** 无 SKU 物料预警（无关联 MaterialSku 的物料，数据质量）。 */
    @BizQuery
    public List<Map<String, Object>> findMaterialWithoutSkuAlert(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpMdMaterial> materials = daoProvider.daoFor(ErpMdMaterial.class).findAll();
            Set<Long> materialIdsWithSku = new HashSet<>();
            for (ErpMdMaterialSku s : daoProvider.daoFor(ErpMdMaterialSku.class).findAll()) {
                if (s.getMaterialId() != null) materialIdsWithSku.add(s.getMaterialId());
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpMdMaterial m : materials) {
                if (!materialIdsWithSku.contains(m.getId())) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("materialId", m.getId());
                    row.put("materialCode", m.getCode());
                    row.put("materialName", m.getName());
                    row.put("status", m.getStatus());
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    /** 无价格物料预警（无任何价格档的 SKU，数据质量）。 */
    @BizQuery
    public List<Map<String, Object>> findSkuWithoutPriceAlert(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpMdMaterialSku> skus = daoProvider.daoFor(ErpMdMaterialSku.class).findAll();
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpMdMaterialSku s : skus) {
                boolean hasPrice = nz(s.getPurchasePrice()).signum() > 0
                        || nz(s.getSalePrice()).signum() > 0
                        || nz(s.getWholesalePrice()).signum() > 0
                        || nz(s.getRetailPrice()).signum() > 0;
                if (!hasPrice) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("skuId", s.getId());
                    row.put("skuCode", s.getSkuCode());
                    row.put("materialId", s.getMaterialId());
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private static java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v == null ? java.math.BigDecimal.ZERO : v;
    }
}
