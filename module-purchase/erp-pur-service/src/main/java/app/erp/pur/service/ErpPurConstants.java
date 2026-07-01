package app.erp.pur.service;

/**
 * 采购域状态码常量。权威值来自 {@code module-purchase/model/app-erp-purchase.orm.xml}
 * 关联字典 {@code erp-pur/approve-status}、{@code erp-pur/doc-status}、{@code erp-pur/receive-status}。
 *
 * <p>三轴状态分离见 {@code docs/design/purchase/state-machine.md}。
 */
public interface ErpPurConstants {

    // 审核轴 approve-status
    int APPROVE_STATUS_UNSUBMITTED = 10;
    int APPROVE_STATUS_SUBMITTED = 20;
    int APPROVE_STATUS_APPROVED = 30;
    int APPROVE_STATUS_REJECTED = 40;

    // 单据生命周期轴 doc-status
    int DOC_STATUS_DRAFT = 10;
    int DOC_STATUS_ACTIVE = 20;
    int DOC_STATUS_CANCELLED = 30;

    // 收货进度（派生）receive-status
    int RECEIVE_STATUS_UNRECEIVED = 10;
    int RECEIVE_STATUS_PARTIAL = 20;
    int RECEIVE_STATUS_RECEIVED = 30;

    // 主数据启用状态 erp-md/active-status
    int PARTNER_STATUS_ACTIVE = 10;

    // 库存作业类型（对齐 erp-inv/operation-type，调用方侧副本避免 main 代码依赖 inventory-service）
    int MOVE_TYPE_INCOMING = 10;

    // 入库联动标识（自由字符串，inventory 侧无字典约束）
    String RELATED_BILL_TYPE_PUR_RECEIVE = "ERP_PUR_RECEIVE";
    String RELATED_BILL_TYPE_REVERSAL = "REVERSAL";
}
