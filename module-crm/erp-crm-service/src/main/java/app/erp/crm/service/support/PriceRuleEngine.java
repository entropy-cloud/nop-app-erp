package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmPriceRule;
import app.erp.crm.service.ErpCrmConstants;

import io.nop.api.core.time.CoreMetrics;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * CPQ 价格规则引擎（plan 2026-07-07-1430-2 §Phase 2）。
 *
 * <p>纯函数式：{@link #resolvePrice(Long, Long, BigDecimal, Long, LocalDate, BigDecimal, List)}
 * 输入产品/客户/数量/币种/当前日期 + 候选价格规则，返回最优价格。
 *
 * <p>对齐 {@code docs/design/crm/cpq.md §4 价格规则优先级}：
 * <ol>
 *   <li>ruleType 优先级：CUSTOMER_SPECIFIC > PROMOTIONAL > VOLUME（无匹配回退标准定价由调用方处理）。</li>
 *   <li>同 ruleType 内按 {@code priority} 数值小者优先。</li>
 *   <li>期间有效（{@code effectiveFrom <= now <= effectiveTo}，空端视为开放）。</li>
 *   <li>数量区间匹配（{@code minQuantity <= quantity <= maxQuantity}，空端视为开放）。</li>
 * </ol>
 *
 * <p>价格结果：{@code priceOverride}（覆盖单价）优先；否则基础价 ± {@code discountPercent}/{@code discountAmount}。
 */
public class PriceRuleEngine {

    /**
     * 按优先级 + 期间 + 数量区间匹配，返回最优价格结果。
     *
     * @param productId    产品 ID（必填）
     * @param customerId   客户 ID（可空，CUSTOMER_SPECIFIC 规则匹配用）
     * @param quantity     数量（可空视为 1）
     * @param currencyId   币种 ID（可空，规则 currencyId 必须匹配或规则 currencyId 为空）
     * @param now          当前日期（可空视为今天）
     * @param basePrice    标准定价（产品主数据价格），用于折扣计算
     * @param activeRules  候选价格规则（须含 isActive=true 的，调用方负责过滤）
     * @return 最优匹配；{@code matched=false} 表示无匹配（调用方回退标准定价）
     */
    public PriceResult resolvePrice(Long productId, Long customerId, BigDecimal quantity,
                                    Long currencyId, LocalDate now,
                                    BigDecimal basePrice, List<ErpCrmPriceRule> activeRules) {
        LocalDate today = now != null ? now : CoreMetrics.currentDate();
        BigDecimal qty = quantity != null ? quantity : BigDecimal.ONE;

        if (activeRules == null || activeRules.isEmpty()) {
            return PriceResult.notMatched();
        }
        List<ErpCrmPriceRule> filtered = new ArrayList<>();
        for (ErpCrmPriceRule rule : activeRules) {
            if (!ruleMatchesProduct(rule, productId)) {
                continue;
            }
            if (!ruleMatchesCustomer(rule, customerId)) {
                continue;
            }
            if (!ruleMatchesCurrency(rule, currencyId)) {
                continue;
            }
            if (!ruleMatchesPeriod(rule, today)) {
                continue;
            }
            if (!ruleMatchesQuantity(rule, qty)) {
                continue;
            }
            filtered.add(rule);
        }
        if (filtered.isEmpty()) {
            return PriceResult.notMatched();
        }
        filtered.sort(Comparator
                .comparingInt((ErpCrmPriceRule r) -> ruleTypeRank(r.getRuleType()))
                .thenComparingInt(r -> r.getPriority() == null ? Integer.MAX_VALUE : r.getPriority()));

        ErpCrmPriceRule winner = filtered.get(0);
        BigDecimal resolved = applyRule(winner, basePrice);
        return PriceResult.matched(winner, resolved);
    }

    protected boolean ruleMatchesProduct(ErpCrmPriceRule rule, Long productId) {
        // productId 为空=全局规则；非空须精确匹配
        return rule.getProductId() == null || Objects.equals(rule.getProductId(), productId);
    }

    protected boolean ruleMatchesCustomer(ErpCrmPriceRule rule, Long customerId) {
        // CUSTOMER_SPECIFIC 须 customerId 非空且匹配
        if (Objects.equals(rule.getRuleType(), ErpCrmConstants.PRICE_RULE_TYPE_CUSTOMER_SPECIFIC)) {
            return customerId != null && Objects.equals(rule.getCustomerId(), customerId);
        }
        // 其他 ruleType：规则 customerId 为空=不限客户；非空须匹配
        return rule.getCustomerId() == null || Objects.equals(rule.getCustomerId(), customerId);
    }

    protected boolean ruleMatchesCurrency(ErpCrmPriceRule rule, Long currencyId) {
        return rule.getCurrencyId() == null || Objects.equals(rule.getCurrencyId(), currencyId);
    }

    protected boolean ruleMatchesPeriod(ErpCrmPriceRule rule, LocalDate today) {
        LocalDate from = rule.getEffectiveFrom();
        LocalDate to = rule.getEffectiveTo();
        if (from != null && today.isBefore(from)) {
            return false;
        }
        return to == null || !today.isAfter(to);
    }

    protected boolean ruleMatchesQuantity(ErpCrmPriceRule rule, BigDecimal quantity) {
        BigDecimal min = rule.getMinQuantity();
        BigDecimal max = rule.getMaxQuantity();
        if (min != null && quantity.compareTo(min) < 0) {
            return false;
        }
        return max == null || quantity.compareTo(max) <= 0;
    }

    /**
     * ruleType 排序值：CUSTOMER_SPECIFIC(0) > PROMOTIONAL(1) > VOLUME(2) > 其他(MAX)。
     * 数值小者优先（最高优先级）。
     */
    protected int ruleTypeRank(String ruleType) {
        if (ruleType == null) {
            return Integer.MAX_VALUE;
        }
        switch (ruleType) {
            case ErpCrmConstants.PRICE_RULE_TYPE_CUSTOMER_SPECIFIC:
                return 0;
            case ErpCrmConstants.PRICE_RULE_TYPE_PROMOTIONAL:
                return 1;
            case ErpCrmConstants.PRICE_RULE_TYPE_VOLUME:
                return 2;
            default:
                return Integer.MAX_VALUE;
        }
    }

    /**
     * 应用规则计算最终价格：
     * <ol>
     *   <li>{@code priceOverride} 非空 → 直接覆盖。</li>
     *   <li>否则基础价 × (1 - discountPercent/100)。</li>
     *   <li>否则基础价 - discountAmount（不低于 0）。</li>
     * </ol>
     */
    protected BigDecimal applyRule(ErpCrmPriceRule rule, BigDecimal basePrice) {
        BigDecimal base = basePrice != null ? basePrice : BigDecimal.ZERO;
        if (rule.getPriceOverride() != null) {
            return rule.getPriceOverride();
        }
        if (rule.getDiscountPercent() != null) {
            BigDecimal factor = BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(rule.getDiscountPercent()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            return base.multiply(factor).setScale(base.scale() >= 0 ? base.scale() : 2, RoundingMode.HALF_UP);
        }
        if (rule.getDiscountAmount() != null) {
            BigDecimal after = base.subtract(rule.getDiscountAmount());
            return after.signum() < 0 ? BigDecimal.ZERO : after;
        }
        return base;
    }

    /**
     * 价格计算结果。
     */
    public static class PriceResult {
        private final boolean matched;
        private final ErpCrmPriceRule matchedRule;
        private final BigDecimal finalPrice;

        private PriceResult(boolean matched, ErpCrmPriceRule matchedRule, BigDecimal finalPrice) {
            this.matched = matched;
            this.matchedRule = matchedRule;
            this.finalPrice = finalPrice;
        }

        public static PriceResult notMatched() {
            return new PriceResult(false, null, null);
        }

        public static PriceResult matched(ErpCrmPriceRule rule, BigDecimal price) {
            return new PriceResult(true, rule, price);
        }

        public boolean isMatched() {
            return matched;
        }

        public ErpCrmPriceRule getMatchedRule() {
            return matchedRule;
        }

        public BigDecimal getFinalPrice() {
            return finalPrice;
        }
    }
}
