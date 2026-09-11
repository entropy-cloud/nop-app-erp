package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmForecast;
import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
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

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpCrmForecast、ErpCrmLead、ErpCrmQuota、ErpCrmTerritory）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
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
    public ErpCrmQuota rollup(String territoryId, String periodType, int fiscalYear, String periodLabel) {
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
        Set<String> subtreeIds = new HashSet<>();
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
    public List<ErpCrmQuota> distributeAnnual(String quotaId, String targetPeriodType) {
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
    public ErpCrmPipelineAccumulator accumulatePipeline(String territoryId, String periodLabel) {
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

        // 预测段：聚合 ErpCrmForecast 按 territoryId（子树）。
        // P1-CK-crm-001 ①：公司级（territoryId=null）聚合全部行（对齐配额段 rollup(null) 全行语义，
        // 三段口径统一）；③ 期间过滤：periodLabel → ErpCrmForecastPeriod.id 集 → in(periodId, ids)
        // （Forecast 无 periodLabel 列，经期间表两步解析）。
        Set<String> subtreeIds = new HashSet<>();
        if (territoryId != null) {
            collectSubtreeIds(territoryId, subtreeIds);
        }
        QueryBean forecastQuery = new QueryBean();
        if (territoryId != null) {
            if (!subtreeIds.isEmpty()) {
                forecastQuery.addFilter(in("territoryId", subtreeIds));
            } else {
                forecastQuery.addFilter(eq("territoryId", territoryId));
            }
        }
        if (periodLabel != null) {
            List<String> periodIds = new ArrayList<>();
            for (ErpCrmForecastPeriod p : forecastPeriodDao().findAllByQuery(
                    new QueryBean().addFilter(eq("label", periodLabel)))) {
                periodIds.add(p.getId());
            }
            if (periodIds.isEmpty()) {
                return acc;
            }
            forecastQuery.addFilter(in("periodId", periodIds));
        }
        for (ErpCrmForecast f : forecastDao().findAllByQuery(forecastQuery)) {
            acc.commitAmount = acc.commitAmount.add(nvl(f.getCommitAmount()));
            acc.upsideAmount = acc.upsideAmount.add(nvl(f.getUpsideAmount()));
            acc.bestCaseAmount = acc.bestCaseAmount.add(nvl(f.getBestCaseAmount()));
            acc.weightedAmount = acc.weightedAmount.add(nvl(f.getWeightedAmount()));
            acc.opportunityCount += f.getOpportunityCount() != null ? f.getOpportunityCount() : 0;
        }

        // 实际段：已 CONVERTED 商机 expectedRevenue。
        // P1-CK-crm-001 ②：leadType=OPPORTUNITY 过滤（convertToCustomer 链原 LEAD + 新建 OPPORTUNITY
        // 双 CONVERTED 双额，仅计商机消除双计）；① 公司级聚合全部行；③ 期间过滤 = 期间内有 ConvLog
        // 事件（丢失/转化事件自 plan 2026-09-11-2350-1 Phase 3 起写 ConvLog）——近义口径，精确转化
        // 时间轴归 P2-CK-crm2-003 successor。
        QueryBean actualQuery = new QueryBean();
        actualQuery.addFilter(eq("docStatus", ErpCrmConstants.DOC_STATUS_CONVERTED));
        actualQuery.addFilter(eq("leadType", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY));
        if (territoryId != null) {
            if (!subtreeIds.isEmpty()) {
                actualQuery.addFilter(in("territoryId", subtreeIds));
            } else {
                actualQuery.addFilter(eq("territoryId", territoryId));
            }
        }
        if (periodLabel != null) {
            java.time.LocalDate pStart = parsePeriodLabelStart(periodLabel);
            java.time.LocalDate pEnd = parsePeriodLabelEnd(periodLabel);
            QueryBean logQuery = new QueryBean();
            logQuery.addFilter(io.nop.api.core.beans.FilterBeans.ge("changedAt", java.sql.Timestamp.valueOf(pStart.atStartOfDay())));
            logQuery.addFilter(io.nop.api.core.beans.FilterBeans.le("changedAt", java.sql.Timestamp.valueOf(pEnd.atTime(23, 59, 59))));
            List<String> activeLeadIds = new ArrayList<>();
            for (ErpCrmLeadConvLog lg : convLogDao().findAllByQuery(logQuery)) {
                if (lg.getLeadId() != null) {
                    activeLeadIds.add(lg.getLeadId());
                }
            }
            if (activeLeadIds.isEmpty()) {
                return acc;
            }
            actualQuery.addFilter(in("id", activeLeadIds));
        }
        for (ErpCrmLead lead : leadDao().findAllByQuery(actualQuery)) {
            acc.actualRevenue = acc.actualRevenue.add(nvl(lead.getExpectedRevenue()));
            acc.convertedCount++;
        }
        return acc;
    }

    // ---------- 辅助 ----------

    protected void collectSubtreeIds(String rootId, Set<String> acc) {
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

    protected IEntityDao<ErpCrmForecastPeriod> forecastPeriodDao() {
        return daoProvider.daoFor(ErpCrmForecastPeriod.class);
    }

    protected IEntityDao<ErpCrmLeadConvLog> convLogDao() {
        return daoProvider.daoFor(ErpCrmLeadConvLog.class);
    }

    /** periodLabel（月度 yyyy-MM / 季度 yyyy-Qn，对齐 formatPeriodLabel）→ 期间首日。 */
    static java.time.LocalDate parsePeriodLabelStart(String periodLabel) {
        String[] parts = periodLabel.split("-");
        int year = Integer.parseInt(parts[0]);
        if (parts[1].startsWith("Q")) {
            int q = Integer.parseInt(parts[1].substring(1));
            return java.time.LocalDate.of(year, q * 3 - 2, 1);
        }
        return java.time.LocalDate.of(year, Integer.parseInt(parts[1]), 1);
    }

    /** periodLabel → 期间末日。 */
    static java.time.LocalDate parsePeriodLabelEnd(String periodLabel) {
        String[] parts = periodLabel.split("-");
        int year = Integer.parseInt(parts[0]);
        if (parts[1].startsWith("Q")) {
            int q = Integer.parseInt(parts[1].substring(1));
            java.time.LocalDate end = java.time.LocalDate.of(year, q * 3, 1);
            return end.withDayOfMonth(end.lengthOfMonth());
        }
        java.time.LocalDate start = parsePeriodLabelStart(periodLabel);
        return start.withDayOfMonth(start.lengthOfMonth());
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
