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
    // O-11 扩展参数键
    String ARG_WORK_ORDER_ID = "workOrderId";
    String ARG_DISPATCH_RULE = "dispatchRule";
    String ARG_CONSTRAINT_ID = "constraintId";
    String ARG_RESOURCE_ID = "resourceId";
    String ARG_DEMAND_DATE = "demandDate";
    String ARG_QTY = "qty";
    String ARG_PRIORITY = "priority";

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

    // ---------- O-11 扩展：排程/约束/资源/需求计划等细粒度错误码 ----------

    ErrorCode ERR_APS_OP_ORDER_ALREADY_SCHEDULED = ErrorCode.define(
            "erp.err.aps.op-order.already-scheduled",
            "工序工单 {operationOrderCode} 已排程，不可重复排程（请先取消）",
            ARG_OP_CODE);

    ErrorCode ERR_APS_WORK_ORDER_NOT_FOUND = ErrorCode.define(
            "erp.err.aps.work-order.not-found",
            "工单 {workOrderId} 不存在",
            ARG_WORK_ORDER_ID);

    ErrorCode ERR_APS_DISPATCH_RULE_INVALID = ErrorCode.define(
            "erp.err.aps.dispatch-rule.invalid",
            "派工规则 {dispatchRule} 不支持（本期仅支持 SPT/EDD/FIFO/CR）",
            ARG_DISPATCH_RULE);

    ErrorCode ERR_APS_CONSTRAINT_HARD_VIOLATED = ErrorCode.define(
            "erp.err.aps.constraint.hard-violated",
            "硬约束 {constraintId} 被违反，排产无法完成",
            ARG_CONSTRAINT_ID);

    ErrorCode ERR_APS_RESOURCE_UNAVAILABLE = ErrorCode.define(
            "erp.err.aps.resource.unavailable",
            "资源 {resourceId} 在排程时段不可用（维护/停机）",
            ARG_RESOURCE_ID);

    ErrorCode ERR_APS_ATP_NOT_AVAILABLE = ErrorCode.define(
            "erp.err.aps.atp.not-available",
            "物料需求日期 {demandDate} ATP 可用量不足（需求 {qty}）",
            ARG_DEMAND_DATE, ARG_QTY);

    ErrorCode ERR_APS_SCHEDULE_FROZEN = ErrorCode.define(
            "erp.err.aps.schedule.frozen",
            "排产方案 {scheduleId} 已冻结，不可修改",
            ARG_SCHEDULE_ID);

    ErrorCode ERR_APS_PRIORITY_INVALID = ErrorCode.define(
            "erp.err.aps.priority.invalid",
            "工序工单 {operationOrderCode} 优先级 {priority} 非法（须 1~100）",
            ARG_OP_CODE, ARG_PRIORITY);
}
