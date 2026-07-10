package app.erp.md.spi;

import java.math.BigDecimal;

import app.erp.md.dao.dto.ResolvedPrice;
import app.erp.md.dao.entity.ErpMdMaterialSku;

import io.nop.core.context.IServiceContext;

/**
 * 客户价格清单解析 SPI（UC-SAL-11 定价引擎入口）。
 *
 * <p>master-data 是基础域，不得反向依赖 sales（依赖环约束，见
 * {@code docs/architecture/domain-module-split-analysis.md}）。销售侧
 * {@code ErpSalPriceList} 价格清单层经本 SPI 解耦：master-data 仅声明端口，
 * 销售域实现并注册（{@link jakarta.inject.Inject}{@code (required=false)} 注入）。
 *
 * <p>默认无实现时返回 {@code null}（价格清单层空转，resolvePrice 回退到 SKU 默认档）。
 */
public interface IErpMdCustomerPriceResolver {

    /**
     * 按客户/客户组 + SKU + 数量阶梯 + 币种 + 期间 解析客户价格清单命中价。
     *
     * @param sku        SKU 实体（已含 materialId）
     * @param partnerId  客户 ID（可空）
     * @param quantity   数量（阶梯匹配用，可空视为 1）
     * @param currencyId 币种 ID（可空，清单 currencyId 须匹配或清单 currencyId 为空）
     * @param context    服务上下文
     * @return 命中的协议价 + 来源信息；无命中返回 null
     */
    ResolvedPrice resolveCustomerPrice(ErpMdMaterialSku sku, Long partnerId,
                                       BigDecimal quantity, Long currencyId,
                                       IServiceContext context);
}
