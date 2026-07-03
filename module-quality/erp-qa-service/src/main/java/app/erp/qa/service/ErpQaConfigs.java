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
