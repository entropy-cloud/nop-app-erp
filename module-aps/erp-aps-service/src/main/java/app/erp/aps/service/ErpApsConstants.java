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

    // ---- schedule-status ----
    String SCHEDULE_STATUS_DRAFT = "DRAFT";
    String SCHEDULE_STATUS_PUBLISHED = "PUBLISHED";
    String SCHEDULE_STATUS_ARCHIVED = "ARCHIVED";

    // ---- scheduling-mode ----
    String SCHEDULING_MODE_FORWARD = "FORWARD";
    String SCHEDULING_MODE_BACKWARD = "BACKWARD";

    // ---- constraint-type（本期仅消费 MAINTENANCE） ----
    String CONSTRAINT_TYPE_MAINTENANCE = "MAINTENANCE";
}
