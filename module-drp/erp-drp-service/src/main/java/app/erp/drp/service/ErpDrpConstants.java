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

    // DDMRP 默认缓冲天数（demandVariabilityDays + orderCycle，无 ORM 列时用配置默认）
    int DDMRP_DEFAULT_DEMAND_VARIABILITY_DAYS = 3;
    int DDMRP_DEFAULT_ORDER_CYCLE_DAYS = 2;

    // 默认订货倍数（ErpDrpParameter.orderMultiple 为空时兜底，1=不取整）
    java.math.BigDecimal DEFAULT_ORDER_MULTIPLE = java.math.BigDecimal.ONE;

    // 默认补货提前期天数（ErpDrpParameter.replenishmentLeadTime 与 SS.leadTimeDays 均空时兜底）
    int DEFAULT_REPLENISHMENT_LEAD_TIME_DAYS = 0;
}
