package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.spi.IErpMdSupplierPriceResolver;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试专用供应商价格表解析 SPI 桩（plan 2026-07-07-0024-1 Phase 2）。
 *
 * <p>替代真实 purchase 域的 {@code ErpPurSupplierPriceList} 解析（master-data 不得反向依赖 purchase，
 * 由本桩在测试侧模拟价格表层命中）。由 {@code test-supplier-price-resolver.beans.xml} 注册，
 * 经 {@code @Nullable @Inject} 注入到 {@code ErpMdMaterialSkuBizModel.supplierPriceResolver}。
 *
 * <p>测试方法在 seed 阶段调 {@link #putPrice} 注入 (skuId, partnerId) → 命中价。
 */
public class TestStubSupplierPriceResolver implements IErpMdSupplierPriceResolver {

    private final Map<String, BigDecimal> prices = new HashMap<>();

    @Override
    public BigDecimal resolveSupplierPrice(ErpMdMaterialSku sku, Long partnerId) {
        if (sku == null || sku.getId() == null) {
            return null;
        }
        String key = key(sku.getId(), partnerId);
        return prices.get(key);
    }

    public void putPrice(Long skuId, Long partnerId, BigDecimal price) {
        prices.put(key(skuId, partnerId), price);
    }

    public void clear() {
        prices.clear();
    }

    private String key(Long skuId, Long partnerId) {
        return skuId + "@" + partnerId;
    }
}
