package app.erp.qa.biz;

import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import java.util.List;

/**
 * SPC 控制图配置业务接口。除标准 CRUD（含审批）外，扩展：
 * <ul>
 *   <li>{@link #collectSamples}：手动触发样本采集。</li>
 *   <li>{@link #recalculateControlLimit}：手动触发控制限重算。</li>
 *   <li>{@link #evaluateRules}：手动触发判异规则评估。</li>
 *   <li>{@link #findOutOfControlSamples}：查询失控样本列表。</li>
 * </ul>
 *
 * <p>权威：{@code docs/design/quality/spc.md §关键流程}，plan 2026-07-07-0305-2。
 */
public interface IErpQaSpcChartBiz extends ICrudBiz<ErpQaSpcChart>, IApprovableBiz<ErpQaSpcChart> {

    /** 手动触发样本采集。返回新增子组数。 */
    @BizMutation
    Integer collectSamples(@Name("chartId") Long chartId, IServiceContext context);

    /** 手动触发控制限重算。子组数不足 20 时返回 false（calcStatus 保持 PENDING）。 */
    @BizMutation
    Boolean recalculateControlLimit(@Name("chartId") Long chartId, IServiceContext context);

    /** 手动触发判异规则评估，回写所有样本的 violatedRules/isOutOfControl。返回失控样本数。 */
    @BizMutation
    Integer evaluateRules(@Name("chartId") Long chartId, IServiceContext context);

    /** 查询失控样本列表（isOutOfControl=true）。 */
    @BizQuery
    List<ErpQaSpcSample> findOutOfControlSamples(@Name("chartId") Long chartId, IServiceContext context);
}
