package app.erp.qa.service.spc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * SPC 数值统计辅助（plan 2026-07-07-0305-2 Phase 2）。
 *
 * <p>所有方法纯函数，可独立单测。返回 BigDecimal 保留计算精度，调用方按需 scale。
 */
final class Statistics {

    private Statistics() {
    }

    static BigDecimal mean(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            sum = sum.add(v);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), 10, RoundingMode.HALF_UP);
    }

    static BigDecimal range(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        BigDecimal min = values.get(0);
        BigDecimal max = values.get(0);
        for (BigDecimal v : values) {
            if (v.compareTo(min) < 0) {
                min = v;
            }
            if (v.compareTo(max) > 0) {
                max = v;
            }
        }
        return max.subtract(min);
    }

    /** 样本标准差（分母 n-1）。 */
    static BigDecimal stdDev(List<BigDecimal> values) {
        if (values == null || values.size() < 2) {
            return null;
        }
        BigDecimal mean = mean(values);
        BigDecimal sqSum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            BigDecimal diff = v.subtract(mean);
            sqSum = sqSum.add(diff.multiply(diff));
        }
        // n - 1
        return BigDecimal.valueOf(Math.sqrt(
                sqSum.divide(BigDecimal.valueOf(values.size() - 1), 20, RoundingMode.HALF_UP).doubleValue()));
    }

    /** 总体标准差（分母 n）。 */
    static BigDecimal populationStdDev(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        BigDecimal mean = mean(values);
        BigDecimal sqSum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            BigDecimal diff = v.subtract(mean);
            sqSum = sqSum.add(diff.multiply(diff));
        }
        return BigDecimal.valueOf(Math.sqrt(
                sqSum.divide(BigDecimal.valueOf(values.size()), 20, RoundingMode.HALF_UP).doubleValue()));
    }
}
