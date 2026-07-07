
package app.erp.crm.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics;

import java.util.List;

/**
 * 漏斗阶段明细业务接口。除标准 CRUD 外，定义 {@link #getStageMetrics}（按 funnelId 查阶段明细列表）。
 *
 * <p>对齐 {@code docs/design/crm/lead-waterfall.md}（阶段度量明细）。
 */
public interface IErpCrmFunnelStageMetricsBiz extends ICrudBiz<ErpCrmFunnelStageMetrics> {

    /**
     * 按 funnelId 查阶段明细列表（按 stageOrder 升序）。
     */
    @BizQuery
    List<ErpCrmFunnelStageMetrics> getStageMetrics(@Name("funnelId") Long funnelId, IServiceContext context);
}
