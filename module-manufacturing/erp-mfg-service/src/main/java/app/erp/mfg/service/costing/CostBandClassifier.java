package app.erp.mfg.service.costing;

import java.math.BigDecimal;

/**
 * E4.1 成本要素档位映射（plan 2026-08-11-0915-3 Phase 1 Decision (b)）。
 *
 * <p>Q1 (d) 冻结：成本分解要素值（material/labor/overhead/subcontract）精确值不可见，
 * 经档位映射暴露为 high/mid/low 离散值。本类实现全局固定阈值（最小可用，(a1)）。
 *
 * <p>阈值（全局，假定 CNY）：
 * <ul>
 *   <li>{@code null} — 底层值为 null</li>
 *   <li>{@code "low"} — value &lt; 100</li>
 *   <li>{@code "mid"} — 100 ≤ value &lt; 1000</li>
 *   <li>{@code "high"} — value ≥ 1000</li>
 * </ul>
 *
 * <p><b>残留风险</b>：按物料类别分位阈值（防单组件 BOM 反推）为 successor（Q1 R1，
 * 触发条件 = 代理视图脱敏强度审计 / 反推风险实证）。
 */
public final class CostBandClassifier {

    private CostBandClassifier() {
    }

    public static final String HIGH = "high";
    public static final String MID = "mid";
    public static final String LOW = "low";

    private static final BigDecimal LOW_UPPER = new BigDecimal("100");
    private static final BigDecimal MID_UPPER = new BigDecimal("1000");

    public static String classify(BigDecimal value) {
        if (value == null)
            return null;
        if (value.compareTo(LOW_UPPER) < 0)
            return LOW;
        if (value.compareTo(MID_UPPER) < 0)
            return MID;
        return HIGH;
    }
}
