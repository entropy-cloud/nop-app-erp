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
    /** 定时 SLA 超时扫描 cron（空=不调度；plan 2026-07-05-0306-1 §配置点）。 */
    String CONFIG_SLA_SCAN_CRON = "erp-cs.sla-scan-cron";

    /** SLA 超期/预警通知派发开关（默认 true；plan 2026-07-06-0642-1 §Phase 1）。关闭时跳过 notify 调用。 */
    String CONFIG_SLA_NOTIFY_ENABLED = "erp-cs.sla-notify-enabled";

    /** 通知事件类型：SLA 超期/预警（对应 erp_sys_notification_template.notification_type）。 */
    String NOTIFY_EVENT_SLA_OVERDUE = "cs.sla-overdue";

    /** 定时 CSAT 调查到期提醒 cron（空=不调度；plan 2026-07-06-0642-1 §Phase 2）。 */
    String CONFIG_CSAT_REMINDER_CRON = "erp-cs.csat-reminder-cron";

    /** 通知事件类型：CSAT 调查到期提醒（对应 erp_sys_notification_template.notification_type；plan 2026-07-06-0642-1 §Phase 2）。 */
    String NOTIFY_EVENT_CSAT_REMINDER = "cs.csat-reminder";

    // === 客户服务权益 / 服务目录（plan 2026-07-07-1430-1）===

    // 权益 serviceType 字典（erp-cs/service-type；权威：app-erp-cs.orm.xml）
    String SERVICE_TYPE_WARRANTY = "WARRANTY";
    String SERVICE_TYPE_SUPPORT_CONTRACT = "SUPPORT_CONTRACT";
    String SERVICE_TYPE_PAY_PER_TICKET = "PAY_PER_TICKET";

    // 履行动作类型字典（erp-cs/fulfillment-action-type；权威：service-catalog.md §3.1）
    String FULFILLMENT_ACTION_CREATE_TICKET = "CREATE_TICKET";
    String FULFILLMENT_ACTION_ASSIGN_TEAM = "ASSIGN_TEAM";
    String FULFILLMENT_ACTION_ASSIGN_AGENT = "ASSIGN_AGENT";
    String FULFILLMENT_ACTION_REQUEST_APPROVAL = "REQUEST_APPROVAL";
    String FULFILLMENT_ACTION_NOTIFY_CUSTOMER = "NOTIFY_CUSTOMER";
    String FULFILLMENT_ACTION_UPDATE_STATUS = "UPDATE_STATUS";
    String FULFILLMENT_ACTION_CREATE_CHILD_TICKET = "CREATE_CHILD_TICKET";
    String FULFILLMENT_ACTION_INVOKE_WORKFLOW = "INVOKE_WORKFLOW";
    String FULFILLMENT_ACTION_CLOSE_TICKET = "CLOSE_TICKET";

    // 履行执行结果（ErpCsTicketAction 审计 content 派生标识；不持久化枚举）
    String FULFILLMENT_RESULT_DONE = "DONE";
    String FULFILLMENT_RESULT_SKIPPED = "SKIPPED";

    // 权益配置项（经 AppConfig.var 读取；权威：entitlement.md §五 + plan Infrastructure）
    String CONFIG_ENTITLEMENT_CHECK_ENABLED = "erp-cs.entitlement-check-enabled";
    String CONFIG_ENTITLEMENT_ALLOW_NO_ENTITLEMENT = "erp-cs.entitlement-allow-no-entitlement";
    String CONFIG_ENTITLEMENT_EXPIRY_WARNING_DAYS = "erp-cs.entitlement-expiry-warning-days";
    String CONFIG_ENTITLEMENT_AUTO_WARRANTY = "erp-cs.entitlement-auto-warranty";
    String CONFIG_ENTITLEMENT_EXPIRY_CRON = "erp-cs.entitlement-expiry-cron";

    // 服务目录配置项（权威：service-catalog.md §六 + plan Infrastructure）
    String CONFIG_SERVICE_CATALOG_ENABLED = "erp-cs.service-catalog-enabled";
    String CONFIG_SERVICE_CATALOG_SELF_SERVICE = "erp-cs.service-catalog-self-service";
    String CONFIG_CATALOG_CATEGORY_MAX_DEPTH = "erp-cs.catalog-category-max-depth";

    /** 通知事件类型：服务权益到期预警（对应 erp_sys_notification_template.notification_type）。 */
    String NOTIFY_EVENT_ENTITLEMENT_EXPIRY = "cs.entitlement-expiry";

    // === 知识库搜索/建议（plan 2026-07-08-0056-2）===

    /** 知识库搜索默认返回条数（默认 5）。 */
    String CONFIG_KNOWLEDGE_SEARCH_DEFAULT_LIMIT = "erp-cs.knowledge-search-default-limit";

    /** 知识库搜索最大返回条数（默认 20，防滥用）。 */
    String CONFIG_KNOWLEDGE_SEARCH_MAX_LIMIT = "erp-cs.knowledge-search-max-limit";

    /** 知识库搜索关键词最大长度（对齐 title 字段 precision=200）。 */
    int KNOWLEDGE_SEARCH_KEYWORD_MAX_LENGTH = 200;

    /** 知识库内容摘要截断长度。 */
    int KNOWLEDGE_CONTENT_SUMMARY_LENGTH = 200;

    /** 工单主题建议最小有效长度（< 2 字符返回空集）。 */
    int SUGGEST_SUBJECT_MIN_LENGTH = 2;

    // === 预设应答（plan 2026-07-11-1234-2）===

    /** 预设应答是否启用（默认 true；canned-response.md §五）。 */
    String CONFIG_CANNED_RESPONSE_ENABLED = "erp-cs.canned-response-enabled";

    /** 宏自动匹配展示条数（默认 3；canned-response.md §五/§二）。 */
    String CONFIG_CANNED_RESPONSE_MACRO_COUNT = "erp-cs.canned-response-macro-count";

    /** 应答分类最大深度（默认 3；canned-response.md §五，本期仅声明不强制校验）。 */
    String CONFIG_CANNED_RESPONSE_CATEGORY_MAX_DEPTH = "erp-cs.canned-response-category-max-depth";
}
