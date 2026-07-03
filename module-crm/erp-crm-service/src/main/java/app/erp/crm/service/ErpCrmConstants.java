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

    // erp-crm/forecast-period-status 字典
    String FORECAST_PERIOD_STATUS_OPEN = "OPEN";
    String FORECAST_PERIOD_STATUS_CLOSED = "CLOSED";
    String FORECAST_PERIOD_STATUS_FROZEN = "FROZEN";

    // erp-crm/forecast-category 字典
    String FORECAST_CATEGORY_COMMIT = "COMMIT";
    String FORECAST_CATEGORY_UPSIDE = "UPSIDE";
    String FORECAST_CATEGORY_BEST_CASE = "BEST_CASE";
}
