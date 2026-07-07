
package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmFunnelStageMetricsBiz;
import app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.util.Comparator;
import java.util.List;

/**
 * 漏斗阶段明细 BizModel。{@link #getStageMetrics} 按 funnelId 查阶段明细（按 stageOrder 升序）。
 *
 * <p>对齐 {@code docs/design/crm/lead-waterfall.md}（阶段度量明细）。
 */
@BizModel("ErpCrmFunnelStageMetrics")
public class ErpCrmFunnelStageMetricsBizModel extends CrudBizModel<ErpCrmFunnelStageMetrics>
        implements IErpCrmFunnelStageMetricsBiz {

    public ErpCrmFunnelStageMetricsBizModel() {
        setEntityName(ErpCrmFunnelStageMetrics.class.getName());
    }

    @Override
    @BizQuery
    public List<ErpCrmFunnelStageMetrics> getStageMetrics(@Name("funnelId") Long funnelId,
                                                            IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("funnelId", funnelId));
        List<ErpCrmFunnelStageMetrics> list = findList(q, null, context);
        list.sort(Comparator
                .comparingInt((ErpCrmFunnelStageMetrics s) ->
                        s.getStageOrder() != null ? s.getStageOrder() : Integer.MAX_VALUE)
                .thenComparing(s -> s.getStageId() != null ? s.getStageId() : Long.MAX_VALUE));
        return list;
    }
}
