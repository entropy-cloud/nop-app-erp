package app.erp.cs.service;

import app.erp.cs.dao.constants.ErpCsDocStatus;

/**
 * 客服域常量。字典码值权威：{@code module-cs/model/app-erp-cs.orm.xml}。
 *
 * <p>权威：{@code docs/design/customer-service/state-machine.md}、
 * {@code docs/design/customer-service/sla.md}、{@code docs/design/customer-service/csat.md}、
 * {@code docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md}。
 *
 * <p>{@code extends ErpCsDocStatus} 复用 dao 层常量定义，保持 approve-status / doc-status 单一真相源；
 * 本接口仅追加 service 层独有的派生状态与配置项。
 */
public interface ErpCsConstants extends ErpCsDocStatus {

    // 工单状态（erp-cs/ticket-status，6 态；权威：state-machine.md §1）
    String TICKET_STATUS_NEW = "NEW";
    String TICKET_STATUS_ASSIGNED = "ASSIGNED";
    String TICKET_STATUS_IN_PROGRESS = "IN_PROGRESS";
    String TICKET_STATUS_RESOLVED = "RESOLVED";
    String TICKET_STATUS_CLOSED = "CLOSED";
    String TICKET_STATUS_CANCELLED = "CANCELLED";

    // 工单优先级（erp-cs/ticket-priority）。数值越高优先级越高（matcher 与 SLA minPriority 比较用）
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

    // approve-status / doc-status 常量继承自 ErpCsDocStatus（dao 层单一真相源）

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

    // === 工单创建自动富化（plan 2026-08-17-2125-1，RC-R1.65，P1-RC-054，UC-CS-01 ②-⑧）===

    /** 自动分配算法（erp-cs/assign-method；plan D4）：轮转——上次分配的下一个，无历史首位。 */
    String ASSIGN_METHOD_ROUND_ROBIN = "ROUND_ROBIN";

    /** 自动分配算法（erp-cs/assign-method；plan D4）：最少未结——活跃工单（ASSIGNED/IN_PROGRESS）最少者。 */
    String ASSIGN_METHOD_LEAST_OPEN = "LEAST_OPEN";

    /** 自动分配算法选择（默认 ROUND_ROBIN；plan D4）。 */
    String CONFIG_ASSIGN_METHOD = "erp-cs.assign-method";

    /** 通知事件类型：工单创建确认（UC-CS-01 ⑥，含 TK 编号；模板种子 7202）。 */
    String NOTIFY_EVENT_TICKET_CREATED = "cs.ticket-created";

    /** 通知事件类型：自动分派无匹配升级（UC-CS-01 ⑧，客服主管；模板种子 7203）。 */
    String NOTIFY_EVENT_TICKET_ASSIGN_NO_MATCH = "cs.ticket-assign-no-match";

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

    // === 工单计时器 session / 计时条目审批（plan 2026-08-17-2125-2，RC-R1.66，P1-RC-055，UC-CS-11）===

    /** 是否启用工单计时（默认 true；time-tracking.md §七，UC-CS-11 ① 前置）。 */
    String CONFIG_TIME_TRACKING_ENABLED = "erp-cs.time-tracking-enabled";

    /** 时间条目是否必须填写描述（默认 true；time-tracking.md §七）。 */
    String CONFIG_TIME_ENTRY_REQUIRE_DESCRIPTION = "erp-cs.time-entry-require-description";

    /** 超过此时长（分钟）的条目需审批（默认 480；time-tracking.md §七）。 */
    String CONFIG_TIME_ENTRY_APPROVAL_THRESHOLD = "erp-cs.time-entry-approval-threshold";

    /** 条目是否自动审批跳过审批流（默认 false；time-tracking.md §七）。 */
    String CONFIG_TIME_ENTRY_AUTO_APPROVE = "erp-cs.time-entry-auto-approve";

    /** 全局默认计费费率（元/小时，默认 0；time-tracking.md §七 + §1.2 优先级 4）。 */
    String CONFIG_DEFAULT_BILLING_RATE = "erp-cs.default-billing-rate";

    /** 单次计时器最大时长（小时，超过惰性自动停止，默认 12；time-tracking.md §七，UC-CS-11 ⑧）。 */
    String CONFIG_TIME_ENTRY_TIMER_MAX_HOURS = "erp-cs.time-entry-timer-max-hours";

    /** 条目审批主管兜底人（userId，空=无兜底；plan D4 §3.3 审批人链末端经 config 显式化）。 */
    String CONFIG_TIME_ENTRY_APPROVER_ID = "erp-cs.time-entry-approver-id";

    // 计时器会话状态（erp-cs/timer-session-status；权威：app-erp-cs.orm.xml + plan D1）
    String TIMER_SESSION_STATUS_RUNNING = "RUNNING";
    String TIMER_SESSION_STATUS_PAUSED = "PAUSED";
    String TIMER_SESSION_STATUS_STOPPED = "STOPPED";

    /** 单计时器 UK 载体：进行中（RUNNING/PAUSED）='Y'，停止置 NULL（plan D1 单活跃约束）。 */
    String TIMER_SESSION_ACTIVE_FLAG = "Y";

    // 条目审批状态（erp-cs/time-entry-approve-status；NULL = DRAFT 未提交，plan D4）
    String TIME_ENTRY_APPROVE_PENDING = "PENDING";
    String TIME_ENTRY_APPROVE_APPROVED = "APPROVED";
    String TIME_ENTRY_APPROVE_REJECTED = "REJECTED";

    // 条目来源（erp-cs/time-entry-source）
    String TIME_ENTRY_SOURCE_MANUAL = "MANUAL";
    String TIME_ENTRY_SOURCE_TIMER_IMPORT = "TIMER_IMPORT";

    /** D2 映射哨兵：session.agentId（userId 字符串）非数字时写 ErpCsTimeEntry.agentId（BIGINT）取 0。 */
    long ENTRY_AGENT_ID_UNMAPPED = 0L;
}
