
package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmLeadBiz;
import app.erp.crm.biz.IErpCrmLeadFunnelBiz;
import app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics;
import app.erp.crm.dao.entity.ErpCrmLeadFunnel;
import app.erp.crm.service.processor.ErpCrmLeadFunnelRefreshFunnelProcessor;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售漏斗 BizModel。{@link #refreshFunnel} 委托 {@link ErpCrmLeadFunnelRefreshFunnelProcessor}
 * （清旧重建快照，聚合 ConvLog + Lead → LeadFunnel 头 + FunnelStageMetrics 明细）；{@link #getFunnelView} 返回可视化数据结构。
 *
 * <p>对齐 {@code docs/design/crm/lead-waterfall.md}（聚合计算流程 / 漏斗可视化数据结构）。
 */
@BizModel("ErpCrmLeadFunnel")
public class ErpCrmLeadFunnelBizModel extends CrudBizModel<ErpCrmLeadFunnel> implements IErpCrmLeadFunnelBiz {

    @Inject
    IErpCrmLeadBiz leadBiz;

    @Inject
    ErpCrmLeadFunnelRefreshFunnelProcessor refreshFunnelProcessor;

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
        return refreshFunnelProcessor.refreshFunnel(periodStart, periodEnd, territoryId, teamId, sourceId, context);
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

    protected List<ErpCrmFunnelStageMetrics> loadStageMetrics(Long funnelId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("funnelId", funnelId));
        return stageMetricsDao().findAllByQuery(q);
    }

    protected IEntityDao<ErpCrmFunnelStageMetrics> stageMetricsDao() {
        return daoProvider().daoFor(ErpCrmFunnelStageMetrics.class);
    }

    

}
