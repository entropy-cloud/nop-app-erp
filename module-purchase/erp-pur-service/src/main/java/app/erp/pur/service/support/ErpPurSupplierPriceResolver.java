package app.erp.pur.service.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;

import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.spi.IErpMdSupplierPriceResolver;
import app.erp.pur.dao.entity.ErpPurSupplierPriceList;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 供应商价格清单解析器（UC-MD-03 采购方向价格表层，P1-RC-063 生产实现）。
 *
 * <p>实现 {@link IErpMdSupplierPriceResolver} SPI，由 master-data 域
 * {@code ErpMdMaterialSkuBizModel} 经 {@code @Inject @Nullable} 类型注入。匹配链：
 * <ol>
 *   <li>{@code supplierId == partnerId}（L1「供应商专属」= partner 即供应商）+ {@code materialId == sku.materialId}
 *       + {@code isActive == true}。</li>
 *   <li>单位匹配（U1 裁决）：{@code uoMId == sku.uoMId} 精确匹配（多单位 SKU 按单位取价）；
 *       {@code sku.uoMId} 为 null 时宽放到仅 materialId 匹配。</li>
 *   <li>效期窗口：{@code validFrom <= today <= validTo}，null 边界视为开放。</li>
 *   <li>命中多条按 priority 数字小优先 + 同 priority unitPrice 低者优先（P1 裁决，orm.xml:399 权威声明）。</li>
 * </ol>
 * 货币维度不参与匹配（U2 裁决——SPI 签名无 currencyId 参数，货币一致性由价格表维护方保证）。
 *
 * <p>使用 QueryBean 构造查询（避免 @SqlLibMapper 引入复杂映射），取价为高频读路径但价格清单行数据量可控。
 * 本类非 BizModel，跨模块读 ErpPurSupplierPriceList 表须经 IDaoProvider（I*Biz 注入不适用——SPI 解析器
 * 由 master-data 反向注入，无 purchase I*Biz 可注入），镜像 {@code ErpSalCustomerPriceResolver} 同型范式。
 */
public class ErpPurSupplierPriceResolver implements IErpMdSupplierPriceResolver {

    @Inject
    IDaoProvider daoProvider;

    @Override
    public BigDecimal resolveSupplierPrice(ErpMdMaterialSku sku, Long partnerId) {
        if (sku == null || partnerId == null || sku.getMaterialId() == null) {
            return null;
        }
        LocalDate today = CoreMetrics.currentDate();
        List<ErpPurSupplierPriceList> candidates = findCandidates(sku, partnerId);
        ErpPurSupplierPriceList best = null;
        for (ErpPurSupplierPriceList line : candidates) {
            if (!matchesPeriod(line, today)) {
                continue;
            }
            if (best == null || isBetter(line, best)) {
                best = line;
            }
        }
        return best == null ? null : best.getUnitPrice();
    }

    protected List<ErpPurSupplierPriceList> findCandidates(ErpMdMaterialSku sku, Long partnerId) {
        IEntityDao<ErpPurSupplierPriceList> dao = daoProvider.daoFor(ErpPurSupplierPriceList.class);
        QueryBean query = new QueryBean();
        query.addFilter(eq("supplierId", partnerId));
        query.addFilter(eq("materialId", sku.getMaterialId()));
        query.addFilter(eq("isActive", Boolean.TRUE));
        // U1 裁决：uoMId 精确匹配；sku.uoMId 为 null 时宽放到仅 materialId
        if (sku.getUoMId() != null) {
            query.addFilter(eq("uoMId", sku.getUoMId()));
        }
        // 效期窗口 + priority 裁决在内存完成（validFrom/validTo 空端开放，避免 IS NULL OR 条件复杂化 QueryBean，
        // 同 ErpSalCustomerPriceResolver.findCandidatePriceLists 范式）
        return dao.findAllByQuery(query);
    }

    /**
     * 效期窗口匹配：null validFrom/validTo 视为开放边界。
     */
    protected boolean matchesPeriod(ErpPurSupplierPriceList line, LocalDate today) {
        LocalDate from = line.getValidFrom();
        LocalDate to = line.getValidTo();
        if (from != null && today.isBefore(from)) {
            return false;
        }
        return to == null || !today.isAfter(to);
    }

    /**
     * P1 裁决：priority 数字小优先；同 priority 时 unitPrice 低者优先（采购保守语义）。
     */
    protected boolean isBetter(ErpPurSupplierPriceList line, ErpPurSupplierPriceList best) {
        int linePriority = line.getPriority() == null ? Integer.MAX_VALUE : line.getPriority();
        int bestPriority = best.getPriority() == null ? Integer.MAX_VALUE : best.getPriority();
        if (linePriority != bestPriority) {
            return linePriority < bestPriority;
        }
        BigDecimal linePrice = line.getUnitPrice();
        BigDecimal bestPrice = best.getUnitPrice();
        if (linePrice == null) {
            return false;
        }
        if (bestPrice == null) {
            return true;
        }
        return linePrice.compareTo(bestPrice) < 0;
    }
}
