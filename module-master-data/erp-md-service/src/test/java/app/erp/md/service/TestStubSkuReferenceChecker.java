package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.spi.IErpMdSkuReferenceChecker;

import java.util.HashSet;
import java.util.Set;

/**
 * 测试专用 SKU 引用检查 SPI 桩（plan 2026-07-07-0024-1 Phase 3）。
 *
 * <p>替代真实下游域（purchase/sales/inventory）的引用检查（master-data 不得反向依赖下游域）。
 * 由 {@code test-sku-reference-checker.beans.xml} 注册，经 {@code @Nullable @Inject} 注入到
 * {@code ErpMdMaterialSkuBizModel.skuReferenceChecker}。
 *
 * <p>测试方法在 seed 阶段调 {@link #markReferenced} 标记被引用的 SKU ID。
 */
public class TestStubSkuReferenceChecker implements IErpMdSkuReferenceChecker {

    private final Set<Long> referencedSkuIds = new HashSet<>();

    @Override
    public boolean isReferencedByBill(ErpMdMaterialSku sku) {
        return sku != null && sku.getId() != null && referencedSkuIds.contains(sku.getId());
    }

    public void markReferenced(Long skuId) {
        referencedSkuIds.add(skuId);
    }

    public void clear() {
        referencedSkuIds.clear();
    }
}
