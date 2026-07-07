package app.erp.crm.service;

import io.nop.api.core.config.AppConfig;

/**
 * CRM 域配置 reader。集中存放各 {@link ErpCrmConstants#CONFIG_*} 键的默认值与读取入口，
 * 便于单测注入与产品化裁剪（对齐 0700-1 范式）。键名常量定义在 {@link ErpCrmConstants}。
 */
public interface ErpCrmConfigs {

    /** 单个配置器允许的最大规则数（默认 100）。 */
    static int cpqMaxRulesPerConfigurator() {
        return AppConfig.var(ErpCrmConstants.CONFIG_CPQ_MAX_RULES_PER_CONFIGURATOR, 100);
    }

    /** 是否启用引导式向导（默认 true；false=单页配置）。 */
    static boolean cpqEnableWizard() {
        return AppConfig.var(ErpCrmConstants.CONFIG_CPQ_ENABLE_WIZARD, Boolean.TRUE);
    }

    /** 定价默认币种（默认 CNY）。 */
    static String cpqDefaultCurrency() {
        return AppConfig.var(ErpCrmConstants.CONFIG_CPQ_DEFAULT_CURRENCY, "CNY");
    }

    // ===== 销售序列 + 漏斗分析（plan 2026-07-07-1430-3） =====

    /** Lead 进入 QUALIFIED 时是否自动分配序列（默认 true）。 */
    static boolean sequenceAutoAssignOnQualify() {
        return AppConfig.var(ErpCrmConstants.CONFIG_SEQUENCE_AUTO_ASSIGN_ON_QUALIFY, Boolean.TRUE);
    }

    /** 步骤逾期宽限期天数（超过 dueDays + grace 视为逾期，默认 2）。 */
    static int sequenceGracePeriodDays() {
        return AppConfig.var(ErpCrmConstants.CONFIG_SEQUENCE_GRACE_PERIOD_DAYS, 2);
    }

    /** 连续逾期步骤上限，超过则提醒（默认 3）。 */
    static int sequenceMaxOverdueSteps() {
        return AppConfig.var(ErpCrmConstants.CONFIG_SEQUENCE_MAX_OVERDUE_STEPS, 3);
    }

    /** 定时序列逾期检查 cron（空=不调度）。 */
    static String sequenceOverdueCheckCron() {
        return AppConfig.var(ErpCrmConstants.CONFIG_SEQUENCE_OVERDUE_CHECK_CRON, "");
    }

    /** 无匹配规则时的默认序列模板类型（默认 NEW_LEAD）。 */
    static String sequenceDefaultTemplate() {
        return AppConfig.var(ErpCrmConstants.CONFIG_SEQUENCE_DEFAULT_TEMPLATE,
                ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
    }

    /** 漏斗聚合定时 cron（空=不调度）。 */
    static String funnelAggregationCron() {
        return AppConfig.var(ErpCrmConstants.CONFIG_FUNNEL_AGGREGATION_CRON, "");
    }

    /** 漏斗快照保留月数（默认 24）。 */
    static int funnelRetentionPeriodMonths() {
        return AppConfig.var(ErpCrmConstants.CONFIG_FUNNEL_RETENTION_PERIOD_MONTHS, 24);
    }

    /** 漏斗每阶段丢失原因 TOP N（默认 5）。 */
    static int funnelTopLostReasons() {
        return AppConfig.var(ErpCrmConstants.CONFIG_FUNNEL_TOP_LOST_REASONS, 5);
    }
}
