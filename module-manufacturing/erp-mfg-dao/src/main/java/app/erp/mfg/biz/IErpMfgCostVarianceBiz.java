
package app.erp.mfg.biz;

import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;
import java.util.Map;

/**
 * 成本差异记录 Biz（plan 2026-07-05-1838-2）。CRUD 之外，承载差异分析的两个入口：
 * <ul>
 *   <li>{@link #calculateVariances} —— 手动（重）算指定工单的生产差异行（幂等：先删旧行再重算）。
 *       仅 COMPLETED 工单允许手动计算；无 FIRMED 标准成本抛 {@code ERR_VARIANCE_NO_STANDARD_COST}。</li>
 *   <li>{@link #findByWorkOrder} / {@link #aggregateByType} —— 差异行/类型聚合查询入口，供报表消费。</li>
 * </ul>
 *
 * <p>权威：{@code docs/design/manufacturing/variance-analysis.md}。
 */
public interface IErpMfgCostVarianceBiz extends ICrudBiz<ErpMfgCostVariance> {

    /**
     * 手动（重）算指定工单的生产差异行。幂等：先删该工单全部旧行再重算。
     *
     * <p>状态门控：仅 COMPLETED 工单允许手动计算（非 COMPLETED 抛 {@code ERR_VARIANCE_WORKORDER_NOT_COMPLETED}）。
     * 完工触发自动计算（{@code erp-mfg.variance-auto-calc-enabled=true}）由 Processor 在完工事务内调用，
     * 不经此入口（避免重复事务包装）。
     *
     * @return 新计算写入的差异行列表
     */
    @BizMutation
    List<ErpMfgCostVariance> calculateVariances(@Name("workOrderId") Long workOrderId, IServiceContext context);

    /**
     * 查询指定工单的全部差异行（按行号升序）。
     */
    @BizQuery
    List<ErpMfgCostVariance> findByWorkOrder(@Name("workOrderId") Long workOrderId, IServiceContext context);

    /**
     * 按差异类型聚合指定工单的差异金额（供报表按类型下钻）。
     *
     * @return 类型→{standardAmount, actualAmount, varianceAmount} 聚合
     */
    @BizQuery
    Map<String, Map<String, Object>> aggregateByType(@Name("workOrderId") Long workOrderId,
                                                     @Optional @Name("costElement") String costElement,
                                                     IServiceContext context);
}
