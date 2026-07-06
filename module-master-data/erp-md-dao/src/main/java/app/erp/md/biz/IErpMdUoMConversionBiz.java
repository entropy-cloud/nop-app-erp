
package app.erp.md.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.md.dao.entity.ErpMdUoMConversion;

/**
 * 单位换算业务服务（UC-MD-02，{@code docs/design/master-data/use-cases.md}）。
 *
 * <p>换算引擎优先级：物料级 ErpMdUoMConversion → 通用 ErpMdUoMConversion →
 * （strict=false 时）回退 SKU.conversionRate；strict=true 未命中抛错。
 */
public interface IErpMdUoMConversionBiz extends ICrudBiz<ErpMdUoMConversion>{

    /**
     * UC-MD-02：按物料+源单位+目标单位换算数量。
     *
     * <p>解析系数优先级：物料级 ErpMdUoMConversion(materialId 非空) → 通用(materialId null)。
     * strict=true 未命中抛 {@link io.nop.api.core.exceptions.NopException}；false 回退源 SKU.conversionRate。
     * 输出 BigDecimal HALF_UP scale=4。
     */
    @BizQuery
    java.math.BigDecimal convertQty(@Name("materialId") Long materialId,
                                    @Name("qty") java.math.BigDecimal qty,
                                    @Name("fromUoMId") Long fromUoMId,
                                    @Name("toUoMId") Long toUoMId,
                                    IServiceContext context);
}
