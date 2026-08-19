package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.spi.IErpMdSkuReferenceChecker;

import java.util.HashSet;
import java.util.Set;

/**
 * 测试专用 SKU 引用检查 SPI 桩（第二实例，RC-R1.72 Phase 2 聚合 OR 语义证明）。
 *
 * <p>与 {@link TestStubSkuReferenceChecker} 同型，经
 * {@code test-sku-reference-checker-multi.beans.xml} 注册为第二个独立 checker bean——
 * 验证 {@code ErpMdSkuReferenceCheckerRegistry} List 收集器的「任一命中即拒绝」聚合语义
 * （任一实例 markReferenced → 拒绝；两实例均未标记 → 放行）。
 */
public class TestStubSkuReferenceCheckerSecondary implements IErpMdSkuReferenceChecker {

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
