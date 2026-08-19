package app.erp.drp.service;

/**
 * DRP 域配置项。字典码值权威：`erp-drp-meta/.../dict/*.dict.yaml` + `module-drp/model/app-erp-drp.orm.xml`。
 *
 * <p>所有配置项经 {@code AppConfig.var(..., defaultValue)} 读取，无 .env/外部服务/密钥。
 *
 * <p>权威：`docs/design/drp/README.md`、`docs/design/drp/safety-stock-optimization.md`、
 * `docs/plans/2026-07-04-1115-2-drp-net-requirement-safety-stock.md`。
 */
public interface ErpDrpConfigs {

    // DRP 净需求：是否消费 APPROVED 预测行填充 forecastDemand（plan 2026-07-05-0427-1 §Phase 3；默认 true）。
    // false 时 forecastDemand=0（与基线一致，回归兼容）。
    String CONFIG_DRP_FORECAST_CONSUME_ENABLED = "erp-drp.forecast-consume-enabled";
    boolean DEFAULT_DRP_FORECAST_CONSUME_ENABLED = true;

    // DRP 净需求：预测需求默认 horizon 天数（无后端预测实体时，forecastDemand 默认 0/手工录入）
    String CONFIG_DRP_DEFAULT_FORECAST_HORIZON_DAYS = "erp-inv.drp-default-forecast-horizon-days";
    int DEFAULT_DRP_DEFAULT_FORECAST_HORIZON_DAYS = 30;

    // DRP 释放：是否自动为 APPROVED 行生成下游单据（默认 false，需人工触发 releaseApproved）
    String CONFIG_DRP_AUTO_GENERATE_ORDER = "erp-inv.drp-auto-generate-order";
    boolean DEFAULT_DRP_AUTO_GENERATE_ORDER = false;

    // 安全库存优化：默认计算方法（STATISTICAL/SIMPLE/DDMRP）
    String CONFIG_DRP_SS_METHOD = "erp-inv.drp-ss-method";
    String DEFAULT_DRP_SS_METHOD = "STATISTICAL";

    // 安全库存优化：默认服务水平（PCT95/PCT97_5/PCT99/PCT99_5）
    String CONFIG_DRP_SS_DEFAULT_SERVICE_LEVEL = "erp-inv.drp-ss-default-service-level";
    String DEFAULT_DRP_SS_DEFAULT_SERVICE_LEVEL = "PCT95";

    // 安全库存优化：默认分析历史月数
    String CONFIG_DRP_SS_HISTORY_MONTHS = "erp-inv.drp-ss-history-months";
    int DEFAULT_DRP_SS_HISTORY_MONTHS = 6;

    // 安全库存优化：零需求月处理策略（EXCLUDE=排除零需求月，KEEP=保留参与标准差计算）
    String CONFIG_DRP_SS_ZERO_DEMAND_POLICY = "erp-inv.drp-ss-zero-demand-policy";
    String DEFAULT_DRP_SS_ZERO_DEMAND_POLICY = "EXCLUDE";

    // 安全库存优化：是否自动回写 ErpDrpParameter.safetyStock（默认 false，强制人工确认门 confirmWriteback）
    String CONFIG_DRP_SS_AUTO_WRITEBACK = "erp-inv.drp-ss-auto-writeback";
    boolean DEFAULT_DRP_SS_AUTO_WRITEBACK = false;

    // ---- 越库执行（RC-R1.81；权威：docs/design/drp/cross-dock.md §配置点） ----

    // 越库功能总开关（默认 false，整体 opt-in；所有越库 mutation 经此门控）
    String CONFIG_DRP_XDOCK_ENABLED = "erp-inv.drp-xdock-enabled";
    boolean DEFAULT_DRP_XDOCK_ENABLED = false;

    // 越库暂存超时（小时），超时未匹配自动转正常入库
    String CONFIG_DRP_XDOCK_STAGING_TIMEOUT = "erp-inv.drp-xdock-staging-timeout";
    int DEFAULT_DRP_XDOCK_STAGING_TIMEOUT = 24;

    // 默认匹配策略（matchingStrategy 列为 null 时回落）
    String CONFIG_DRP_XDOCK_DEFAULT_STRATEGY = "erp-inv.drp-xdock-default-strategy";
    String DEFAULT_DRP_XDOCK_DEFAULT_STRATEGY = "ON_RECEIPT";

    // 越库质检守卫开关（D2 裁决：物料存在有效检验模板即视为需质检，match 前置守卫；默认 false）
    String CONFIG_DRP_XDOCK_QUALITY_GATE_ENABLED = "erp-inv.drp-xdock-quality-gate-enabled";
    boolean DEFAULT_DRP_XDOCK_QUALITY_GATE_ENABLED = false;

    // 越库暂存超时扫描 cron（空值=跳过，R1.38 范式「不调度」语义）
    String CONFIG_DRP_XDOCK_STAGING_TIMEOUT_CRON = "erp-inv.drp-xdock-staging-timeout-cron";
    String DEFAULT_DRP_XDOCK_STAGING_TIMEOUT_CRON = "";

    // ---- 提前期跟踪（RC-R1.82；权威：docs/design/drp/lead-time-tracking.md） ----

    // 准时判定容差系数（actualLeadTime 在 expected×(1±容差) 内为 ON_TIME；lead-time-tracking.md:77 默认 0.1）
    String CONFIG_DRP_LT_TOLERANCE = "erp-inv.drp-lt-tolerance";
    String DEFAULT_DRP_LT_TOLERANCE = "0.1";

    // 提前期统计窗口天数（D5 裁决：实时计算不留列，窗口可控；<=0 视为全历史）
    String CONFIG_DRP_LT_STATS_WINDOW_DAYS = "erp-inv.drp-lt-stats-window-days";
    int DEFAULT_DRP_LT_STATS_WINDOW_DAYS = 365;

    // 联合变分接入的最小样本数（L1 UC-DRP-08 字面「样本 <5 降级」→ 样本不足走标准公式）
    int LT_JOINT_VARIATION_MIN_SAMPLES = 5;
}
