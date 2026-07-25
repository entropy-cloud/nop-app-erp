
package app.erp.sal.service.entity;

import app.erp.md.dao.daterange.IDateRange;
import app.erp.md.service.daterange.ErpDateRangeOverlapValidator;
import app.erp.sal.biz.IErpSalPricingRuleBiz;
import app.erp.sal.dao.entity.ErpSalPricingRule;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售促销规则 BizModel。
 *
 * <p>基础 CRUD 走 {@link CrudBizModel}。扩展 C3 日期范围有效性保存钩子（plan 2026-07-26-0315-1 Phase 2）：
 * STACKABLE 混合策略——同维度（ruleType + targetType + materialId/materialCategoryId +
 * customerGroupCode/partnerId）的规则中，{@code stackable=false} 规则之间互斥；
 * 任一方 {@code stackable=true} 允许重叠。在 {@code defaultPrepareSave/Update} 中调用
 * {@link ErpDateRangeOverlapValidator#enforceStackableAware}，冲突抛
 * {@link ErpSalErrors#ERR_SAL_PRICING_RULE_OVERLAP}。详见
 * {@code docs/design/date-ranged-validity-pattern.md} §5 STACKABLE 策略段。
 *
 * <p><b>TIMESTAMP 变体适配</b>（Phase 2 Decision (a)）：{@code ErpSalPricingRule.validFrom/validTo}
 * 为 {@code java.sql.Timestamp}（含时间分量），而 {@link IDateRange#getValidFrom()} 返回 {@link LocalDate}。
 * 因 ORM 生成基类的 {@code getValidFrom()} 为 {@code final} 返回 {@code Timestamp}，
 * 直接 {@code implements IDateRange} 会导致返回类型不兼容——故按 C3 owner doc §8.1「跨域接入」
 * 范式在 BizModel 内构造 {@link PricingRuleDateRange} 适配器，将 TIMESTAMP 截断到 {@code LocalDate}。
 */
@BizModel("ErpSalPricingRule")
public class ErpSalPricingRuleBizModel extends CrudBizModel<ErpSalPricingRule> implements IErpSalPricingRuleBiz {

    public ErpSalPricingRuleBizModel() {
        setEntityName(ErpSalPricingRule.class.getName());
    }

    @Override
    protected void defaultPrepareQuery(QueryBean query, IServiceContext context) {
        super.defaultPrepareQuery(query, context);
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpSalPricingRule> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        enforceStackableAware(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpSalPricingRule> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        enforceStackableAware(entityData.getEntity());
    }

    /**
     * STACKABLE 混合策略校验：同维度双非 stackable 重叠 → 抛异常；任一方 stackable=true 允许重叠。
     *
     * <p>维度键：ruleType + targetType + materialId + materialCategoryId + customerGroupCode + partnerId
     * （对齐 ORM 实测 + owner doc §10 follow-up 清单）。
     */
    protected void enforceStackableAware(ErpSalPricingRule entity) {
        if (entity == null) {
            return;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("ruleType", entity.getRuleType()));
        query.addFilter(eq("targetType", entity.getTargetType()));
        query.addFilter(eq("materialId", entity.getMaterialId()));
        query.addFilter(eq("materialCategoryId", entity.getMaterialCategoryId()));
        query.addFilter(eq("customerGroupCode", entity.getCustomerGroupCode()));
        query.addFilter(eq("partnerId", entity.getPartnerId()));
        List<ErpSalPricingRule> existing = dao().findAllByQuery(query);
        if (existing.isEmpty()) {
            return;
        }
        PricingRuleDateRange candidate = new PricingRuleDateRange(entity);
        List<PricingRuleDateRange> existingAdapters = new ArrayList<>(existing.size());
        for (ErpSalPricingRule r : existing) {
            existingAdapters.add(new PricingRuleDateRange(r));
        }
        ErpDateRangeOverlapValidator.enforceStackableAware(
                candidate,
                existingAdapters,
                ErpSalErrors.ERR_SAL_PRICING_RULE_OVERLAP,
                entity.getId(),
                PricingRuleDateRange::getStackable);
    }

    /**
     * IDateRange 适配器：将 {@link ErpSalPricingRule} 的 TIMESTAMP {@code validFrom/validTo} 截断到 {@link LocalDate}，
     * 同时暴露 {@code id} + {@code stackable} 字段供 {@code enforceStackableAware} 的反射 idOf / Predicate 使用。
     */
    static final class PricingRuleDateRange implements IDateRange {
        private final Long id;
        private final LocalDate from;
        private final LocalDate to;
        private final Boolean stackable;

        PricingRuleDateRange(ErpSalPricingRule rule) {
            this.id = rule.getId();
            this.from = rule.getValidFrom() != null ? rule.getValidFrom().toLocalDateTime().toLocalDate() : null;
            this.to = rule.getValidTo() != null ? rule.getValidTo().toLocalDateTime().toLocalDate() : null;
            this.stackable = rule.getStackable();
        }

        @Override
        public LocalDate getValidFrom() {
            return from;
        }

        @Override
        public LocalDate getValidTo() {
            return to;
        }

        public Long getId() {
            return id;
        }

        public Boolean getStackable() {
            return stackable;
        }
    }
}
