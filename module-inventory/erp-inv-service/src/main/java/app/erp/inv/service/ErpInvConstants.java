package app.erp.inv.service;

/**
 * 库存域状态码与作业类型常量。权威值来自 {@code module-inventory/model/app-erp-inventory.orm.xml}
 * 关联字典 {@code erp-inv/move-status} 与 {@code erp-inv/operation-type}。
 */
public interface ErpInvConstants {

    String CONFIG_ALLOW_NEGATIVE_STOCK = "erp-inv.allow-negative-stock";

    // 并发扣减乐观锁重试配置（plan 2026-07-07-0024-2；concurrency-and-transactions.md §模式四 tryLock+orm_unload 重试）
    // 默认 5 次：覆盖典型并发冲突场景；高冲突场景可经 NopSysVariable 运行时上调
    String CONFIG_CONCURRENT_DEDUCT_MAX_RETRY = "erp-inv.concurrent-deduct-max-retry";
    int CONCURRENT_DEDUCT_MAX_RETRY_DEFAULT = 5;
    // 重试退避（毫秒），默认 0=同步重试（H2 单库场景足够；生产高竞争可配 > 0 减少热点行 CPU 占用）
    String CONFIG_CONCURRENT_DEDUCT_RETRY_BACKOFF_MS = "erp-inv.concurrent-deduct-retry-backoff-ms";
    int CONCURRENT_DEDUCT_RETRY_BACKOFF_MS_DEFAULT = 0;

    // 追溯链配置项（trace-chain.md §配置项，缺失走默认，无 .env/外部服务）
    String CONFIG_TRACE_CHAIN_ENABLED = "erp-inv.trace-chain-enabled";
    String CONFIG_TRACE_CHAIN_MAX_DEPTH = "erp-inv.trace-chain-max-depth";
    int TRACE_CHAIN_MAX_DEPTH_DEFAULT = 10;

    // 成本核算配置项（costing-methods.md §配置项 / plan 2026-07-02-1538-1）
    // costing-enabled=false 时记账器退化为既有硬编码移动加权平均行为（兜底开关）
    String CONFIG_COSTING_ENABLED = "erp-inv.costing-enabled";
    // 物料/账套均未配 costMethod 时的回退默认值（dict erp-md/cost-method 的 int 码）
    String CONFIG_DEFAULT_COST_METHOD = "erp-inv.default-cost-method";
    String DEFAULT_COST_METHOD = "MOVING_AVERAGE"; // COST_METHOD_MOVING_AVERAGE
    // STANDARD 计价：无已 FIRMED 卷算行时是否回退物料主数据标准成本列（plan 2026-07-05-0427-2）
    String CONFIG_STANDARD_COST_FALLBACK_TO_MATERIAL_MASTER = "erp-inv.standard-cost-fallback-to-material-master";
    // STANDARD 计价：采购价差（PPV）捕获总开关（plan 2026-07-05-0427-2）
    String CONFIG_STANDARD_COST_PPV_ENABLED = "erp-inv.standard-cost-ppv-enabled";
    // 成本调整审批门控（plan 2026-07-05-2352-3；costing-methods.md §配置项 erp-fin.cost-adjust-approval，默认 true）
    String CONFIG_COST_ADJUST_APPROVAL = "erp-fin.cost-adjust-approval";

    // 所有权维度配置项（consignment.md §配置点）
    // ownership-tracking-enabled=false（默认关，对齐 Odoo feature group）时 ownerId 一律 null、ownershipType=OWNED，
    // 既有余额键行为逐字节不变（非 VMI 用户无感知）。启用后方入余额键。
    String CONFIG_OWNERSHIP_TRACKING_ENABLED = "erp-inv.ownership-tracking-enabled";
    // VMI 消耗转移 DONE 时是否自动生成应付（默认 true）
    String CONFIG_VMI_AUTO_GENERATE_AP = "erp-inv.vmi-auto-generate-ap";

    // 追溯链 linkType 标识（TraceLink）
    String TRACE_LINK_FORWARD = "FORWARD";
    String TRACE_LINK_RETURN = "RETURN";

    String DOC_STATUS_DRAFT = "DRAFT";
    String DOC_STATUS_CONFIRMED = "CONFIRMED";
    String DOC_STATUS_DONE = "DONE";
    String DOC_STATUS_CANCELLED = "CANCELLED";

    String MOVE_TYPE_INCOMING = "INCOMING";
    String MOVE_TYPE_OUTGOING = "OUTGOING";
    String MOVE_TYPE_INTERNAL_TRANSFER = "INTERNAL";
    String MOVE_TYPE_MANUFACTURING = "MANUFACTURE";

    // 存货计价方法码值（字典 erp-md/cost-method）。本计划覆盖 10/30；20/40/50/60/70 为后续 successor。
    String COST_METHOD_MOVING_AVERAGE = "MOVING_AVERAGE";
    String COST_METHOD_MONTHLY_WEIGHTED_AVERAGE = "WEIGHTED_AVERAGE";
    String COST_METHOD_FIFO = "FIFO";
    String COST_METHOD_LIFO = "LIFO";
    String COST_METHOD_STANDARD = "STANDARD";
    String COST_METHOD_INDIVIDUAL = "SPECIFIC";
    String COST_METHOD_BATCH = "BATCH";

