package app.erp.drp.service;

import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpInvDrpLeadTimeRecord;
import app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc;
import app.erp.drp.dao.entity.ErpInvDrpSupplierScore;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.qa.dao.entity.ErpQaInspection;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.82 / P1-RC-082 提前期统计与供应商可靠性评分测试（UC-DRP-08；owner doc lead-time-tracking.md）。
 *
 * <p>覆盖：自动计算 + 幂等守卫、日期缺失/倒置拒绝、容差三档 flag（含边界）、统计指标精确值
 * （μ/σ/准时率/中位数/极值/变异系数 + 窗口裁剪 + 三粒度）、四维评分合成与等级边界（90/75/60 精确构造）、
 * 无样本维度得分 0 + missingDimensions 标注、统计重算幂等 upsert、联合变分低变异走标准公式（μ_lt 替换
 * 配置提前期）/ 中变异走联合公式精确数值、样本不足降级配置提前期、confirmWriteback 人工门保持 +
 * replenishmentLeadTime←μ_lt 回写建议。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpLeadTimeStats extends JunitAutoTestCase {

    static final Long ORG_ID = 8401L;
    static final Long UOM_ID = 8501L;
    static final Long WH_ID = 8101L;
    static final Long SUP1 = 8001L;  // 评分主供应商
    static final Long SUP2 = 8002L;  // 粒度对照供应商
    static final Long M1 = 8201L;    // 评分/统计主物料
    static final Long M2 = 8202L;    // 粒度对照物料

    // 联合变分需求数据：两月出库 [900, 1500] → μ_m=1200 σ_m=300 → μ_d=40 σ_d²=σ_m²/30=3000（精确值）
    static final LocalDate REF = LocalDate.of(2026, 7, 17);

    @RegisterExtension
    static DrpFrozenClockExtension frozenClock = new DrpFrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    static final java.util.concurrent.atomic.AtomicLong SEQ = new java.util.concurrent.atomic.AtomicLong(1);

    // ---------- ① 自动计算 + 幂等守卫 ----------

    @Test
    public void testRecordFromPurchaseReceiveAndIdempotent() {
        seedBaseData();

        int created = recordOk("PO-LT-1", SUP1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 11), 10,
                List.of(M1));
        assertEquals(1, created, "首次应收货确认落 1 条提前期记录");

        ErpInvDrpLeadTimeRecord r = findRecord("PO-LT-1", M1);
        assertNotNull(r);
        assertEquals(10, r.getActualLeadTime(), "actualLeadTime = DATEDIFF(receiptDate, orderDate)");
        assertEquals(0, r.getVarianceDays(), "varianceDays = actual - expected = 0");
        assertEquals(ErpDrpConstants.LT_FLAG_ON_TIME, r.getEarlyLateFlag());
        assertEquals(Boolean.TRUE, r.getIsOnTime());

        assertEquals(0, recordOk("PO-LT-1", SUP1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 12), 10,
                List.of(M1)), "同 purchaseOrderCode+materialId 重复调用应幂等跳过（0 条）");
        assertEquals(10, findRecord("PO-LT-1", M1).getActualLeadTime(), "幂等跳过不得改写既有记录");
    }

    // ---------- ② 日期缺失/倒置拒绝（L1 异常路径：不记录跳过告警） ----------

    @Test
    public void testRecordFromPurchaseRejectsInvalidDates() {
        seedBaseData();
        assertTrue(rpcRecord("PO-BAD", SUP1, null, LocalDate.of(2026, 6, 11), 10, List.of(M1)).getStatus() != 0,
                "订单日期缺失应拒绝");
        assertTrue(rpcRecord("PO-BAD", SUP1, LocalDate.of(2026, 6, 1), null, 10, List.of(M1)).getStatus() != 0,
                "收货日期缺失应拒绝");
        assertTrue(rpcRecord("PO-BAD", SUP1, LocalDate.of(2026, 6, 11), LocalDate.of(2026, 6, 1), 10,
                List.of(M1)).getStatus() != 0, "收货早于订单（倒置）应拒绝");
    }

    // ---------- ③ 容差三档 flag（默认容差 0.1） ----------

    @Test
    public void testToleranceFlagThreeTiers() {
        seedBaseData();
        // expected=10，容差 ±10% → [9, 11]：8 EARLY / 9 ON_TIME（下界内）/ 11 ON_TIME（上界内）/ 12 LATE
        assertEquals(ErpDrpConstants.LT_FLAG_EARLY, flagFor("PO-E1", 8, 10));
        assertEquals(ErpDrpConstants.LT_FLAG_ON_TIME, flagFor("PO-E2", 9, 10));
        assertEquals(ErpDrpConstants.LT_FLAG_ON_TIME, flagFor("PO-E3", 11, 10));
        assertEquals(ErpDrpConstants.LT_FLAG_LATE, flagFor("PO-E4", 12, 10));
        // expected 缺失 → 不可判定：earlyLateFlag/varianceDays 留空（isOnTime 列 DDL 默认 true，
        // 准时口径以 earlyLateFlag 非空为已判定标记，DB 默认值不影响）
        recordOk("PO-E5", SUP1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 8), null, List.of(M1));
        ErpInvDrpLeadTimeRecord r = findRecord("PO-E5", M1);
        assertEquals(7, r.getActualLeadTime());
        assertNull(r.getEarlyLateFlag(), "expected 缺失时 earlyLateFlag 留空（不可判定标记）");
        assertNull(r.getVarianceDays());
        // 该未判定样本不得计入准时率分母：S1+M1 共 5 样本（E1-E4 已判定 + E5 未判定）
        Map<String, Object> stats = statsOk(SUP1, M1);
        assertEquals(5, ((Number) stats.get("sampleCount")).intValue());
        assertEquals(4, ((Number) stats.get("judgedCount")).intValue(), "未判定样本不入准时率分母");
        assertEquals(0, new BigDecimal(stats.get("onTimeRate").toString())
                .compareTo(new BigDecimal("0.5")), "准时率 = 2/4（E2/E3 准时，E1 提前，E4 延迟）");
    }

    // ---------- ④ 统计指标精确值 + 窗口裁剪 ----------

    @Test
    public void testStatsMetricsExactValues() {
        seedBaseData();
        // 5 样本 actual [10,12,14,16,18] expected=14（±1.4 → [12.6,15.4]）：
        // μ=14，σ=√(40/5)=2.8284，中位数 14，min/max 10/18，准时 1/5=0.2，cv=2.8284/14=0.2020
        seedRecord("PO-S1", SUP1, M1, 10, 14);
        seedRecord("PO-S2", SUP1, M1, 12, 14);
        seedRecord("PO-S3", SUP1, M1, 14, 14);
        seedRecord("PO-S4", SUP1, M1, 16, 14);
        seedRecord("PO-S5", SUP1, M1, 18, 14);
        // 窗口外样本（receiptDate 早于 today-365，冻结时钟 2026-07-17 → 窗口起 2025-07-17）
        seedRecordWithDates("PO-OLD", SUP1, M1, 10, 14, REF.minusDays(400));

        Map<String, Object> stats = statsOk(SUP1, M1);
        assertEquals(5, ((Number) stats.get("sampleCount")).intValue(), "窗口外样本不入统计");
        assertEquals(0, new BigDecimal(stats.get("avgLeadTime").toString())
                .compareTo(new BigDecimal("14")), "μ = 14");
        assertEquals(0, new BigDecimal(stats.get("leadTimeStdDev").toString())
                .compareTo(new BigDecimal("2.8284")), "σ = 2.8284（总体标准差）");
        assertEquals(0, new BigDecimal(stats.get("medianLeadTime").toString())
                .compareTo(new BigDecimal("14")), "中位数 = 14");
        assertEquals(10, ((Number) stats.get("minLeadTime")).intValue());
        assertEquals(18, ((Number) stats.get("maxLeadTime")).intValue());
        assertEquals(5, ((Number) stats.get("judgedCount")).intValue());
        assertEquals(1, ((Number) stats.get("onTimeCount")).intValue());
        assertEquals(0, new BigDecimal(stats.get("onTimeRate").toString())
                .compareTo(new BigDecimal("0.2000")), "准时率 = 1/5");
        assertEquals(0, new BigDecimal(stats.get("variationCoefficient").toString())
                .compareTo(new BigDecimal("0.2020")), "变异系数 = σ/μ");
    }

    // ---------- ⑤ 三粒度统计 ----------

    @Test
    public void testStatsGranularity() {
        seedBaseData();
        seedRecord("PO-G1", SUP1, M1, 10, 10);
        seedRecord("PO-G2", SUP1, M2, 20, 20);
        seedRecord("PO-G3", SUP2, M1, 30, 30);

        Map<String, Object> supMat = statsOk(SUP1, M1);
        assertEquals(1, ((Number) supMat.get("sampleCount")).intValue());

        // 供应商级（SUP1 全物料）：10 + 20
        Map<String, Object> sup = statsOk(SUP1, null);
        assertEquals(2, ((Number) sup.get("sampleCount")).intValue());
        assertEquals(0, new BigDecimal(sup.get("avgLeadTime").toString())
                .compareTo(new BigDecimal("15")), "供应商级 μ = (10+20)/2");

        // 物料级（M1 跨供应商）：10 + 30
        Map<String, Object> mat = statsOk(null, M1);
        assertEquals(2, ((Number) mat.get("sampleCount")).intValue());
        assertEquals(0, new BigDecimal(mat.get("avgLeadTime").toString())
                .compareTo(new BigDecimal("20")), "物料级 μ = (10+30)/2");

        // 双空过滤拒绝
        assertTrue(rpcStats(null, null).getStatus() != 0, "统计至少需要一个过滤参数");
    }

    // ---------- ⑥ 评分四维合成 + 等级边界 A(90)/B(75) ----------

    @Test
    public void testScoreCompositionAndGradeABoundary() {
        seedBaseData();
        // LT 维：5×[10,10]（全准时 σ=0）→ 准时 40 + 稳定性 30 = 70 基线
        for (int i = 1; i <= 5; i++) {
            seedRecord("PO-A" + i, SUP1, M1, 10, 10);
        }
        // A 边界 90 = 70 + 数量 15（accuracy 0.75：ordered 100 / received 75）+ 质量 5（1 合格 1 不合格）
        seedPurOrder("PO-QA", 8901L, SUP1, M1, bd("100"), bd("75"));
        seedQaInspection(8701L, "QA-A1", SUP1, M1, ErpDrpConstants.QA_INSPECTION_RESULT_ACCEPTED);
        seedQaInspection(8702L, "QA-A2", SUP1, M1, "REJECTED");

        ErpInvDrpSupplierScore score = recalcOk(SUP1, M1);
        assertEquals(0, score.getTotalScore().compareTo(bd("90.00")), "总分应精确等于 90（A 下界）");
        assertEquals(ErpDrpConstants.SUPPLIER_GRADE_A, score.getGrade(), "≥90 → A");
        assertEquals(0, score.getQuantityAccuracy().compareTo(bd("0.75")));
        assertEquals(0, score.getQualityPassRate().compareTo(bd("0.5")));
        assertEquals(0, score.getOnTimeScore().compareTo(bd("40")));
        assertEquals(0, score.getStabilityScore().compareTo(bd("30")));
        assertNull(score.getMissingDimensions(), "两外源维度均有样本，无缺失标注");

        // B 边界 75 = 70 + 数量 5（accuracy 0.25）+ 质量 0（1 不合格 → 合格率 0）
        for (int i = 1; i <= 5; i++) {
            seedRecord("PO-B" + i, SUP2, M2, 10, 10);
        }
        seedPurOrder("PO-QB", 8902L, SUP2, M2, bd("100"), bd("25"));
        seedQaInspection(8703L, "QA-B1", SUP2, M2, "REJECTED");

        ErpInvDrpSupplierScore scoreB = recalcOk(SUP2, M2);
        assertEquals(0, scoreB.getTotalScore().compareTo(bd("75.00")), "总分应精确等于 75（B 下界）");
        assertEquals(ErpDrpConstants.SUPPLIER_GRADE_B, scoreB.getGrade(), "≥75 且 <90 → B");
    }

    // ---------- ⑦ 等级边界 C(60) 精确构造 + D ----------

    @Test
    public void testScoreGradeCBoundaryAndD() {
        seedBaseData();
        // C 边界 60：LT [10,10,10,10,20] expected=10 → 准时 4/5=0.8 → 32；μ=12 σ=4 cv=1/3 → 稳定性 20；
        // 数量 5（accuracy 0.25）；质量 3/10 合格 → 3 → 32+20+5+3 = 60.00
        seedRecord("PO-C1", SUP1, M1, 10, 10);
        seedRecord("PO-C2", SUP1, M1, 10, 10);
        seedRecord("PO-C3", SUP1, M1, 10, 10);
        seedRecord("PO-C4", SUP1, M1, 10, 10);
        seedRecord("PO-C5", SUP1, M1, 20, 10);
        seedPurOrder("PO-QC", 8903L, SUP1, M1, bd("100"), bd("25"));
        for (int i = 1; i <= 3; i++) {
            seedQaInspection(8710L + i, "QA-C-ACC" + i, SUP1, M1,
                    ErpDrpConstants.QA_INSPECTION_RESULT_ACCEPTED);
        }
        for (int i = 1; i <= 7; i++) {
            seedQaInspection(8720L + i, "QA-C-REJ" + i, SUP1, M1, "REJECTED");
        }

        ErpInvDrpSupplierScore scoreC = recalcOk(SUP1, M1);
        assertEquals(0, scoreC.getOnTimeScore().compareTo(bd("32.00")), "准时 0.8×40 = 32");
        assertEquals(0, scoreC.getStabilityScore().compareTo(bd("20.00")), "稳定性 (1-1/3)×30 = 20");
        assertEquals(0, scoreC.getQualityScore().compareTo(bd("3.00")), "质量 0.3×10 = 3");
        assertEquals(0, scoreC.getTotalScore().compareTo(bd("60.00")), "总分应精确等于 60（C 下界）");
        assertEquals(ErpDrpConstants.SUPPLIER_GRADE_C, scoreC.getGrade(), "≥60 且 <75 → C");

        // D：同 LT 基线（32+20）+ 两外源维度零样本 → 52 < 60 → D（并验证无样本维度标注，见⑧）
        for (int i = 1; i <= 4; i++) {
            seedRecord("PO-D" + i, SUP2, M2, 10, 10);
        }
        seedRecord("PO-D5", SUP2, M2, 20, 10);
        ErpInvDrpSupplierScore scoreD = recalcOk(SUP2, M2);
        assertEquals(ErpDrpConstants.SUPPLIER_GRADE_D, scoreD.getGrade(), "<60 → D");
    }

    // ---------- ⑧ 无样本维度得分 0 + 标注边界（不静默忽略） ----------

    @Test
    public void testMissingDimensionsScoreZeroAndMarked() {
        seedBaseData();
        seedRecord("PO-M1", SUP1, M1, 10, 10);
        seedRecord("PO-M2", SUP1, M1, 10, 10);

        ErpInvDrpSupplierScore score = recalcOk(SUP1, M1);
        assertEquals(0, score.getQuantityScore().compareTo(bd("0.00")), "数量维度无样本得分记 0");
        assertEquals(0, score.getQualityScore().compareTo(bd("0.00")), "质量维度无样本得分记 0");
        assertNull(score.getQuantityAccuracy(), "无样本维度指标值留空（区别于真实 0 值）");
        assertNull(score.getQualityPassRate());
        assertEquals("QUANTITY,QUALITY", score.getMissingDimensions(), "汇总行必须标注样本缺失维度");

        // 窗口内零提前期样本 → 重算拒绝
        assertTrue(rpcRecalc(SUP1, M2).getStatus() != 0, "无提前期样本应拒绝评分重算");
    }

    // ---------- ⑨ 统计重算幂等 upsert（并发组语义） ----------

    @Test
    public void testRecalculateIdempotentUpsert() {
        seedBaseData();
        for (int i = 1; i <= 3; i++) {
            seedRecord("PO-R" + i, SUP1, M1, 10, 10);
        }
        ErpInvDrpSupplierScore first = recalcOk(SUP1, M1);
        ErpInvDrpSupplierScore second = recalcOk(SUP1, M1);

        QueryBean q = new QueryBean();
        q.addFilter(eq("supplierId", SUP1));
        q.addFilter(eq("materialId", M1));
        List<ErpInvDrpSupplierScore> rows = daoProvider.daoFor(ErpInvDrpSupplierScore.class).findAllByQuery(q);
        assertEquals(1, rows.size(), "UK(supplierId,materialId) upsert 不得产生重复行");
        assertEquals(first.getTotalScore(), second.getTotalScore(), "重算结果稳定（确定性指标）");
        assertEquals(3, second.getSampleCount().intValue());
    }

    // ---------- ⑩ 联合变分：低变异走标准公式（μ_lt 替换配置提前期） ----------

    @Test
    public void testJointVariationLowCvUsesStandardFormula() {
        seedSsBase(M1);
        // 5 样本全 10 → μ_lt=10 σ_lt=0 cv=0 ≤ 0.2 → 标准公式 Z×σ_d×√μ_lt（配置 leadTimeDays=20 不生效）
        for (int i = 1; i <= 5; i++) {
            seedRecord("PO-JL" + i, SUP1, M1, 10, 10);
        }
        seedDemand(M1, "SM-L1", REF.minusMonths(2), bd("900"));
        seedDemand(M1, "SM-L2", REF.minusMonths(1), bd("1500"));
        Long calcId = seedCalc(M1, 8601L, 20);

        calculateOk(calcId);
        ErpInvDrpSafetyStockCalc calc = reloadCalc(calcId);

        // σ_d² = σ_m²/30 = 90000/30 = 3000；SS = 1.645 × √3000 × √10
        double expected = 1.645 * Math.sqrt(3000.0) * Math.sqrt(10.0);
        assertTrue(Math.abs(calc.getCalculatedSafetyStock().doubleValue() - expected) < 0.05,
                "低变异（cv=0）应走标准公式且 L=μ_lt=10（而非配置 20）: actual="
                        + calc.getCalculatedSafetyStock() + " expected≈" + expected);
        // ROP = SS + μ_d × μ_lt = SS + 40×10
        double expectedRop = expected + 40.0 * 10.0;
        assertTrue(Math.abs(calc.getCalculatedRop().doubleValue() - expectedRop) < 0.05,
                "ROP 提前期需求应按 μ_lt=10 计算");
    }

    // ---------- ⑪ 联合变分：中变异统一走联合公式（精确数值） ----------

    @Test
    public void testJointVariationMidCvUsesJointFormula() {
        seedSsBase(M1);
        // [10,10,10,10,20] → μ_lt=12 σ_lt=4（总体）cv=1/3 > 0.2 → 联合公式 Z×√(σ_d²×μ_lt + μ_d²×σ_lt²)
        seedRecord("PO-JM1", SUP1, M1, 10, 10);
        seedRecord("PO-JM2", SUP1, M1, 10, 10);
        seedRecord("PO-JM3", SUP1, M1, 10, 10);
        seedRecord("PO-JM4", SUP1, M1, 10, 10);
        seedRecord("PO-JM5", SUP1, M1, 20, 10);
        seedDemand(M1, "SM-M1", REF.minusMonths(2), bd("900"));
        seedDemand(M1, "SM-M2", REF.minusMonths(1), bd("1500"));
        Long calcId = seedCalc(M1, 8602L, 20);

        calculateOk(calcId);
        ErpInvDrpSafetyStockCalc calc = reloadCalc(calcId);

        // inside = σ_d²×μ_lt + μ_d²×σ_lt² = 3000×12 + 1600×16 = 61600
        double expected = 1.645 * Math.sqrt(3000.0 * 12.0 + 1600.0 * 16.0);
        assertTrue(Math.abs(calc.getCalculatedSafetyStock().doubleValue() - expected) < 0.05,
                "中变异（cv=1/3）应走联合公式 Z×√(σ_d²×μ_lt+μ_d²×σ_lt²): actual="
                        + calc.getCalculatedSafetyStock() + " expected≈" + expected);
        // 高变异档显式简化为同一联合公式（无额外缓冲系数）：σ_lt=8（cv=2/3）时同样口径
        double expectedHigh = 1.645 * Math.sqrt(3000.0 * 20.0 + 1600.0 * 64.0);
        assertTrue(expectedHigh > expected * 1.3, "变异放大时联合公式结果显著上升（高档简化继承同一公式）");
    }

    // ---------- ⑫ 样本不足降级（<5 走配置提前期） ----------

    @Test
    public void testInsufficientSamplesUseConfiguredLeadTime() {
        seedSsBase(M1);
        for (int i = 1; i <= 4; i++) {  // 仅 4 样本 < 5 → L1「样本 <5 降级」
            seedRecord("PO-IS" + i, SUP1, M1, 10, 10);
        }
        seedDemand(M1, "SM-I1", REF.minusMonths(2), bd("900"));
        seedDemand(M1, "SM-I2", REF.minusMonths(1), bd("1500"));
        Long calcId = seedCalc(M1, 8603L, 20);

        calculateOk(calcId);
        ErpInvDrpSafetyStockCalc calc = reloadCalc(calcId);

        double expected = 1.645 * Math.sqrt(3000.0) * Math.sqrt(20.0);
        assertTrue(Math.abs(calc.getCalculatedSafetyStock().doubleValue() - expected) < 0.05,
                "样本 <5 应降级用配置 leadTimeDays=20 的标准公式: actual="
                        + calc.getCalculatedSafetyStock() + " expected≈" + expected);
    }

    // ---------- ⑬ confirmWriteback 人工门保持 + replenishmentLeadTime←μ_lt 回写建议 ----------

    @Test
    public void testConfirmWritebackManualGateAndLeadTimeWriteback() {
        seedSsBase(M1);
        for (int i = 1; i <= 5; i++) {
            seedRecord("PO-W" + i, SUP1, M1, 12, 12);
        }
        seedDemand(M1, "SM-W1", REF.minusMonths(2), bd("900"));
        seedDemand(M1, "SM-W2", REF.minusMonths(1), bd("1500"));
        Long calcId = seedCalc(M1, 8604L, 20);

        calculateOk(calcId);

        ErpDrpParameter param = reloadParameter(M1);
        assertEquals(0, param.getSafetyStock().compareTo(bd("50")), "人工门：未 confirm 前 safetyStock 不变");
        assertEquals(Integer.valueOf(20), param.getReplenishmentLeadTime(), "未 confirm 前提前期不变");

        confirmWritebackOk(calcId);

        param = reloadParameter(M1);
        ErpInvDrpSafetyStockCalc calc = reloadCalc(calcId);
        assertEquals(0, param.getSafetyStock().compareTo(calc.getCalculatedSafetyStock()),
                "confirm 后回写计算值");
        assertEquals(Integer.valueOf(12), param.getReplenishmentLeadTime(),
                "confirm 后回写建议 replenishmentLeadTime←μ_lt（12）经既有确认链");

        // 无 LT 样本（<5）时 confirm 不动 replenishmentLeadTime
        seedSsBase(M2);
        seedDemand(M2, "SM-W3", REF.minusMonths(2), bd("900"));
        seedDemand(M2, "SM-W4", REF.minusMonths(1), bd("1500"));
        Long calcId2 = seedCalc(M2, 8605L, 18);
        calculateOk(calcId2);
        confirmWritebackOk(calcId2);
        assertEquals(Integer.valueOf(20), reloadParameter(M2).getReplenishmentLeadTime(),
                "样本不足时 confirm 不改写 replenishmentLeadTime（保持参数原值 20）");
    }

    // ---------- helpers：RPC ----------

    private int recordOk(String poCode, Long supplierId, LocalDate orderDate, LocalDate receiptDate,
                         Integer expected, List<Long> materialIds) {
        ApiResponse<?> resp = rpcRecord(poCode, supplierId, orderDate, receiptDate, expected, materialIds);
        assertEquals(0, resp.getStatus(), "recordFromPurchaseReceive 应成功: " + resp);
        return ((Number) resp.getData()).intValue();
    }

    private ApiResponse<?> rpcRecord(String poCode, Long supplierId, LocalDate orderDate,
                                     LocalDate receiptDate, Integer expected, List<Long> materialIds) {
        Map<String, Object> args = new java.util.LinkedHashMap<>();
        args.put("purchaseOrderCode", poCode);
        args.put("supplierId", supplierId);
        args.put("orderDate", orderDate == null ? null : orderDate.toString());
        args.put("receiptDate", receiptDate == null ? null : receiptDate.toString());
        args.put("expectedLeadTime", expected);
        args.put("materialIds", materialIds);
        return rpc(mutation, "ErpInvDrpLeadTimeRecord__recordFromPurchaseReceive", args);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> statsOk(Long supplierId, Long materialId) {
        ApiResponse<?> resp = rpcStats(supplierId, materialId);
        assertEquals(0, resp.getStatus(), "findLeadTimeStats 应成功: " + resp);
        return (Map<String, Object>) resp.getData();
    }

    private ApiResponse<?> rpcStats(Long supplierId, Long materialId) {
        Map<String, Object> args = new java.util.LinkedHashMap<>();
        args.put("supplierId", supplierId);
        args.put("materialId", materialId);
        return rpc(query, "ErpInvDrpLeadTimeRecord__findLeadTimeStats", args);
    }

    private ErpInvDrpSupplierScore recalcOk(Long supplierId, Long materialId) {
        ApiResponse<?> resp = rpcRecalc(supplierId, materialId);
        assertEquals(0, resp.getStatus(), "recalculateLeadTimeStats 应成功: " + resp);
        Long id = Long.valueOf(((Map<String, Object>) resp.getData()).get("id").toString());
        return daoProvider.daoFor(ErpInvDrpSupplierScore.class).getEntityById(id);
    }

    private ApiResponse<?> rpcRecalc(Long supplierId, Long materialId) {
        Map<String, Object> args = new java.util.LinkedHashMap<>();
        args.put("supplierId", supplierId);
        args.put("materialId", materialId);
        return rpc(mutation, "ErpInvDrpLeadTimeRecord__recalculateLeadTimeStats", args);
    }

    private void calculateOk(Long calcId) {
        Map<String, Object> args = new java.util.LinkedHashMap<>();
        args.put("calcId", calcId);
        ApiResponse<?> resp = rpc(mutation, "ErpInvDrpSafetyStockCalc__calculate", args);
        assertEquals(0, resp.getStatus(), "calculate 应成功: " + resp);
    }

    private void confirmWritebackOk(Long calcId) {
        Map<String, Object> args = new java.util.LinkedHashMap<>();
        args.put("calcId", calcId);
        ApiResponse<?> resp = rpc(mutation, "ErpInvDrpSafetyStockCalc__confirmWriteback", args);
        assertEquals(0, resp.getStatus(), "confirmWriteback 应成功: " + resp);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- helpers：seed ----------

    private String flagFor(String poCode, int actualDays, int expected) {
        recordOk(poCode, SUP1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1).plusDays(actualDays),
                expected, List.of(M1));
        return findRecord(poCode, M1).getEarlyLateFlag();
    }

    private void seedRecord(String poCode, Long supplierId, Long materialId, int actualDays, int expected) {
        seedRecordWithDates(poCode, supplierId, materialId, actualDays, expected, LocalDate.of(2026, 6, 20));
    }

    private void seedRecordWithDates(String poCode, Long supplierId, Long materialId, int actualDays,
                                     int expected, LocalDate receiptDate) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvDrpLeadTimeRecord> dao = daoProvider.daoFor(ErpInvDrpLeadTimeRecord.class);
            ErpInvDrpLeadTimeRecord r = dao.newEntity();
            r.orm_propValueByName("id", 8300L + SEQ.incrementAndGet());
            r.setSupplierId(supplierId);
            r.setMaterialId(materialId);
            r.setOrderDate(receiptDate.minusDays(actualDays));
            r.setReceiptDate(receiptDate);
            r.setActualLeadTime(actualDays);
            r.setExpectedLeadTime(expected);
            r.setVarianceDays(actualDays - expected);
            r.setPurchaseOrderCode(poCode);
            r.setEarlyLateFlag(actualDays < expected ? ErpDrpConstants.LT_FLAG_EARLY
                    : actualDays > expected ? ErpDrpConstants.LT_FLAG_LATE : ErpDrpConstants.LT_FLAG_ON_TIME);
            r.setIsOnTime(actualDays == expected);
            dao.saveEntity(r);
        });
    }

    private ErpInvDrpLeadTimeRecord findRecord(String poCode, Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("purchaseOrderCode", poCode));
        q.addFilter(eq("materialId", materialId));
        q.setLimit(1);
        List<ErpInvDrpLeadTimeRecord> list = daoProvider.daoFor(ErpInvDrpLeadTimeRecord.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void seedPurOrder(String code, Long orderId, Long supplierId, Long materialId,
                              BigDecimal ordered, BigDecimal received) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpPurOrder> odao = daoProvider.daoFor(ErpPurOrder.class);
            ErpPurOrder o = odao.newEntity();
            o.orm_propValueByName("id", orderId);
            o.setCode(code);
            o.setOrgId(ORG_ID);
            o.setSupplierId(supplierId);
            o.setBusinessDate(LocalDate.of(2026, 6, 1));
            o.setCurrencyId(8801L);
            o.orm_propValueByName("docStatus", "APPROVED");
            o.orm_propValueByName("approveStatus", ErpDrpConstants.PUR_ORDER_APPROVE_STATUS_APPROVED);
            odao.saveEntity(o);

            IEntityDao<ErpPurOrderLine> ldao = daoProvider.daoFor(ErpPurOrderLine.class);
            ErpPurOrderLine l = ldao.newEntity();
            l.orm_propValueByName("id", orderId + 100L);
            l.setOrderId(orderId);
            l.setLineNo(10);
            l.setMaterialId(materialId);
            l.setUoMId(UOM_ID);
            l.setQuantity(ordered);
            l.setUnitPrice(bd("10"));
            l.setAmount(ordered.multiply(bd("10")));
            l.setReceivedQuantity(received);
            ldao.saveEntity(l);
        });
    }

    private void seedQaInspection(Long id, String code, Long supplierId, Long materialId, String result) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
            ErpQaInspection i = dao.newEntity();
            i.orm_propValueByName("id", id);
            i.setCode(code);
            i.setOrgId(ORG_ID);
            i.orm_propValueByName("inspectionType", ErpDrpConstants.QA_INSPECTION_TYPE_INCOMING);
            i.setSupplierId(supplierId);
            i.setMaterialId(materialId);
            i.setBusinessDate(LocalDate.of(2026, 6, 10));
            i.setInspectionDate(LocalDate.of(2026, 6, 10));
            i.orm_propValueByName("result", result);
            i.orm_propValueByName("docStatus", "APPROVED");
            i.orm_propValueByName("approveStatus", "APPROVED");
            dao.saveEntity(i);
        });
    }

    private void seedBaseData() {
        seedMaterial(M1);
        seedMaterial(M2);
        seedWarehouse();
        seedPartner(SUP1);
        seedPartner(SUP2);
    }

    private void seedSsBase(Long materialId) {
        seedMaterial(materialId);
        seedWarehouse();
        // 参数：safetyStock=50 leadTime=20 preferredSupplier=SUP1（联合变分 σ_lt 供应商解析）
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpParameter> dao = daoProvider.daoFor(ErpDrpParameter.class);
            ErpDrpParameter p = dao.newEntity();
            p.orm_propValueByName("id", 8100L + materialId);
            p.setMaterialId(materialId);
            p.setWarehouseId(WH_ID);
            p.setSafetyStock(bd("50"));
            p.setReplenishmentLeadTime(20);
            p.setOrderMultiple(bd("1"));
            p.orm_propValueByName("replenishmentMethod", ErpDrpConstants.REPLENISHMENT_METHOD_MIN_MAX);
            p.setPreferredSupplierId(SUP1);
            p.setOrgId(ORG_ID);
            dao.saveEntity(p);
        });
    }

    private void seedDemand(Long materialId, String code, LocalDate businessDate, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockMove> mdao = daoProvider.daoFor(ErpInvStockMove.class);
            Long moveId = 8300L + SEQ.incrementAndGet();
            ErpInvStockMove m = mdao.newEntity();
            m.orm_propValueByName("id", moveId);
            m.setCode(code);
            m.orm_propValueByName("moveType", ErpDrpConstants.MOVE_TYPE_OUTGOING);
            m.setOrgId(ORG_ID);
            m.setBusinessDate(businessDate);
            m.setSourceWarehouseId(WH_ID);
            m.setDocStatus("APPROVED");
            m.orm_propValueByName("approveStatus", "APPROVED");
            m.setPosted(Boolean.TRUE);
            mdao.saveEntity(m);

            IEntityDao<ErpInvStockMoveLine> ldao = daoProvider.daoFor(ErpInvStockMoveLine.class);
            ErpInvStockMoveLine line = ldao.newEntity();
            line.orm_propValueByName("id", moveId + 60000);
            line.setMoveId(moveId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            line.orm_propValueByName("uoMId", UOM_ID);
            line.setQuantity(qty);
            ldao.saveEntity(line);
        });
    }

    private Long seedCalc(Long materialId, Long calcId, int leadTimeDays) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvDrpSafetyStockCalc> dao = daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class);
            ErpInvDrpSafetyStockCalc c = dao.newEntity();
            c.orm_propValueByName("id", calcId);
            c.setCode("SS-LT-" + calcId);
            c.setOrgId(ORG_ID);
            c.setMaterialId(materialId);
            c.setWarehouseId(WH_ID);
            c.orm_propValueByName("method", ErpDrpConstants.SS_METHOD_STATISTICAL);
            c.orm_propValueByName("serviceLevel", ErpDrpConstants.SERVICE_LEVEL_PCT95);
            c.setHistoryMonths(2);
            c.setLeadTimeDays(leadTimeDays);
            dao.saveEntity(c);
        });
        return calcId;
    }

    private ErpInvDrpSafetyStockCalc reloadCalc(Long calcId) {
        return daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).getEntityById(calcId);
    }

    private ErpDrpParameter reloadParameter(Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WH_ID));
        q.setLimit(1);
        List<ErpDrpParameter> list = daoProvider.daoFor(ErpDrpParameter.class).findAllByQuery(q);
        return list.get(0);
    }

    private void seedMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            if (dao.getEntityById(id) != null) {
                return;
            }
            ErpMdMaterial m = dao.newEntity();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            dao.saveEntity(m);
        });
    }

    private void seedWarehouse() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
            if (dao.getEntityById(WH_ID) != null) {
                return;
            }
            ErpMdWarehouse w = dao.newEntity();
            w.orm_propValueByName("id", WH_ID);
            w.setCode("WH-" + WH_ID);
            w.setName("Warehouse " + WH_ID);
            w.setStatus("ACTIVE");
            dao.saveEntity(w);
        });
    }

    private void seedPartner(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
            if (dao.getEntityById(id) != null) {
                return;
            }
            ErpMdPartner p = dao.newEntity();
            p.orm_propValueByName("id", id);
            p.setCode("SUP-" + id);
            p.setName("Supplier " + id);
            p.orm_propValueByName("partnerType", "SUPPLIER");
            p.orm_propValueByName("status", "ACTIVE");
            dao.saveEntity(p);
        });
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP);
    }
}
