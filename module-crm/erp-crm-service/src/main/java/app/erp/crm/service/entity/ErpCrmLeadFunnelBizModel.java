
package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmLeadBiz;
import app.erp.crm.biz.IErpCrmLeadFunnelBiz;
import app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
import app.erp.crm.dao.entity.ErpCrmLeadFunnel;
import app.erp.crm.dao.entity.ErpCrmLostReason;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.service.ErpCrmConfigs;
import app.erp.crm.service.support.FunnelAggregationEngine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;
import java.util.Collections;

/**
 * 销售漏斗 BizModel。{@link #refreshFunnel} 清旧重建快照（对齐 0700-1 forecast），委托 {@link FunnelAggregationEngine}
 * 聚合 ConvLog + Lead → LeadFunnel 头 + FunnelStageMetrics 明细；{@link #getFunnelView} 返回可视化数据结构。
 *
 * <p>对齐 {@code docs/design/crm/lead-waterfall.md}（聚合计算流程 / 漏斗可视化数据结构）。
 */
@BizModel("ErpCrmLeadFunnel")
public class ErpCrmLeadFunnelBizModel extends CrudBizModel<ErpCrmLeadFunnel> implements IErpCrmLeadFunnelBiz {

    @Inject
    FunnelAggregationEngine funnelAggregationEngine;
    @Inject
    IErpCrmLeadBiz leadBiz;

    public ErpCrmLeadFunnelBizModel() {
        setEntityName(ErpCrmLeadFunnel.class.getName());
    }

    @Override
    @BizMutation
    public ErpCrmLeadFunnel refreshFunnel(@Name("periodStart") LocalDate periodStart,
                                           @Name("periodEnd") LocalDate periodEnd,
                                           @Optional @Name("territoryId") Long territoryId,
                                           @Optional @Name("teamId") Long teamId,
                                           @Optional @Name("sourceId") Long sourceId,
                                           IServiceContext context) {
        if (periodStart != null && periodEnd != null && periodStart.isAfter(periodEnd)) {
            throw new NopException(app.erp.crm.service.ErpCrmErrors.ERR_FUNNEL_PERIOD_INVALID)
                    .param(app.erp.crm.service.ErpCrmErrors.ARG_PERIOD_START, periodStart)
                    .param(app.erp.crm.service.ErpCrmErrors.ARG_PERIOD_END, periodEnd);
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
        ErpCrmLeadFunnel funnel = newEntity();
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
        funnel.setAvgSalesCycleDays(snapshot.getAvgSalesCycleDays());
        funnel.setCalculatedAt(snapshot.getCalculatedAt() != null ? Timestamp.valueOf(snapshot.getCalculatedAt()) : null);
        saveEntity(funnel, null, context);

        // 持久化 FunnelStageMetrics 明细（upsert by funnelId + stageId）
        for (ErpCrmFunnelStageMetrics m : snapshot.getStageMetrics()) {
            m.setFunnelId(funnel.getId());
            stageMetricsDao().saveEntity(m);
        }
        return funnel;
    }

    @Override
    @BizQuery
    public Map<String, Object> getFunnelView(@Name("funnelId") Long funnelId, IServiceContext context) {
        ErpCrmLeadFunnel funnel = requireEntity(String.valueOf(funnelId), null, context);
        List<ErpCrmFunnelStageMetrics> stages = loadStageMetrics(funnelId);
        stages.sort(Comparator
                .comparingInt((ErpCrmFunnelStageMetrics s) ->
                        s.getStageOrder() != null ? s.getStageOrder() : Integer.MAX_VALUE)
                .thenComparing(s -> s.getStageId() != null ? s.getStageId() : Long.MAX_VALUE));

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("funnelId", funnel.getId());
        view.put("funnelName", funnel.getFunnelName());
        view.put("periodStart", funnel.getPeriodStart());
        view.put("periodEnd", funnel.getPeriodEnd());
        view.put("territoryId", funnel.getTerritoryId());
        view.put("teamId", funnel.getTeamId());
        view.put("sourceId", funnel.getSourceId());
        view.put("totalLeadsAtTop", funnel.getTotalLeadsAtTop());
        view.put("totalOpportunities", funnel.getTotalOpportunities());
        view.put("totalWon", funnel.getTotalWon());
        view.put("totalLost", funnel.getTotalLost());
        view.put("totalRevenue", funnel.getTotalRevenue());
        view.put("lostRevenue", funnel.getLostRevenue());
        view.put("weightedRevenue", funnel.getWeightedRevenue());
        view.put("avgDealSize", funnel.getAvgDealSize());
        view.put("avgSalesCycleDays", funnel.getAvgSalesCycleDays());
        view.put("calculatedAt", funnel.getCalculatedAt());

        List<Map<String, Object>> stageList = new ArrayList<>();
        for (ErpCrmFunnelStageMetrics s : stages) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stageId", s.getStageId());
            row.put("stageOrder", s.getStageOrder());
            row.put("stageName", s.getStageName());
            row.put("leadCountIn", s.getLeadCountIn());
            row.put("leadCountOut", s.getLeadCountOut());
            row.put("leadCountRemaining", s.getLeadCountRemaining());
            row.put("conversionRate", s.getConversionRate());
            row.put("dropOffRate", s.getDropOffRate());
            row.put("avgDaysInStage", s.getAvgDaysInStage());
            row.put("lostCount", s.getLostCount());
            row.put("lostAmount", s.getLostAmount());
            row.put("lostReasonTop", s.getLostReasonTop());
            stageList.add(row);
        }
        view.put("stages", stageList);
        return view;
    }

    // ---------- 内部辅助 ----------

    /**
     * 清理既有快照（重算前的 invalidate 步骤）。
     *
     * <p>实现说明：经 {@code dao().findAllByQuery(q)} 直接查询绕过 findList 管道——本步骤需读取全部匹配行做
     * 级联删除（子表 ErpCrmFunnelStageMetrics + 头表 ErpCrmLeadFunnel），数据权限在
     * 调用方 @BizMutation 入口已校验；同域只读+级联写场景。M-6（plan 2026-07-20-2200-1）补注释。
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
            deleteEntity(f, null, context);
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

    protected IEntityDao<ErpCrmLeadConvLog> convLogDao() {
        return daoProvider().daoFor(ErpCrmLeadConvLog.class);
    }

    protected IEntityDao<ErpCrmLead> leadDao() {
        return daoProvider().daoFor(ErpCrmLead.class);
    }

    protected IEntityDao<ErpCrmStage> stageDao() {
        return daoProvider().daoFor(ErpCrmStage.class);
    }

    protected IEntityDao<ErpCrmLostReason> lostReasonDao() {
        return daoProvider().daoFor(ErpCrmLostReason.class);
    }

    protected IEntityDao<ErpCrmFunnelStageMetrics> stageMetricsDao() {
        return daoProvider().daoFor(ErpCrmFunnelStageMetrics.class);
    }

    

}
