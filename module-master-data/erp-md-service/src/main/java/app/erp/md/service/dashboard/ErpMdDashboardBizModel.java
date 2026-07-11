package app.erp.md.service.dashboard;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.beans.query.QueryBean;
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

    /** 数据质量预警扫描的服务端硬上限（类 D 裁决保留）。 */
    private static final int ALERT_MAX_ROWS = 5000;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @BizQuery
    public Map<String, Object> getDashboardKpi(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpMdMaterial> materialDao = daoProvider.daoFor(ErpMdMaterial.class);
            IEntityDao<ErpMdPartner> partnerDao = daoProvider.daoFor(ErpMdPartner.class);

            long materialCount = materialDao.countByQuery(new QueryBean());
            long customerCount = partnerDao.countByQuery(eqQuery("partnerType", PARTNER_TYPE_CUSTOMER));
            long vendorCount = partnerDao.countByQuery(eqQuery("partnerType", PARTNER_TYPE_VENDOR));
            long inactiveMaterialCount = materialDao.countByQuery(eqQuery("status", STATUS_INACTIVE));
            long inactivePartnerCount = partnerDao.countByQuery(eqQuery("status", STATUS_INACTIVE));

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
            // 类 C：仅取 SKU 的 materialId 集合（单字段，带硬上限）
            IEntityDao<ErpMdMaterialSku> skuDao = daoProvider.daoFor(ErpMdMaterialSku.class);
            QueryBean skuQ = new QueryBean();
            skuQ.setLimit(ALERT_MAX_ROWS);
            Set<Long> materialIdsWithSku = new HashSet<>();
            for (ErpMdMaterialSku s : skuDao.findAllByQuery(skuQ)) {
                if (s.getMaterialId() != null) materialIdsWithSku.add(s.getMaterialId());
            }
            // 类 D：物料明细逐行比对，带硬上限的受限扫描
            IEntityDao<ErpMdMaterial> materialDao = daoProvider.daoFor(ErpMdMaterial.class);
            QueryBean mq = new QueryBean();
            mq.setLimit(ALERT_MAX_ROWS);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpMdMaterial m : materialDao.findAllByQuery(mq)) {
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
            // 类 D 裁决：SKU 明细逐行价格比对，带硬上限的受限扫描
            IEntityDao<ErpMdMaterialSku> skuDao = daoProvider.daoFor(ErpMdMaterialSku.class);
            QueryBean q = new QueryBean();
            q.setLimit(ALERT_MAX_ROWS);
            List<ErpMdMaterialSku> skus = skuDao.findAllByQuery(q);
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

    private static QueryBean eqQuery(String field, String value) {
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq(field, value));
        return q;
    }

    private static java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v == null ? java.math.BigDecimal.ZERO : v;
    }
}
