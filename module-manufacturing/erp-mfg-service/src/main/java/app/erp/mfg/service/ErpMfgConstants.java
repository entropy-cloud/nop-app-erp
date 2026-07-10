package app.erp.mfg.service;

/**
 * 制造域常量。字典码值权威：`erp-mfg-meta/.../dict/*.dict.yaml` + `module-manufacturing/model/*.orm.xml`。
 *
 * <p>权威：`docs/design/manufacturing/bom-and-routing.md`、`docs/design/manufacturing/state-machine.md`、
 * `docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`、`docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`。
 */
public interface ErpMfgConstants {

    // BOM 类型（erp-mfg/bom-type）
    String BOM_TYPE_MANUFACTURED = "NORMAL";
    String BOM_TYPE_PHANTOM = "PHANTOM";

    // 成本滚算状态（erp-mfg/cost-rollup-status）
    String COST_ROLLUP_STATUS_DRAFT = "DRAFT";
    String COST_ROLLUP_STATUS_CALCULATED = "CALCULATED";
    String COST_ROLLUP_STATUS_FIRMED = "FIRMED";

    // 多级 BOM 展开深度上限 / 环兜底（`erp-mfg.bom-max-depth`）
    String CONFIG_BOM_MAX_DEPTH = "erp-mfg.bom-max-depth";
    int DEFAULT_BOM_MAX_DEPTH = 15;

    // 工单状态（erp-mfg/work-order-status，10 态；权威：state-machine.md §适用对象一）
    String WORK_ORDER_STATUS_DRAFT = "DRAFT";
    String WORK_ORDER_STATUS_SUBMITTED = "SUBMITTED";
    String WORK_ORDER_STATUS_NOT_STARTED = "NOT_STARTED";
    String WORK_ORDER_STATUS_IN_PROCESS = "IN_PROCESS";
    String WORK_ORDER_STATUS_STOCK_RESERVED = "STOCK_RESERVED";
    String WORK_ORDER_STATUS_STOCK_PARTIAL = "STOCK_PARTIAL";
    String WORK_ORDER_STATUS_COMPLETED = "COMPLETED";
    String WORK_ORDER_STATUS_STOPPED = "STOPPED";
    String WORK_ORDER_STATUS_CLOSED = "CLOSED";
    String WORK_ORDER_STATUS_CANCELLED = "CANCELLED";

    // 审核状态（erp-mfg/approve-status，三轴审批的审核轴）
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";

    // 领料状态（erp-mfg/issue-status）
    String ISSUE_STATUS_DRAFT = "DRAFT";
    String ISSUE_STATUS_CONFIRMED = "CONFIRMED";
    String ISSUE_STATUS_DONE = "DONE";
    String ISSUE_STATUS_CANCELLED = "CANCELLED";

    // 作业卡状态（erp-mfg/job-card-status，8 态；权威：state-machine.md §适用对象二）
    String JOB_CARD_STATUS_OPEN = "OPEN";
    String JOB_CARD_STATUS_WORK_IN_PROGRESS = "WORK_IN_PROGRESS";
    String JOB_CARD_STATUS_PARTIALLY_TRANSFERRED = "PARTIALLY_TRANSFERRED";
    String JOB_CARD_STATUS_MATERIAL_TRANSFERRED = "MATERIAL_TRANSFERRED";
    String JOB_CARD_STATUS_ON_HOLD = "ON_HOLD";
    String JOB_CARD_STATUS_SUBMITTED = "SUBMITTED";
    String JOB_CARD_STATUS_COMPLETED = "COMPLETED";
    String JOB_CARD_STATUS_CANCELLED = "CANCELLED";

    // 工单行类型（ErpMfgWorkOrderLine.lineType 自由字符串）
    String WORK_ORDER_LINE_TYPE_OUTPUT = "OUTPUT";
    String WORK_ORDER_LINE_TYPE_INPUT = "INPUT";
    String WORK_ORDER_LINE_TYPE_BYPRODUCT = "BYPRODUCT";

    // 业务联动源单类型（manufacturing 发起的库存移动单 relatedBillType 自由字符串）
    String RELATED_BILL_TYPE_MFG_ISSUE = "ERP_MFG_ISSUE";
    String RELATED_BILL_TYPE_MFG_WORK_ORDER = "ERP_MFG_WORK_ORDER";

