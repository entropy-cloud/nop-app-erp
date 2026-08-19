package app.erp.drp.service;

/**
 * DRP 域常量。字典码值权威：`erp-drp-meta/.../dict/*.dict.yaml` + `module-drp/model/app-erp-drp.orm.xml`。
 *
 * <p>权威：`docs/design/drp/README.md`、`docs/design/drp/state-machine.md`、`docs/design/drp/safety-stock-optimization.md`、
 * `docs/plans/2026-07-04-1115-2-drp-net-requirement-safety-stock.md`。
 */
public interface ErpDrpConstants {

    // DRP 计划状态（erp-drp/drp-plan-status）
    String DRP_PLAN_STATUS_DRAFT = "DRAFT";
    String DRP_PLAN_STATUS_COMPUTED = "COMPUTED";
    String DRP_PLAN_STATUS_APPROVED = "APPROVED";
    String DRP_PLAN_STATUS_EXECUTED = "EXECUTED";

    // DRP 明细行状态（erp-drp/drp-line-status）
    String DRP_LINE_STATUS_SUGGESTED = "SUGGESTED";
    String DRP_LINE_STATUS_APPROVED = "APPROVED";
    String DRP_LINE_STATUS_ORDERED = "ORDERED";
    String DRP_LINE_STATUS_CANCELLED = "CANCELLED";

    // 补货类型（erp-drp/drp-replenishment-type）
    String REPLENISHMENT_TYPE_TRANSFER = "TRANSFER";
    String REPLENISHMENT_TYPE_PURCHASE = "PURCHASE";

    // 补货方法（erp-drp/drp-replenishment-method）
    String REPLENISHMENT_METHOD_MIN_MAX = "MIN_MAX";
    String REPLENISHMENT_METHOD_PERIODIC = "PERIODIC";
    String REPLENISHMENT_METHOD_LOT_FOR_LOT = "LOT_FOR_LOT";

    // 安全库存计算方法（erp-inv/drp-ss-method）
    String SS_METHOD_STATISTICAL = "STATISTICAL";
    String SS_METHOD_SIMPLE = "SIMPLE";
    String SS_METHOD_DDMRP = "DDMRP";

    // 服务水平（erp-inv/drp-service-level）→ Z 值映射（标准正态分布）
    String SERVICE_LEVEL_PCT95 = "PCT95";
    String SERVICE_LEVEL_PCT97_5 = "PCT97_5";
    String SERVICE_LEVEL_PCT99 = "PCT99";
    String SERVICE_LEVEL_PCT99_5 = "PCT99_5";

    // 零需求月处理策略（配置 erp-inv.drp-ss-zero-demand-policy）
    String ZERO_DEMAND_POLICY_EXCLUDE = "EXCLUDE";
    String ZERO_DEMAND_POLICY_KEEP = "KEEP";

    // 释放生成的目标单据类型（自由字符串，写入 DrpLine.orderBillType 用于追溯）
    String ORDER_BILL_TYPE_TRANSFER_ORDER = "ERP_INV_TRANSFER_ORDER";
    String ORDER_BILL_TYPE_PURCHASE_ORDER = "ERP_PUR_ORDER";

    // 释放生成的目标单据 code 前缀
    String RELEASE_TO_CODE_PREFIX = "DRP-";

    // 下游单据初始状态（对齐 inventory/purchase 域码值，避免跨 service 模块依赖）
    String DOWNSTREAM_DOC_STATUS_DRAFT = "DRAFT";
    String DOWNSTREAM_APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";

    // 销售订单作废状态（erp-sal/doc-status CANCELLED），历史聚合排除作废单
    String SAL_DOC_STATUS_CANCELLED = "CANCELLED";

    // 库存移动单作业类型（对齐 ErpInvConstants 码值，出库=需求历史来源）
    String MOVE_TYPE_OUTGOING = "OUTGOING";

    // 库存移动单作业类型：内部转移（越库超时回退 staging→正常存储位；对齐 ErpInvConstants 码值避免跨 service 依赖）
    String MOVE_TYPE_INTERNAL_TRANSFER = "INTERNAL";

    // DDMRP 默认缓冲天数（demandVariabilityDays + orderCycle，无 ORM 列时用配置默认）
    int DDMRP_DEFAULT_DEMAND_VARIABILITY_DAYS = 3;
    int DDMRP_DEFAULT_ORDER_CYCLE_DAYS = 2;

    // 默认订货倍数（ErpDrpParameter.orderMultiple 为空时兜底，1=不取整）
    java.math.BigDecimal DEFAULT_ORDER_MULTIPLE = java.math.BigDecimal.ONE;

    // 默认补货提前期天数（ErpDrpParameter.replenishmentLeadTime 与 SS.leadTimeDays 均空时兜底）
    int DEFAULT_REPLENISHMENT_LEAD_TIME_DAYS = 0;

    // ---- DRP 仿真引擎（plan 2026-07-22-1000-2；权威：docs/design/manufacturing/simulation-engine.md §DRP 对应物） ----

