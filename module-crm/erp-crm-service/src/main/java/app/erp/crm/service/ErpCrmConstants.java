package app.erp.crm.service;

/**
 * CRM 域状态码常量。权威值来自 {@code module-crm/model/app-erp-crm.orm.xml}
 * 关联字典 {@code erp-crm/lead-doc-status}、{@code erp-crm/lead-type}。
 */
public interface ErpCrmConstants {

    // 线索/商机类型 erp-crm/lead-type
    String LEAD_TYPE_LEAD = "LEAD";
    String LEAD_TYPE_OPPORTUNITY = "OPPORTUNITY";

    // 单据生命周期 doc-status erp-crm/lead-doc-status
    String DOC_STATUS_NEW = "NEW";
    String DOC_STATUS_QUALIFIED = "QUALIFIED";
    String DOC_STATUS_CONVERTED = "CONVERTED";
    String DOC_STATUS_LOST = "LOST";
    String DOC_STATUS_CANCELLED = "CANCELLED";

    // 主数据启用状态 erp-md/active-status（与 sales/master-data 一致）
    String PARTNER_STATUS_ACTIVE = "ACTIVE";
    String PARTNER_TYPE_CUSTOMER = "CUSTOMER";

    // 转化结果弱指针单据类型（自由字符串，sales/master-data 侧无字典约束）
    String RELATED_BILL_TYPE_SALES_QUOTATION = "SALES_QUOTATION";
    String RELATED_BILL_TYPE_CRM_LEAD = "CRM_LEAD";

    // 配置项（默认 false，发现重复线索仅提示不自动合并）
    String CONFIG_AUTO_CONVERT_DUPLICATE_LEAD = "erp-crm.auto-convert-duplicate-lead";
    String CONFIG_DEFAULT_TEAM_ID = "erp-crm.default-team-id";

    // ===== 活动提醒（3.2）配置点 =====
    String CONFIG_EVENT_REMINDER_ENABLED = "erp-crm.event-reminder-enabled";

    /** 定时活动到期提醒 cron（空=不调度；plan 2026-07-06-0642-1 §Phase 2）。 */
    String CONFIG_EVENT_REMINDER_CRON = "erp-crm.event-reminder-cron";

    /** 通知事件类型：活动到期提醒（对应 erp_sys_notification_template.notification_type；plan 2026-07-06-0642-1 §Phase 2）。 */
    String NOTIFY_EVENT_EVENT_REMINDER = "crm.event-reminder";

    // erp-crm/event-status 字典
    String EVENT_STATUS_PLANNED = "PLANNED";
    String EVENT_STATUS_COMPLETED = "COMPLETED";
    String EVENT_STATUS_CANCELLED = "CANCELLED";

    // 时间线来源标识（派生值，非字典）
    String TIMELINE_SOURCE_EVENT = "EVENT";
    String TIMELINE_SOURCE_ACTIVITY = "ACTIVITY";

    // ===== 线索评分（3.3）配置点 =====
    String CONFIG_LEAD_SCORING_AUTO_QUALIFY = "erp-crm.lead-scoring.auto-qualify";
    String CONFIG_LEAD_SCORING_RECALC_ON_LEAD_UPDATE = "erp-crm.lead-scoring.recalc-on-lead-update";
    /** 定时线索评分批量重算 cron（空=不调度；plan 2026-07-05-0306-1 §配置点）。 */
    String CONFIG_LEAD_SCORING_SCHEDULE_CRON = "erp-crm.lead-scoring.schedule-cron";

    // erp-crm/scoring-method 字典
    String SCORING_METHOD_LOOKUP = "LOOKUP";
    String SCORING_METHOD_FORMULA = "FORMULA";
    String SCORING_METHOD_BOOLEAN = "BOOLEAN";

    // erp-crm/scoring-trigger-event 字典
    String TRIGGER_EVENT_MANUAL = "MANUAL";
    String TRIGGER_EVENT_LEAD_UPDATE = "LEAD_UPDATE";
    String TRIGGER_EVENT_SCHEDULED = "SCHEDULED";

    // erp-crm/scoring-triggered-action 字典
    String TRIGGERED_ACTION_NONE = "NONE";
    String TRIGGERED_ACTION_AUTO_QUALIFY = "AUTO_QUALIFY";
    String TRIGGERED_ACTION_NOTIFY_OWNER = "NOTIFY_OWNER";

