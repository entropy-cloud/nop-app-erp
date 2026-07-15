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
}
