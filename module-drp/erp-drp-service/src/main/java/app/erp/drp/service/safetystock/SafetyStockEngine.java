package app.erp.drp.service.safetystock;

import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpInvDrpLeadTimeRecord;
import app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc;
import app.erp.drp.service.ErpDrpConfigs;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 安全库存优化引擎。服务于 {@code IErpInvDrpSafetyStockCalcBiz.calculate}
 * （{@code drp/safety-stock-optimization.md §统计安全库存公式}、{@code drp/use-cases.md UC-DRP-05}）。
 *
 * <p>支持三法：
 * <ul>
 *   <li><b>STATISTICAL</b>：{@code SS = Z × σ_d × √L}，{@code ROP = SS + μ_d × L}。
 *       σ_d/μ_d 基于历史出库（{@link ErpInvStockMove} OUT 已过账）按月聚合，转日均值/日标准差。
 *       历史不足抛 {@link ErpDrpErrors#ERR_DRP_SS_INSUFFICIENT_HISTORY}，调用方降级 SIMPLE。</li>
 *   <li><b>SIMPLE</b>：{@code SS = μ_d × leadTime × safetyFactor}（数据稀缺兜底，safetyFactor 默认 0.5）。</li>
 *   <li><b>DDMRP</b>：{@code SS = μ_d × (leadTime + demandVariabilityDays + orderCycle)}。</li>
 * </ul>
 *
 * <p><b>联合变分（RC-R1.82 / P1-RC-082）</b>：STATISTICAL 法在提前期统计可得（供应商+物料
 * {@link ErpInvDrpLeadTimeRecord} 样本数 ≥ 5，L1 UC-DRP-08「样本 &lt;5 降级」；订单/收货日期缺失行不入统计）
 * 时以 μ_lt 替换配置提前期 L；且 σ_lt/μ_lt &gt; 0.2（lead-time-tracking.md §调整策略中/高变异档，
 * 高档「额外缓冲」量化系数无 L1/owner doc 依据，显式简化为联合变异值——owner doc 实现注记）时按
 * {@code SS = Z × √(σ_d² × μ_lt + μ_d² × σ_lt²)} 计算；低变异档（cv ≤ 0.2）维持标准公式。
 * 供应商解析自 ErpDrpParameter.preferredSupplierId。confirmWriteback 人工审查门不变，
 * 回写建议 replenishmentLeadTime←μ_lt 经既有确认回写链（D5 裁决选项 A：实时计算不留列）。
 *
 * <p>数据清洗：零需求月按 {@code erp-inv.drp-ss-zero-demand-policy}（EXCLUDE=排除，KEEP=保留参与标准差）。
 *
 * <p>Z 值映射（{@code drp/safety-stock-optimization.md §核心公式}）：95%→1.645 / 97.5%→1.96 / 99%→2.326 / 99.5%→2.576。
 *
 * <p><b>Non-Goal</b>：预测来源；20% 偏差预警（{@code erp-inv.drp-ss-alert-threshold}，归 follow-up）。
 *
 * <p>本类为非 BizModel 服务助手（对齐 MRP {@code MrpEngine} 范式），跨域只读聚合（inventory）与本域
 * LeadTimeRecord 读取直接用 {@link IDaoProvider}。
 */
public class SafetyStockEngine {

    static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30");
    static final BigDecimal DEFAULT_SIMPLE_SAFETY_FACTOR = new BigDecimal("0.5");
    static final MathContext MC = new MathContext(8, RoundingMode.HALF_UP);
    // 联合变分触发阈值：σ_lt/μ_lt > 0.2（lead-time-tracking.md §调整策略中变异档下界）
    static final BigDecimal JOINT_VARIATION_CV_THRESHOLD = new BigDecimal("0.2");

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 运行安全库存计算：按 {@code calc.method} 算 {@code calculatedSafetyStock/calculatedRop}，
     * 回写 {@code lastCalculatedAt}。
     *
     * @throws NopException {@link ErpDrpErrors#ERR_DRP_SS_METHOD_UNSUPPORTED} 未支持方法；
     *                      {@link ErpDrpErrors#ERR_DRP_SS_INSUFFICIENT_HISTORY} STATISTICAL 历史不足（调用方降级 SIMPLE）
     */
    public ErpInvDrpSafetyStockCalc calculate(Long calcId) {
        ErpInvDrpSafetyStockCalc calc = requireCalc(calcId);
        String method = calc.getMethod() != null ? calc.getMethod()
                : AppConfig.var(ErpDrpConfigs.CONFIG_DRP_SS_METHOD, ErpDrpConfigs.DEFAULT_DRP_SS_METHOD);
        try {
            return doCalculate(calc, method);
        } catch (NopException e) {
            // STATISTICAL 历史不足 → 降级 SIMPLE（design: 历史不足降级使用 SIMPLE 方法提醒）
            if (ErpDrpErrors.ERR_DRP_SS_INSUFFICIENT_HISTORY.getErrorCode().equals(e.getErrorCode())
                    && ErpDrpConstants.SS_METHOD_STATISTICAL.equals(method)) {
                calc.setMethod(ErpDrpConstants.SS_METHOD_SIMPLE);
                return doCalculate(calc, ErpDrpConstants.SS_METHOD_SIMPLE);
            }
            throw e;
        }
    }

    private ErpInvDrpSafetyStockCalc doCalculate(ErpInvDrpSafetyStockCalc calc, String method) {
        long leadTimeDays = resolveLeadTimeDays(calc);

        List<BigDecimal> monthlyDemands = monthlyDemands(calc.getMaterialId(), calc.getWarehouseId(),
                historyMonths(calc));
        monthlyDemands = applyZeroDemandPolicy(monthlyDemands);

        BigDecimal safetyStock;
        BigDecimal rop;
        if (ErpDrpConstants.SS_METHOD_STATISTICAL.equals(method)) {
            if (monthlyDemands.size() < 2) {
                throw new NopException(ErpDrpErrors.ERR_DRP_SS_INSUFFICIENT_HISTORY)
                        .param(ErpDrpErrors.ARG_MATERIAL_ID, calc.getMaterialId())
                        .param(ErpDrpErrors.ARG_HISTORY_MONTHS, historyMonths(calc));
            }
            BigDecimal meanMonthly = mean(monthlyDemands);
            BigDecimal stddevMonthly = stddev(monthlyDemands, meanMonthly);
            BigDecimal meanDaily = meanMonthly.divide(DAYS_PER_MONTH, 8, RoundingMode.HALF_UP);
            BigDecimal stddevDaily = stddevMonthly.divide(sqrt(DAYS_PER_MONTH), 8, RoundingMode.HALF_UP);
            BigDecimal z = zForServiceLevel(calc.getServiceLevel());
            // 联合变分（RC-R1.82）：样本 ≥5 时 μ_lt 替换配置提前期；σ_lt/μ_lt > 0.2 走联合变异公式
            LeadTimeSample lt = leadTimeSample(calc);
            BigDecimal leadTime = lt != null && lt.mean.signum() > 0
                    ? lt.mean : BigDecimal.valueOf(leadTimeDays);
            boolean jointVariation = lt != null && lt.stddev.signum() > 0 && leadTime.signum() > 0
                    && lt.stddev.divide(leadTime, 8, RoundingMode.HALF_UP)
                            .compareTo(JOINT_VARIATION_CV_THRESHOLD) > 0;
            if (jointVariation) {
                // SS = Z × √(σ_d² × μ_lt + μ_d² × σ_lt²)（lead-time-tracking.md §联合变异公式）
                BigDecimal inside = stddevDaily.multiply(stddevDaily).multiply(leadTime)
                        .add(meanDaily.multiply(meanDaily).multiply(lt.stddev).multiply(lt.stddev));
                safetyStock = z.multiply(sqrt(inside)).setScale(2, RoundingMode.HALF_UP);
            } else {
                safetyStock = z.multiply(stddevDaily).multiply(sqrt(leadTime)).setScale(2, RoundingMode.HALF_UP);
            }
            rop = safetyStock.add(meanDaily.multiply(leadTime)).setScale(2, RoundingMode.HALF_UP);
        } else if (ErpDrpConstants.SS_METHOD_SIMPLE.equals(method)) {
            BigDecimal meanDaily = mean(monthlyDemands).divide(DAYS_PER_MONTH, 8, RoundingMode.HALF_UP);
            BigDecimal lt = BigDecimal.valueOf(leadTimeDays);
            safetyStock = meanDaily.multiply(lt).multiply(DEFAULT_SIMPLE_SAFETY_FACTOR)
                    .setScale(2, RoundingMode.HALF_UP);
            rop = safetyStock.add(meanDaily.multiply(lt)).setScale(2, RoundingMode.HALF_UP);
        } else if (ErpDrpConstants.SS_METHOD_DDMRP.equals(method)) {
            BigDecimal meanDaily = mean(monthlyDemands).divide(DAYS_PER_MONTH, 8, RoundingMode.HALF_UP);
            long bufferDays = leadTimeDays
                    + ErpDrpConstants.DDMRP_DEFAULT_DEMAND_VARIABILITY_DAYS
                    + ErpDrpConstants.DDMRP_DEFAULT_ORDER_CYCLE_DAYS;
            safetyStock = meanDaily.multiply(BigDecimal.valueOf(bufferDays)).setScale(2, RoundingMode.HALF_UP);
            rop = safetyStock;
        } else {
            throw new NopException(ErpDrpErrors.ERR_DRP_SS_METHOD_UNSUPPORTED)
                    .param(ErpDrpErrors.ARG_METHOD, method);
        }

        calc.setCalculatedSafetyStock(safetyStock);
        calc.setCalculatedRop(rop);
        calc.setLastCalculatedAt(CoreMetrics.currentTimestamp());
        daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).updateEntity(calc);
        return calc;
    }

    /**
     * 查询参数的有效安全库存：优先级
     * {@code overrideSafetyStock > calculatedSafetyStock > ErpDrpParameter.safetyStock}。
     */
    public BigDecimal findEffectiveSafetyStock(Long materialId, Long warehouseId, Long orgId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        if (orgId != null) {
            q.addFilter(eq("orgId", orgId));
        }
        q.setLimit(1);
        List<ErpInvDrpSafetyStockCalc> calcs = daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).findAllByQuery(q);
        if (!calcs.isEmpty()) {
            ErpInvDrpSafetyStockCalc calc = calcs.get(0);
            if (calc.getOverrideSafetyStock() != null && calc.getOverrideSafetyStock().signum() >= 0) {
                return calc.getOverrideSafetyStock();
            }
            if (calc.getCalculatedSafetyStock() != null && calc.getCalculatedSafetyStock().signum() >= 0) {
                return calc.getCalculatedSafetyStock();
            }
        }
        ErpDrpParameter param = findParameter(materialId, warehouseId, orgId);
        return param != null && param.getSafetyStock() != null ? param.getSafetyStock() : BigDecimal.ZERO;
    }

    /**
     * 按 ErpDrpParameter 主键查询有效安全库存（{@code IErpInvDrpSafetyStockCalcBiz.findEffectiveSafetyStock} 入口）。
     */
    public BigDecimal findEffectiveSafetyStockByParameterId(Long parameterId) {
        ErpDrpParameter param = daoProvider.daoFor(ErpDrpParameter.class).getEntityById(parameterId);
        if (param == null || param.getMaterialId() == null || param.getWarehouseId() == null) {
            return BigDecimal.ZERO;
        }
        return findEffectiveSafetyStock(param.getMaterialId(), param.getWarehouseId(), param.getOrgId());
    }

    /**
     * 人工确认后回写 ErpDrpParameter.safetyStock。overrideSafetyStock 非空则回写覆盖值，否则回写计算值。
     * RC-R1.82：提前期统计样本 ≥5 时同步回写建议 replenishmentLeadTime←μ_lt（经既有确认回写链，
     * 不绕过人工门；D5 裁决选项 A：实时计算不留列）。
     */
    public void confirmWriteback(Long calcId) {
        ErpInvDrpSafetyStockCalc calc = requireCalc(calcId);
        BigDecimal value = calc.getOverrideSafetyStock() != null && calc.getOverrideSafetyStock().signum() >= 0
                ? calc.getOverrideSafetyStock()
                : calc.getCalculatedSafetyStock();
        if (value == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SS_METHOD_UNSUPPORTED)
                    .param(ErpDrpErrors.ARG_METHOD, "无计算结果，请先 calculate");
        }
        ErpDrpParameter param = findParameter(calc.getMaterialId(), calc.getWarehouseId(), calc.getOrgId());
        if (param == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_PARAMETER_MISSING)
                    .param(ErpDrpErrors.ARG_MATERIAL_ID, calc.getMaterialId())
                    .param(ErpDrpErrors.ARG_WAREHOUSE_ID, calc.getWarehouseId());
        }
        param.setSafetyStock(value);
        LeadTimeSample lt = leadTimeSample(calc);
        if (lt != null && lt.mean.signum() > 0) {
            param.setReplenishmentLeadTime(lt.mean.setScale(0, RoundingMode.HALF_UP).intValue());
        }
        daoProvider.daoFor(ErpDrpParameter.class).updateEntity(param);
    }

    /**
     * 提前期统计样本（D5 裁决选项 A：实时计算）：ErpDrpParameter.preferredSupplierId + materialId
     * 在统计窗口（erp-inv.drp-lt-stats-window-days，≤0 全历史）内的 ErpInvDrpLeadTimeRecord，
     * 订单/收货日期缺失行不入统计；样本数 &lt; 5（L1 UC-DRP-08「样本 &lt;5 降级」）返回 null（走配置提前期）。
     */
    protected LeadTimeSample leadTimeSample(ErpInvDrpSafetyStockCalc calc) {
        if (calc.getMaterialId() == null) {
            return null;
        }
        ErpDrpParameter param = findParameter(calc.getMaterialId(), calc.getWarehouseId(), calc.getOrgId());
        Long supplierId = param != null ? param.getPreferredSupplierId() : null;
        int windowDays = AppConfig.var(ErpDrpConfigs.CONFIG_DRP_LT_STATS_WINDOW_DAYS,
                ErpDrpConfigs.DEFAULT_DRP_LT_STATS_WINDOW_DAYS);
        LocalDate since = windowDays > 0 ? CoreMetrics.today().minusDays(windowDays) : null;

        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", calc.getMaterialId()));
        if (supplierId != null) {
            q.addFilter(eq("supplierId", supplierId));
        }
        if (since != null) {
            q.addFilter(ge("receiptDate", since));
        }
        List<BigDecimal> values = new ArrayList<>();
        for (ErpInvDrpLeadTimeRecord r : daoProvider.daoFor(ErpInvDrpLeadTimeRecord.class).findAllByQuery(q)) {
            if (r.getActualLeadTime() != null && r.getOrderDate() != null && r.getReceiptDate() != null) {
                values.add(BigDecimal.valueOf(r.getActualLeadTime()));
            }
        }
        if (values.size() < ErpDrpConfigs.LT_JOINT_VARIATION_MIN_SAMPLES) {
            return null;
        }
        LeadTimeSample sample = new LeadTimeSample();
        sample.count = values.size();
        sample.mean = mean(values);
        sample.stddev = stddev(values, sample.mean);
        return sample;
    }

    protected static final class LeadTimeSample {
        int count;
        BigDecimal mean;
        BigDecimal stddev;
    }

    private List<BigDecimal> monthlyDemands(Long materialId, Long warehouseId, int historyMonths) {
        IEntityDao<ErpInvStockMove> moveDao = daoProvider.daoFor(ErpInvStockMove.class);
        IEntityDao<ErpInvStockMoveLine> lineDao = daoProvider.daoFor(ErpInvStockMoveLine.class);

        LocalDate since = CoreMetrics.today().minusMonths(historyMonths);
        QueryBean mq = new QueryBean();
        mq.addFilter(eq("moveType", ErpDrpConstants.MOVE_TYPE_OUTGOING));
        mq.addFilter(eq("posted", Boolean.TRUE));
        if (warehouseId != null) {
            mq.addFilter(eq("sourceWarehouseId", warehouseId));
        }
        mq.addFilter(ge("businessDate", since));
        List<ErpInvStockMove> moves = moveDao.findAllByQuery(mq);
        if (moves.isEmpty()) {
            // 无历史出库 → 视为零需求（与下方 byMonth 空集语义一致），避免 mean() 除零
            List<BigDecimal> zero = new ArrayList<>();
            zero.add(BigDecimal.ZERO);
            return zero;
        }
        List<Long> moveIds = new ArrayList<>();
        Map<Long, ErpInvStockMove> moveById = new HashMap<>();
        for (ErpInvStockMove m : moves) {
            moveIds.add(m.getId());
            moveById.put(m.getId(), m);
        }
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("materialId", materialId));
        lq.addFilter(in("moveId", moveIds));
        List<ErpInvStockMoveLine> lines = lineDao.findAllByQuery(lq);

        Map<YearMonth, BigDecimal> byMonth = new HashMap<>();
        for (ErpInvStockMoveLine l : lines) {
            ErpInvStockMove m = moveById.get(l.getMoveId());
            if (m == null || m.getBusinessDate() == null) {
                continue;
            }
            YearMonth ym = YearMonth.from(m.getBusinessDate());
            byMonth.merge(ym, nz(l.getQuantity()), BigDecimal::add);
        }
        List<BigDecimal> result = new ArrayList<>(byMonth.values());
        if (result.isEmpty()) {
            result.add(BigDecimal.ZERO);
        }
        return result;
    }

    private List<BigDecimal> applyZeroDemandPolicy(List<BigDecimal> monthlyDemands) {
        String policy = AppConfig.var(ErpDrpConfigs.CONFIG_DRP_SS_ZERO_DEMAND_POLICY,
                ErpDrpConfigs.DEFAULT_DRP_SS_ZERO_DEMAND_POLICY);
        if (!ErpDrpConstants.ZERO_DEMAND_POLICY_EXCLUDE.equals(policy)) {
            return monthlyDemands; // KEEP
        }
        List<BigDecimal> filtered = new ArrayList<>();
        for (BigDecimal d : monthlyDemands) {
            if (d.signum() > 0) {
                filtered.add(d);
            }
        }
        return filtered.isEmpty() ? monthlyDemands : filtered; // 全零则保留原（避免空样本）
    }

    private int historyMonths(ErpInvDrpSafetyStockCalc calc) {
        if (calc.getHistoryMonths() != null && calc.getHistoryMonths() > 0) {
            return calc.getHistoryMonths();
        }
        return AppConfig.var(ErpDrpConfigs.CONFIG_DRP_SS_HISTORY_MONTHS,
                ErpDrpConfigs.DEFAULT_DRP_SS_HISTORY_MONTHS);
    }

    private long resolveLeadTimeDays(ErpInvDrpSafetyStockCalc calc) {
        if (calc.getLeadTimeDays() != null && calc.getLeadTimeDays() > 0) {
            return calc.getLeadTimeDays();
        }
        ErpDrpParameter param = findParameter(calc.getMaterialId(), calc.getWarehouseId(), calc.getOrgId());
        if (param != null && param.getReplenishmentLeadTime() != null && param.getReplenishmentLeadTime() > 0) {
            return param.getReplenishmentLeadTime();
        }
        return ErpDrpConstants.DEFAULT_REPLENISHMENT_LEAD_TIME_DAYS;
    }

    private BigDecimal zForServiceLevel(String serviceLevel) {
        if (ErpDrpConstants.SERVICE_LEVEL_PCT97_5.equals(serviceLevel)) {
            return new BigDecimal("1.960");
        }
        if (ErpDrpConstants.SERVICE_LEVEL_PCT99.equals(serviceLevel)) {
            return new BigDecimal("2.326");
        }
        if (ErpDrpConstants.SERVICE_LEVEL_PCT99_5.equals(serviceLevel)) {
            return new BigDecimal("2.576");
        }
        return new BigDecimal("1.645"); // PCT95 默认
    }

    private ErpDrpParameter findParameter(Long materialId, Long warehouseId, Long orgId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        if (orgId != null) {
            q.addFilter(eq("orgId", orgId));
        }
        q.setLimit(1);
        List<ErpDrpParameter> list = daoProvider.daoFor(ErpDrpParameter.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvDrpSafetyStockCalc requireCalc(Long calcId) {
        if (calcId == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SS_METHOD_UNSUPPORTED)
                    .param(ErpDrpErrors.ARG_METHOD, "calcId 为空");
        }
        ErpInvDrpSafetyStockCalc calc = daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).getEntityById(calcId);
        if (calc == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SS_METHOD_UNSUPPORTED)
                    .param(ErpDrpErrors.ARG_METHOD, "安全库存计算记录不存在: " + calcId);
        }
        return calc;
    }

    static BigDecimal mean(List<BigDecimal> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            sum = sum.add(v);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);
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
        return sqrt(variance);
    }

    static BigDecimal sqrt(BigDecimal v) {
        if (v == null || v.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.sqrt(v.doubleValue())).round(MC);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
