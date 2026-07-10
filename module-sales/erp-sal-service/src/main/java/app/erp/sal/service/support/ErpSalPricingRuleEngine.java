package app.erp.sal.service.support;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalPricingRule;

import io.nop.api.core.time.CoreMetrics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 销售促销规则引擎（UC-SAL-11 定价引擎促销层）。
 *
 * <p>纯函数式：{@link #evaluate(ErpSalOrder, List, String, List)} 输入订单头 + 订单行 +
 * 客户上下文 + 候选规则，返回修改后的订单行折扣/赠品行快照 + 应用的规则列表。
 *
 * <p>对齐 {@code docs/design/sales/use-cases.md UC-SAL-11}：
 * <ul>
 *   <li>ruleType：PERCENT_DISCOUNT（行级百分比折扣）/ AMOUNT_OFF（头级满减）/ GIFT（买赠送赠品行）/ PRICE_OVERRIDE（行级价格覆盖）。</li>
 *   <li>targetType：LINE 逐行匹配，ORDER 头级匹配。</li>
 *   <li>stackable=false 的规则命中后跳过同类型后续规则；stackable=true 可叠加。</li>
 *   <li>优先级 priority 数值小者优先；同优先级取首条（不冲突报错由调用方裁决）。</li>
 *   <li>期间有效（validFrom/validTo，空端开放）。</li>
 * </ul>
 *
 * <p>参考 CRM {@code PriceRuleEngine} 的优先级 + 期间 + 数量区间评估模式。
 */
public class ErpSalPricingRuleEngine {

    public static final String RULE_TYPE_PERCENT_DISCOUNT = "PERCENT_DISCOUNT";
    public static final String RULE_TYPE_AMOUNT_OFF = "AMOUNT_OFF";
    public static final String RULE_TYPE_GIFT = "GIFT";
    public static final String RULE_TYPE_PRICE_OVERRIDE = "PRICE_OVERRIDE";

    public static final String TARGET_TYPE_LINE = "LINE";
    public static final String TARGET_TYPE_ORDER = "ORDER";

    public static final String PRICING_SOURCE_PROMOTION = "PROMOTION";

    /**
     * 评估促销规则，返回结果快照（不修改输入实体的原始状态，调用方负责持久化）。
     *
     * @param order            订单头
     * @param lines            订单行列表
     * @param customerGroupCode 客户组标签（从 partner.customerGroup 解析）
     * @param activeRules      候选规则（须含 isActive=true 的，调用方负责过滤）
     * @return 评估结果（修改后的行 + 应用的规则列表 + 头级折扣金额）
     */
    public EvaluationResult evaluate(ErpSalOrder order, List<ErpSalOrderLine> lines,
                                     String customerGroupCode, List<ErpSalPricingRule> activeRules) {
        EvaluationResult result = new EvaluationResult();
        result.setModifiedLines(new ArrayList<>(lines));
        result.setAppliedRules(new ArrayList<>());
        result.setOrderDiscountAmount(BigDecimal.ZERO);

        if (activeRules == null || activeRules.isEmpty() || lines.isEmpty()) {
            return result;
        }

        LocalDateTime now = CoreMetrics.currentDateTime();
        List<ErpSalPricingRule> matched = filterAndSortRules(activeRules, order, customerGroupCode, now);
        if (matched.isEmpty()) {
            return result;
        }

        // 按 targetType 分组处理
        for (ErpSalPricingRule rule : matched) {
            if (TARGET_TYPE_LINE.equals(rule.getTargetType())) {
                applyLineRule(rule, result, now);
            } else if (TARGET_TYPE_ORDER.equals(rule.getTargetType())) {
                applyOrderRule(rule, order, result);
            }
            result.getAppliedRules().add(rule);
            if (!Boolean.TRUE.equals(rule.getStackable())) {
                break;
            }
        }

        return result;
    }

    /**
     * 过滤匹配订单客户/客户组的规则 + 期间有效 + 按 priority 排序。
     */
    protected List<ErpSalPricingRule> filterAndSortRules(List<ErpSalPricingRule> rules,
                                                         ErpSalOrder order,
                                                         String customerGroupCode,
                                                         LocalDateTime now) {
        List<ErpSalPricingRule> filtered = new ArrayList<>();
        for (ErpSalPricingRule rule : rules) {
            if (!Boolean.TRUE.equals(rule.getIsActive())) {
                continue;
            }
            if (!ruleMatchesPeriod(rule, now)) {
                continue;
            }
            if (!ruleMatchesCustomer(rule, order.getCustomerId(), customerGroupCode)) {
                continue;
            }
            if (!ruleMatchesCurrency(rule, order.getCurrencyId())) {
                continue;
            }
            filtered.add(rule);
        }
        filtered.sort(Comparator.comparingInt(
                r -> r.getPriority() == null ? Integer.MAX_VALUE : r.getPriority()));
        return filtered;
    }

    protected boolean ruleMatchesPeriod(ErpSalPricingRule rule, LocalDateTime now) {
        LocalDateTime from = toLocalDateTime(rule.getValidFrom());
        LocalDateTime to = toLocalDateTime(rule.getValidTo());
        if (from != null && now.isBefore(from)) {
            return false;
        }
        return to == null || !now.isAfter(to);
    }

    protected LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    protected boolean ruleMatchesCustomer(ErpSalPricingRule rule, Long customerId, String customerGroupCode) {
        if (rule.getPartnerId() != null) {
            return Objects.equals(rule.getPartnerId(), customerId);
        }
        if (rule.getCustomerGroupCode() != null && !rule.getCustomerGroupCode().isEmpty()) {
            return Objects.equals(rule.getCustomerGroupCode(), customerGroupCode);
        }
        return true;
    }

    protected boolean ruleMatchesCurrency(ErpSalPricingRule rule, Long currencyId) {
        return rule.getCurrencyId() == null || Objects.equals(rule.getCurrencyId(), currencyId);
    }

    /**
     * 行级规则：PERCENT_DISCOUNT / PRICE_OVERRIDE / GIFT。
     */
    protected void applyLineRule(ErpSalPricingRule rule, EvaluationResult result, LocalDateTime now) {
        ErpSalOrderLine referenceLine = null;
        for (ErpSalOrderLine line : result.getModifiedLines()) {
            if (!lineMatchesRuleTarget(rule, line)) {
                continue;
            }
            referenceLine = line;
            String ruleType = rule.getRuleType();
            if (RULE_TYPE_PERCENT_DISCOUNT.equals(ruleType)) {
                applyPercentDiscount(rule, line);
            } else if (RULE_TYPE_PRICE_OVERRIDE.equals(ruleType)) {
                applyPriceOverride(rule, line);
            }
        }
        if (RULE_TYPE_GIFT.equals(rule.getRuleType()) && rule.getGiftMaterialId() != null) {
            addGiftLine(rule, result, referenceLine);
        }
    }

    protected boolean lineMatchesRuleTarget(ErpSalPricingRule rule, ErpSalOrderLine line) {
        if (rule.getMaterialId() != null) {
            return Objects.equals(rule.getMaterialId(), line.getMaterialId());
        }
        return true;
    }

    protected void applyPercentDiscount(ErpSalPricingRule rule, ErpSalOrderLine line) {
        BigDecimal percent = rule.getDiscountPercent();
        if (percent == null) {
            return;
        }
        line.setDiscountRate(percent);
        BigDecimal unitPrice = nullSafe(line.getUnitPrice());
        BigDecimal qty = nullSafe(line.getQuantity());
        BigDecimal gross = unitPrice.multiply(qty);
        BigDecimal discountAmt = gross.multiply(percent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        line.setDiscountAmount(discountAmt);
        line.setPricingSource(PRICING_SOURCE_PROMOTION);
    }

    protected void applyPriceOverride(ErpSalPricingRule rule, ErpSalOrderLine line) {
        BigDecimal override = rule.getPriceOverride();
        if (override == null) {
            return;
        }
        BigDecimal oldPrice = nullSafe(line.getUnitPrice());
        BigDecimal qty = nullSafe(line.getQuantity());
        BigDecimal gross = oldPrice.multiply(qty);
        BigDecimal newGross = override.multiply(qty);
        BigDecimal discountAmt = gross.subtract(newGross).max(BigDecimal.ZERO);
        line.setUnitPrice(override);
        line.setDiscountAmount(discountAmt);
        line.setPricingSource(PRICING_SOURCE_PROMOTION);
    }

    protected void addGiftLine(ErpSalPricingRule rule, EvaluationResult result, ErpSalOrderLine referenceLine) {
        ErpSalOrderLine giftLine = new ErpSalOrderLine();
        giftLine.setMaterialId(rule.getGiftMaterialId());
        giftLine.setSkuId(rule.getGiftSkuId());
        giftLine.setUoMId(referenceLine != null ? referenceLine.getUoMId() : null);
        giftLine.setUnitPrice(BigDecimal.ZERO);
        giftLine.setQuantity(rule.getGiftQuantity() != null ? rule.getGiftQuantity() : BigDecimal.ONE);
        giftLine.setAmount(BigDecimal.ZERO);
        giftLine.setPricingSource(PRICING_SOURCE_PROMOTION);
        giftLine.setRemark("赠品行");
        result.getModifiedLines().add(giftLine);
        result.getGiftRuleIds().add(rule.getId());
    }

    /**
     * 头级规则：AMOUNT_OFF（满减）。
     */
    protected void applyOrderRule(ErpSalPricingRule rule, ErpSalOrder order, EvaluationResult result) {
        if (!RULE_TYPE_AMOUNT_OFF.equals(rule.getRuleType())) {
            return;
        }
        BigDecimal orderAmount = nullSafe(order.getTotalAmount());
        BigDecimal threshold = rule.getMinOrderAmount();
        if (threshold != null && orderAmount.compareTo(threshold) < 0) {
            return;
        }
        BigDecimal discountAmt = nullSafe(rule.getDiscountAmount());
        BigDecimal current = nullSafe(result.getOrderDiscountAmount());
        result.setOrderDiscountAmount(current.add(discountAmt));
    }

    protected BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 评估结果快照。
     */
    public static class EvaluationResult {
        private List<ErpSalOrderLine> modifiedLines;
        private List<ErpSalPricingRule> appliedRules;
        private BigDecimal orderDiscountAmount;
        private final List<Long> giftRuleIds = new ArrayList<>();

        public List<ErpSalOrderLine> getModifiedLines() {
            return modifiedLines;
        }

        public void setModifiedLines(List<ErpSalOrderLine> modifiedLines) {
            this.modifiedLines = modifiedLines;
        }

        public List<ErpSalPricingRule> getAppliedRules() {
            return appliedRules;
        }

        public void setAppliedRules(List<ErpSalPricingRule> appliedRules) {
            this.appliedRules = appliedRules;
        }

        public BigDecimal getOrderDiscountAmount() {
            return orderDiscountAmount;
        }

        public void setOrderDiscountAmount(BigDecimal orderDiscountAmount) {
            this.orderDiscountAmount = orderDiscountAmount;
        }

        public List<Long> getGiftRuleIds() {
            return giftRuleIds;
        }
    }
}
