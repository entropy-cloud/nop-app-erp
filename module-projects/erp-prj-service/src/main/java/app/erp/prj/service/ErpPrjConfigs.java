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

    /** 物料（采购入库）归集默认启用。 */
    boolean DEFAULT_MATERIAL_AGGREGATION_ENABLED = true;

    /** 损益汇总自动计算默认关闭（双层门控第二层，需显式开启）。 */
    boolean DEFAULT_PNL_AUTO_CALC_ENABLED = false;

    /** 损益汇总 cron 默认值（每日凌晨 1 点；对齐 job.yaml cronExpr @cfg 默认，空值=禁用语义）。 */
    String DEFAULT_PNL_CALC_CRON = "0 0 1 * * ?";

    /** 项目结算强制审批默认启用。 */
    boolean DEFAULT_SETTLEMENT_REQUIRE_APPROVAL = true;

    /** 结算质保金留存比例默认 0（设计性 opt-in：留存逻辑存在且配置驱动，零为显式 opt-in 默认非静默缺失；
     *  RC-R1.63 / P1-RC-052 D1 选项 A）。 */
    java.math.BigDecimal DEFAULT_SETTLEMENT_RETENTION_RATIO = java.math.BigDecimal.ZERO;

    /** 结算质保金到期月数默认 12。 */
    int DEFAULT_SETTLEMENT_RETENTION_DUE_MONTHS = 12;

    /** 任务依赖上行链深度上限默认 100（对齐 task-dag.md §2.3，防恶意长链耗尽栈/堆）。 */
    int DEFAULT_TASK_DEPENDENCY_MAX_DEPTH = 100;

    /** 任务 startTask 前置任务完成强校验默认启用（STRICT 模式；对齐 task-dag.md §4.3）。 */
    boolean DEFAULT_TASK_STRICT_PREDECESSOR_CHECK = true;

    /** closeProject 任务结束前置校验默认启用（STRICT 模式；对齐 state-machine.md §迁移完整性 OPEN→COMPLETED）。 */
    boolean DEFAULT_STRICT_PROJECT_TASK_COMPLETION_CHECK = true;

    /** startProject 字段前置校验默认启用（STRICT 模式；对齐 state-machine.md §迁移完整性 DRAFT→OPEN）。 */
    boolean DEFAULT_STRICT_PROJECT_START_PRECHECK = true;

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

    static boolean materialAggregationEnabled() {
        Boolean flag = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_MATERIAL_AGGREGATION_ENABLED, DEFAULT_MATERIAL_AGGREGATION_ENABLED);
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

    /** 结算质保金留存比例（默认 0=设计性 opt-in；非法值回退 0）。 */
    static java.math.BigDecimal settlementRetentionRatio() {
        String v = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_RATIO, "0");
        if (v == null || v.trim().isEmpty()) {
            return DEFAULT_SETTLEMENT_RETENTION_RATIO;
        }
        try {
            return new java.math.BigDecimal(v.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_SETTLEMENT_RETENTION_RATIO;
        }
    }

    /** 结算质保金到期月数（默认 12；非正数回退默认）。 */
    static int settlementRetentionDueMonths() {
        Integer months = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_DUE_MONTHS,
                DEFAULT_SETTLEMENT_RETENTION_DUE_MONTHS);
        if (months == null || months <= 0) {
            return DEFAULT_SETTLEMENT_RETENTION_DUE_MONTHS;
        }
        return months;
    }

    /** 损益汇总 cron（默认 {@code 0 0 1 * * ?}；显式置空=禁用——「空值=跳过」语义，消费点：
     *  job.yaml cronExpr {@code @cfg} 引用 + {@code ErpPrjProjectPnlCalcHelper} 门控）。 */
    static String pnlCalcCron() {
        String cron = io.nop.api.core.config.AppConfig.var(ErpPrjConstants.CONFIG_PNL_CALC_CRON,
                ErpPrjConfigs.DEFAULT_PNL_CALC_CRON);
        if (cron == null || cron.trim().isEmpty()) {
            return "";
        }
        return cron.trim();
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

    static boolean strictProjectTaskCompletionCheck() {
        Boolean flag = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_STRICT_PROJECT_TASK_COMPLETION_CHECK,
                ErpPrjConfigs.DEFAULT_STRICT_PROJECT_TASK_COMPLETION_CHECK);
        return flag == null || flag;
    }

    static boolean strictProjectStartPrecheck() {
        Boolean flag = io.nop.api.core.config.AppConfig.var(
                ErpPrjConstants.CONFIG_STRICT_PROJECT_START_PRECHECK,
                ErpPrjConfigs.DEFAULT_STRICT_PROJECT_START_PRECHECK);
        return flag == null || flag;
    }
}
