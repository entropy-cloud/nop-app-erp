package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmForecast;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmQuota;
import app.erp.crm.dao.entity.ErpCrmTerritory;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.isNull;

/**
 * 配额层级聚合计算器。
 *
 * <p>对齐 {@code docs/design/crm/territory.md §配额层级汇总 / §业务规则 5 定稿锁定 / §实现注记 4 显式值优先}：
 * <ul>
 *   <li>{@code territoryId=null} 公司级：聚合所有 territory/team/owner 维度配额行；</li>
 *   <li>{@code territoryId≠null} 且 teamId/ownerId=null：聚合该区域子树所有团队/个人配额行；</li>
 *   <li>显式值优先：该层级已直接配置 quotaAmount 则直接返回；否则向下聚合子节点求和。</li>
 * </ul>
 */
public class QuotaRollupCalculator {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 返回 territoryId 子树配额聚合结果。无任何配额行时返回 null（调用方自行构造空对象）。
     * fiscalYear=0 表示不按财年过滤（管道对比入口经 periodLabel 唯一定位）。
     */
    public ErpCrmQuota rollup(Long territoryId, String periodType, int fiscalYear, String periodLabel) {
        // 显式值优先：先查该层级的显式配额行
        QueryBean explicit = new QueryBean();
        if (territoryId == null) {
            explicit.addFilter(isNull("territoryId"));
            explicit.addFilter(isNull("teamId"));
            explicit.addFilter(isNull("ownerId"));
        } else {
            explicit.addFilter(eq("territoryId", territoryId));
            explicit.addFilter(isNull("teamId"));
            explicit.addFilter(isNull("ownerId"));
        }
        explicit.addFilter(eq("periodType", periodType));
        if (fiscalYear > 0) {
            explicit.addFilter(eq("fiscalYear", fiscalYear));
        }
        if (periodLabel != null) {
            explicit.addFilter(eq("periodLabel", periodLabel));
        }
        explicit.setLimit(1);
        ErpCrmQuota explicitQuota = quotaDao().findAllByQuery(explicit).stream().findFirst().orElse(null);
        if (explicitQuota != null && explicitQuota.getQuotaAmount() != null) {
            return explicitQuota;
        }

        // 聚合子节点
        Set<Long> subtreeIds = new HashSet<>();
        if (territoryId != null) {
            collectSubtreeIds(territoryId, subtreeIds);
        }
        QueryBean aggregate = new QueryBean();
        if (territoryId == null) {
            // 公司级：所有配额行都参与聚合（不限制 territoryId）
        } else if (subtreeIds.isEmpty()) {
            // 子树仅含自身（叶子节点）：聚合所有 territoryId=territoryId 的配额行
            aggregate.addFilter(eq("territoryId", territoryId));
        } else {
            // 区域级：聚合子树所有配额行
            aggregate.addFilter(in("territoryId", subtreeIds));
        }
        aggregate.addFilter(eq("periodType", periodType));
        if (fiscalYear > 0) {
            aggregate.addFilter(eq("fiscalYear", fiscalYear));
        }
        if (periodLabel != null) {
            aggregate.addFilter(eq("periodLabel", periodLabel));
        }
        // 聚合时排除当前层级的显式配额行（已尝试过）
        List<ErpCrmQuota> rows = quotaDao().findAllByQuery(aggregate);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpCrmQuota row : rows) {
            if (row.getQuotaAmount() != null
                    && !(territoryId != null && territoryId.equals(row.getTerritoryId())
                    && row.getTeamId() == null && row.getOwnerId() == null)) {
                sum = sum.add(row.getQuotaAmount());
            }
        }
        // 构造虚拟聚合行（不持久化）反映聚合结果。无任何匹配行 → 返回 null，调用方可降级到其他 periodType。
        if (rows.isEmpty() && explicitQuota == null) {
            return null;
        }
        ErpCrmQuota virtual = new ErpCrmQuota();
        virtual.setTerritoryId(territoryId);
        virtual.setPeriodType(periodType);
        virtual.setFiscalYear(fiscalYear);
        virtual.setPeriodLabel(periodLabel);
        virtual.setQuotaAmount(sum);
        virtual.setIsFinalized(Boolean.FALSE);
        return virtual;
    }

    /**
     * 均分年度配额为季（4 行）或月（12 行）子期间配额行。仅 periodType=ANNUAL 可均分；目标配额须未定稿。
     */
    public List<ErpCrmQuota> distributeAnnual(Long quotaId, String targetPeriodType) {
        ErpCrmQuota annual = quotaDao().getEntityById(quotaId);
        if (annual == null) {
            throw new NopException(ErpCrmErrors.ERR_QUOTA_NO_MATCH)
                    .param(ErpCrmErrors.ARG_QUOTA_ID, quotaId);
        }
        if (!ErpCrmConstants.QUOTA_PERIOD_ANNUAL.equals(annual.getPeriodType())) {
            throw new NopException(ErpCrmErrors.ERR_QUOTA_NO_MATCH)
                    .param(ErpCrmErrors.ARG_QUOTA_ID, quotaId)
                    .param(ErpCrmErrors.ARG_PERIOD_TYPE, annual.getPeriodType());
        }
        if (Boolean.TRUE.equals(annual.getIsFinalized())) {
            throw new NopException(ErpCrmErrors.ERR_QUOTA_FINALIZED)
                    .param(ErpCrmErrors.ARG_QUOTA_ID, quotaId);
        }
        String resolvedType = targetPeriodType != null ? targetPeriodType
                : (distributeMonthly() ? ErpCrmConstants.QUOTA_PERIOD_MONTHLY
                : ErpCrmConstants.QUOTA_PERIOD_QUARTERLY);
        int count = ErpCrmConstants.QUOTA_PERIOD_MONTHLY.equals(resolvedType) ? 12 : 4;
        BigDecimal total = annual.getQuotaAmount() != null ? annual.getQuotaAmount() : BigDecimal.ZERO;
        BigDecimal each = total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);

        List<ErpCrmQuota> created = new ArrayList<>();
        int fiscalYear = annual.getFiscalYear() != null ? annual.getFiscalYear() : 0;
        for (int i = 1; i <= count; i++) {
            ErpCrmQuota sub = quotaDao().newEntity();
            sub.setOrgId(annual.getOrgId());
            sub.setTerritoryId(annual.getTerritoryId());
            sub.setTeamId(annual.getTeamId());
            sub.setOwnerId(annual.getOwnerId());
            sub.setPeriodType(resolvedType);
            sub.setFiscalYear(fiscalYear);
            sub.setPeriodLabel(formatPeriodLabel(resolvedType, fiscalYear, i));
            sub.setQuotaAmount(each);
            sub.setCurrencyId(annual.getCurrencyId());
            sub.setIsFinalized(Boolean.FALSE);
            quotaDao().saveEntity(sub);
            created.add(sub);
        }
        return created;
    }

    /**
     * 区域管道对比：聚合 territoryId 子树内 Quota + Forecast + 已转化 Lead 实际收入。
     */
    public ErpCrmPipelineAccumulator accumulatePipeline(Long territoryId, String periodLabel) {
        ErpCrmPipelineAccumulator acc = new ErpCrmPipelineAccumulator();

        // 目标段：取该层级显式配额行（无则子树聚合）
        ErpCrmQuota quota = rollup(territoryId, ErpCrmConstants.QUOTA_PERIOD_MONTHLY, 0, periodLabel);
        if (quota == null && periodLabel != null) {
            quota = rollup(territoryId, ErpCrmConstants.QUOTA_PERIOD_QUARTERLY, 0, periodLabel);
        }
        if (quota == null) {
            quota = rollup(territoryId, ErpCrmConstants.QUOTA_PERIOD_ANNUAL, 0, periodLabel);
        }
        if (quota != null) {
            acc.quotaAmount = nvl(quota.getQuotaAmount());
            acc.quotaFinalized = Boolean.TRUE.equals(quota.getIsFinalized());
            acc.quotaPeriodType = quota.getPeriodType();
        }

        // 预测段：聚合 ErpCrmForecast 按 territoryId（子树）
        Set<Long> subtreeIds = new HashSet<>();
        if (territoryId != null) {
            collectSubtreeIds(territoryId, subtreeIds);
        }
        QueryBean forecastQuery = new QueryBean();
        if (territoryId == null) {
            forecastQuery.addFilter(isNull("territoryId"));
        } else if (!subtreeIds.isEmpty()) {
            forecastQuery.addFilter(in("territoryId", subtreeIds));
        } else {
            forecastQuery.addFilter(eq("territoryId", territoryId));
        }
        for (ErpCrmForecast f : forecastDao().findAllByQuery(forecastQuery)) {
            acc.commitAmount = acc.commitAmount.add(nvl(f.getCommitAmount()));
            acc.upsideAmount = acc.upsideAmount.add(nvl(f.getUpsideAmount()));
            acc.bestCaseAmount = acc.bestCaseAmount.add(nvl(f.getBestCaseAmount()));
            acc.weightedAmount = acc.weightedAmount.add(nvl(f.getWeightedAmount()));
            acc.opportunityCount += f.getOpportunityCount() != null ? f.getOpportunityCount() : 0;
        }

        // 实际段：聚合 territoryId 子树内已 CONVERTED 商机 expectedRevenue
        QueryBean actualQuery = new QueryBean();
        actualQuery.addFilter(eq("docStatus", ErpCrmConstants.DOC_STATUS_CONVERTED));
        if (territoryId == null) {
            actualQuery.addFilter(isNull("territoryId"));
        } else if (!subtreeIds.isEmpty()) {
            actualQuery.addFilter(in("territoryId", subtreeIds));
        } else {
            actualQuery.addFilter(eq("territoryId", territoryId));
        }
        for (ErpCrmLead lead : leadDao().findAllByQuery(actualQuery)) {
            acc.actualRevenue = acc.actualRevenue.add(nvl(lead.getExpectedRevenue()));
            acc.convertedCount++;
        }
        return acc;
    }

    // ---------- 辅助 ----------

    protected void collectSubtreeIds(Long rootId, Set<Long> acc) {
        acc.add(rootId);
        QueryBean q = new QueryBean();
        q.addFilter(eq("parentId", rootId));
        for (ErpCrmTerritory child : territoryDao().findAllByQuery(q)) {
            collectSubtreeIds(child.getId(), acc);
        }
    }

    protected String formatPeriodLabel(String periodType, int fiscalYear, int seq) {
        if (ErpCrmConstants.QUOTA_PERIOD_MONTHLY.equals(periodType)) {
            return String.format("%d-%02d", fiscalYear, seq);
        }
        // quarterly
        return String.format("%d-Q%d", fiscalYear, seq);
    }

    protected boolean distributeMonthly() {
        return io.nop.api.core.config.AppConfig.var(
                ErpCrmConstants.CONFIG_QUOTA_DISTRIBUTE_MONTHLY, Boolean.FALSE);
    }

    protected BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    protected IEntityDao<ErpCrmQuota> quotaDao() {
        return daoProvider.daoFor(ErpCrmQuota.class);
    }

    protected IEntityDao<ErpCrmForecast> forecastDao() {
        return daoProvider.daoFor(ErpCrmForecast.class);
    }

    protected IEntityDao<ErpCrmLead> leadDao() {
        return daoProvider.daoFor(ErpCrmLead.class);
    }

    protected IEntityDao<ErpCrmTerritory> territoryDao() {
        return daoProvider.daoFor(ErpCrmTerritory.class);
    }

    // ---------- 管道累加器（返回 DTO 的中间结构）----------

    public static class ErpCrmPipelineAccumulator {
        public BigDecimal quotaAmount = BigDecimal.ZERO;
        public boolean quotaFinalized;
        public String quotaPeriodType;
        public BigDecimal commitAmount = BigDecimal.ZERO;
        public BigDecimal upsideAmount = BigDecimal.ZERO;
        public BigDecimal bestCaseAmount = BigDecimal.ZERO;
        public BigDecimal weightedAmount = BigDecimal.ZERO;
        public int opportunityCount;
        public BigDecimal actualRevenue = BigDecimal.ZERO;
        public int convertedCount;
    }
}