    // 库存移动单作业类型（对齐 ErpInvConstants 码值，避免跨 service 模块依赖）
    String MOVE_TYPE_OUTGOING_ISSUE = "OUTGOING";        // 领料出库（ErpInvConstants.MOVE_TYPE_OUTGOING）：库存域 bookCompletion 据此扣减余额
    String MOVE_TYPE_MANUFACTURING = "MANUFACTURE";         // 完工入库（ErpInvConstants.MOVE_TYPE_MANUFACTURING）：库存域视为入库，加产成品库存

    // 工单状态机配置项（无 .env/外部服务，经 AppConfig.var 读取）
    String CONFIG_ALLOW_PARTIAL_KIT_START = "erp-mfg.allow-partial-kit-start";
    String CONFIG_INSPECTION_GATE_ENABLED = "erp-mfg.inspection-gate-enabled";

    // 工单来源单据类型（erp-mfg/source-order-type，plan 2026-07-05-0427-3）
    String SOURCE_ORDER_TYPE_SALES_ORDER = "SALES_ORDER";
    String SOURCE_ORDER_TYPE_FORECAST = "FORECAST";
    String SOURCE_ORDER_TYPE_MANUAL = "MANUAL";
    String SOURCE_ORDER_TYPE_APS_SCHEDULE = "APS_SCHEDULE";

    // APS 排程→工序卡自动生成配置项（plan 2026-07-05-0427-3 §Infrastructure）。
    // jobcard-auto-generate-on-schedule：nop-job 批量入口总开关（默认 false=手动触发为主）。
    // jobcard-incremental-rebuild：重复调用时仅补建缺失工序卡而非抛错（默认 false=严格幂等抛错）。
    String CONFIG_JOBCARD_AUTO_GENERATE_ON_SCHEDULE = "erp-mfg.jobcard-auto-generate-on-schedule";
    String CONFIG_JOBCARD_INCREMENTAL_REBUILD = "erp-mfg.jobcard-incremental-rebuild";
    /** 定时 APS 排程→工序卡批量生成 cron（空=不调度；plan 2026-07-05-0427-3 §Decision）。 */
    String CONFIG_JOBCARD_AUTO_GENERATE_CRON = "erp-mfg.jobcard-auto-generate-cron";

    // MRP 计划状态（erp-mfg/mrp-status）
    String MRP_STATUS_DRAFT = "DRAFT";
    String MRP_STATUS_RUNNING = "RUNNING";
    String MRP_STATUS_COMPLETED = "COMPLETED";
    String MRP_STATUS_FIRMED = "FIRMED";
    String MRP_STATUS_CANCELLED = "CANCELLED";

    // MRP 计划订单类型（erp-mfg/mrp-order-type）
    String MRP_ORDER_TYPE_PLANNED_ORDER = "PLANNED_ORDER";
    String MRP_ORDER_TYPE_PURCHASE_REQUEST = "PURCHASE_REQUEST";
    String MRP_ORDER_TYPE_WORK_ORDER_REQUEST = "WORK_ORDER_REQUEST";
    String MRP_ORDER_TYPE_SUBCONTRACT_REQUEST = "SUBCONTRACT_REQUEST";

    // MRP 需求来源（erp-mfg/mrp-demand-source）
    String MRP_DEMAND_SOURCE_SALES_ORDER = "SALES_ORDER";
    String MRP_DEMAND_SOURCE_FORECAST = "FORECAST";
    String MRP_DEMAND_SOURCE_SAFETY_STOCK = "SAFETY_STOCK";
    String MRP_DEMAND_SOURCE_MANUAL = "MANUAL";

    // MRP 批量与提前期配置（经 AppConfig.var 读取；无 .env/外部服务）
    // default-lot-size: 0=lot-for-lot（净需求即建议量），>0 时按倍数向上取整
    String CONFIG_MRP_DEFAULT_LOT_SIZE = "erp-mfg.default-lot-size";
    int DEFAULT_MRP_DEFAULT_LOT_SIZE = 0;    // 制造件 routing 累计工时换算提前期天数：days = hours × days-per-routing-hour（默认 0.125=8h/天）
    String CONFIG_MFG_LEADTIME_DAYS_PER_ROUTING_HOUR = "erp-mfg.mfg-leadtime-days-per-routing-hour";
    double DEFAULT_MFG_LEADTIME_DAYS_PER_ROUTING_HOUR = 0.125;

    // 需求来源单据类型（自由字符串，写入 ErpMfgMrpDemand/PlanLine 的 source 字段用于 pegging 追溯）
    String SOURCE_BILL_TYPE_SAL_ORDER = "ERP_SAL_ORDER";
    String SOURCE_BILL_TYPE_MD_MATERIAL = "ERP_MD_MATERIAL";
    String SOURCE_BILL_TYPE_MRP_MANUAL = "ERP_MFG_MRP_DEMAND";
    String SOURCE_BILL_TYPE_MFG_FORECAST = "ERP_MFG_FORECAST";

