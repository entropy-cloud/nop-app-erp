package app.erp.mfg.biz;

import java.time.LocalDate;
import java.util.List;

/**
 * APS 排程时间负荷来源 SPI（CRP 跨域读 APS OperationOrder 排程时间的解耦契约，plan 2026-07-05-0306-2）。
 *
 * <p>声明于 mfg-dao（消费方），由 {@code module-aps/erp-aps-service/.../loadsource/ApsLoadSourceProvider} 实现，
 * 通过 {@code ioc:collect-beans by-type} 在 {@code app-erp-all} 合并上下文收集（镜像 finance
 * {@code IErpFinAcctDocProvider} 跨域 SPI 范式）。{@code CrpLoadCalculator} 经 {@code @Inject List} 可选注入——
 * APS 模块缺失或未注册实现时返回空 list，CRP 回退 WorkOrder 计划日期（行为不变）。
 *
 * <p>放置层裁决：mfg-dao（aps-service 已 compile 依赖 mfg-dao，aps 可零新依赖实现；
 * mfg-service 已依赖 mfg-dao，零新依赖注入）。替代 mfg-service 放置层 rejected——会强制
 * aps-service 新增 compile 依赖 mfg-service（更重）。
 */
public interface IErpApsLoadSourceProvider {

    /**
     * 按 {@code workOrderIds} 批量查询 APS 已排程（{@code status=PLANNED}）的 OperationOrder，
     * 返回每个工序的 workCenter×plannedStartT~plannedEndT 时段。
     *
     * <p>{@code periodFrom/periodTo} 用于过滤排程时段与窗口相交（避免拉全表）——仅返回
     * {@code plannedEndT ≥ periodFrom AND plannedStartT ≤ periodTo} 的时段。
     *
     * <p>APS 未启用 / 无匹配 / 工序时间未回填 → 返回空 List（不返回 null）。
     * 调用方据此回退 WorkOrder 计划日期。
     *
     * @param workOrderIds 工单 ID 集合（CRP 当前窗口内已识别的工单）；空或 null 时返回空 list
     * @param periodFrom   CRP 计算窗口起点（含）
     * @param periodTo     CRP 计算窗口终点（含）
     * @return 排程时段列表（按 workOrderId 分组聚合，元素含工序序号+工作中心+排程起止时间+换模时间）
     */
    List<ApsLoadSlot> findScheduledSlots(List<Long> workOrderIds, LocalDate periodFrom, LocalDate periodTo);
}
