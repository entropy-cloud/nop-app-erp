package app.erp.inv.service;

/**
 * 库存域状态码与作业类型常量。权威值来自 {@code module-inventory/model/app-erp-inventory.orm.xml}
 * 关联字典 {@code erp-inv/move-status} 与 {@code erp-inv/operation-type}。
 */
public interface ErpInvConstants {

    String CONFIG_ALLOW_NEGATIVE_STOCK = "erp-inv.allow-negative-stock";

    int DOC_STATUS_DRAFT = 10;
    int DOC_STATUS_CONFIRMED = 20;
    int DOC_STATUS_DONE = 30;
    int DOC_STATUS_CANCELLED = 40;

    int MOVE_TYPE_INCOMING = 10;
    int MOVE_TYPE_OUTGOING = 20;
    int MOVE_TYPE_INTERNAL_TRANSFER = 30;
    int MOVE_TYPE_MANUFACTURING = 40;

    int COST_METHOD_MOVING_AVERAGE = 10;
}
