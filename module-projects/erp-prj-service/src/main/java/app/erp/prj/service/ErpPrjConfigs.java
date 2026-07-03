package app.erp.prj.service;

/**
 * 项目域配置默认值集中入口。所有 {@code erp-prj.*} 配置键经 {@link io.nop.api.core.config.AppConfig#var}
 * 读取时，统一经本接口提供默认值与解释器，避免散落在业务代码中。
 */
public interface ErpPrjConfigs {

    /** 默认预算控制模式（WARNING）。 */
    String DEFAULT_BUDGET_CONTROL_MODE = ErpPrjConstants.BUDGET_MODE_WARNING;

    /** 费用报销归集默认启用。 */
    boolean DEFAULT_EXPENSE_AGGREGATION_ENABLED = true;

    static String budgetControlMode() {
        String mode = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_BUDGET_CONTROL_MODE, DEFAULT_BUDGET_CONTROL_MODE);
        if (mode == null || mode.trim().isEmpty()) {
            return DEFAULT_BUDGET_CONTROL_MODE;
        }
        return mode.trim().toUpperCase();
    }

    static boolean budgetControlStrict() {
        return ErpPrjConstants.BUDGET_MODE_STRICT.equals(budgetControlMode());
    }

    static String defaultLaborCostRate() {
        String rate = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_DEFAULT_LABOR_COST_RATE, "");
        if (rate == null || rate.trim().isEmpty()) {
            return null;
        }
        return rate.trim();
    }

    static String defaultPayrollSubjectCode() {
        String code = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "");
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return code.trim();
    }

    static boolean expenseAggregationEnabled() {
        Boolean flag = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_EXPENSE_AGGREGATION_ENABLED, DEFAULT_EXPENSE_AGGREGATION_ENABLED);
        return flag == null || flag;
    }
}