    // ===== 销售预测（3.4）配置点 =====
    String CONFIG_FORECAST_COMMIT_THRESHOLD = "erp-crm.forecast.commit-threshold";
    String CONFIG_FORECAST_UPSIDE_THRESHOLD = "erp-crm.forecast.upside-threshold";
    String CONFIG_FORECAST_ACCURACY_AUTO_COMPUTE = "erp-crm.forecast.accuracy-auto-compute";
    /** 定时销售预测重算 cron（空=不调度；plan 2026-07-05-0306-1 §配置点）。 */
    String CONFIG_FORECAST_RECALC_CRON = "erp-crm.forecast.recalc-cron";

    // erp-crm/forecast-period-status 字典
    String FORECAST_PERIOD_STATUS_OPEN = "OPEN";
    String FORECAST_PERIOD_STATUS_CLOSED = "CLOSED";
    String FORECAST_PERIOD_STATUS_FROZEN = "FROZEN";

    // erp-crm/forecast-category 字典
    String FORECAST_CATEGORY_COMMIT = "COMMIT";
    String FORECAST_CATEGORY_UPSIDE = "UPSIDE";
    String FORECAST_CATEGORY_BEST_CASE = "BEST_CASE";

    // ===== 区域管理 / 配额（plan 2026-07-07-1100-1）配置点 =====
    /** 线索创建时是否自动分配区域（默认 true）。 */
    String CONFIG_TERRITORY_AUTO_ASSIGN_ON_CREATE = "erp-crm.territory.auto-assign-on-create";
    /** 无匹配规则时的默认团队（可空）。 */
    String CONFIG_TERRITORY_DEFAULT_TEAM_ID = "erp-crm.territory.default-team-id";
    /** 区域树最大层级（默认 4：REGION → AREA → BRANCH → TEAM）。 */
    String CONFIG_TERRITORY_MAX_DEPTH = "erp-crm.territory.max-depth";
    /** 年度配额均分时是否按月（true=12 月均分，false=4 季均分）。 */
    String CONFIG_QUOTA_DISTRIBUTE_MONTHLY = "erp-crm.quota.distribute-monthly";

    // erp-crm/territory-type 字典
    String TERRITORY_TYPE_REGION = "REGION";
    String TERRITORY_TYPE_AREA = "AREA";
    String TERRITORY_TYPE_BRANCH = "BRANCH";
    String TERRITORY_TYPE_TEAM = "TEAM";

    // erp-crm/assignment-condition-type 字典
    String ASSIGNMENT_CONDITION_GEOGRAPHY = "GEOGRAPHY";
    String ASSIGNMENT_CONDITION_INDUSTRY = "INDUSTRY";
    String ASSIGNMENT_CONDITION_CUSTOMER_SIZE = "CUSTOMER_SIZE";
    String ASSIGNMENT_CONDITION_CUSTOM_FIELD = "CUSTOM_FIELD";

    // erp-crm/assignment-method 字典
    String ASSIGNMENT_METHOD_ROUND_ROBIN = "ROUND_ROBIN";
    String ASSIGNMENT_METHOD_LOAD_BALANCED = "LOAD_BALANCED";
    String ASSIGNMENT_METHOD_MANUAL = "MANUAL";

    // erp-crm/quota-period-type 字典
    String QUOTA_PERIOD_ANNUAL = "ANNUAL";
    String QUOTA_PERIOD_QUARTERLY = "QUARTERLY";
    String QUOTA_PERIOD_MONTHLY = "MONTHLY";

    // ===== CPQ 配置-定价-报价（plan 2026-07-07-1430-2）配置点 =====
    /** 单个配置器允许的最大规则数（默认 100）。 */
    String CONFIG_CPQ_MAX_RULES_PER_CONFIGURATOR = "erp-crm.cpq.max-rules-per-configurator";
    /** 是否启用引导式向导（false=单页配置，默认 true）。 */
    String CONFIG_CPQ_ENABLE_WIZARD = "erp-crm.cpq.enable-wizard";
    /** 定价默认币种（默认 CNY）。 */
    String CONFIG_CPQ_DEFAULT_CURRENCY = "erp-crm.cpq.default-currency";

    // erp-crm/config-rule-type 字典
    String CONFIG_RULE_TYPE_REQUIRED = "REQUIRED";
    String CONFIG_RULE_TYPE_OPTIONAL = "OPTIONAL";
    String CONFIG_RULE_TYPE_EXCLUDED = "EXCLUDED";
    String CONFIG_RULE_TYPE_RECOMMENDED = "RECOMMENDED";

