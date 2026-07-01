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

    // 收款进度（派生）received-status：销售发票的收款进度，收款单的核销状态复用本字典（对齐采购域 paid-status）
    int RECEIVED_STATUS_UNRECEIVED = 10;
    int RECEIVED_STATUS_PARTIAL = 20;
    int RECEIVED_STATUS_RECEIVED = 30;

    // 主数据启用状态 erp-md/active-status
    int PARTNER_STATUS_ACTIVE = 10;

    // 库存作业类型（对齐 erp-inv/operation-type，调用方侧副本避免 main 代码依赖 inventory-service）
    int MOVE_TYPE_OUTGOING = 20;
    int MOVE_TYPE_INCOMING = 10;

    // 出库联动标识（自由字符串，inventory 侧无字典约束）
    String RELATED_BILL_TYPE_SAL_DELIVERY = "ERP_SAL_DELIVERY";
    String RELATED_BILL_TYPE_SAL_RETURN = "ERP_SAL_RETURN";
    String RELATED_BILL_TYPE_REVERSAL = "REVERSAL";

    // 信用额度控制配置项 erp-sal.credit-check-level（默认 SOFT_WARNING）
    String CONFIG_CREDIT_CHECK_LEVEL = "erp-sal.credit-check-level";
    String CREDIT_CHECK_LEVEL_SOFT_WARNING = "SOFT_WARNING";
    String CREDIT_CHECK_LEVEL_SPECIAL_APPROVAL = "SPECIAL_APPROVAL";
    String CREDIT_CHECK_LEVEL_HARD_BLOCK = "HARD_BLOCK";

    // 退货配置项（returns.md §配置项，缺失走默认，无需 .env）
    String CONFIG_RETURN_REASON_REQUIRED = "erp-sal.return-reason-required";
    String CONFIG_RETURN_APPROVAL_REQUIRED = "erp-sal.return-approval-required";
}
