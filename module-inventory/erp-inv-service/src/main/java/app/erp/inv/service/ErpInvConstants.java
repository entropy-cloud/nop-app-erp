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
    int DEFAULT_COST_METHOD = 10; // COST_METHOD_MOVING_AVERAGE

    // 追溯链 linkType 标识（TraceLink）
    String TRACE_LINK_FORWARD = "FORWARD";
    String TRACE_LINK_RETURN = "RETURN";

    int DOC_STATUS_DRAFT = 10;
    int DOC_STATUS_CONFIRMED = 20;
    int DOC_STATUS_DONE = 30;
    int DOC_STATUS_CANCELLED = 40;

    int MOVE_TYPE_INCOMING = 10;
    int MOVE_TYPE_OUTGOING = 20;
    int MOVE_TYPE_INTERNAL_TRANSFER = 30;
    int MOVE_TYPE_MANUFACTURING = 40;

    // 存货计价方法码值（字典 erp-md/cost-method）。本计划覆盖 10/30；20/40/50/60/70 为后续 successor。
    int COST_METHOD_MOVING_AVERAGE = 10;
    int COST_METHOD_MONTHLY_WEIGHTED_AVERAGE = 20;
    int COST_METHOD_FIFO = 30;
    int COST_METHOD_LIFO = 40;
    int COST_METHOD_STANDARD = 50;
    int COST_METHOD_INDIVIDUAL = 60;
    int COST_METHOD_BATCH = 70;

    // 业务联动源单类型（自由字符串）。采购退货出库移动 / 销售退货入库移动的存货估值过账分别由 purchase/sales 域独占
    // （PURCHASE_RETURN/SALES_RETURN），故 inventory 域对此类联动移动跳过默认估值过账，避免与之双计存货。
    String RELATED_BILL_TYPE_PUR_RETURN = "ERP_PUR_RETURN";
    String RELATED_BILL_TYPE_SAL_RETURN = "ERP_SAL_RETURN";
}
