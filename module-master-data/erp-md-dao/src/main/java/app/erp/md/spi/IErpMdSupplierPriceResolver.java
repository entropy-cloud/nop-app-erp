package app.erp.md.spi;

import java.math.BigDecimal;

import app.erp.md.dao.entity.ErpMdMaterialSku;

/**
 * 供应商价格表解析 SPI（UC-MD-03 价格表层入口）。
 *
 * <p>master-data 是基础域，不得反向依赖 purchase/sales（依赖环约束，见
 * {@code docs/architecture/domain-module-split-analysis.md}）。采购侧
 * {@code ErpPurSupplierPriceList} 价格表层经本 SPI 解耦：master-data 仅声明端口，
 * 下游域（purchase）实现并注册（{@link jakarta.inject.Inject}{@code (required=false)} 注入）。
 *
 * <p>默认无实现时返回 {@code null}（价格表层空转，resolvePrice 回退到 SKU 默认档）。
 * 下游接线归 Deferred（见计划 Follow-up）。
 */
public interface IErpMdSupplierPriceResolver {

    /**
     * 按供应商/客户 + SKU 解析价格表层命中价。
     *
     * @param sku       SKU 实体（已含 materialId）
     * @param partnerId 供应商/客户 ID
     * @return 命中的协议单价；无命中返回 null
     */
    BigDecimal resolveSupplierPrice(ErpMdMaterialSku sku, Long partnerId);
}
