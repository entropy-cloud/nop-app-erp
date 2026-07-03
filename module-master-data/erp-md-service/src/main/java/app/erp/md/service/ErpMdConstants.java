package app.erp.md.service;

/**
 * 主数据域状态码常量。权威值来自 {@code module-master-data/model/app-erp-master-data.orm.xml}
 * 关联字典 {@code erp-md/active-status}、{@code erp-md/supplier-approval-status}、{@code erp-md/supplier-approval-type}。
 */
public interface ErpMdConstants {

    // 主数据启用状态 erp-md/active-status
    int ACTIVE_STATUS_ACTIVE = 10;

    // AVL 准入状态 erp-md/supplier-approval-status（APPLIED→APPROVED→PROBATION→SUSPENDED→REJECTED）
    int APPROVAL_STATUS_APPLIED = 10;
    int APPROVAL_STATUS_APPROVED = 20;
    int APPROVAL_STATUS_PROBATION = 30;
    int APPROVAL_STATUS_SUSPENDED = 40;
    int APPROVAL_STATUS_REJECTED = 50;

    // AVL 准入类型 erp-md/supplier-approval-type
    int APPROVAL_TYPE_NEW = 10;
    int APPROVAL_TYPE_RENEWAL = 20;
}
