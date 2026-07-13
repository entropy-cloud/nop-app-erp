
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.crm.biz.IErpCrmBundlePricingBiz;
import app.erp.crm.dao.entity.ErpCrmBundlePricing;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;

import java.math.BigDecimal;
import java.time.LocalDate;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 捆绑定价 BizModel（plan 2026-07-07-1430-2 §Phase 1）。
 *
 * <p>维护钩子：
 * <ul>
 *   <li>{@code discountType}-{@code discountValue} 一致性（PERCENTAGE 须 0-100；FIXED 须非负）。</li>
 *   <li>生效日期 {@code effectiveFrom <= effectiveTo}。</li>
 * </ul>
 */
@BizModel("ErpCrmBundlePricing")
public class ErpCrmBundlePricingBizModel extends CrudBizModel<ErpCrmBundlePricing> implements IErpCrmBundlePricingBiz {
    public ErpCrmBundlePricingBizModel() {
        setEntityName(ErpCrmBundlePricing.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCrmBundlePricing> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        validate(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpCrmBundlePricing> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        validate(entityData.getEntity());
    }

    protected void validate(ErpCrmBundlePricing bundle) {
        if (bundle == null) {
            return;
        }
        validateEffectiveDate(bundle.getEffectiveFrom(), bundle.getEffectiveTo());
        validateDiscount(bundle.getDiscountType(), bundle.getDiscountValue());
    }

    protected void validateEffectiveDate(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new NopException(ErpCrmErrors.ERR_CPQ_EFFECTIVE_DATE_INVALID)
                    .param(ErpCrmErrors.ARG_EFFECTIVE_FROM, from)
                    .param(ErpCrmErrors.ARG_EFFECTIVE_TO, to);
        }
    }

    protected void validateDiscount(String discountType, BigDecimal discountValue) {
        if (discountType == null) {
            return;
        }
        if (discountValue == null) {
            // discountValue 为空时按 0 处理：不强制要求填值（bundleAmount 可独立覆盖）
            return;
        }
        BigDecimal value = discountValue;
        switch (discountType) {
            case ErpCrmConstants.BUNDLE_DISCOUNT_TYPE_PERCENTAGE:
                if (value.signum() < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new NopException(ErpCrmErrors.ERR_CPQ_DISCOUNT_INCONSISTENT)
                            .param(ErpCrmErrors.ARG_DISCOUNT_TYPE, discountType);
                }
                break;
            case ErpCrmConstants.BUNDLE_DISCOUNT_TYPE_FIXED:
                if (value.signum() < 0) {
                    throw new NopException(ErpCrmErrors.ERR_CPQ_DISCOUNT_INCONSISTENT)
                            .param(ErpCrmErrors.ARG_DISCOUNT_TYPE, discountType);
                }
                break;
            default:
                // 未知 discountType 由 xmeta 字典校验兜底，此处不重复
        }
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmBundlePricing.class)
    public List<String> orgName(@ContextSource List<ErpCrmBundlePricing> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmBundlePricing row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
