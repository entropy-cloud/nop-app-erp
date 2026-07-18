package app.erp.qa.service.spc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * SPC 计数型控制图（P/NP/C/U）控制限公式（plan 2026-07-19-0120-2 Phase 2）。
 *
 * <p>四静态方法对应四种行业标准公式，均返回 {@link Result}（CL/UCL/LCL 三元组）：
 * <ul>
 *   <li>{@link #calcP}：P 图（不合格品率），CL=p̄=Σdᵢ/Σnᵢ，UCL/LCL=p̄±3√(p̄(1-p̄)/n̄)；</li>
 *   <li>{@link #calcNp}：NP 图（不合格品数），CL=n·p̄，UCL/LCL=n·p̄±3√(n·p̄(1-p̄))；</li>
 *   <li>{@link #calcC}：C 图（缺陷数），CL=c̄=Σcᵢ/k，UCL/LCL=c̄±3√c̄；</li>
 *   <li>{@link #calcU}：U 图（单位缺陷数），CL=ū=Σcᵢ/Σnᵢ，UCL/LCL=ū±3√(ū/n̄)。</li>
 * </ul>
 *
 * <p>所有计算使用 BigDecimal HALF_UP scale=6 对齐 {@link SpcControlLimitCalculator} 范式；
 * 负数下限（CL−3σ &lt; 0）钳到 0（行业标准，缺陷率/数不可能为负）。
 */
public final class AttributesControlLimitFormulas {

    private static final BigDecimal THREE = new BigDecimal("3");
    private static final BigDecimal SIX = new BigDecimal("6");
    private static final int SCALE = 6;

    private AttributesControlLimitFormulas() {
    }

    /** P 图（不合格品率图）控制限：CL=p̄=Σdᵢ/Σnᵢ，σ̂=√(p̄(1-p̄)/n̄)。 */
    public static Result calcP(List<Integer> defects, List<Integer> inspected) {
        validateSameSize(defects, inspected, "P");
        if (defects.isEmpty()) {
            return new Result(null, null, null);
        }
        BigDecimal sumDefects = BigDecimal.ZERO;
        BigDecimal sumInspected = BigDecimal.ZERO;
        for (int i = 0; i < defects.size(); i++) {
            sumDefects = sumDefects.add(BigDecimal.valueOf(defects.get(i)));
            sumInspected = sumInspected.add(BigDecimal.valueOf(inspected.get(i)));
        }
        if (sumInspected.signum() == 0) {
            return new Result(null, null, null);
        }
        BigDecimal pBar = sumDefects.divide(sumInspected, 10, RoundingMode.HALF_UP);
        BigDecimal avgInspected = sumInspected.divide(BigDecimal.valueOf(inspected.size()), 10, RoundingMode.HALF_UP);
        BigDecimal sigma = sqrt(pBar.multiply(BigDecimal.ONE.subtract(pBar)).divide(avgInspected, 10, RoundingMode.HALF_UP));
        return buildResult(pBar, sigma);
    }

    /** NP 图（不合格品数图）控制限：CL=n·p̄，σ̂=√(n·p̄(1-p̄))，n 取平均 inspected。 */
    public static Result calcNp(List<Integer> defects, List<Integer> inspected) {
        validateSameSize(defects, inspected, "NP");
        if (defects.isEmpty()) {
            return new Result(null, null, null);
        }
        BigDecimal sumDefects = BigDecimal.ZERO;
        BigDecimal sumInspected = BigDecimal.ZERO;
        for (int i = 0; i < defects.size(); i++) {
            sumDefects = sumDefects.add(BigDecimal.valueOf(defects.get(i)));
            sumInspected = sumInspected.add(BigDecimal.valueOf(inspected.get(i)));
        }
        if (sumInspected.signum() == 0) {
            return new Result(null, null, null);
        }
        BigDecimal pBar = sumDefects.divide(sumInspected, 10, RoundingMode.HALF_UP);
        BigDecimal avgInspected = sumInspected.divide(BigDecimal.valueOf(inspected.size()), 10, RoundingMode.HALF_UP);
        BigDecimal cl = pBar.multiply(avgInspected);
        BigDecimal sigma = sqrt(avgInspected.multiply(pBar).multiply(BigDecimal.ONE.subtract(pBar)));
        return buildResult(cl, sigma);
    }

    /** C 图（缺陷数图）控制限：CL=c̄=Σcᵢ/k，σ̂=√c̄。 */
    public static Result calcC(List<Integer> defects) {
        if (defects.isEmpty()) {
            return new Result(null, null, null);
        }
        BigDecimal sumDefects = BigDecimal.ZERO;
        for (Integer d : defects) {
            sumDefects = sumDefects.add(BigDecimal.valueOf(d));
        }
        BigDecimal cBar = sumDefects.divide(BigDecimal.valueOf(defects.size()), 10, RoundingMode.HALF_UP);
        BigDecimal sigma = sqrt(cBar);
        return buildResult(cBar, sigma);
    }

    /** U 图（单位缺陷数图）控制限：CL=ū=Σcᵢ/Σnᵢ，σ̂=√(ū/n̄)，n̄=平均 inspected。 */
    public static Result calcU(List<Integer> defects, List<Integer> inspected) {
        validateSameSize(defects, inspected, "U");
        if (defects.isEmpty()) {
            return new Result(null, null, null);
        }
        BigDecimal sumDefects = BigDecimal.ZERO;
        BigDecimal sumInspected = BigDecimal.ZERO;
        for (int i = 0; i < defects.size(); i++) {
            sumDefects = sumDefects.add(BigDecimal.valueOf(defects.get(i)));
            sumInspected = sumInspected.add(BigDecimal.valueOf(inspected.get(i)));
        }
        if (sumInspected.signum() == 0) {
            return new Result(null, null, null);
        }
        BigDecimal uBar = sumDefects.divide(sumInspected, 10, RoundingMode.HALF_UP);
        BigDecimal avgInspected = sumInspected.divide(BigDecimal.valueOf(inspected.size()), 10, RoundingMode.HALF_UP);
        BigDecimal sigma = sqrt(uBar.divide(avgInspected, 10, RoundingMode.HALF_UP));
        return buildResult(uBar, sigma);
    }

    private static Result buildResult(BigDecimal cl, BigDecimal sigma) {
        if (cl == null) {
            return new Result(null, null, null);
        }
        BigDecimal scaledCl = scale(cl);
        if (sigma == null) {
            return new Result(scaledCl, null, null);
        }
        BigDecimal threeSigma = sigma.multiply(THREE);
        BigDecimal ucl = scale(cl.add(threeSigma));
        BigDecimal lclRaw = cl.subtract(threeSigma);
        // 负数下限钳到 0（缺陷率/数下界 >= 0）
        BigDecimal lcl = lclRaw.signum() < 0 ? BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP) : scale(lclRaw);
        return new Result(scaledCl, ucl, lcl);
    }

    private static void validateSameSize(List<Integer> defects, List<Integer> inspected, String chartType) {
        if (defects == null || inspected == null) {
            throw new IllegalArgumentException(chartType + " chart: defects/inspected lists must not be null");
        }
        if (defects.size() != inspected.size()) {
            throw new IllegalArgumentException(chartType + " chart: defects/inspected lists must have same size, got "
                    + defects.size() + " vs " + inspected.size());
        }
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal sqrt(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.sqrt(value.doubleValue()));
    }

    /** CL/UCL/LCL 三元组结果。任一字段可为 null（空数据/零方差）。 */
    public static final class Result {
        private final BigDecimal cl;
        private final BigDecimal ucl;
        private final BigDecimal lcl;

        public Result(BigDecimal cl, BigDecimal ucl, BigDecimal lcl) {
            this.cl = cl;
            this.ucl = ucl;
            this.lcl = lcl;
        }

        public BigDecimal getCl() {
            return cl;
        }

        public BigDecimal getUcl() {
            return ucl;
        }

        public BigDecimal getLcl() {
            return lcl;
        }
    }
}