    // 释放生成的目标单据 code 前缀
    String RELEASE_PO_CODE_PREFIX = "PO-MRP-";
    String RELEASE_WO_CODE_PREFIX = "WO-MRP-";

    // 销售订单作废状态（erp-sal/doc-status CANCELLED=30），需求整合排除作废单
    String SAL_DOC_STATUS_CANCELLED = "CANCELLED";

    // 班次类型（erp-mfg/shift-type）
    String SHIFT_TYPE_ONE_SHIFT = "ONE_SHIFT";
    String SHIFT_TYPE_MORNING = "MORNING";
    String SHIFT_TYPE_AFTERNOON = "AFTERNOON";
    String SHIFT_TYPE_NIGHT = "NIGHT";

    // 工作日模式（erp-mfg/work-date-pattern）
    String WORK_DATE_PATTERN_ALL_WEEK = "ALL_WEEK";
    String WORK_DATE_PATTERN_WEEKDAYS = "WEEKDAYS";
    String WORK_DATE_PATTERN_WEEKEND = "WEEKEND";

    // CRP 超负荷阈值（经 AppConfig.var 读取；默认 1.0=负荷率超过 100% 即告警）。无 .env/外部服务。
    String CONFIG_CRP_OVERLOAD_THRESHOLD = "erp-mfg.crp-overload-threshold";
    double DEFAULT_CRP_OVERLOAD_THRESHOLD = 1.0;

    // CRP 负荷来源（plan 2026-07-05-0306-2）。WORK_ORDER=按 WorkOrder 计划日期+RoutingOperation 标准工时均匀分派
    // （行为不变，默认）；APS=按已排程 ErpApsOperationOrder 排程时间精确分派到 workcenter×date，无排程时段的工单
    // 回退 WorkOrder 计划日期（混合 tolerated）。APS 模式需 APS 引擎（计划 0831-1）已落地+SPI 实现已注册。
    String CONFIG_CRP_LOAD_SOURCE = "erp-mfg.crp-load-source";
    String CRP_LOAD_SOURCE_WORK_ORDER = "WORK_ORDER";
    String CRP_LOAD_SOURCE_APS = "APS";

    /** 定时 CRP 负荷计算 cron（空=不调度；plan 2026-07-05-0306-1 §配置点）。 */
    String CONFIG_CRP_RUN_CRON = "erp-mfg.crp-run-cron";
    /** CRP 定时计算默认向前窗口（月），0=当月（plan 2026-07-05-0306-1 Decision）。 */
    String CONFIG_CRP_RUN_DEFAULT_WINDOW_MONTHS = "erp-mfg.crp-run-default-window-months";
    /** CONFIG_CRP_RUN_DEFAULT_WINDOW_MONTHS 缺省值。 */
    int DEFAULT_CRP_RUN_WINDOW_MONTHS = 0;

    // 需求预测状态（erp-mfg/forecast-status，4 态；权威：plan 2026-07-05-0427-1 §Goals）
    String FORECAST_STATUS_DRAFT = "DRAFT";
    String FORECAST_STATUS_APPROVED = "APPROVED";
    String FORECAST_STATUS_CONSUMED = "CONSUMED";
    String FORECAST_STATUS_CANCELLED = "CANCELLED";

    // 预测需求来源（写入 ErpMfgMrpDemand.demandSource 的 FORECAST 码值；与 erp-mfg/mrp-demand-source 字典一致）
    // 已在 MRP_DEMAND_SOURCE_FORECAST 常量中定义，不重复声明

    // MRP 消费预测总开关（plan 2026-07-05-0427-1 §Infrastructure）。默认 true=消费 APPROVED 预测行。
    String CONFIG_MFG_FORECAST_CONSUME_ENABLED = "erp-mfg.forecast-consume-enabled";
    boolean DEFAULT_MFG_FORECAST_CONSUME_ENABLED = true;

    // 生产差异自动计算开关（plan 2026-07-05-1838-2 §Goals）。默认 false=完工不自动触发差异计算，
    // 需手动经 ErpMfgCostVariance__calculateVariances 入口计算；true=工单完工（willFinish）时自动算。
    String CONFIG_VARIANCE_AUTO_CALC_ENABLED = "erp-mfg.variance-auto-calc-enabled";

