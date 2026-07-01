package app.erp.sal.service;

/**
 * 销售域状态码常量。权威值来自 {@code module-sales/model/app-erp-sales.orm.xml}
 * 关联字典 {@code erp-sal/approve-status}、{@code erp-sal/doc-status}、{@code erp-sal/delivery-status}。
 *
 * <p>三轴状态分离见 {@code docs/design/sales/state-machine.md}（与采购域镜像对称）。
 */
public interface ErpSalConstants {

    // 审核轴 approve-status
    int APPROVE_STATUS_UNSUBMITTED = 10;
    int APPROVE_STATUS_SUBMITTED = 20;
    int APPROVE_STATUS_APPROVED = 30;
    int APPROVE_STATUS_REJECTED = 40;

    // 单据生命周期轴 doc-status
    int DOC_STATUS_DRAFT = 10;
    int DOC_STATUS_ACTIVE = 20;
    int DOC_STATUS_CANCELLED = 30;

    // 发货进度（派生）delivery-status
    int DELIVERY_STATUS_UNDELIVERED = 10;
    int DELIVERY_STATUS_PARTIAL = 20;
    int DELIVERY_STATUS_DELIVERED = 30;

    // 主数据启用状态 erp-md/active-status
    int PARTNER_STATUS_ACTIVE = 10;

    // 库存作业类型（对齐 erp-inv/operation-type，调用方侧副本避免 main 代码依赖 inventory-service）
    int MOVE_TYPE_OUTGOING = 20;

    // 出库联动标识（自由字符串，inventory 侧无字典约束）
    String RELATED_BILL_TYPE_SAL_DELIVERY = "ERP_SAL_DELIVERY";
    String RELATED_BILL_TYPE_REVERSAL = "REVERSAL";
}
