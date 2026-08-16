package app.erp.inv.service;

import io.nop.api.core.config.AppConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;

public interface ErpInvConfigs {

    String CONFIG_COST_SCALE = "erp.inv.costing.unit-cost-scale";

    int DEFAULT_COST_SCALE = 4;

    static int costScale() {
        return AppConfig.var(CONFIG_COST_SCALE, DEFAULT_COST_SCALE);
    }

    static BigDecimal roundCost(BigDecimal value) {
        return value == null ? null : value.setScale(costScale(), RoundingMode.HALF_UP);
    }

    /** 盘点差异移动单生成失败告警总开关（RC-R1.56 D4-b；默认 false = 失败仅 LOG.warn 不派发 notify 告警）。 */
    static boolean isStocktakeDiffAlertEnabled() {
        Boolean flag = AppConfig.var(ErpInvConstants.CONFIG_STOCKTAKE_DIFF_ALERT_ENABLED, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }
}
