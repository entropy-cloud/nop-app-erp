package app.erp.aps.service;

/**
 * APS 域状态码与字典常量。权威值来自 {@code module-aps/model/app-erp-aps.orm.xml} 关联字典
 * {@code erp-aps/operation-order-status}、{@code erp-aps/schedule-status}、
 * {@code erp-aps/scheduling-mode}、{@code erp-aps/constraint-type}。
 */
public interface ErpApsConstants {

    // ---- operation-order-status ----
    String OP_STATUS_DRAFT = "DRAFT";
    String OP_STATUS_PLANNED = "PLANNED";
    String OP_STATUS_IN_PROGRESS = "IN_PROGRESS";
    String OP_STATUS_FINISHED = "FINISHED";
    String OP_STATUS_CANCELLED = "CANCELLED";
    /** 不可排产（RC-R1.87：全部候选路由无可用产能/被过滤；重排时与 DRAFT 同池重试，自愈语义）。 */
    String OP_STATUS_UNSCHEDULABLE = "UNSCHEDULABLE";
    /** 保持（RC-R1.88：计划员暂不派工，PLANNED↔HOLD 迁移）。 */
    String OP_STATUS_HOLD = "HOLD";
    /** 缺料暂停（RC-R1.88：派工窗口内物料不齐，系统置 ON_HOLD + 通知计划员）。 */
    String OP_STATUS_ON_HOLD = "ON_HOLD";

    // ---- routing-selection-reason（RC-R1.87，alternative-routing.md §2.2） ----
    String ROUTING_REASON_DEFAULT = "DEFAULT";
    String ROUTING_REASON_PRIMARY_OVERBOOKED = "PRIMARY_OVERBOOKED";
    String ROUTING_REASON_PRIMARY_DOWN = "PRIMARY_DOWN";
    String ROUTING_REASON_BATCH_CONSTRAINT = "BATCH_CONSTRAINT";

    // ---- dispatch-type（RC-R1.88，auto-dispatch.md §4.1） ----
    String DISPATCH_TYPE_AUTO = "AUTO";
    String DISPATCH_TYPE_MANUAL = "MANUAL";
    String DISPATCH_TYPE_HOLD = "HOLD";
    String DISPATCH_TYPE_UNHOLD = "UNHOLD";

    // ---- schedule-status ----
    String SCHEDULE_STATUS_DRAFT = "DRAFT";
    String SCHEDULE_STATUS_PUBLISHED = "PUBLISHED";
    String SCHEDULE_STATUS_ARCHIVED = "ARCHIVED";

    // ---- scheduling-mode ----
    String SCHEDULING_MODE_FORWARD = "FORWARD";
    String SCHEDULING_MODE_BACKWARD = "BACKWARD";

    // ---- constraint-type（本期仅消费 MAINTENANCE） ----
    String CONSTRAINT_TYPE_MAINTENANCE = "MAINTENANCE";

    // ---- notify 事件类型（RC-R1.86/R1.88，R1.4 范式：无 ACTIVE 模板 config-gated 静默跳过） ----
    String NOTIFY_EVENT_WORKORDER_NO_ROUTING = "aps.workorder-no-routing";
    String NOTIFY_EVENT_OPERATION_WORKCENTER_MISSING = "aps.operation-workcenter-missing";
    String NOTIFY_EVENT_DISPATCH_MATERIAL_SHORTAGE = "aps.dispatch-material-shortage";
}
