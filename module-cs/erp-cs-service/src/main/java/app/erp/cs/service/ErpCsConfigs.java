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
