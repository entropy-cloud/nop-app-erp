package app.erp.qa.service.spc;

import app.erp.qa.biz.IErpQaQualityGoalBiz;
import app.erp.qa.biz.IErpQaRiskRegisterBiz;
import app.erp.qa.dao.entity.ErpQaQualityGoal;
import app.erp.qa.dao.entity.ErpQaRiskRegister;
import app.erp.qa.dao.entity.ErpQaSpcCapability;
import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * SPC 过程能力分析引擎（{@code docs/design/quality/spc.md §关键流程 4}，plan 2026-07-07-0305-2 Phase 4）。
 *
 * <p>对指定 chart 计算 {@link ErpQaSpcCapability}（grandMean/overallStdDev/withinStdDev=R̄/d2/
 * Cp=(USL−LSL)/6σ̂/Cpk=min((USL−X̄̄),(X̄̄−LSL))/3σ̂/Pp/Ppk/Cpm + capabilityLevel 按 Cpk 阈值分档：
 * <ul>
 *   <li>INADEQUATE: Cpk < 1.0</li>
 *   <li>ACCEPTABLE: 1.0 ≤ Cpk < 1.33</li>
 *   <li>CAPABLE: 1.33 ≤ Cpk < 1.67</li>
 *   <li>EXCELLENT: Cpk ≥ 1.67</li>
 * </ul>
 *
 * <p>等级 < ACCEPTABLE（即 INADEQUATE）触发：
 * <ol>
 *   <li>回写 {@link IErpQaQualityGoalBiz#update}（currentValue=Cpk）—— 仅当 chart 关联的质量目标存在。</li>
 *   <li>登记 {@link IErpQaRiskRegisterBiz}（新增风险登记，category=SPC_PROCESS_CAPABILITY）。</li>
 * </ol>
 */
public class SpcCapabilityCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(SpcCapabilityCalculator.class);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    SpcControlLimitCalculator controlLimitCalculator;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setControlLimitCalculator(SpcControlLimitCalculator controlLimitCalculator) {
        this.controlLimitCalculator = controlLimitCalculator;
    }

    /**
     * 计算 chart 在指定周期内的过程能力。返回新创建的 ErpQaSpcCapability（已持久化）。
     */
    public ErpQaSpcCapability calculateCapability(Long chartId, LocalDate periodFrom, LocalDate periodTo,
                                                   IServiceContext context) {
        IEntityDao<ErpQaSpcChart> chartDao = daoProvider.daoFor(ErpQaSpcChart.class);
        ErpQaSpcChart chart = chartDao.getEntityById(chartId);
        if (chart == null) {
            throw new NopException(ErpQaErrors.ERR_QA_SPC_CHART_NOT_FOUND)
                    .param(ErpQaErrors.ARG_CHART_ID, chartId);
        }
        List<ErpQaSpcSample> samples = findSamplesInRange(chartId, periodFrom, periodTo);
        if (samples.isEmpty()) {
            return null;
        }

        // 收集所有 measuredValues 平铺成单值序列
        List<BigDecimal> allValues = new ArrayList<>();
        List<BigDecimal> sampleMeans = new ArrayList<>();
        List<BigDecimal> sampleRanges = new ArrayList<>();
        for (ErpQaSpcSample s : samples) {
            if (s.getMean() != null) sampleMeans.add(s.getMean());
            if (s.getRange() != null) sampleRanges.add(s.getRange());
            if (!Objects.equals(s.getIsOutOfControl(), Boolean.TRUE)) {
                allValues.addAll(parseValues(s.getMeasuredValues()));
            }
        }
        if (allValues.isEmpty()) {
            return null;
        }

        // grandMean = mean(allValues)
        BigDecimal grandMean = Statistics.mean(allValues);
        BigDecimal overallStdDev = Statistics.populationStdDev(allValues);
        // withinStdDev = R̄/d2
        int subgroupSize = chart.getSubgroupSize() == null ? ErpQaConstants.DEFAULT_SPC_SUBGROUP_SIZE : chart.getSubgroupSize();
        BigDecimal averageRange = sampleRanges.isEmpty() ? null
                : Statistics.mean(sampleRanges);
        BigDecimal withinStdDev = controlLimitCalculator.estimateWithinStdDev(subgroupSize, averageRange);

        BigDecimal usl = chart.getSpecMax();
        BigDecimal lsl = chart.getSpecMin();
        BigDecimal cp = computeCp(usl, lsl, withinStdDev);
        BigDecimal cpk = computeCpk(usl, lsl, grandMean, withinStdDev);
        BigDecimal pp = computeCp(usl, lsl, overallStdDev);
        BigDecimal ppk = computeCpk(usl, lsl, grandMean, overallStdDev);
        BigDecimal cpm = computeCpm(usl, lsl, grandMean, overallStdDev);
        String capabilityLevel = classifyByCpk(cpk);
        boolean isStable = samples.stream().noneMatch(s -> Objects.equals(s.getIsOutOfControl(), Boolean.TRUE));

        IEntityDao<ErpQaSpcCapability> capDao = daoProvider.daoFor(ErpQaSpcCapability.class);
        ErpQaSpcCapability cap = capDao.newEntity();
        cap.setChartId(chartId);
        cap.setOrgId(chart.getOrgId());
        cap.setPeriodFrom(periodFrom);
        cap.setPeriodTo(periodTo);
        cap.setSampleCount(samples.size());
        cap.setTotalObservations(allValues.size());
        cap.setGrandMean(scale(grandMean));
        cap.setOverallStdDev(scale(overallStdDev));
        cap.setWithinStdDev(scale(withinStdDev));
        cap.setCp(scale(cp));
        cap.setCpk(scale(cpk));
        cap.setPp(scale(pp));
        cap.setPpk(scale(ppk));
        cap.setCpm(scale(cpm));
        cap.setCapabilityLevel(capabilityLevel);
        cap.setIsStable(isStable);
        cap.setCalculatedAt(CoreMetrics.currentDateTime());
        capDao.saveEntity(cap);

        // 等级 < ACCEPTABLE（INADEQUATE）触发 QualityGoal 回写 + RiskRegister 登记
        if (ErpQaConstants.SPC_CAPABILITY_INADEQUATE.equals(capabilityLevel)) {
            try {
                writeBackQualityGoal(chart, cpk, context);
            } catch (Exception e) {
                LOG.warn("spc-capability-quality-goal-writeback-failed: chartId={}", chartId, e);
            }
            try {
                registerRisk(chart, cpk, capabilityLevel, context);
            } catch (Exception e) {
                LOG.warn("spc-capability-risk-register-failed: chartId={}", chartId, e);
            }
        }
        return cap;
    }

    private List<ErpQaSpcSample> findSamplesInRange(Long chartId, LocalDate periodFrom, LocalDate periodTo) {
        IEntityDao<ErpQaSpcSample> dao = daoProvider.daoFor(ErpQaSpcSample.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("chartId", chartId));
        if (periodFrom != null) {
            q.addFilter(io.nop.api.core.beans.FilterBeans.ge("sampleTime", periodFrom.atStartOfDay()));
        }
        if (periodTo != null) {
            q.addFilter(io.nop.api.core.beans.FilterBeans.lt("sampleTime", periodTo.plusDays(1).atStartOfDay()));
        }
        q.addOrderField("subgroupNo", false);
        return dao.findAllByQuery(q);
    }

    private List<BigDecimal> parseValues(String measuredValues) {
        if (measuredValues == null || measuredValues.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            @SuppressWarnings("unchecked")
            List<Object> list = io.nop.core.lang.json.JsonTool.parseBeanFromText(measuredValues, List.class);
            List<BigDecimal> result = new ArrayList<>(list.size());
            for (Object o : list) {
                if (o == null) continue;
                try {
                    result.add(new BigDecimal(o.toString()));
                } catch (NumberFormatException ignored) {
                }
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    static BigDecimal computeCp(BigDecimal usl, BigDecimal lsl, BigDecimal sigma) {
        if (usl == null || lsl == null || sigma == null || sigma.signum() == 0) {
            return null;
        }
        return usl.subtract(lsl).divide(sigma.multiply(new BigDecimal("6")), 6, RoundingMode.HALF_UP);
    }

    static BigDecimal computeCpk(BigDecimal usl, BigDecimal lsl, BigDecimal mean, BigDecimal sigma) {
        if (usl == null || lsl == null || mean == null || sigma == null || sigma.signum() == 0) {
            return null;
        }
        BigDecimal upper = usl.subtract(mean).divide(sigma.multiply(new BigDecimal("3")), 6, RoundingMode.HALF_UP);
        BigDecimal lower = mean.subtract(lsl).divide(sigma.multiply(new BigDecimal("3")), 6, RoundingMode.HALF_UP);
        return upper.min(lower);
    }

    /** Cpm = (USL−LSL) / (6 * sqrt(σ² + (μ−T)²))，T = (USL+LSL)/2。 */
    static BigDecimal computeCpm(BigDecimal usl, BigDecimal lsl, BigDecimal mean, BigDecimal sigma) {
        if (usl == null || lsl == null || mean == null || sigma == null || sigma.signum() == 0) {
            return null;
        }
        BigDecimal target = usl.add(lsl).divide(new BigDecimal("2"), 10, RoundingMode.HALF_UP);
        BigDecimal drift = mean.subtract(target);
        BigDecimal variance = sigma.multiply(sigma).add(drift.multiply(drift));
        BigDecimal denom = new BigDecimal("6").multiply(BigDecimal.valueOf(Math.sqrt(variance.doubleValue())));
        if (denom.signum() == 0) {
            return null;
        }
        return usl.subtract(lsl).divide(denom, 6, RoundingMode.HALF_UP);
    }

    /** 按 Cpk 阈值分档：INADEQUATE<1.0 / ACCEPTABLE 1.0-1.33 / CAPABLE 1.33-1.67 / EXCELLENT>1.67。 */
    static String classifyByCpk(BigDecimal cpk) {
        if (cpk == null) {
            return ErpQaConstants.SPC_CAPABILITY_INADEQUATE;
        }
        double v = cpk.doubleValue();
        if (v < 1.0) {
            return ErpQaConstants.SPC_CAPABILITY_INADEQUATE;
        }
        if (v < 1.33) {
            return ErpQaConstants.SPC_CAPABILITY_ACCEPTABLE;
        }
        if (v < 1.67) {
            return ErpQaConstants.SPC_CAPABILITY_CAPABLE;
        }
        return ErpQaConstants.SPC_CAPABILITY_EXCELLENT;
    }

    private void writeBackQualityGoal(ErpQaSpcChart chart, BigDecimal cpk, IServiceContext context) {
        // chart.code 作为质量目标 code 关联（约定：同名 qualityGoal.code 反查）
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", chart.getCode()));
        q.setLimit(1);
        List<ErpQaQualityGoal> goals = daoProvider.daoFor(ErpQaQualityGoal.class).findAllByQuery(q);
        if (goals.isEmpty()) {
            return;
        }
        ErpQaQualityGoal goal = goals.get(0);
        goal.setCurrentValue(cpk == null ? null : cpk.setScale(4, RoundingMode.HALF_UP));
        daoProvider.daoFor(ErpQaQualityGoal.class).updateEntity(goal);
    }

    private void registerRisk(ErpQaSpcChart chart, BigDecimal cpk, String capabilityLevel,
                              IServiceContext context) {
        IEntityDao<ErpQaRiskRegister> dao = daoProvider.daoFor(ErpQaRiskRegister.class);
        ErpQaRiskRegister risk = dao.newEntity();
        risk.setCode("RISK-SPC-" + chart.getCode() + "-" + CoreMetrics.today().toString().replace("-", ""));
        risk.setRiskDate(CoreMetrics.today());
        risk.setDescription("SPC 过程能力不足：chart=" + chart.getCode() + " Cpk=" + cpk
                + " level=" + capabilityLevel + "（<ACCEPTABLE），需调查并改进过程");
        risk.setCategory("SPC_PROCESS_CAPABILITY");
        risk.setLikelihood(3);
        risk.setSeverity(4);
        risk.setRiskScore(12);
        risk.setStatus("OPEN");
        dao.saveEntity(risk);
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }
}
