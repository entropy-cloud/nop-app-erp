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

    /** 损益汇总自动计算默认关闭（双层门控第二层，需显式开启）。 */
    boolean DEFAULT_PNL_AUTO_CALC_ENABLED = false;

    /** 项目结算强制审批默认启用。 */
    boolean DEFAULT_SETTLEMENT_REQUIRE_APPROVAL = true;

    /** 任务依赖上行链深度上限默认 100（对齐 task-dag.md §2.3，防恶意长链耗尽栈/堆）。 */
    int DEFAULT_TASK_DEPENDENCY_MAX_DEPTH = 100;

    /** 任务 startTask 前置任务完成强校验默认启用（STRICT 模式；对齐 task-dag.md §4.3）。 */
    boolean DEFAULT_TASK_STRICT_PREDECESSOR_CHECK = true;

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

    static boolean pnlAutoCalcEnabled() {
        Boolean flag = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_PNL_AUTO_CALC_ENABLED, DEFAULT_PNL_AUTO_CALC_ENABLED);
        return flag != null && flag;
    }

    static boolean settlementRequireApproval() {
        Boolean flag = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_SETTLEMENT_REQUIRE_APPROVAL, DEFAULT_SETTLEMENT_REQUIRE_APPROVAL);
        return flag == null || flag;
    }

    static String pnlCalcCron() {
        return io.nop.api.core.config.AppConfig.var(ErpPrjConstants.CONFIG_PNL_CALC_CRON, "");
    }

    static int taskDependencyMaxDepth() {
        Integer depth = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_TASK_DEPENDENCY_MAX_DEPTH,
                ErpPrjConfigs.DEFAULT_TASK_DEPENDENCY_MAX_DEPTH);
        if (depth == null || depth <= 0) {
            return ErpPrjConfigs.DEFAULT_TASK_DEPENDENCY_MAX_DEPTH;
        }
        return depth;
    }

    static boolean taskStrictPredecessorCheck() {
        Boolean flag = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_TASK_STRICT_PREDECESSOR_CHECK,
                ErpPrjConfigs.DEFAULT_TASK_STRICT_PREDECESSOR_CHECK);
        return flag == null || flag;
    }
}