    // 成本差异类型（erp-mfg/variance-type，plan 2026-07-05-1838-2）
    String VARIANCE_TYPE_MATERIAL_USAGE = "MATERIAL_USAGE";
    String VARIANCE_TYPE_LABOR_EFFICIENCY = "LABOR_EFFICIENCY";
    String VARIANCE_TYPE_LABOR_RATE = "LABOR_RATE";
    String VARIANCE_TYPE_OVERHEAD = "OVERHEAD";
    String VARIANCE_TYPE_VOLUME = "VOLUME";

    // 成本要素（erp-mfg/cost-element，plan 2026-07-05-1838-2）
    String COST_ELEMENT_MATERIAL = "MATERIAL";
    String COST_ELEMENT_LABOR = "LABOR";
    String COST_ELEMENT_OVERHEAD = "OVERHEAD";
    String COST_ELEMENT_SUBCONTRACT = "SUBCONTRACT";

    /** 生产差异阈值告警开关（默认 true；plan 2026-07-06-0642-1 §Phase 3）。关闭时跳过 notify 调用。 */
    String CONFIG_VARIANCE_ALERT_ENABLED = "erp-mfg.variance-alert-enabled";

    /** 生产差异告警阈值（本位币净金额绝对值；默认 100；plan 2026-07-06-0642-1 §Phase 3）。超阈值时派发 mfg.production-variance 通知。 */
    String CONFIG_VARIANCE_ALERT_THRESHOLD = "erp-mfg.variance-alert-threshold";

    /** CONFIG_VARIANCE_ALERT_THRESHOLD 缺省值（本位币）。 */
    java.math.BigDecimal DEFAULT_VARIANCE_ALERT_THRESHOLD = new java.math.BigDecimal("100");

    /** 通知事件类型：生产差异超阈值告警（对应 erp_sys_notification_template.notification_type；plan 2026-07-06-0642-1 §Phase 3）。 */
    String NOTIFY_EVENT_PRODUCTION_VARIANCE = "mfg.production-variance";

    // ---- 生产批次基因链追溯（plan 2026-07-07-0305-3；权威：docs/design/manufacturing/batch-genealogy.md） ----

    /** 完工写入基因链总开关（默认 true=完工时写入 input→output 消耗行；plan 2026-07-07-0305-3 §Phase 1 Decision 失败语义）。 */
    String CONFIG_GENEALOGY_WRITE_ENABLED = "erp-mfg.genealogy-write-enabled";
    /** 基因链递归追溯深度上限（默认 50，防环路/爆炸；plan 2026-07-07-0305-3 §Infrastructure）。 */
    String CONFIG_GENEALOGY_MAX_TRACE_DEPTH = "erp-mfg.genealogy-max-trace-depth";
    /** CONFIG_GENEALOGY_MAX_TRACE_DEPTH 缺省值。 */
    int DEFAULT_GENEALOGY_MAX_TRACE_DEPTH = 50;

    /** 基因链批次状态（自由字符串，对齐设计 batch-genealogy.md 数据模型 lotStatus 字段）。 */
    String LOT_STATUS_RELEASED = "RELEASED";
    String LOT_STATUS_QUARANTINE = "QUARANTINE";
    String LOT_STATUS_ACTIVE = "ACTIVE";
    String LOT_STATUS_REJECTED = "REJECTED";

    /** 基因链追溯方向（traceChain 入参 direction 自由字符串）。 */
    String TRACE_DIRECTION_FORWARD = "FORWARD";   // 成品 → 原料（用 output_lot 找 input 再向上游递归）
    String TRACE_DIRECTION_BACKWARD = "BACKWARD"; // 原料 → 成品（用 input 找 output 再向下游递归）

    /** 库存批次状态（对齐 erp-inv/batch-status，避免跨 service 模块依赖）。 */
    String INV_BATCH_STATUS_OPEN = "OPEN";

    /** 产出批次 batchNo 派生前缀（工单 code + 序号）。 */
    String GENEALOGY_OUTPUT_BATCH_PREFIX = "FG";

    // ---- 制造业财一体过账（plan 2026-07-10-1100-5） ----

    /** WIP 在制品科目编码配置项（默认 1411；完工入库 Cr / 领料出库 Dr 共用）。经 AppConfig.var 读取，NopSysVariable 可运行时覆盖。 */
    String CONFIG_WIP_SUBJECT_CODE = "erp-mfg.wip-subject-code";
    String DEFAULT_WIP_SUBJECT_CODE = "1411";
    /** 产成品存货科目编码（与 InvAcctDocProvider.SUBJECT_INVENTORY 一致，完工入库 Dr）。 */
    String SUBJECT_FINISHED_GOODS = "1401";
}
