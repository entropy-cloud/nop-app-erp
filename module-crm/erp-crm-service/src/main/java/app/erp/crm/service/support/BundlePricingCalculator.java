package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmBundlePricing;
import app.erp.crm.dao.entity.ErpCrmBundlePricingLine;
import app.erp.crm.service.ErpCrmConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * CPQ 捆绑定价计算器（plan 2026-07-07-1430-2 §Phase 2）。
 *
 * <p>纯函数式：{@link #calculate(ErpCrmBundlePricing, List)} 不依赖 IoC 容器。
 *
 * <p>对齐 {@code docs/design/crm/cpq.md §3 捆绑定价}：
 * <ol>
 *   <li>单品合计 = Σ(line.unitPrice × line.quantity)。</li>
 *   <li>{@code bundleAmount} 非空 → 手工定价覆盖（直接取 bundleAmount，跳过折扣计算）。</li>
 *   <li>否则按 {@code discountType}：PERCENTAGE → subtotal × (1 - discountValue/100)；
 *       FIXED → subtotal - discountValue（不低于 0）。</li>
 * </ol>
 */
public class BundlePricingCalculator {

    /**
     * 计算捆绑包最终价格。
     *
     * @param bundle 捆绑包（可空）
     * @param lines  捆绑包明细行列表（可空）
     * @return 计算结果（{@code subtotal}/{@code finalAmount}/{@code appliedRule}）
     */
    public BundleResult calculate(ErpCrmBundlePricing bundle, List<ErpCrmBundlePricingLine> lines) {
        BigDecimal subtotal = computeSubtotal(lines);
        if (bundle == null) {
            return new BundleResult(subtotal, subtotal, "NONE");
        }
        // bundleAmount 非空 → 手工定价覆盖
        if (bundle.getBundleAmount() != null) {
            return new BundleResult(subtotal, bundle.getBundleAmount(), "BUNDLE_AMOUNT_OVERRIDE");
        }
        String discountType = bundle.getDiscountType();
        BigDecimal discountValue = bundle.getDiscountValue();
        if (discountType == null || discountValue == null || discountValue.signum() <= 0) {
            return new BundleResult(subtotal, subtotal, "NO_DISCOUNT");
        }
        BigDecimal finalAmount;
        String appliedRule;
        switch (discountType) {
            case ErpCrmConstants.BUNDLE_DISCOUNT_TYPE_PERCENTAGE:
                BigDecimal factor = BigDecimal.ONE.subtract(
                        discountValue.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                finalAmount = subtotal.multiply(factor).setScale(subtotal.scale() >= 0 ? subtotal.scale() : 2, RoundingMode.HALF_UP);
                appliedRule = "PERCENTAGE";
                break;
            case ErpCrmConstants.BUNDLE_DISCOUNT_TYPE_FIXED:
                BigDecimal after = subtotal.subtract(discountValue);
                finalAmount = after.signum() < 0 ? BigDecimal.ZERO : after;
                appliedRule = "FIXED";
                break;
            default:
                finalAmount = subtotal;
                appliedRule = "NO_DISCOUNT";
        }
        return new BundleResult(subtotal, finalAmount, appliedRule);
    }

    /**
     * Σ(line.unitPrice × line.quantity)，空行/空字段视为 0。
     */
    protected BigDecimal computeSubtotal(List<ErpCrmBundlePricingLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpCrmBundlePricingLine line : lines) {
            BigDecimal unit = line.getUnitPrice();
            BigDecimal qty = line.getQuantity();
            if (unit == null || qty == null) {
                continue;
            }
            sum = sum.add(unit.multiply(qty));
        }
        return sum;
    }

    /**
     * 捆绑计算结果。
     */
    public static class BundleResult {
        private final BigDecimal subtotal;
        private final BigDecimal finalAmount;
        private final String appliedRule;

        public BundleResult(BigDecimal subtotal, BigDecimal finalAmount, String appliedRule) {
            this.subtotal = subtotal;
            this.finalAmount = finalAmount;
            this.appliedRule = appliedRule;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public BigDecimal getFinalAmount() {
            return finalAmount;
        }

        public String getAppliedRule() {
            return appliedRule;
        }
    }
}