    // 业务联动源单类型（自由字符串）。采购退货出库移动 / 销售退货入库移动的存货估值过账分别由 purchase/sales 域独占
    // （PURCHASE_RETURN/SALES_RETURN），故 inventory 域对此类联动移动跳过默认估值过账，避免与之双计存货。
    // 维护领料出库移动（MNT_SPARE_PART）非销售出库，其维修费用过账（MAINTENANCE_ISSUE）由 maintenance 域独占
    // （当前为 Non-Goal，延后），故 inventory 域同样跳过默认估值过账，避免误派 SALES_OUTPUT 凭证。
    String RELATED_BILL_TYPE_PUR_RETURN = "ERP_PUR_RETURN";
    String RELATED_BILL_TYPE_SAL_RETURN = "ERP_SAL_RETURN";
    String RELATED_BILL_TYPE_MNT_SPARE_PART = "ERP_MNT_SPARE_PART";

    // 所有权类型（dict erp-inv/ownership-type，consignment.md）
    String OWNERSHIP_TYPE_OWNED = "OWNED";
    String OWNERSHIP_TYPE_VMI_SUPPLIER = "VMI_SUPPLIER";
    String OWNERSHIP_TYPE_CONSIGNMENT_OUT = "CONSIGNMENT_OUT";
    String OWNERSHIP_TYPE_CUSTOMER_PROVIDED = "CUSTOMER_PROVIDED";

    // 所有权转移类型（dict erp-inv/ownership-transfer-type）
    String TRANSFER_TYPE_VMI_CONSUME = "VMI_CONSUME";
    String TRANSFER_TYPE_CONSIGNMENT_RETURN = "CONSIGNMENT_RETURN";
    String TRANSFER_TYPE_OWNERSHIP_TO_CUSTOMER = "OWNERSHIP_TO_CUSTOMER";

    // 所有权转移单据状态（dict erp-inv/ownership-transfer-status；独立于移动单 move-status，不复用）
    String OWNERSHIP_TRANSFER_STATUS_DRAFT = "DRAFT";
    String OWNERSHIP_TRANSFER_STATUS_CONFIRMED = "CONFIRMED";
    String OWNERSHIP_TRANSFER_STATUS_DONE = "DONE";
    String OWNERSHIP_TRANSFER_STATUS_CANCELLED = "CANCELLED";

    // 业财回链/PostingEvent billType 标识（所有权转移单源单识别）
    String BILL_TYPE_OWNERSHIP_TRANSFER = "OWNERSHIP_TRANSFER";

    // 成本调整类型（dict erp-inv/adjust-type，plan 2026-07-05-2352-3）
    String ADJUST_TYPE_PURCHASE_PRICE_ADJUST = "PURCHASE_PRICE_ADJUST";
    String ADJUST_TYPE_COST_DIFFERENCE = "COST_DIFFERENCE";
    String ADJUST_TYPE_STANDARD_REVALUATION = "STANDARD_REVALUATION";
    String ADJUST_TYPE_LANDED_COST_SUPPLEMENT = "LANDED_COST_SUPPLEMENT";

    // 成本调整过账 PostingEvent.billData 键（派发器填入，Provider 读取）
    String BILL_DATA_ADJUST_AMOUNT = "ADJUST_AMOUNT";        // Σ 行调整金额（带符号）
    String BILL_DATA_ADJUST_DIRECTION = "ADJUST_DIRECTION";  // INCREASE / DECREASE
    String BILL_DATA_ADJUST_TYPE = "ADJUST_TYPE";            // 头调整类型
    String DIRECTION_INCREASE = "INCREASE";
    String DIRECTION_DECREASE = "DECREASE";

    // 成本调整科目编码（costing-methods.md §成本调整凭证；借/贷 1401 存货 / 6603 成本差异）
    String SUBJECT_INVENTORY = "1401";
    String SUBJECT_COST_VARIANCE = "6603";

    // 成本调整流水 moveId 哨兵：成本调整为纯成本变更（无 StockMove），流水 moveId/moveLineId 置 0 标识非移动单来源
    long LEDGER_MOVE_ID_COST_ADJUST = 0L;

    // ---- 看板预警阈值配置项（dashboards.md §实现约定 §5，经 AppConfig.var 读取，NopSysVariable 可运行时覆盖）----
    /** 滞销库存阈值天数（最后出库日期 > 此值 且 qty > 0）；默认 0=关闭预警。 */
    String CONFIG_DASH_INV_SLOW_MOVING_DAYS = "erp-dash.inv-slow-moving-days";
    int DEFAULT_DASH_INV_SLOW_MOVING_DAYS = 0;
    /** 批次效期预警阈值天数（expiryDate - today < 此值）；默认 0=关闭预警。 */
    String CONFIG_DASH_INV_BATCH_EXPIRY_DAYS = "erp-dash.inv-batch-expiry-days";
    int DEFAULT_DASH_INV_BATCH_EXPIRY_DAYS = 0;
}
