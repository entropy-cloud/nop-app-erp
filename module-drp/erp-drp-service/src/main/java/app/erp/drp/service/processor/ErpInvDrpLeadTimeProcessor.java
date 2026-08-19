package app.erp.drp.service.processor;

import app.erp.drp.biz.IErpInvDrpLeadTimeRecordBiz;
import app.erp.drp.biz.LeadTimeStatsBean;
import app.erp.drp.dao.entity.ErpInvDrpLeadTimeRecord;
import app.erp.drp.dao.entity.ErpInvDrpSupplierScore;
import app.erp.drp.service.ErpDrpConfigs;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.dao.entity.ErpQaInspection;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;

/**
 * 提前期统计与供应商可靠性评分编排 Processor（RC-R1.82 / P1-RC-082，UC-DRP-08）。
 *
 * <p>三入口：{@link #recordFromPurchaseReceive}（D4 裁决选项 A：purchase 收货审批后置 Facade，
 * actualLeadTime = DATEDIFF(receiptDate, orderDate)，幂等守卫同 purchaseOrderCode+materialId 不重复落记录）/
 * {@link #findLeadTimeStats}（供应商级/供应商+物料级/物料级 μ/σ/准时率/中位数/样本数）/
 * {@link #recalculateLeadTimeStats}（四维评分 40/30/20/10 + 等级 A/B/C/D 阈值 90/75/60，回写
 * {@link ErpInvDrpSupplierScore} UK(supplierId,materialId) upsert；无样本维度得分记 0 且
 * missingDimensions 标注样本缺失，不静默忽略）。
 *
 * <p>跨域只读 Java 边（matrix §2.4 登记）：数量准确率维度读 purchase 订单行
 * quantity/receivedQuantity 偏差（drp→pur，@Nullable 容错）；质量合格率维度读 quality 来料检验
 * （drp→qa，合格 = ACCEPTED 或 CONDITIONAL 让步接收，与越库质检快检口径一致）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类或逐个覆盖 protected step。
 */
public class ErpInvDrpLeadTimeProcessor {

    static final BigDecimal WEIGHT_ON_TIME = new BigDecimal("40");
    static final BigDecimal WEIGHT_STABILITY = new BigDecimal("30");
    static final BigDecimal WEIGHT_QUANTITY = new BigDecimal("20");
    static final BigDecimal WEIGHT_QUALITY = new BigDecimal("10");
    static final String MISSING_DIM_QUANTITY = "QUANTITY";
    static final String MISSING_DIM_QUALITY = "QUALITY";

    @Inject
    IDaoProvider daoProvider;

    @Inject
    @Nullable
    IErpPurOrderBiz purOrderBiz;

    @Inject
    @Nullable
    IErpQaInspectionBiz inspectionBiz;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setPurOrderBiz(IErpPurOrderBiz purOrderBiz) {
        this.purOrderBiz = purOrderBiz;
    }

    public void setInspectionBiz(IErpQaInspectionBiz inspectionBiz) {
        this.inspectionBiz = inspectionBiz;
    }

    // ---------- 入口 ----------

