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

    int COST_METHOD_MOVING_AVERAGE = 10;

    // 业务联动源单类型（自由字符串）。采购退货出库移动 / 销售退货入库移动的存货估值过账分别由 purchase/sales 域独占
    // （PURCHASE_RETURN/SALES_RETURN），故 inventory 域对此类联动移动跳过默认估值过账，避免与之双计存货。
    String RELATED_BILL_TYPE_PUR_RETURN = "ERP_PUR_RETURN";
    String RELATED_BILL_TYPE_SAL_RETURN = "ERP_SAL_RETURN";
}