    /** DRP 仿真入口总开关（默认 false=不启用 runSimulation / promoteToFormalPlan）。 */
    String CONFIG_DRP_SIMULATION_ENABLED = "erp-drp.simulation-enabled";
    boolean DEFAULT_DRP_SIMULATION_ENABLED = false;

    // DRP 仿真场景/版本状态（erp-drp/simulation-status，4 态；同 MRP）
    String SIMULATION_STATUS_DRAFT = "DRAFT";
    String SIMULATION_STATUS_RUNNING = "RUNNING";
    String SIMULATION_STATUS_COMPLETED = "COMPLETED";
    String SIMULATION_STATUS_ARCHIVED = "ARCHIVED";

    // DRP 仿真参数类型（erp-drp/simulation-param-type，3 键）
    String SIMULATION_PARAM_TYPE_SAFETY_STOCK = "SAFETY_STOCK";
    String SIMULATION_PARAM_TYPE_LEAD_TIME = "LEAD_TIME";
    String SIMULATION_PARAM_TYPE_REPLENISHMENT_QTY = "REPLENISHMENT_QTY";

    /** promoteToFormalPlan 生成的正式计划 code 后缀模板（{0}=versionNo）。 */
    String SIMULATION_PROMOTED_PLAN_CODE_SUFFIX = "-PROMOTED-{0}";

    // ---- 越库执行（RC-R1.81 / P1-RC-081，UC-DRP-07；权威：docs/design/drp/cross-dock.md） ----

    // 越库状态（erp-inv/drp-xdock-status）
    String XDOCK_STATUS_PENDING = "PENDING";
    String XDOCK_STATUS_MATCHED = "MATCHED";
    String XDOCK_STATUS_STAGING = "STAGING";
    String XDOCK_STATUS_LOADED = "LOADED";
    String XDOCK_STATUS_COMPLETED = "COMPLETED";
    String XDOCK_STATUS_CANCELLED = "CANCELLED";

    // 越库匹配策略（erp-inv/drp-xdock-strategy）
    String XDOCK_STRATEGY_PRE_ALLOCATED = "PRE_ALLOCATED";
    String XDOCK_STRATEGY_ON_RECEIPT = "ON_RECEIPT";
    String XDOCK_STRATEGY_MANUAL = "MANUAL";

    // 越库弱指针 billType（data-dependency-matrix §5.2 登记）：出站移动/暂存区快检反查越库记录
    String RELATED_BILL_TYPE_DRP_XDOCK = "DRP_XDOCK";
    // 越库超时回退移动（staging→正常存储位 putaway）billType，独立于 DRP_XDOCK 保证幂等键不冲突
    String RELATED_BILL_TYPE_DRP_XDOCK_PUTAWAY = "DRP_XDOCK_PUTAWAY";

    // 采购单来源单据类型（CrossDock.sourceBillType 取值，对齐 data-dependency-matrix §5.2 PUR_ORDER）
    String XDOCK_SOURCE_BILL_TYPE_PUR_ORDER = "PUR_ORDER";

    // 销售订单审核状态（wf/approve-status，ON_RECEIPT 策略只扫 APPROVED 单；字符串对齐避免跨 service 依赖）
    String SAL_ORDER_APPROVE_STATUS_APPROVED = "APPROVED";

    // ---- 提前期跟踪（RC-R1.82 / P1-RC-082，UC-DRP-08；权威：docs/design/drp/lead-time-tracking.md） ----

    // 提前期偏差标记（erp-inv/drp-lt-flag）
    String LT_FLAG_ON_TIME = "ON_TIME";
    String LT_FLAG_EARLY = "EARLY";
    String LT_FLAG_LATE = "LATE";

    // 供应商评分等级（erp-inv/drp-supplier-grade）阈值（lead-time-tracking.md §评分计算）
    java.math.BigDecimal GRADE_THRESHOLD_A = new java.math.BigDecimal("90");
    java.math.BigDecimal GRADE_THRESHOLD_B = new java.math.BigDecimal("75");
    java.math.BigDecimal GRADE_THRESHOLD_C = new java.math.BigDecimal("60");
    String SUPPLIER_GRADE_A = "A";
    String SUPPLIER_GRADE_B = "B";
    String SUPPLIER_GRADE_C = "C";
    String SUPPLIER_GRADE_D = "D";

    // 质检结果（对齐 erp-qa/inspection-result 码值；快检通过 = ACCEPTED 或 CONDITIONAL 让步接收）
    String QA_INSPECTION_RESULT_ACCEPTED = "ACCEPTED";
    String QA_INSPECTION_RESULT_CONDITIONAL = "CONDITIONAL";
    String QA_INSPECTION_TYPE_INCOMING = "INCOMING";

    // 采购订单审核状态（wf/approve-status，评分数量准确率维度只统计 APPROVED 单；字符串对齐避免跨 service 依赖）
    String PUR_ORDER_APPROVE_STATUS_APPROVED = "APPROVED";
}