    public int recordFromPurchaseReceive(String purchaseOrderCode, Long supplierId, LocalDate orderDate,
                                         LocalDate receiptDate, Integer expectedLeadTime,
                                         List<Long> materialIds, IServiceContext context) {
        if (purchaseOrderCode == null || purchaseOrderCode.isEmpty() || supplierId == null
                || materialIds == null || materialIds.isEmpty()) {
            return 0;
        }
        if (orderDate == null || receiptDate == null || receiptDate.isBefore(orderDate)) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LT_DATES_INVALID)
                    .param(ErpDrpErrors.ARG_PURCHASE_ORDER_CODE, purchaseOrderCode);
        }
        long actual = ChronoUnit.DAYS.between(orderDate, receiptDate);
        Integer expected = expectedLeadTime != null && expectedLeadTime >= 0 ? expectedLeadTime : null;
        int created = 0;
        for (Long materialId : new LinkedHashSet<>(materialIds)) {
            if (materialId == null || existsRecord(purchaseOrderCode, materialId)) {
                continue;
            }
            created += createRecord(purchaseOrderCode, supplierId, materialId, orderDate, receiptDate,
                    (int) actual, expected, context);
        }
        return created;
    }

    public LeadTimeStatsBean findLeadTimeStats(Long supplierId, Long materialId, IServiceContext context) {
        if (supplierId == null && materialId == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LT_STATS_FILTER_REQUIRED);
        }
        return computeStats(loadRecords(supplierId, materialId), supplierId, materialId);
    }

    public ErpInvDrpSupplierScore recalculateLeadTimeStats(Long supplierId, Long materialId,
                                                           IServiceContext context) {
        if (supplierId == null || materialId == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LT_STATS_FILTER_REQUIRED);
        }
        List<ErpInvDrpLeadTimeRecord> records = loadRecords(supplierId, materialId);
        if (records.isEmpty()) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LT_NO_SAMPLES)
                    .param(ErpDrpErrors.ARG_SUPPLIER_ID, supplierId)
                    .param(ErpDrpErrors.ARG_MATERIAL_ID, materialId);
        }
        LeadTimeStatsBean stats = computeStats(records, supplierId, materialId);
        return upsertScore(stats, context);
    }

    // ---------- step：记录写入 ----------

    protected int createRecord(String purchaseOrderCode, Long supplierId, Long materialId,
                               LocalDate orderDate, LocalDate receiptDate, int actual, Integer expected,
                               IServiceContext context) {
        ErpInvDrpLeadTimeRecord record = recordDao().newEntity();
        record.setSupplierId(supplierId);
        record.setMaterialId(materialId);
        record.setOrderDate(orderDate);
        record.setReceiptDate(receiptDate);
        record.setActualLeadTime(actual);
        record.setExpectedLeadTime(expected);
        record.setPurchaseOrderCode(purchaseOrderCode);
        if (expected != null) {
            record.setVarianceDays(actual - expected);
            String flag = resolveFlag(actual, expected);
            record.setEarlyLateFlag(flag);
            record.setIsOnTime(ErpDrpConstants.LT_FLAG_ON_TIME.equals(flag));
        }
        // expected 缺失时 earlyLateFlag/varianceDays 留空（不可判定）；isOnTime 列有 DDL 默认 true，
        // 准时统计以 earlyLateFlag 非空为已判定标记（见 computeStats），DB 默认值不影响口径
        recordDao().saveEntity(record);
        return 1;
    }

    /**
     * 三档偏差标记（owner doc lead-time-tracking.md §字典）：actual 在 expected×(1±容差) 内 ON_TIME；
     * 低于下界 EARLY；高于上界 LATE。容差系数 config {@code erp-inv.drp-lt-tolerance} 默认 0.1。
     */
    protected String resolveFlag(int actual, int expected) {
        BigDecimal tolerance = new BigDecimal(AppConfig.var(ErpDrpConfigs.CONFIG_DRP_LT_TOLERANCE,
                ErpDrpConfigs.DEFAULT_DRP_LT_TOLERANCE));
        BigDecimal lower = BigDecimal.valueOf(expected).multiply(BigDecimal.ONE.subtract(tolerance));
        BigDecimal upper = BigDecimal.valueOf(expected).multiply(BigDecimal.ONE.add(tolerance));
        BigDecimal a = BigDecimal.valueOf(actual);
        if (a.compareTo(lower) < 0) {
            return ErpDrpConstants.LT_FLAG_EARLY;
        }
        if (a.compareTo(upper) > 0) {
            return ErpDrpConstants.LT_FLAG_LATE;
        }
        return ErpDrpConstants.LT_FLAG_ON_TIME;
    }

    protected boolean existsRecord(String purchaseOrderCode, Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("purchaseOrderCode", purchaseOrderCode));
        q.addFilter(eq("materialId", materialId));
        q.setLimit(1);
        return !recordDao().findAllByQuery(q).isEmpty();
    }

    // ---------- step：统计 ----------

    protected List<ErpInvDrpLeadTimeRecord> loadRecords(Long supplierId, Long materialId) {
        QueryBean q = new QueryBean();
        if (supplierId != null) {
            q.addFilter(eq("supplierId", supplierId));
        }
        if (materialId != null) {
            q.addFilter(eq("materialId", materialId));
        }
        LocalDate since = statsWindowFrom();
        if (since != null) {
            q.addFilter(ge("receiptDate", since));
        }
        List<ErpInvDrpLeadTimeRecord> records = new ArrayList<>();
        for (ErpInvDrpLeadTimeRecord r : recordDao().findAllByQuery(q)) {
            // 订单/收货日期缺失/为空样本不入统计（写入侧已守卫，防御手工数据）
            if (r.getActualLeadTime() != null && r.getOrderDate() != null && r.getReceiptDate() != null) {
                records.add(r);
            }
        }
        return records;
    }

    protected LeadTimeStatsBean computeStats(List<ErpInvDrpLeadTimeRecord> records, Long supplierId,
                                             Long materialId) {
        LeadTimeStatsBean stats = new LeadTimeStatsBean();
        stats.setSupplierId(supplierId);
        stats.setMaterialId(materialId);
        stats.setWindowFrom(statsWindowFrom());
        stats.setWindowTo(CoreMetrics.today());
        stats.setSampleCount(records.size());
        if (records.isEmpty()) {
            return stats;
        }
        List<BigDecimal> values = new ArrayList<>();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int onTime = 0;
        int judged = 0;
        for (ErpInvDrpLeadTimeRecord r : records) {
            values.add(BigDecimal.valueOf(r.getActualLeadTime()));
            min = Math.min(min, r.getActualLeadTime());
            max = Math.max(max, r.getActualLeadTime());
            // 已判定标记 = earlyLateFlag 非空（expected 缺失行不可判定；isOnTime 列 DDL 默认 true 不可作标记）
            if (r.getEarlyLateFlag() != null) {
                judged++;
                if (Boolean.TRUE.equals(r.getIsOnTime())) {
                    onTime++;
                }
            }
        }
        BigDecimal mean = mean(values);
        stats.setAvgLeadTime(mean);
        stats.setLeadTimeStdDev(stddev(values, mean));
        stats.setMinLeadTime(min);
        stats.setMaxLeadTime(max);
        stats.setMedianLeadTime(median(values));
        stats.setOnTimeCount(onTime);
        stats.setJudgedCount(judged);
        if (judged > 0) {
            stats.setOnTimeRate(BigDecimal.valueOf(onTime).divide(BigDecimal.valueOf(judged), 4, RoundingMode.HALF_UP));
        }
        if (mean.signum() > 0) {
            stats.setVariationCoefficient(stats.getLeadTimeStdDev().divide(mean, 4, RoundingMode.HALF_UP));
        }
        return stats;
    }

    // ---------- step：评分 ----------

    /**
     * 四维评分合成并 upsert 汇总行：准时率×40 + 稳定性 max(0,1-σ/μ)×30 + 数量准确率×20 + 质量合格率×10；
     * 数量/质量维度无样本时得分记 0 且 missingDimensions 标注（QUANTITY/QUALITY），不静默忽略。
     */
    protected ErpInvDrpSupplierScore upsertScore(LeadTimeStatsBean stats, IServiceContext context) {
        BigDecimal onTimeRate = stats.getOnTimeRate() != null ? stats.getOnTimeRate() : BigDecimal.ZERO;
        BigDecimal onTimeScore = onTimeRate.multiply(WEIGHT_ON_TIME);

        BigDecimal stabilityScore;
        if (stats.getVariationCoefficient() != null) {
            BigDecimal stability = BigDecimal.ONE.subtract(stats.getVariationCoefficient());
            if (stability.signum() < 0) {
                stability = BigDecimal.ZERO;
            }
            stabilityScore = stability.multiply(WEIGHT_STABILITY);
        } else {
            // μ≤0（如全部当日到货）时 σ 必同为 0，视为完全稳定
            stabilityScore = stats.getSampleCount() != null && stats.getSampleCount() > 0
                    ? WEIGHT_STABILITY : BigDecimal.ZERO;
        }

        BigDecimal quantityAccuracy = computeQuantityAccuracy(stats.getSupplierId(), stats.getMaterialId(),
                context);
        BigDecimal quantityScore = quantityAccuracy != null
                ? quantityAccuracy.multiply(WEIGHT_QUANTITY) : BigDecimal.ZERO;

        BigDecimal qualityPassRate = computeQualityPassRate(stats.getSupplierId(), stats.getMaterialId(),
                context);
        BigDecimal qualityScore = qualityPassRate != null
                ? qualityPassRate.multiply(WEIGHT_QUALITY) : BigDecimal.ZERO;

        BigDecimal total = onTimeScore.add(stabilityScore).add(quantityScore).add(qualityScore)
                .setScale(2, RoundingMode.HALF_UP);

        List<String> missing = new ArrayList<>();
        if (quantityAccuracy == null) {
            missing.add(MISSING_DIM_QUANTITY);
        }
        if (qualityPassRate == null) {
            missing.add(MISSING_DIM_QUALITY);
        }

        ErpInvDrpSupplierScore score = findScore(stats.getSupplierId(), stats.getMaterialId());
        if (score == null) {
            score = scoreDao().newEntity();
            score.setSupplierId(stats.getSupplierId());
            score.setMaterialId(stats.getMaterialId());
        }
        score.setSampleCount(stats.getSampleCount());
        score.setAvgLeadTime(stats.getAvgLeadTime());
        score.setLeadTimeStdDev(stats.getLeadTimeStdDev());
        score.setOnTimeRate(stats.getOnTimeRate());
        score.setVariationCoefficient(stats.getVariationCoefficient());
        score.setQuantityAccuracy(quantityAccuracy);
        score.setQualityPassRate(qualityPassRate);
        score.setOnTimeScore(onTimeScore.setScale(2, RoundingMode.HALF_UP));
        score.setStabilityScore(stabilityScore.setScale(2, RoundingMode.HALF_UP));
        score.setQuantityScore(quantityScore.setScale(2, RoundingMode.HALF_UP));
        score.setQualityScore(qualityScore.setScale(2, RoundingMode.HALF_UP));
        score.setTotalScore(total);
        score.setGrade(resolveGrade(total));
        score.setMissingDimensions(missing.isEmpty() ? null : String.join(",", missing));
        score.setWindowFrom(stats.getWindowFrom());
        score.setWindowTo(stats.getWindowTo());
        score.setLastCalculatedAt(CoreMetrics.currentTimestamp());
        scoreDao().saveOrUpdateEntity(score);
        return score;
    }

    protected String resolveGrade(BigDecimal total) {
        if (total.compareTo(ErpDrpConstants.GRADE_THRESHOLD_A) >= 0) {
            return ErpDrpConstants.SUPPLIER_GRADE_A;
        }
        if (total.compareTo(ErpDrpConstants.GRADE_THRESHOLD_B) >= 0) {
            return ErpDrpConstants.SUPPLIER_GRADE_B;
        }
        if (total.compareTo(ErpDrpConstants.GRADE_THRESHOLD_C) >= 0) {
            return ErpDrpConstants.SUPPLIER_GRADE_C;
        }
        return ErpDrpConstants.SUPPLIER_GRADE_D;
    }

    /**
     * 数量准确率（drp→pur 只读 Java 边，matrix §2.4 登记）：统计窗口内该供应商 APPROVED 采购单
     * 该物料行的 ΣreceivedQuantity / Σquantity 偏差，accuracy = max(0, 1 - |Σreceived-Σordered|/Σordered)。
     * 窗口裁剪在 Java 侧执行（biz 查询管道过滤操作集不保证支持 ge，对齐 CrossDockProcessor ne 先例）。
     * 无候选订单行（或 pur 模块未部署 @Nullable）→ null（样本缺失）。
     */
    protected BigDecimal computeQuantityAccuracy(Long supplierId, Long materialId, IServiceContext context) {
        if (purOrderBiz == null || supplierId == null || materialId == null) {
            return null;
        }
        QueryBean oq = new QueryBean();
        oq.addFilter(eq("supplierId", supplierId));
        oq.addFilter(eq("approveStatus", ErpDrpConstants.PUR_ORDER_APPROVE_STATUS_APPROVED));
        List<ErpPurOrder> orders = purOrderBiz.findList(oq, null, context);
        LocalDate since = statsWindowFrom();
        BigDecimal ordered = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        boolean hasLine = false;
        for (ErpPurOrder order : orders) {
            if (since != null && order.getBusinessDate() != null && order.getBusinessDate().isBefore(since)) {
                continue;
            }
            for (ErpPurOrderLine line : order.getLines()) {
                if (!Objects.equals(line.getMaterialId(), materialId)) {
                    continue;
                }
                hasLine = true;
                ordered = ordered.add(nz(line.getQuantity()));
                received = received.add(nz(line.getReceivedQuantity()));
            }
        }
        if (!hasLine || ordered.signum() <= 0) {
            return null;
        }
        BigDecimal deviation = received.subtract(ordered).abs().divide(ordered, 8, RoundingMode.HALF_UP);
        BigDecimal accuracy = BigDecimal.ONE.subtract(deviation);
        return accuracy.signum() < 0 ? BigDecimal.ZERO : accuracy;
    }

    /**
     * 质量合格率（drp→qa 只读 Java 边，matrix §2.4 登记）：统计窗口内该供应商+物料 INCOMING 检验
     * 合格（ACCEPTED 或 CONDITIONAL 让步接收，与越库快检口径一致）占比。窗口裁剪同上 Java 侧执行。
     * 无候选检验单（或 qa 模块未部署 @Nullable）→ null（样本缺失）。
     */
    protected BigDecimal computeQualityPassRate(Long supplierId, Long materialId, IServiceContext context) {
        if (inspectionBiz == null || supplierId == null || materialId == null) {
            return null;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("supplierId", supplierId));
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("inspectionType", ErpDrpConstants.QA_INSPECTION_TYPE_INCOMING));
        List<ErpQaInspection> inspections = inspectionBiz.findList(q, null, context);
        LocalDate since = statsWindowFrom();
        long total = 0;
        long passed = 0;
        for (ErpQaInspection i : inspections) {
            if (since != null && i.getBusinessDate() != null && i.getBusinessDate().isBefore(since)) {
                continue;
            }
            total++;
            if (ErpDrpConstants.QA_INSPECTION_RESULT_ACCEPTED.equals(i.getResult())
                    || ErpDrpConstants.QA_INSPECTION_RESULT_CONDITIONAL.equals(i.getResult())) {
                passed++;
            }
        }
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(passed).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    protected ErpInvDrpSupplierScore findScore(Long supplierId, Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("supplierId", supplierId));
        q.addFilter(eq("materialId", materialId));
        q.setLimit(1);
        List<ErpInvDrpSupplierScore> scores = scoreDao().findAllByQuery(q);
        return scores.isEmpty() ? null : scores.get(0);
    }

    // ---------- 辅助 ----------

    protected LocalDate statsWindowFrom() {
        int windowDays = AppConfig.var(ErpDrpConfigs.CONFIG_DRP_LT_STATS_WINDOW_DAYS,
                ErpDrpConfigs.DEFAULT_DRP_LT_STATS_WINDOW_DAYS);
        return windowDays > 0 ? CoreMetrics.today().minusDays(windowDays) : null;
    }

    protected IEntityDao<ErpInvDrpLeadTimeRecord> recordDao() {
        // 本域自有实体的 Processor 内 dao 访问（对齐 ErpInvDrpCrossDockProcessor.dockDao 先例）
        return daoProvider.daoFor(ErpInvDrpLeadTimeRecord.class);
    }

    protected IEntityDao<ErpInvDrpSupplierScore> scoreDao() {
        return daoProvider.daoFor(ErpInvDrpSupplierScore.class);
    }

    static BigDecimal mean(List<BigDecimal> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            sum = sum.add(v);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    static BigDecimal stddev(List<BigDecimal> values, BigDecimal mean) {
        if (values.size() < 2) {
            return BigDecimal.ZERO;
        }
        BigDecimal sumSq = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            BigDecimal diff = v.subtract(mean);
            sumSq = sumSq.add(diff.multiply(diff));
        }
        BigDecimal variance = sumSq.divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(4, RoundingMode.HALF_UP);
    }

    static BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(mid);
        }
        return sorted.get(mid - 1).add(sorted.get(mid))
                .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
