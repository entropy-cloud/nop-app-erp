package app.erp.aps.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * APS 域业务错误码（{@code scheduling.md} 排产引擎异常场景）。所有排产/插单/ATP-CTP 业务异常
 * 使用 {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 * 描述用中文，框架经 i18n 翻译。
 */
public interface ErpApsErrors {

    // --- 作用域参数键 ---
    String ARG_OP_ORDER_ID = "operationOrderId";
    String ARG_OP_CODE = "operationOrderCode";
    String ARG_MACHINE_ID = "machineId";
    String ARG_SCHEDULE_ID = "scheduleId";
    String ARG_DEADLINE = "deadline";
    String ARG_DESIRED_DATE = "desiredDate";
    String ARG_EARLIEST_COMPLETION = "earliestCompletion";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_REASON = "reason";

    /** 工作中心在展望期内无可用时段容纳工序。 */
    ErrorCode ERR_APS_NO_AVAILABLE_SLOT = ErrorCode.define(
            "erp.err.aps.no-available-slot",
            "工序工单 {operationOrderCode} 在工作中心 {machineId} 上无连续可用时段",
            ARG_OP_CODE, ARG_MACHINE_ID);

    /** 后向排产交期不可达：推算开工早于最早可开工时间。 */
    ErrorCode ERR_APS_DEADLINE_NOT_REACHABLE = ErrorCode.define(
            "erp.err.aps.deadline-not-reachable",
            "工序工单 {operationOrderCode} 期望交期 {deadline} 不可达，最早可完工 {earliestCompletion}",
            ARG_OP_CODE, ARG_DEADLINE, ARG_EARLIEST_COMPLETION);

    /** 已 IN_PROGRESS 的工序不可回退重排。 */
    ErrorCode ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE = ErrorCode.define(
            "erp.err.aps.op-in-progress-not-reschedulable",
            "工序工单 {operationOrderCode} 当前状态={currentStatus}，已开工不可重排",
            ARG_OP_CODE, ARG_CURRENT_STATUS);

    /** 同一工作中心同时段产能冲突（capacity=1）。 */
    ErrorCode ERR_APS_CAPACITY_CONFLICT = ErrorCode.define(
            "erp.err.aps.capacity-conflict",
            "工作中心 {machineId} 时段已被占用，工序工单 {operationOrderCode} 无法排入",
            ARG_MACHINE_ID, ARG_OP_CODE);

    /** 工艺路线工序顺序非法（同 WorkOrder 下 sequence 不连续或逆序）。 */
    ErrorCode ERR_APS_ROUTING_SEQUENCE_INVALID = ErrorCode.define(
            "erp.err.aps.routing-sequence-invalid",
            "工序工单 {operationOrderCode} 工艺顺序非法：{reason}",
            ARG_OP_CODE, ARG_REASON);

    /** 排产方案不存在。 */
    ErrorCode ERR_APS_SCHEDULE_NOT_FOUND = ErrorCode.define(
            "erp.err.aps.schedule.not-found",
            "排产方案 {scheduleId} 不存在",
            ARG_SCHEDULE_ID);

    /** 排产方案状态迁移非法。 */
    ErrorCode ERR_APS_SCHEDULE_ILLEGAL_STATUS = ErrorCode.define(
            "erp.err.aps.schedule.illegal-status",
            "排产方案 {scheduleId} 当前状态={currentStatus}，不允许执行该操作",
            ARG_SCHEDULE_ID, ARG_CURRENT_STATUS);

    /** 工序工单不存在。 */
    ErrorCode ERR_APS_OP_ORDER_NOT_FOUND = ErrorCode.define(
            "erp.err.aps.op-order.not-found",
            "工序工单 {operationOrderId} 不存在",
            ARG_OP_ORDER_ID);
}
