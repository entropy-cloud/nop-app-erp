package app.erp.md.service;

/**
 * 主数据域状态码常量。权威值来自 {@code module-master-data/model/app-erp-master-data.orm.xml}
 * 关联字典 {@code erp-md/active-status}、{@code erp-md/supplier-approval-status}、{@code erp-md/supplier-approval-type}。
 */
public interface ErpMdConstants {

    // 主数据启用状态 erp-md/active-status
    String ACTIVE_STATUS_ACTIVE = "ACTIVE";

    // AVL 准入状态 erp-md/supplier-approval-status（APPLIED→APPROVED→PROBATION→SUSPENDED→REJECTED）
    String APPROVAL_STATUS_APPLIED = "APPLIED";
    String APPROVAL_STATUS_APPROVED = "APPROVED";
    String APPROVAL_STATUS_PROBATION = "PROBATION";
    String APPROVAL_STATUS_SUSPENDED = "SUSPENDED";
    String APPROVAL_STATUS_REJECTED = "REJECTED";

    // AVL 准入类型 erp-md/supplier-approval-type
    String APPROVAL_TYPE_NEW = "NEW";
    String APPROVAL_TYPE_RENEWAL = "RENEWAL";
}
