package app.erp.qa.service;

import io.nop.api.core.config.AppConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 质量域配置读取助手。配置项权威：{@code docs/plans/2026-07-02-2237-3-...} Infrastructure And Config Prereqs。
 *
 * <p>所有配置经 {@link AppConfig#var(String, String)} 读取，无 .env/外部服务。
 */
public final class ErpQaConfigs {

    private ErpQaConfigs() {
    }

    /** 强制质检的业务单据类型列表（逗号分隔）；空配置=不强制。 */
    public static List<String> getMandatoryInspectionBillTypes() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_MANDATORY_INSPECTION_BILL_TYPES, "");
        return parseCsv(raw);
    }

    /** billType 是否属强制质检类型。 */
    public static boolean isMandatoryInspectionBill(String billType) {
        if (billType == null || billType.isEmpty()) {
            return false;
        }
        return getMandatoryInspectionBillTypes().contains(billType);
    }

    /** 全局默认质检模板 ID；空=无。 */
    public static Long getDefaultInspectionTemplateId() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_DEFAULT_INSPECTION_TEMPLATE, "");
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** REJECTED 时是否自动生成 NCR（默认 true）。 */
    public static boolean isAutoCreateNcrOnReject() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_AUTO_CREATE_NCR_ON_REJECT, "true");
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    /** NCR 过账模式：AUTO_POST（resolve 自动过账）/ MANUAL_POST（人工 postNcr 触发）。默认 AUTO_POST。 */
    public static String getNcrPostingMode() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_NCR_POSTING_MODE, ErpQaConstants.NCR_POSTING_MODE_AUTO);
        if (raw == null || raw.trim().isEmpty()) {
            return ErpQaConstants.NCR_POSTING_MODE_AUTO;
        }
        return raw.trim();
    }

    /** NCR 过账是否自动模式（resolve 时自动触发）。 */
    public static boolean isNcrAutoPosting() {
        return ErpQaConstants.NCR_POSTING_MODE_AUTO.equalsIgnoreCase(getNcrPostingMode());
    }

    /** 召回是否强制审批（默认 true：OPEN→APPROVED 须经 submit/approve）。 */
    public static boolean isRecallRequireApproval() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_RECALL_REQUIRE_APPROVAL, "true");
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    /** 召回关闭是否要求全部目标已通知（默认 true）。 */
    public static boolean isRecallNotifyRequiredToClose() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_RECALL_NOTIFY_REQUIRED_TO_CLOSE, "true");
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    /** 库存追溯链是否启用（inventory 既有配置，召回定位消费）。 */
    public static boolean isTraceChainEnabled() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_INV_TRACE_CHAIN_ENABLED, "true");
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    // ---- SPC（2.4b，spc.md）----

    /** SPC 采样 cron 表达式；空=不采样（双层门控第一层）。 */
    public static String getSpcSamplingCron() {
        return AppConfig.var(ErpQaConstants.CONFIG_SPC_SAMPLING_CRON, "");
    }

    /** SPC 过程能力分析 cron 表达式；空=不计算（门控第一层）。 */
    public static String getSpcCapabilityCron() {
        return AppConfig.var(ErpQaConstants.CONFIG_SPC_CAPABILITY_CRON, "");
    }

    /** SPC 总开关；默认 false。 */
    public static boolean isSpcEnabled() {
        Boolean flag = AppConfig.var(ErpQaConstants.CONFIG_SPC_ENABLED, Boolean.FALSE);
        return flag != null && flag;
    }

    /** 失控样本自动建 NCR 开关；默认 true。 */
    public static boolean isSpcAutoNcrEnabled() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_SPC_AUTO_NCR_ENABLED, "true");
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    // ---- 看板 SPC 失控预警纳入开关（dashboards.md §9）----

    /** SPC 失控预警看板是否纳入 INADEQUATE 能力图数（默认 true）。 */
    public static boolean isDashQaSpcIncludeInadequate() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_DASH_QA_SPC_INCLUDE_INADEQUATE, "true");
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    /** SPC 失控预警看板是否纳入待处置 SPC NCR 计数（默认 true）。 */
    public static boolean isDashQaSpcIncludeNcr() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_DASH_QA_SPC_INCLUDE_NCR, "true");
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    /** 看板 SPC 控制图默认 chartId（null=未配置时取最近一张 ErpQaSpcChart；plan 2026-07-17-2010-1）。 */
    public static Long getDashQaSpcDefaultChartId() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_DASH_QA_SPC_DEFAULT_CHART_ID, "");
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 看板 SPC 计数型（P/NP/C/U）控制图默认 chartId（null=未配置；plan 2026-07-19-0120-2 Phase 1 Decision (a)）。 */
    public static Long getDashQaSpcDefaultAttributesChartId() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_DASH_QA_SPC_DEFAULT_ATTRIBUTES_CHART_ID, "");
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> parseCsv(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }
        for (String part : Arrays.asList(raw.split(","))) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
