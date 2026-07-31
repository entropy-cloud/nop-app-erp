package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
import app.erp.crm.dao.entity.ErpCrmLeadFunnel;
import app.erp.crm.dao.entity.ErpCrmLostReason;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.service.ErpCrmConfigs;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.support.FunnelAggregationEngine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * ErpCrmLeadFunnel refreshFunnel per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含销售漏斗刷新编排：清旧重建快照，委托 {@link FunnelAggregationEngine} 聚合 ConvLog + Lead → LeadFunnel 头 + FunnelStageMetrics 明细。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmLeadFunnelRefreshFunnelProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    FunnelAggregationEngine funnelAggregationEngine;

    public ErpCrmLeadFunnel refreshFunnel(LocalDate periodStart,
                                          LocalDate periodEnd,
                                          Long territoryId,
                                          Long teamId,
                                          Long sourceId,
                                          IServiceContext context) {
        if (periodStart != null && periodEnd != null && periodStart.isAfter(periodEnd)) {
            throw new NopException(ErpCrmErrors.ERR_FUNNEL_PERIOD_INVALID)
                    .param(ErpCrmErrors.ARG_PERIOD_START, periodStart)
                    .param(ErpCrmErrors.ARG_PERIOD_END, periodEnd);
        }

        // 清旧：按 periodStart/periodEnd + 维度精确匹配既有 funnel + stage metrics 删除
        clearExistingSnapshots(periodStart, periodEnd, territoryId, teamId, sourceId, context);

        // 加载原始数据
        List<ErpCrmLeadConvLog> convLogs = loadConvLogs(periodStart, periodEnd);
        List<Long> leadIds = convLogs.stream().map(ErpCrmLeadConvLog::getLeadId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        List<ErpCrmLead> leads = loadLeads(leadIds, territoryId, teamId, sourceId);
        List<ErpCrmStage> stages = loadAllStages();
        Map<Long, ErpCrmLostReason> lostReasons = loadLostReasonMap();

        int topLostN = ErpCrmConfigs.funnelTopLostReasons();
        FunnelAggregationEngine.FunnelSnapshot snapshot = funnelAggregationEngine.aggregate(
                periodStart, periodEnd, territoryId, teamId, sourceId,
                convLogs, leads, stages, lostReasons, topLostN);

        // 持久化 LeadFunnel 头
        ErpCrmLeadFunnel funnel = dao().newEntity();
        funnel.setFunnelName(buildFunnelName(periodStart, periodEnd, territoryId, teamId, sourceId));
        funnel.setPeriodStart(periodStart);
        funnel.setPeriodEnd(periodEnd);
        funnel.setTerritoryId(territoryId);
        funnel.setTeamId(teamId);
        funnel.setSourceId(sourceId);
        funnel.setTotalLeadsAtTop(snapshot.getTotalLeadsAtTop());
        funnel.setTotalOpportunities(snapshot.getTotalOpportunities());
        funnel.setTotalWon(snapshot.getTotalWon());
        funnel.setTotalLost(snapshot.getTotalLost());
        funnel.setTotalRevenue(snapshot.getTotalRevenue());
        funnel.setLostRevenue(snapshot.getLostRevenue());
        funnel.setWeightedRevenue(snapshot.getWeightedRevenue());
        funnel.setAvgDealSize(snapshot.getAvgDealSize());
        funnel.setAvgSalesCycleDays(java.math.BigDecimal.valueOf(snapshot.getAvgSalesCycleDays()));
        funnel.setCalculatedAt(snapshot.getCalculatedAt() != null ? Timestamp.valueOf(snapshot.getCalculatedAt()) : null);
        dao().saveEntity(funnel);

        // 持久化 FunnelStageMetrics 明细（upsert by funnelId + stageId）
        for (ErpCrmFunnelStageMetrics m : snapshot.getStageMetrics()) {
            m.setFunnelId(funnel.getId());
            stageMetricsDao().saveEntity(m);
        }
        return funnel;
    }

    // ---------- 内部辅助 ----------

    /**
     * 清理既有快照（重算前的 invalidate 步骤）。
     *
     * <p>实现说明：经 {@code dao().findAllByQuery(q)} 直接查询绕过 findList 管道——本步骤需读取全部匹配行做
     * 级联删除（子表 ErpCrmFunnelStageMetrics + 头表 ErpCrmLeadFunnel），数据权限在
     * 调用方 @BizMutation 入口已校验；同域只读+级联写场景。
     */
    protected void clearExistingSnapshots(LocalDate periodStart, LocalDate periodEnd,
                                          Long territoryId, Long teamId, Long sourceId,
                                          IServiceContext context) {
        QueryBean q = new QueryBean();
        if (periodStart != null) {
            q.addFilter(eq("periodStart", periodStart));
        }
        if (periodEnd != null) {
            q.addFilter(eq("periodEnd", periodEnd));
        }
        if (territoryId != null) {
            q.addFilter(eq("territoryId", territoryId));
        } else {
            q.addFilter(io.nop.api.core.beans.FilterBeans.isNull("territoryId"));
        }
        if (teamId != null) {
            q.addFilter(eq("teamId", teamId));
        } else {
            q.addFilter(io.nop.api.core.beans.FilterBeans.isNull("teamId"));
        }
        if (sourceId != null) {
            q.addFilter(eq("sourceId", sourceId));
        } else {
            q.addFilter(io.nop.api.core.beans.FilterBeans.isNull("sourceId"));
        }
        List<ErpCrmLeadFunnel> existing = dao().findAllByQuery(q);
        for (ErpCrmLeadFunnel f : existing) {
            for (ErpCrmFunnelStageMetrics m : loadStageMetrics(f.getId())) {
                stageMetricsDao().deleteEntity(m);
            }
            dao().deleteEntity(f);
        }
    }

    protected List<ErpCrmLeadConvLog> loadConvLogs(LocalDate periodStart, LocalDate periodEnd) {
        QueryBean q = new QueryBean();
        if (periodStart != null) {
            LocalDateTime from = periodStart.atStartOfDay();
            q.addFilter(ge("changedAt", from));
        }
        if (periodEnd != null) {
            LocalDateTime to = periodEnd.plusDays(1).atStartOfDay();
            q.addFilter(le("changedAt", to));
        }
        return convLogDao().findAllByQuery(q);
    }

    protected List<ErpCrmLead> loadLeads(List<Long> leadIds, Long territoryId, Long teamId, Long sourceId) {
        if (leadIds == null || leadIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("id", leadIds));
        if (territoryId != null) {
            q.addFilter(eq("territoryId", territoryId));
        }
        if (teamId != null) {
            q.addFilter(eq("teamId", teamId));
        }
        if (sourceId != null) {
            q.addFilter(eq("sourceId", sourceId));
        }
        return leadDao().findAllByQuery(q);
    }

    protected List<ErpCrmStage> loadAllStages() {
        QueryBean q = new QueryBean();
        return stageDao().findAllByQuery(q);
    }

    protected Map<Long, ErpCrmLostReason> loadLostReasonMap() {
        List<ErpCrmLostReason> all = lostReasonDao().findAllByQuery(new QueryBean());
        Map<Long, ErpCrmLostReason> map = new HashMap<>();
        for (ErpCrmLostReason r : all) {
            map.put(r.getId(), r);
        }
        return map;
    }

    protected List<ErpCrmFunnelStageMetrics> loadStageMetrics(Long funnelId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("funnelId", funnelId));
        return stageMetricsDao().findAllByQuery(q);
    }

    protected String buildFunnelName(LocalDate periodStart, LocalDate periodEnd,
                                     Long territoryId, Long teamId, Long sourceId) {
        StringBuilder sb = new StringBuilder("Funnel");
        if (periodStart != null || periodEnd != null) {
            sb.append('[').append(periodStart).append('~').append(periodEnd).append(']');
        }
        if (territoryId != null) {
            sb.append(".T").append(territoryId);
        }
        if (teamId != null) {
            sb.append(".Team").append(teamId);
        }
        if (sourceId != null) {
            sb.append(".Src").append(sourceId);
        }
        return sb.toString();
    }

    private IEntityDao<ErpCrmLeadFunnel> dao() {
        return daoProvider.daoFor(ErpCrmLeadFunnel.class);
    }

    protected IEntityDao<ErpCrmLeadConvLog> convLogDao() {
        return daoProvider.daoFor(ErpCrmLeadConvLog.class);
    }

    protected IEntityDao<ErpCrmLead> leadDao() {
        return daoProvider.daoFor(ErpCrmLead.class);
    }

    protected IEntityDao<ErpCrmStage> stageDao() {
        return daoProvider.daoFor(ErpCrmStage.class);
    }

    protected IEntityDao<ErpCrmLostReason> lostReasonDao() {
        return daoProvider.daoFor(ErpCrmLostReason.class);
    }

    protected IEntityDao<ErpCrmFunnelStageMetrics> stageMetricsDao() {
        return daoProvider.daoFor(ErpCrmFunnelStageMetrics.class);
    }
}
