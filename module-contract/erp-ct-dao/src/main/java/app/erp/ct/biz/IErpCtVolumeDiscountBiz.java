
package app.erp.ct.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.contract.dao.entity.ErpCtVolumeDiscount;
import app.erp.ct.dao.dto.DiscountResult;

/**
 * 批量折扣业务接口。除标准 CRUD 外，定义数量区间折扣解析契约
 * （对齐 {@code docs/design/contract/volume-discount.md} §折扣应用逻辑）：
 *
 * <ul>
 *   <li>{@link #resolveDiscount}：按数量命中区间带（{@code fromQty <= qty < toQty}，末端闭区间 toQty=null 无上限），
 *       返回折扣单价/行金额。{@code discountPercent} 优先；若带设 {@code unitPrice} 覆盖价则用覆盖价。无命中回退原价。</li>
 * </ul>
 *
 * <p>保存时校验同 {@code contractLineId} 下区间带无重叠，重叠抛
 * {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpCtVolumeDiscountBiz extends ICrudBiz<ErpCtVolumeDiscount> {

    @BizQuery
    DiscountResult resolveDiscount(@Name("contractLineId") Long contractLineId,
                                   @Name("qty") java.math.BigDecimal qty,
                                   @Name("unitPrice") java.math.BigDecimal unitPrice,
                                   IServiceContext context);
}
