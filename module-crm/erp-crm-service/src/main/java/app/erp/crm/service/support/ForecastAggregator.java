package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmForecast;
import app.erp.crm.dao.entity.ErpCrmForecastAccuracy;
import app.erp.crm.dao.entity.ErpCrmForecastLine;
import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 销售预测聚合引擎。{@link #refreshForecast} 按 ownerId 聚合 commit/upside/best-case/weighted →
 * upsert {@link ErpCrmForecast} + 重建 {@link ErpCrmForecastLine}（商机级快照）；
 * {@link #computeAccuracy} 期间关闭后对比预测与实际收入算准确率。
 *
 * <p>层级 rollup：个人（ownerId 非空）→ 团队（teamId 非空、ownerId 空）→ 公司（均为空）。
 * 仅 OPEN 期间可重算；FROZEN/CLOSED 拒绝（抛 {@code ERR_FORECAST_PERIOD_NOT_OPEN}）。
 *
 * <p>对齐 {@code docs/design/crm/sales-forecast.md}（加权管道计算 / commit-upside 分类 / 层级聚合 / 准确率）。
 */
public class ForecastAggregator {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 刷新指定期间的预测：聚合商机 → 写 Forecast + ForecastLine → 层级 rollup。
     * 仅 OPEN 期间可刷新；FROZEN/CLOSED 抛 {@code ERR_FORECAST_PERIOD_NOT_OPEN}。
     */
    public void refreshForecast(Long periodId, IServiceContext context) {
        ErpCrmForecastPeriod period = requirePeriod(periodId);
        requireOpen(period);

        List<ErpCrmLead> opportunities = loadOpportunities(period);
        clearPeriodForecasts(periodId);

        int commitThreshold = commitThreshold();
        int upsideThreshold = upsideThreshold();

        // 个人预测（按 ownerId 分组）
        Map<String, List<ErpCrmLead>> byOwner = new HashMap<>();
        Map<String, Long> ownerTeam = new HashMap<>();
        for (ErpCrmLead opp : opportunities) {
            String owner = opp.getOwnerId();
            if (owner == null) {
                continue;
            }
            byOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(opp);
            if (opp.getTeamId() != null) {
                ownerTeam.putIfAbsent(owner, opp.getTeamId());
            }
        }

        Map<Long, ForecastTotals> teamTotals = new HashMap<>();
        ForecastTotals companyTotals = new ForecastTotals();

        for (Map.Entry<String, List<ErpCrmLead>> entry : byOwner.entrySet()) {
            String owner = entry.getKey();
            List<ErpCrmLead> ownerOpps = entry.getValue();
            Long teamId = ownerTeam.get(owner);

            ForecastTotals totals = ForecastTotals.of(ownerOpps, commitThreshold, upsideThreshold);
            ErpCrmForecast forecast = buildForecast(period, owner, teamId, totals);
            forecastDao().saveEntity(forecast);
            rebuildLines(forecast, ownerOpps, commitThreshold, upsideThreshold);

            if (teamId != null) {
                teamTotals.computeIfAbsent(teamId, k -> new ForecastTotals()).add(totals);
            }
            companyTotals.add(totals);
        }

        // 团队 rollup
        for (Map.Entry<Long, ForecastTotals> entry : teamTotals.entrySet()) {
            ErpCrmForecast teamForecast = buildForecast(period, null, entry.getKey(), entry.getValue());
            forecastDao().saveEntity(teamForecast);
        }

        // 公司 rollup
        ErpCrmForecast companyForecast = buildForecast(period, null, null, companyTotals);
        forecastDao().saveEntity(companyForecast);
    }

    /**
     * 计算期间关闭后的预测准确率。对比每条个人 Forecast 的 commitAmount/upsideAmount 与实际已关闭收入。
     */
    public void computeAccuracy(Long periodId, IServiceContext context) {
        ErpCrmForecastPeriod period = requirePeriod(periodId);
        List<ErpCrmForecast> personalForecasts = loadPersonalForecasts(periodId);
        for (ErpCrmForecast forecast : personalForecasts) {
            BigDecimal actualClosed = sumConvertedRevenue(period, forecast.getOwnerId());
            ErpCrmForecastAccuracy accuracy = buildAccuracy(forecast, period, actualClosed);
            accuracyDao().saveEntity(accuracy);
        }
    }

    // ---------- 预测聚合 ----------

    protected ErpCrmForecast buildForecast(ErpCrmForecastPeriod period, String ownerId, Long teamId,
                                           ForecastTotals totals) {
        ErpCrmForecast forecast = forecastDao().newEntity();
        forecast.setOrgId(period.getOrgId());
        forecast.setPeriodId(period.getId());
        forecast.setOwnerId(ownerId);
        forecast.setTeamId(teamId);
        forecast.setCommitAmount(totals.commitAmount);
        forecast.setUpsideAmount(totals.upsideAmount);
        forecast.setWeightedAmount(totals.weightedAmount);
        forecast.setBestCaseAmount(totals.bestCaseAmount);
        forecast.setOpportunityCount(totals.opportunityCount);
        forecast.setCommitOpportunityCount(totals.commitOpportunityCount);
        forecast.setExpectedClosedRevenue(BigDecimal.ZERO);
        forecast.setLastCalculatedAt(CoreMetrics.currentTimestamp());
        return forecast;
    }

    protected void rebuildLines(ErpCrmForecast forecast, List<ErpCrmLead> opportunities,
                                int commitThreshold, int upsideThreshold) {
        for (ErpCrmLead opp : opportunities) {
            int probability = opp.getProbability() != null ? opp.getProbability() : 0;
            BigDecimal expectedRevenue = nvl(opp.getExpectedRevenue());
            BigDecimal weighted = expectedRevenue.multiply(BigDecimal.valueOf(probability))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            String category = classifyCategory(probability, commitThreshold, upsideThreshold);
            boolean inCommit = probability >= commitThreshold;

            ErpCrmForecastLine line = lineDao().newEntity();
            line.setForecastId(forecast.getId());
            line.setOrgId(forecast.getOrgId());
            line.setLeadId(opp.getId());
            line.setProbability(probability);
            line.setExpectedRevenue(expectedRevenue);
            line.setWeightedRevenue(weighted);
            line.setForecastCategory(category);
            line.setIncludedInCommit(inCommit);
            line.setStageName(resolveStageName(opp));
            lineDao().saveEntity(line);
        }
    }

    protected String classifyCategory(int probability, int commitThreshold, int upsideThreshold) {
        if (probability >= commitThreshold) {
            return ErpCrmConstants.FORECAST_CATEGORY_COMMIT;
        }
        if (probability >= upsideThreshold) {
            return ErpCrmConstants.FORECAST_CATEGORY_UPSIDE;
        }
        return ErpCrmConstants.FORECAST_CATEGORY_BEST_CASE;
    }

    protected String resolveStageName(ErpCrmLead opp) {
        return null;
    }

    // ---------- 准确率 ----------

    protected ErpCrmForecastAccuracy buildAccuracy(ErpCrmForecast forecast, ErpCrmForecastPeriod period,
                                                    BigDecimal actualClosed) {
        BigDecimal commit = nvl(forecast.getCommitAmount());
        BigDecimal upside = nvl(forecast.getUpsideAmount());
        BigDecimal actual = nvl(actualClosed);

        BigDecimal commitAccuracy = accuracyOf(commit, actual);
        BigDecimal upsideAccuracy = accuracyOf(upside, actual);
        BigDecimal deviation = commit.subtract(actual).abs();

        ErpCrmForecastAccuracy accuracy = accuracyDao().newEntity();
        accuracy.setForecastId(forecast.getId());
        accuracy.setOrgId(forecast.getOrgId());
        accuracy.setPeriodId(period.getId());
        accuracy.setOwnerId(forecast.getOwnerId());
        accuracy.setTeamId(forecast.getTeamId());
        accuracy.setCommitAmount(commit);
        accuracy.setUpsideAmount(upside);
        accuracy.setActualClosedRevenue(actual);
        accuracy.setCommitAccuracy(commitAccuracy.doubleValue());
        accuracy.setUpsideAccuracy(upsideAccuracy.doubleValue());
        accuracy.setDeviationAmount(deviation);
        accuracy.setCalculatedAt(CoreMetrics.currentTimestamp());
        return accuracy;
    }

    /**
     * 准确率 = 1 - |预测 - 实际| / MAX(预测, 实际)。预测与实际均为 0 时返回 1。
     */
    protected BigDecimal accuracyOf(BigDecimal forecast, BigDecimal actual) {
        BigDecimal max = forecast.max(actual);
        if (max.signum() <= 0) {
            return BigDecimal.ONE;
        }
        BigDecimal deviation = forecast.subtract(actual).abs();
        return BigDecimal.ONE.subtract(deviation.divide(max, 4, RoundingMode.HALF_UP));
    }

    // ---------- 数据加载 ----------

    protected List<ErpCrmLead> loadOpportunities(ErpCrmForecastPeriod period) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadType", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY));
        q.addFilter(eq("docStatus", ErpCrmConstants.DOC_STATUS_QUALIFIED));
        LocalDate start = period.getPeriodStart();
        LocalDate end = period.getPeriodEnd();
        if (start != null) {
            q.addFilter(ge("expectedCloseDate", start));
        }
        if (end != null) {
            q.addFilter(le("expectedCloseDate", end));
        }
        return leadDao().findAllByQuery(q);
    }

    protected List<ErpCrmForecast> loadPersonalForecasts(Long periodId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        return forecastDao().findAllByQuery(q).stream()
                .filter(f -> f.getOwnerId() != null)
                .collect(java.util.stream.Collectors.toList());
    }

    protected BigDecimal sumConvertedRevenue(ErpCrmForecastPeriod period, String ownerId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadType", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY));
        q.addFilter(eq("docStatus", ErpCrmConstants.DOC_STATUS_CONVERTED));
        if (ownerId != null) {
            q.addFilter(eq("ownerId", ownerId));
        }
        LocalDate start = period.getPeriodStart();
        LocalDate end = period.getPeriodEnd();
        if (start != null) {
            q.addFilter(ge("expectedCloseDate", start));
        }
        if (end != null) {
            q.addFilter(le("expectedCloseDate", end));
        }
        return leadDao().findAllByQuery(q).stream()
                .map(l -> nvl(l.getExpectedRevenue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected void clearPeriodForecasts(Long periodId) {
        List<ErpCrmForecast> existing = forecastDao().findAllByQuery(byPeriod(periodId));
        for (ErpCrmForecast forecast : existing) {
            List<ErpCrmForecastLine> lines = lineDao().findAllByQuery(byForecast(forecast.getId()));
            for (ErpCrmForecastLine line : lines) {
                lineDao().deleteEntity(line);
            }
            forecastDao().deleteEntity(forecast);
        }
    }

    // ---------- 校验/辅助 ----------

    protected ErpCrmForecastPeriod requirePeriod(Long periodId) {
        ErpCrmForecastPeriod period = periodDao().getEntityById(periodId);
        if (period == null) {
            throw new NopException(ErpCrmErrors.ERR_FORECAST_PERIOD_NOT_FOUND)
                    .param(ErpCrmErrors.ARG_PERIOD_ID, periodId);
        }
        return period;
    }

    protected void requireOpen(ErpCrmForecastPeriod period) {
        if (!Objects.equals(period.getStatus(), ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN)) {
            throw new NopException(ErpCrmErrors.ERR_FORECAST_PERIOD_NOT_OPEN)
                    .param(ErpCrmErrors.ARG_PERIOD_ID, period.getId())
                    .param(ErpCrmErrors.ARG_CURRENT_STATUS, period.getStatus())
                    .param(ErpCrmErrors.ARG_EXPECTED_STATUS, ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN);
        }
    }

    protected int commitThreshold() {
        return io.nop.api.core.config.AppConfig.var(
                ErpCrmConstants.CONFIG_FORECAST_COMMIT_THRESHOLD, 80);
    }

    protected int upsideThreshold() {
        return io.nop.api.core.config.AppConfig.var(
                ErpCrmConstants.CONFIG_FORECAST_UPSIDE_THRESHOLD, 30);
    }

    protected BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    protected QueryBean byPeriod(Long periodId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        return q;
    }

    protected QueryBean byForecast(Long forecastId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("forecastId", forecastId));
        return q;
    }

    protected IEntityDao<ErpCrmLead> leadDao() {
        return daoProvider.daoFor(ErpCrmLead.class);
    }

    protected IEntityDao<ErpCrmForecast> forecastDao() {
        return daoProvider.daoFor(ErpCrmForecast.class);
    }

    protected IEntityDao<ErpCrmForecastLine> lineDao() {
        return daoProvider.daoFor(ErpCrmForecastLine.class);
    }

    protected IEntityDao<ErpCrmForecastPeriod> periodDao() {
        return daoProvider.daoFor(ErpCrmForecastPeriod.class);
    }

    protected IEntityDao<ErpCrmForecastAccuracy> accuracyDao() {
        return daoProvider.daoFor(ErpCrmForecastAccuracy.class);
    }

    // ---------- 聚合累加器 ----------

    protected static class ForecastTotals {
        BigDecimal commitAmount = BigDecimal.ZERO;
        BigDecimal upsideAmount = BigDecimal.ZERO;
        BigDecimal weightedAmount = BigDecimal.ZERO;
        BigDecimal bestCaseAmount = BigDecimal.ZERO;
        int opportunityCount;
        int commitOpportunityCount;

        static ForecastTotals of(List<ErpCrmLead> opps, int commitThreshold, int upsideThreshold) {
            ForecastTotals t = new ForecastTotals();
            for (ErpCrmLead opp : opps) {
                t.add(opp, commitThreshold, upsideThreshold);
            }
            return t;
        }

        void add(ErpCrmLead opp, int commitThreshold, int upsideThreshold) {
            BigDecimal revenue = opp.getExpectedRevenue() != null ? opp.getExpectedRevenue() : BigDecimal.ZERO;
            int probability = opp.getProbability() != null ? opp.getProbability() : 0;
            opportunityCount++;
            bestCaseAmount = bestCaseAmount.add(revenue);
            weightedAmount = weightedAmount.add(
                    revenue.multiply(BigDecimal.valueOf(probability))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            if (probability >= commitThreshold) {
                commitAmount = commitAmount.add(revenue);
                commitOpportunityCount++;
            } else if (probability >= upsideThreshold) {
                upsideAmount = upsideAmount.add(revenue);
            }
        }

        void add(ForecastTotals other) {
            commitAmount = commitAmount.add(other.commitAmount);
            upsideAmount = upsideAmount.add(other.upsideAmount);
            weightedAmount = weightedAmount.add(other.weightedAmount);
            bestCaseAmount = bestCaseAmount.add(other.bestCaseAmount);
            opportunityCount += other.opportunityCount;
            commitOpportunityCount += other.commitOpportunityCount;
        }
    }
}
