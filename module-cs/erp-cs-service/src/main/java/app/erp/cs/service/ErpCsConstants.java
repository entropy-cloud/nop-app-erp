package app.erp.cs.service;

/**
 * 客服域常量。字典码值权威：{@code module-cs/model/app-erp-cs.orm.xml}。
 *
 * <p>权威：{@code docs/design/customer-service/state-machine.md}、
 * {@code docs/design/customer-service/sla.md}、{@code docs/design/customer-service/csat.md}、
 * {@code docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md}。
 */
public interface ErpCsConstants {

    // 工单状态（erp-cs/ticket-status，6 态；权威：state-machine.md §1）
    String TICKET_STATUS_NEW = "NEW";
    String TICKET_STATUS_ASSIGNED = "ASSIGNED";
    String TICKET_STATUS_IN_PROGRESS = "IN_PROGRESS";
    String TICKET_STATUS_RESOLVED = "RESOLVED";
    String TICKET_STATUS_CLOSED = "CLOSED";
    String TICKET_STATUS_CANCELLED = "CANCELLED";

    // 工单优先级（erp-cs/ticket-priority）。数值越大优先级越高（matcher 与 SLA minPriority 比较用）
    String TICKET_PRIORITY_LOW = "LOW";
    String TICKET_PRIORITY_NORMAL = "NORMAL";
    String TICKET_PRIORITY_HIGH = "HIGH";
    String TICKET_PRIORITY_URGENT = "URGENT";

    // 工单操作日志类型（erp-cs/action-type）。
    // 决策：字典无 START/RESOLVE/REOPEN 码（见 plan Decision）；迁移语义由 fromStatus/toStatus 承载，
    //       start/resolve/reopen 复用 NOTE（最接近的通用码）。
    String ACTION_TYPE_ASSIGN = "ASSIGN";
    String ACTION_TYPE_NOTE = "NOTE";
    String ACTION_TYPE_ATTACH = "ATTACH";
    String ACTION_TYPE_ESCALATE = "ESCALATE";
    String ACTION_TYPE_CLOSE = "CLOSE";
    String ACTION_TYPE_CANCEL = "CANCEL";

    // 单据状态（erp-cs/doc-status）
    String DOC_STATUS_DRAFT = "DRAFT";
    String DOC_STATUS_ACTIVE = "ACTIVE";
    String DOC_STATUS_CANCELLED = "CANCELLED";

    // 审核状态（erp-cs/approve-status）
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";

    // 调查发送渠道（erp-cs/survey-channel）
    String SURVEY_CHANNEL_EMAIL = "EMAIL";
    String SURVEY_CHANNEL_PORTAL = "PORTAL";

    // NPS 分类（派生，不持久化；权威：csat.md §1.2）
    String NPS_CATEGORY_PROMOTER = "PROMOTER";   // 9-10 推荐者
    String NPS_CATEGORY_PASSIVE = "PASSIVE";      // 7-8 被动者
    String NPS_CATEGORY_DETRACTOR = "DETRACTOR";  // 0-6 贬损者

    // 客服域配置项（经 AppConfig.var 读取；权威：plan Infrastructure And Config Prereqs）
    String CONFIG_SLA_ENABLED = "erp-cs.sla-enabled";
    String CONFIG_SLA_WARNING_BEFORE = "erp-cs.sla-warning-before";        // 单位：分钟
    String CONFIG_AUTO_ASSIGN_ON_CREATE = "erp-cs.auto-assign-on-create";  // 新建工单是否自动分派
    String CONFIG_SURVEY_ENABLED = "erp-cs.survey-enabled";
    String CONFIG_SURVEY_TRIGGER_STATUS = "erp-cs.survey-trigger-status";  // 默认 RESOLVED
    String CONFIG_SURVEY_SEND_DELAY = "erp-cs.survey-send-delay";          // 单位：小时
    String CONFIG_SURVEY_CSAT_ENABLED = "erp-cs.survey-csat-enabled";
    String CONFIG_SURVEY_NPS_ENABLED = "erp-cs.survey-nps-enabled";
    String CONFIG_SURVEY_CES_ENABLED = "erp-cs.survey-ces-enabled";
    String CONFIG_SURVEY_REMINDER_HOURS = "erp-cs.survey-reminder-hours";  // 默认 48
    String CONFIG_SURVEY_EXPIRE_DAYS = "erp-cs.survey-expire-days";        // 默认 7
}
