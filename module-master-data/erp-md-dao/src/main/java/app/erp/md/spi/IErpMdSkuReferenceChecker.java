package app.erp.md.spi;

import app.erp.md.dao.entity.ErpMdMaterialSku;

/**
 * SKU 业务单据引用检查 SPI（UC-MD-06 删除/停用引用校验入口）。
 *
 * <p>master-data 是基础域，不得反向依赖 purchase/sales/inventory（依赖环约束，见
 * {@code docs/architecture/domain-module-split-analysis.md}）。SKU 被未完成单据引用的检查经本 SPI 解耦：
 * master-data 仅声明端口，下游域（purchase/sales/inventory 等）各自实现并注册
 * （{@link jakarta.inject.Inject}{@code (required=false)} 注入）。
 *
 * <p>默认无实现时跨域引用校验空转（仅域内默认 SKU 守卫生效）。下游接线归 Deferred（见计划 Follow-up）。
 */
public interface IErpMdSkuReferenceChecker {

    /**
     * 检查指定 SKU 是否被未完成业务单据引用。
     *
     * @param sku SKU 实体
     * @return 被引用返回 true（应拒绝删除/停用）；未被引用返回 false
     */
    boolean isReferencedByBill(ErpMdMaterialSku sku);
}
