package app.erp.cs.service;

import io.nop.api.core.config.AppConfig;

/**
 * 客服域配置读取助手。配置项权威：{@code docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md} Infrastructure And Config Prereqs。
 *
 * <p>所有配置经 {@link AppConfig#var(String, String)} 读取，无 .env/外部服务。
 */
public final class ErpCsConfigs {

    private ErpCsConfigs() {
    }

    /** SLA 计时是否启用（默认 true）。 */
    public static boolean isSlaEnabled() {
        return boolVar(ErpCsConstants.CONFIG_SLA_ENABLED, true);
    }

    /** SLA 超时预警提前量（分钟，默认 60）。 */
    public static int getSlaWarningBeforeMinutes() {
        return intVar(ErpCsConstants.CONFIG_SLA_WARNING_BEFORE, 60);
    }

    /** 新建工单是否自动分派（默认 true）。 */
    public static boolean isAutoAssignOnCreate() {
        return boolVar(ErpCsConstants.CONFIG_AUTO_ASSIGN_ON_CREATE, true);
    }

    /** 满意度调查是否启用（默认 true）。 */
    public static boolean isSurveyEnabled() {
        return boolVar(ErpCsConstants.CONFIG_SURVEY_ENABLED, true);
    }

    /** 触发调查的工单状态（默认 RESOLVED）。 */
    public static String getSurveyTriggerStatus() {
        String raw = AppConfig.var(ErpCsConstants.CONFIG_SURVEY_TRIGGER_STATUS, ErpCsConstants.TICKET_STATUS_RESOLVED);
        if (raw == null || raw.trim().isEmpty()) {
            return ErpCsConstants.TICKET_STATUS_RESOLVED;
        }
        return raw.trim();
    }

    /** 调查触发后延迟发送小时数（默认 0 = 立即发送）。 */
    public static int getSurveySendDelayHours() {
        return intVar(ErpCsConstants.CONFIG_SURVEY_SEND_DELAY, 0);
    }

    public static boolean isSurveyCsatEnabled() {
        return boolVar(ErpCsConstants.CONFIG_SURVEY_CSAT_ENABLED, true);
    }

    public static boolean isSurveyNpsEnabled() {
        return boolVar(ErpCsConstants.CONFIG_SURVEY_NPS_ENABLED, false);
    }

    public static boolean isSurveyCesEnabled() {
        return boolVar(ErpCsConstants.CONFIG_SURVEY_CES_ENABLED, false);
    }

    /** 未响应调查提醒延迟（小时，默认 48）。 */
    public static int getSurveyReminderHours() {
        return intVar(ErpCsConstants.CONFIG_SURVEY_REMINDER_HOURS, 48);
    }

    /** 调查链接有效期（天，默认 7）。 */
    public static int getSurveyExpireDays() {
        return intVar(ErpCsConstants.CONFIG_SURVEY_EXPIRE_DAYS, 7);
    }

    /** SLA 超期/预警通知派发是否启用（默认 true；plan 2026-07-06-0642-1 §Phase 1）。 */
    public static boolean isSlaNotifyEnabled() {
        return boolVar(ErpCsConstants.CONFIG_SLA_NOTIFY_ENABLED, true);
    }

    // === 客户服务权益 / 服务目录（plan 2026-07-07-1430-1）===

    /** 创建工单时是否校验权益（默认 true；entitlement.md §五）。 */
    public static boolean isEntitlementCheckEnabled() {
        return boolVar(ErpCsConstants.CONFIG_ENTITLEMENT_CHECK_ENABLED, true);
    }

    /** 无有效权益时是否允许创建工单（默认 true；entitlement.md §五）。 */
    public static boolean isAllowNoEntitlement() {
        return boolVar(ErpCsConstants.CONFIG_ENTITLEMENT_ALLOW_NO_ENTITLEMENT, true);
    }

    /** 权益到期预警提前天数（默认 30；entitlement.md §五）。 */
    public static int getEntitlementExpiryWarningDays() {
        return intVar(ErpCsConstants.CONFIG_ENTITLEMENT_EXPIRY_WARNING_DAYS, 30);
    }

    /** 销售出库是否自动创建保修权益（默认 false；entitlement.md §五 Non-Goal）。 */
    public static boolean isAutoWarrantyEnabled() {
        return boolVar(ErpCsConstants.CONFIG_ENTITLEMENT_AUTO_WARRANTY, false);
    }

    /** 服务目录是否启用（默认 true；service-catalog.md §六）。 */
    public static boolean isServiceCatalogEnabled() {
        return boolVar(ErpCsConstants.CONFIG_SERVICE_CATALOG_ENABLED, true);
    }

    /** 是否允许客户自助提交（默认 true；service-catalog.md §六 Non-Goal — 前端门户归 successor）。 */
    public static boolean isServiceCatalogSelfService() {
        return boolVar(ErpCsConstants.CONFIG_SERVICE_CATALOG_SELF_SERVICE, true);
    }

    /** 目录分类最大深度（默认 3；service-catalog.md §六）。 */
    public static int getCatalogCategoryMaxDepth() {
        return intVar(ErpCsConstants.CONFIG_CATALOG_CATEGORY_MAX_DEPTH, 3);
    }

    // === 知识库搜索/建议（plan 2026-07-08-0056-2）===

    /** 知识库搜索默认返回条数（默认 5；UC-CS-05 Top 5）。 */
    public static int getKnowledgeSearchDefaultLimit() {
        return intVar(ErpCsConstants.CONFIG_KNOWLEDGE_SEARCH_DEFAULT_LIMIT, 5);
    }

    /** 知识库搜索最大返回条数（默认 20，防滥用）。 */
    public static int getKnowledgeSearchMaxLimit() {
        return intVar(ErpCsConstants.CONFIG_KNOWLEDGE_SEARCH_MAX_LIMIT, 20);
    }

    private static boolean boolVar(String key, boolean defaultValue) {
        String raw = AppConfig.var(key, String.valueOf(defaultValue));
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static int intVar(String key, int defaultValue) {
        String raw = AppConfig.var(key, String.valueOf(defaultValue));
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