    // erp-crm/bundle-discount-type 字典
    String BUNDLE_DISCOUNT_TYPE_PERCENTAGE = "PERCENTAGE";
    String BUNDLE_DISCOUNT_TYPE_FIXED = "FIXED";

    // erp-crm/price-rule-type 字典
    String PRICE_RULE_TYPE_CUSTOMER_SPECIFIC = "CUSTOMER_SPECIFIC";
    String PRICE_RULE_TYPE_PROMOTIONAL = "PROMOTIONAL";
    String PRICE_RULE_TYPE_VOLUME = "VOLUME";

    // ===== 销售序列 + 漏斗分析（plan 2026-07-07-1430-3）配置点 =====
    /** Lead 进入 QUALIFIED 时是否自动分配序列（默认 true）。 */
    String CONFIG_SEQUENCE_AUTO_ASSIGN_ON_QUALIFY = "erp-crm.sequence.auto-assign-on-qualify";
    /** 步骤逾期宽限期天数（超过 dueDays + grace 视为逾期，默认 2）。 */
    String CONFIG_SEQUENCE_GRACE_PERIOD_DAYS = "erp-crm.sequence.grace-period-days";
    /** 连续逾期步骤上限，超过则提醒（默认 3）。 */
    String CONFIG_SEQUENCE_MAX_OVERDUE_STEPS = "erp-crm.sequence.max-overdue-steps";
    /** 定时序列逾期检查 cron（空=不调度；设计默认每日 06:00）。 */
    String CONFIG_SEQUENCE_OVERDUE_CHECK_CRON = "erp-crm.sequence.overdue-check-cron";
    /** 无匹配规则时的默认序列模板类型（默认 NEW_LEAD）。 */
    String CONFIG_SEQUENCE_DEFAULT_TEMPLATE = "erp-crm.sequence.default-template";
    /** 漏斗聚合定时 cron（空=不调度；设计默认每日 03:00）。 */
    String CONFIG_FUNNEL_AGGREGATION_CRON = "erp-crm.funnel.aggregation-cron";
    /** 漏斗快照保留月数（默认 24）。 */
    String CONFIG_FUNNEL_RETENTION_PERIOD_MONTHS = "erp-crm.funnel.retention-period-months";
    /** 漏斗每阶段丢失原因 TOP N（默认 5）。 */
    String CONFIG_FUNNEL_TOP_LOST_REASONS = "erp-crm.funnel.top-lost-reasons";

    // erp-crm/sequence-template-type 字典
    String SEQUENCE_TEMPLATE_NEW_LEAD = "NEW_LEAD";
    String SEQUENCE_TEMPLATE_QUALIFICATION = "QUALIFICATION";
    String SEQUENCE_TEMPLATE_NEGOTIATION = "NEGOTIATION";
    String SEQUENCE_TEMPLATE_RE_ENGAGEMENT = "RE_ENGAGEMENT";

    // erp-crm/step-completion-condition 字典
    String STEP_COMPLETION_CALL_COMPLETED = "CALL_COMPLETED";
    String STEP_COMPLETION_EMAIL_OPENED = "EMAIL_OPENED";
    String STEP_COMPLETION_EMAIL_REPLIED = "EMAIL_REPLIED";
    String STEP_COMPLETION_MEETING_HELD = "MEETING_HELD";
    String STEP_COMPLETION_TASK_DONE = "TASK_DONE";

    // erp-crm/sequence-progress-status 字典
    String SEQUENCE_PROGRESS_IN_PROGRESS = "IN_PROGRESS";
    String SEQUENCE_PROGRESS_COMPLETED = "COMPLETED";
    String SEQUENCE_PROGRESS_SKIPPED = "SKIPPED";

    // erp-crm/seq-assignment-condition-type 字典
    String SEQ_ASSIGNMENT_CONDITION_LEAD_SOURCE = "LEAD_SOURCE";
    String SEQ_ASSIGNMENT_CONDITION_TERRITORY = "TERRITORY";
    String SEQ_ASSIGNMENT_CONDITION_PRODUCT_LINE = "PRODUCT_LINE";
    String SEQ_ASSIGNMENT_CONDITION_CUSTOM_FIELD = "CUSTOM_FIELD";

    // 通知事件类型：序列逾期提醒（对应 erp_sys_notification_template.notification_type）。
    String NOTIFY_EVENT_SEQUENCE_OVERDUE = "crm.sequence-overdue";
}
