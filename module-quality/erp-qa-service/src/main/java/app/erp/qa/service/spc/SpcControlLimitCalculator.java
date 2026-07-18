package app.erp.qa.service.spc;

import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * SPC 控制限计算引擎（{@code docs/design/quality/spc.md §关键流程 2}，plan 2026-07-07-0305-2 Phase 2）。
 *
 * <p>当 chart 下样本数 ≥ {@link ErpQaConstants#SPC_MIN_SUBGROUPS_FOR_CONTROL_LIMIT}（默认 20）触发
 * 重算 chart.ucl/lcl/cl。
 *
 * <p>chartType 两分支（plan 2026-07-19-0120-2 增计数型）：
 * <ul>
 *   <li><b>计量型</b>（X_BAR_R/X_BAR_S/X_MR）：grandMean = mean(sample.mean)，sigmaHat = R̄/d2，
 *       UCL/LCL = cl ± 3σ̂；clCenterType 三分支（AUTO_FROM_DATA/MANUAL/TARGET）。</li>
 *   <li><b>计数型</b>（P/NP/C/U）：调 {@link AttributesControlLimitFormulas} 对应公式；
 *       CL/UCL/LCL 由公式直接派生，clCenterType 不适用（计数型 CL 为统计均值）；
 *       负数下限钳 0（缺陷率/数下界 ≥ 0）。</li>
 * </ul>
 *
 * <p>样本数不足 20 时，calcStatus 保持 PENDING，不抛错。
 */
public class SpcControlLimitCalculator {

    /** subgroupSize → d2 系数（短表，覆盖 n=2..10）。来自 SPC 标准常量表。 */
    private static final Map<Integer, BigDecimal> D2 = new HashMap<>();
    /** subgroupSize → D3 系数（极差下限系数；D3=0 表示无下限）。来自 SPC 标准常量表。 */
    private static final Map<Integer, BigDecimal> D3 = new HashMap<>();
    /** subgroupSize → D4 系数（极差上限系数）。来自 SPC 标准常量表。 */
    private static final Map<Integer, BigDecimal> D4 = new HashMap<>();

    static {
        // n=2..10 标准 SPC 系数
        D2.put(2, new BigDecimal("1.128"));
        D2.put(3, new BigDecimal("1.693"));
        D2.put(4, new BigDecimal("2.059"));
        D2.put(5, new BigDecimal("2.326"));
        D2.put(6, new BigDecimal("2.534"));
        D2.put(7, new BigDecimal("2.704"));
        D2.put(8, new BigDecimal("2.847"));
        D2.put(9, new BigDecimal("2.970"));
        D2.put(10, new BigDecimal("3.078"));

        D3.put(2, BigDecimal.ZERO);
        D3.put(3, BigDecimal.ZERO);
        D3.put(4, BigDecimal.ZERO);
        D3.put(5, BigDecimal.ZERO);
        D3.put(6, BigDecimal.ZERO);
        D3.put(7, new BigDecimal("0.076"));
        D3.put(8, new BigDecimal("0.136"));
        D3.put(9, new BigDecimal("0.184"));
        D3.put(10, new BigDecimal("0.223"));

        D4.put(2, new BigDecimal("3.267"));
        D4.put(3, new BigDecimal("2.574"));
        D4.put(4, new BigDecimal("2.282"));
        D4.put(5, new BigDecimal("2.114"));
        D4.put(6, new BigDecimal("2.004"));
        D4.put(7, new BigDecimal("1.924"));
        D4.put(8, new BigDecimal("1.864"));
        D4.put(9, new BigDecimal("1.816"));
        D4.put(10, new BigDecimal("1.777"));
    }

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 对 chart 重算控制限。子组数不足 20 时 calcStatus 保持 PENDING，不重算。
     *
     * @return true 表示已重算（calcStatus→CALCULATED）；false 表示样本不足未重算
     */
    public boolean recalculate(Long chartId) {
        IEntityDao<ErpQaSpcChart> chartDao = daoProvider.daoFor(ErpQaSpcChart.class);
        ErpQaSpcChart chart = chartDao.getEntityById(chartId);
        if (chart == null) {
            throw new NopException(ErpQaErrors.ERR_QA_SPC_CHART_NOT_FOUND)
                    .param(ErpQaErrors.ARG_CHART_ID, chartId);
        }
        List<ErpQaSpcSample> samples = findSamples(chartId);
        if (samples.size() < ErpQaConstants.SPC_MIN_SUBGROUPS_FOR_CONTROL_LIMIT) {
            // 样本不足，保持 PENDING
            return false;
        }

        BigDecimal cl;
        BigDecimal ucl;
        BigDecimal lcl;
        String chartType = chart.getChartType();
        if (isAttributesChart(chartType)) {
            // 计数型分支：调 AttributesControlLimitFormulas
            AttributesControlLimitFormulas.Result r = calculateAttributes(chartType, samples);
            cl = r.getCl();
            ucl = r.getUcl();
            lcl = r.getLcl();
        } else {
            // 计量型分支：既有 X̄̄-R 范式（默认 chartType==null 时走计量型以保持向后兼容）
            int subgroupSize = chart.getSubgroupSize() == null ? ErpQaConstants.DEFAULT_SPC_SUBGROUP_SIZE : chart.getSubgroupSize();
            BigDecimal d2 = lookupD2(subgroupSize);
            BigDecimal d3 = lookupD3(subgroupSize);
            BigDecimal d4 = lookupD4(subgroupSize);

            // grandMean = mean(sample.mean)
            BigDecimal sumMean = BigDecimal.ZERO;
            BigDecimal sumRange = BigDecimal.ZERO;
            for (ErpQaSpcSample s : samples) {
                if (s.getMean() != null) {
                    sumMean = sumMean.add(s.getMean());
                }
                if (s.getRange() != null) {
                    sumRange = sumRange.add(s.getRange());
                }
            }
            BigDecimal grandMean = sumMean.divide(BigDecimal.valueOf(samples.size()), 10, RoundingMode.HALF_UP);
            BigDecimal averageRange = sumRange.divide(BigDecimal.valueOf(samples.size()), 10, RoundingMode.HALF_UP);

            // cl 按 clCenterType 分支
            cl = resolveCenterLine(chart, grandMean);
            BigDecimal sigmaHat = d2.equals(BigDecimal.ZERO) ? null
                    : averageRange.divide(d2, 10, RoundingMode.HALF_UP);

            if (sigmaHat != null) {
                BigDecimal threeSigma = sigmaHat.multiply(new BigDecimal("3"));
                ucl = cl.add(threeSigma);
                lcl = cl.subtract(threeSigma);
            } else {
                // 异常路径（d2 缺失）：仅置 cl，ucl/lcl 留空
                ucl = null;
                lcl = null;
            }
            // 极差控制限（可选记录于 remark；本实现不持久化 R 控制限字段，仅日志）
            BigDecimal rangeUpper = averageRange.multiply(d4);
            BigDecimal rangeLower = averageRange.multiply(d3);
            // rangeUpper/rangeLower 仅本地变量供未来扩展，不计入 chart 字段
            if (rangeUpper == null) { /* no-op 守门避免 unused warning */ }
            if (rangeLower == null) { /* no-op */ }
        }

        chart.setCl(scale(cl));
        chart.setUcl(scale(ucl));
        chart.setLcl(scale(lcl));
        chart.setCalcStatus(ErpQaConstants.SPC_CALC_STATUS_CALCULATED);
        chartDao.updateEntity(chart);
        return true;
    }

    /** 判定 chartType 是否属计数型（P/NP/C/U）。 */
    public static boolean isAttributesChart(String chartType) {
        return ErpQaConstants.SPC_CHART_TYPE_P.equals(chartType)
                || ErpQaConstants.SPC_CHART_TYPE_NP.equals(chartType)
                || ErpQaConstants.SPC_CHART_TYPE_C.equals(chartType)
                || ErpQaConstants.SPC_CHART_TYPE_U.equals(chartType);
    }

    private AttributesControlLimitFormulas.Result calculateAttributes(String chartType, List<ErpQaSpcSample> samples) {
        List<Integer> defects = new ArrayList<>(samples.size());
        List<Integer> inspected = new ArrayList<>(samples.size());
        for (ErpQaSpcSample s : samples) {
            int d = s.getDefectCount() == null ? 0 : s.getDefectCount();
            int n = s.getInspectedCount() == null ? 0 : s.getInspectedCount();
            defects.add(d);
            inspected.add(n);
        }
        if (ErpQaConstants.SPC_CHART_TYPE_P.equals(chartType)) {
            return AttributesControlLimitFormulas.calcP(defects, inspected);
        }
        if (ErpQaConstants.SPC_CHART_TYPE_NP.equals(chartType)) {
            return AttributesControlLimitFormulas.calcNp(defects, inspected);
        }
        if (ErpQaConstants.SPC_CHART_TYPE_C.equals(chartType)) {
            return AttributesControlLimitFormulas.calcC(defects);
        }
        if (ErpQaConstants.SPC_CHART_TYPE_U.equals(chartType)) {
            return AttributesControlLimitFormulas.calcU(defects, inspected);
        }
        return new AttributesControlLimitFormulas.Result(null, null, null);
    }

    /** R̄/d2 标准差估计（Cp/Cpk 计算复用）。 */
    public BigDecimal estimateWithinStdDev(int subgroupSize, BigDecimal averageRange) {
        if (averageRange == null) {
            return null;
        }
        BigDecimal d2 = lookupD2(subgroupSize);
        if (d2.equals(BigDecimal.ZERO)) {
            return null;
        }
        return averageRange.divide(d2, 10, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveCenterLine(ErpQaSpcChart chart, BigDecimal grandMean) {
        String centerType = chart.getClCenterType();
        if (ErpQaConstants.SPC_CL_CENTER_TARGET.equals(centerType)) {
            if (chart.getSpecMin() != null && chart.getSpecMax() != null) {
                return chart.getSpecMin().add(chart.getSpecMax())
                        .divide(new BigDecimal("2"), 10, RoundingMode.HALF_UP);
            }
            // 规格缺失：回落 grandMean
            return grandMean;
        }
        if (ErpQaConstants.SPC_CL_CENTER_MANUAL.equals(centerType)) {
            return chart.getCl() != null ? chart.getCl() : grandMean;
        }
        // AUTO_FROM_DATA 默认
        return grandMean;
    }

    private List<ErpQaSpcSample> findSamples(Long chartId) {
        IEntityDao<ErpQaSpcSample> dao = daoProvider.daoFor(ErpQaSpcSample.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("chartId", chartId));
        return dao.findAllByQuery(q);
    }

    private BigDecimal lookupD2(int subgroupSize) {
        BigDecimal v = D2.get(subgroupSize);
        if (v == null) {
            // 越界回落 n=10（保守）
            return D2.get(10);
        }
        return v;
    }

    private BigDecimal lookupD3(int subgroupSize) {
        BigDecimal v = D3.get(subgroupSize);
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal lookupD4(int subgroupSize) {
        BigDecimal v = D4.get(subgroupSize);
        return v == null ? D4.get(10) : v;
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }
}
