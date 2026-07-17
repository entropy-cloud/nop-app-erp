package app.erp.mfg.service;

import io.nop.api.core.config.AppConfig;

/**
 * 制造域配置读取助手。配置项权威：{@code ErpMfgConstants} 与对应 plan。
 *
 * <p>所有配置经 {@link AppConfig#var(String, String)} 读取，无 .env/外部服务。
 */
public final class ErpMfgConfigs {

    private ErpMfgConfigs() {
    }

    /** 看板 CRP 负荷图默认向前窗口天数（dateFrom/dateTo 缺省时取近 N 天；plan 2026-07-17-2010-1）。 */
    public static int getDashMfgCrpDefaultDays() {
        Integer n = AppConfig.var(ErpMfgConstants.CONFIG_DASH_MFG_CRP_DEFAULT_DAYS,
                ErpMfgConstants.DEFAULT_DASH_MFG_CRP_DEFAULT_DAYS);
        if (n == null || n <= 0) {
            return ErpMfgConstants.DEFAULT_DASH_MFG_CRP_DEFAULT_DAYS;
        }
        return n;
    }
}
