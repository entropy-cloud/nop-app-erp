package app.erp.crm.service;

/**
 * CRM 域状态码常量。权威值来自 {@code module-crm/model/app-erp-crm.orm.xml}
 * 关联字典 {@code erp-crm/lead-doc-status}、{@code erp-crm/lead-type}。
 */
public interface ErpCrmConstants {

    // 线索/商机类型 erp-crm/lead-type
    String LEAD_TYPE_LEAD = "LEAD";
    String LEAD_TYPE_OPPORTUNITY = "OPPORTUNITY";

    // 单据生命周期 doc-status erp-crm/lead-doc-status
    String DOC_STATUS_NEW = "NEW";
    String DOC_STATUS_QUALIFIED = "QUALIFIED";
    String DOC_STATUS_CONVERTED = "CONVERTED";
    String DOC_STATUS_LOST = "LOST";
    String DOC_STATUS_CANCELLED = "CANCELLED";

    // 主数据启用状态 erp-md/active-status（与 sales/master-data 一致）
    String PARTNER_STATUS_ACTIVE = "ACTIVE";
    String PARTNER_TYPE_CUSTOMER = "CUSTOMER";

    // 转化结果弱指针单据类型（自由字符串，sales/master-data 侧无字典约束）
    String RELATED_BILL_TYPE_SALES_QUOTATION = "SALES_QUOTATION";
    String RELATED_BILL_TYPE_CRM_LEAD = "CRM_LEAD";

    // 配置项（默认 false，发现重复线索仅提示不自动合并）
    String CONFIG_AUTO_CONVERT_DUPLICATE_LEAD = "erp-crm.auto-convert-duplicate-lead";
    String CONFIG_DEFAULT_TEAM_ID = "erp-crm.default-team-id";
}
