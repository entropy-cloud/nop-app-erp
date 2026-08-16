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

    /**
     * BOM 快照策略（plan 2026-08-16-0904-1 RC-R1.49）。空值/未知值 = 默认 LOCK_AT_CREATION。
     * 读侧（齐套/工序标准）按此决定：LOCK_AT_CREATION=恒用快照；AUTO_UPGRADE=re-resolve 默认 BOM 实时展开。
     */
    public static String getBomSnapshotStrategy() {
        String raw = AppConfig.var(ErpMfgConstants.CONFIG_BOM_SNAPSHOT_STRATEGY, null);
        if (raw == null || raw.trim().isEmpty()) {
            return ErpMfgConstants.DEFAULT_BOM_SNAPSHOT_STRATEGY;
        }
        return raw.trim();
    }

    public static boolean isBomSnapshotAutoUpgrade() {
        return ErpMfgConstants.BOM_SNAPSHOT_STRATEGY_AUTO_UPGRADE.equals(getBomSnapshotStrategy());
    }
}
