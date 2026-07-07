
package app.erp.crm.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.crm.dao.entity.ErpCrmLeadFunnel;

import java.util.Map;

/**
 * 销售漏斗业务接口。除标准 CRUD 外，定义 {@link #refreshFunnel}（清旧重建快照）+ {@link #getFunnelView}（可视化数据结构）。
 *
 * <p>对齐 {@code docs/design/crm/lead-waterfall.md}（聚合计算流程 / 漏斗可视化数据结构）。
 */
public interface IErpCrmLeadFunnelBiz extends ICrudBiz<ErpCrmLeadFunnel> {

    /**
     * 刷新漏斗：清旧重建快照范式（对齐 0700-1 forecast）。聚合 ConvLog + Lead → LeadFunnel 头 + FunnelStageMetrics 明细。
     * 维度参数全部为 null 时聚合全量；任一非空时按维度过滤。
     */
    @BizMutation
    ErpCrmLeadFunnel refreshFunnel(@Name("periodStart") java.time.LocalDate periodStart,
                                   @Name("periodEnd") java.time.LocalDate periodEnd,
                                   @Optional @Name("territoryId") Long territoryId,
                                   @Optional @Name("teamId") Long teamId,
                                   @Optional @Name("sourceId") Long sourceId,
                                   IServiceContext context);

    /**
     * 漏斗可视化数据结构：stages 数组 + 各阶段 conversionRate/dropOffRate/lostReasonTop（对齐 lead-waterfall.md §4）。
     */
    @BizQuery
    Map<String, Object> getFunnelView(@Name("funnelId") Long funnelId, IServiceContext context);
}
