
package app.erp.aps.biz;

import app.erp.aps.dao.entity.ErpApsOperationOrder;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IErpApsOperationOrderBiz extends ICrudBiz<ErpApsOperationOrder>{

    /**
     * 前向排产：按 ErpApsSchedule.horizonStart/horizonEnd 拉取 DRAFT 工序，
     * 从 earliestStartDateT 正向填充工作中心可用时段，写回 plannedStart/EndDateT 并置 PLANNED。
     */
    @BizMutation
    SchedulingResult scheduleForward(@Name("scheduleId") Long scheduleId, IServiceContext context);

    /**
     * 后向排产：从 latestEndDateT 逆向倒推每工序最晚开工；交期不可达标记冲突。
     */
    @BizMutation
    SchedulingResult scheduleBackward(@Name("scheduleId") Long scheduleId, IServiceContext context);

    /**
     * 插单区间重排：急单窗口内优先级低于新单的 PLANNED 工序回退 DRAFT 重排，
     * IN_PROGRESS 永不回退，窗口外工序不受影响（{@code scheduling.md §六}）。
     */
    @BizMutation
    SchedulingResult insertRushOrder(@Name("operationOrderId") Long operationOrderId, IServiceContext context);

    /**
     * 最早可交付日期（ATP/CTP）：ATP 充足立即承诺，否则影子模拟最早完工。
     */
    @BizQuery
    LocalDateTime earliestCompletionDate(@Name("materialId") Long materialId, @Name("qty") BigDecimal qty);

    /**
     * 期望交期可行性检查（CTP）：返回 {@link CtpResult}。
     */
    @BizQuery
    CtpResult checkFeasibility(@Name("materialId") Long materialId,
                               @Name("qty") BigDecimal qty,
                               @Name("desiredDate") LocalDateTime desiredDate);

    /**
     * 启动工序工单：PLANNED→IN_PROGRESS。
     */
    @BizMutation
    ErpApsOperationOrder start(@Name("operationOrderId") Long operationOrderId, IServiceContext context);

    /**
     * 完成工序工单：IN_PROGRESS→FINISHED。
     */
    @BizMutation
    ErpApsOperationOrder complete(@Name("operationOrderId") Long operationOrderId, IServiceContext context);

    /**
     * 作废工序工单：DRAFT/PLANNED→CANCELLED。
     */
    @BizMutation
    ErpApsOperationOrder cancel(@Name("operationOrderId") Long operationOrderId, IServiceContext context);
}
