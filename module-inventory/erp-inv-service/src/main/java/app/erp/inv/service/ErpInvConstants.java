package app.erp.inv.service;

/**
 * 库存域状态码与作业类型常量。权威值来自 {@code module-inventory/model/app-erp-inventory.orm.xml}
 * 关联字典 {@code erp-inv/move-status} 与 {@code erp-inv/operation-type}。
 */
public interface ErpInvConstants {

    String CONFIG_ALLOW_NEGATIVE_STOCK = "erp-inv.allow-negative-stock";

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
}
