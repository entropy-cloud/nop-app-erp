
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.contract.dao.entity.ErpCtVolumeDiscount;
import app.erp.ct.biz.IErpCtVolumeDiscountBiz;
import app.erp.ct.dao.dto.DiscountResult;
import app.erp.ct.service.ErpCtErrors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 批量折扣 BizModel。数量区间折扣解析 + 区间带无重叠校验
 * （对齐 {@code docs/design/contract/volume-discount.md} §折扣应用逻辑 / §ErpCtVolumeDiscount）。
 *
 * <p>命中规则：{@code fromQty <= qty < toQty}（toQty=null 为末端闭区间，无上限）。
 * {@code discountPercent} 优先；若带设 {@code unitPrice} 覆盖价则用覆盖价。无命中回退原价。
 *
 * <p>无重叠校验：保存时校验同 {@code contractLineId} 下 [fromQty, toQty) 不相交，
 * 重叠抛 {@link ErpCtErrors#ERR_CT_DISCOUNT_BAND_OVERLAP}。
 */
@BizModel("ErpCtVolumeDiscount")
public class ErpCtVolumeDiscountBizModel extends CrudBizModel<ErpCtVolumeDiscount>
        implements IErpCtVolumeDiscountBiz {

    public ErpCtVolumeDiscountBizModel() {
        setEntityName(ErpCtVolumeDiscount.class.getName());
    }

    @Override
    @BizQuery
    public DiscountResult resolveDiscount(@Name("contractLineId") Long contractLineId,
                                          @Name("qty") BigDecimal qty,
                                          @Name("unitPrice") BigDecimal unitPrice,
                                          IServiceContext context) {
        BigDecimal basePrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        BigDecimal quantity = qty == null ? BigDecimal.ZERO : qty;

        List<ErpCtVolumeDiscount> bands = findBands(contractLineId, context);
        ErpCtVolumeDiscount matched = matchBand(bands, quantity);
        if (matched == null) {
            // 无命中回退原价
            return new DiscountResult(basePrice, basePrice.multiply(quantity), false);
        }

        BigDecimal discountedUnitPrice;
        if (matched.getUnitPrice() != null && matched.getUnitPrice().signum() > 0) {
            // 覆盖价优先
            discountedUnitPrice = matched.getUnitPrice();
        } else {
            // discountPercent 优先（0~100）
            BigDecimal percent = matched.getDiscountPercent() == null ? BigDecimal.ZERO : matched.getDiscountPercent();
            BigDecimal factor = BigDecimal.ONE.subtract(percent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            discountedUnitPrice = basePrice.multiply(factor).setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal lineAmount = discountedUnitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        return new DiscountResult(discountedUnitPrice, lineAmount, true);
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCtVolumeDiscount> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        validateNoOverlap(entityData.getEntity(), context);
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpCtVolumeDiscount> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        validateNoOverlap(entityData.getEntity(), context);
    }

    // ---------- 区间带匹配 + 无重叠校验 ----------

    protected ErpCtVolumeDiscount matchBand(List<ErpCtVolumeDiscount> bands, BigDecimal qty) {
        for (ErpCtVolumeDiscount band : bands) {
            BigDecimal from = band.getFromQty();
            BigDecimal to = band.getToQty();
            if (from != null && qty.compareTo(from) < 0) {
                continue;
            }
            if (to != null && qty.compareTo(to) >= 0) {
                continue;
            }
            return band;
        }
        return null;
    }

    protected void validateNoOverlap(ErpCtVolumeDiscount entity, IServiceContext context) {
        if (entity.getContractLineId() == null) {
            return;
        }
        BigDecimal newFrom = entity.getFromQty() == null ? BigDecimal.ZERO : entity.getFromQty();
        BigDecimal newTo = entity.getToQty();
        for (ErpCtVolumeDiscount existing : findBands(entity.getContractLineId(), context)) {
            // 编辑自身时跳过
            if (entity.getId() != null && entity.getId().equals(existing.getId())) {
                continue;
            }
            BigDecimal exFrom = existing.getFromQty() == null ? BigDecimal.ZERO : existing.getFromQty();
            BigDecimal exTo = existing.getToQty();
            if (overlaps(newFrom, newTo, exFrom, exTo)) {
                throw new NopException(ErpCtErrors.ERR_CT_DISCOUNT_BAND_OVERLAP)
                        .param(ErpCtErrors.ARG_CONTRACT_LINE_ID, entity.getContractLineId())
                        .param(ErpCtErrors.ARG_FROM_QTY, newFrom)
                        .param(ErpCtErrors.ARG_TO_QTY, newTo == null ? "∞" : newTo);
            }
        }
    }

    /**
     * [newFrom, newTo) 与 [exFrom, exTo) 是否相交（半开区间，null 上限=无穷）。
     */
    protected boolean overlaps(BigDecimal newFrom, BigDecimal newTo, BigDecimal exFrom, BigDecimal exTo) {
        // 相交条件：newFrom < exTo(or ∞) && exFrom < newTo(or ∞)
        boolean newStartsBeforeExEnd = exTo == null || newFrom.compareTo(exTo) < 0;
        boolean exStartsBeforeNewEnd = newTo == null || exFrom.compareTo(newTo) < 0;
        return newStartsBeforeExEnd && exStartsBeforeNewEnd;
    }

    @SuppressWarnings("unchecked")
    protected List<ErpCtVolumeDiscount> findBands(Long contractLineId, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("contractLineId", contractLineId));
        Object result = findList(query, null, context);
        return (List<ErpCtVolumeDiscount>) result;
    }
}
