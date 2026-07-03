package app.erp.ast.service.service;

import app.erp.ast.service.ErpAstConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 折旧金额计算器（depreciation-and-posting.md §1.3）。三种折旧方法策略，残值约束统一兜底
 * （折旧后净值不得低于残值，§1.4）。纯函数式静态方法，由 {@code ErpAstDepreciationScheduleBizModel} 调用。
 *
 * <ul>
 *   <li>直线法：(原值−残值)/使用年限月数，每期等额。</li>
 *   <li>双倍余额递减：2×账面净值/使用年限月数；最后 24 个月（剩余寿命≤2 年）改为直线法 (账面净值−残值)/剩余月数。</li>
 *   <li>工作量法：(原值−残值)/预计总工作量×本期工作量。</li>
 * </ul>
 */
public final class DepreciationCalculator {

    private static final int SCALE = 4;

    private DepreciationCalculator() {
    }

    public static BigDecimal calculate(String method, BigDecimal originalValue, BigDecimal residualValue,
                                       BigDecimal netBookValue, int usefulLifeMonths, int elapsedMonths,
                                       BigDecimal periodUnits, BigDecimal estimatedTotalUnits) {
        BigDecimal original = nz(originalValue);
        BigDecimal residual = nz(residualValue);
        BigDecimal nbv = netBookValue != null ? netBookValue : original;

        // 已达/低于残值 → 不再折旧
        if (nbv.compareTo(residual) <= 0) {
            return BigDecimal.ZERO;
        }
        int months = usefulLifeMonths <= 0 ? 1 : usefulLifeMonths;

        BigDecimal amount;
        switch (method) {
            case ErpAstConstants.DEPRECIATION_METHOD_DECLINING: {
                int remaining = months - Math.max(elapsedMonths, 0);
                if (remaining <= 0) {
                    remaining = 1;
                }
                // 最后 24 个月改直线法（剩余寿命≤2 年），确保残值约束
                if (remaining <= 24) {
                    amount = nbv.subtract(residual).divide(BigDecimal.valueOf(remaining), SCALE, RoundingMode.HALF_UP);
                } else {
                    amount = nbv.multiply(BigDecimal.valueOf(2))
                            .divide(BigDecimal.valueOf(months), SCALE, RoundingMode.HALF_UP);
                }
                break;
            }
            case ErpAstConstants.DEPRECIATION_METHOD_UNITS: {
                if (estimatedTotalUnits == null || estimatedTotalUnits.signum() <= 0 || periodUnits == null) {
                    return BigDecimal.ZERO;
                }
                BigDecimal rate = original.subtract(residual)
                        .divide(estimatedTotalUnits, SCALE, RoundingMode.HALF_UP);
                amount = rate.multiply(periodUnits);
                break;
            }
            case ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE:
            default: {
                amount = original.subtract(residual).divide(BigDecimal.valueOf(months), SCALE, RoundingMode.HALF_UP);
                break;
            }
        }

        // 残值约束：折旧后净值不得低于残值
        if (nbv.subtract(amount).compareTo(residual) < 0) {
            amount = nbv.subtract(residual);
        }
        return amount.signum() < 0 ? BigDecimal.ZERO : amount;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
