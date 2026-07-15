
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.crm.biz.IErpCrmPriceRuleBiz;
import app.erp.crm.dao.entity.ErpCrmPriceRule;
import app.erp.crm.service.ErpCrmErrors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 价格规则 BizModel（plan 2026-07-07-1430-2 §Phase 1）。
 *
 * <p>维护钩子：
 * <ul>
 *   <li>{@code maxQuantity >= minQuantity}（同填时）。</li>
 *   <li>生效日期 {@code effectiveFrom <= effectiveTo}。</li>
 *   <li>{@code discountPercent}（0-100）与 {@code discountAmount}（非负）合理性。</li>
 * </ul>
 */
@BizModel("ErpCrmPriceRule")
public class ErpCrmPriceRuleBizModel extends CrudBizModel<ErpCrmPriceRule> implements IErpCrmPriceRuleBiz {
    public ErpCrmPriceRuleBizModel() {
        setEntityName(ErpCrmPriceRule.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCrmPriceRule> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        validate(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpCrmPriceRule> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        validate(entityData.getEntity());
    }

    protected void validate(ErpCrmPriceRule rule) {
        if (rule == null) {
            return;
        }
        validateEffectiveDate(rule.getEffectiveFrom(), rule.getEffectiveTo());
        validateQtyRange(rule.getMinQuantity(), rule.getMaxQuantity());
        validateDiscounts(rule.getDiscountPercent(), rule.getDiscountAmount());
    }

    protected void validateEffectiveDate(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new NopException(ErpCrmErrors.ERR_CPQ_EFFECTIVE_DATE_INVALID)
                    .param(ErpCrmErrors.ARG_EFFECTIVE_FROM, from)
                    .param(ErpCrmErrors.ARG_EFFECTIVE_TO, to);
        }
    }

    protected void validateQtyRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new NopException(ErpCrmErrors.ERR_CPQ_QTY_RANGE_INVALID)
                    .param(ErpCrmErrors.ARG_MIN_QTY, min)
                    .param(ErpCrmErrors.ARG_MAX_QTY, max);
        }
    }

    protected void validateDiscounts(Double percent, BigDecimal amount) {
        if (percent != null && (percent < 0 || percent > 100)) {
            throw new NopException(ErpCrmErrors.ERR_CPQ_DISCOUNT_INCONSISTENT)
                    .param(ErpCrmErrors.ARG_DISCOUNT_TYPE, "discountPercent=" + percent);
        }
        if (amount != null && amount.signum() < 0) {
            throw new NopException(ErpCrmErrors.ERR_CPQ_DISCOUNT_INCONSISTENT)
                    .param(ErpCrmErrors.ARG_DISCOUNT_TYPE, "discountAmount=" + amount);
        }
    }

    

}
