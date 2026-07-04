package app.erp.aps.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ATP/CTP 交期承诺模拟服务（{@code scheduling.md §七}）。
 *
 * <p>ATP（Available-to-Promise）：库存现有量 + 计划入库 − 已预约量 − 安全库存。
 * CTP（Capable-to-Promise）：ATP 不足时创建影子 OperationOrder（不持久化），
 * 在现有排产方案上模拟前向排产，返回最早可交付日期与瓶颈。
 *
 * <p>跨实体聚合走 inventory 域 {@code IErpInvStockBalanceBiz}/{@code IErpInvReservationBiz}
 * 只读查询，遵循 Nop 跨实体访问规范（不直接 IDaoProvider 跨库）。
 */
public interface IErpApsAtpCtpService {

    /**
     * 最早可交付日期：ATP 充足返回当前时间（立即承诺）；不足触发 CTP 模拟。
     */
    @BizQuery
    LocalDateTime earliestCompletionDate(@Name("materialId") Long materialId, @Name("qty") java.math.BigDecimal qty);

    /**
     * 检查期望交期是否可行：CTP 模拟，返回 {@link CtpResult}。
     */
    @BizQuery
    CtpResult checkFeasibility(@Name("materialId") Long materialId,
                               @Name("qty") java.math.BigDecimal qty,
                               @Name("desiredDate") LocalDateTime desiredDate);

    /**
     * 模拟排程：返回模拟得出的各工序时间（用于展示承诺排程，不落库）。
     */
    @BizQuery
    List<ScheduledOperationView> simulateSchedule(@Name("materialId") Long materialId,
                                                  @Name("qty") java.math.BigDecimal qty,
                                                  @Name("startDate") LocalDateTime startDate);
}
