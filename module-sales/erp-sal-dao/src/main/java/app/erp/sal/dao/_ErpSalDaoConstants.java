package app.erp.sal.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpSalDaoConstants {
    
    /**
     * 单据状态: 草稿 
     */
    String DOC_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 单据状态: 已生效 
     */
    String DOC_STATUS_ACTIVE = "ACTIVE";
                    
    /**
     * 单据状态: 已作废 
     */
    String DOC_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 审核状态: 未提交 
     */
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
                    
    /**
     * 审核状态: 已提交 
     */
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 审核状态: 已审核 
     */
    String APPROVE_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 审核状态: 已驳回 
     */
    String APPROVE_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 收款进度: 未收款 
     */
    String RECEIVED_STATUS_UNRECEIVED = "UNRECEIVED";
                    
    /**
     * 收款进度: 部分收款 
     */
    String RECEIVED_STATUS_PARTIAL = "PARTIAL";
                    
    /**
     * 收款进度: 已收清 
     */
    String RECEIVED_STATUS_RECEIVED = "RECEIVED";
                    
    /**
     * 发货状态: 未发货 
     */
    String DELIVERY_STATUS_UNDELIVERED = "UNDELIVERED";
                    
    /**
     * 发货状态: 部分发货 
     */
    String DELIVERY_STATUS_PARTIAL = "PARTIAL";
                    
    /**
     * 发货状态: 已发清 
     */
    String DELIVERY_STATUS_DELIVERED = "DELIVERED";
                    
    /**
     * 销售单据类型: 销售报价 
     */
    String BIZ_TYPE_QUOTATION = "QUOTATION";
                    
    /**
     * 销售单据类型: 销售合同 
     */
    String BIZ_TYPE_CONTRACT = "CONTRACT";
                    
    /**
     * 销售单据类型: 销售订单 
     */
    String BIZ_TYPE_ORDER = "ORDER";
                    
    /**
     * 销售单据类型: 销售出库 
     */
    String BIZ_TYPE_DELIVERY = "DELIVERY";
                    
    /**
     * 销售单据类型: 销售发票 
     */
    String BIZ_TYPE_INVOICE = "INVOICE";
                    
    /**
     * 销售单据类型: 收款单 
     */
    String BIZ_TYPE_RECEIPT = "RECEIPT";
                    
    /**
     * 销售单据类型: 销售退货 
     */
    String BIZ_TYPE_RETURN = "RETURN";
                    
    /**
     * 发票类型: 增值税专用 
     */
    String INVOICE_TYPE_VAT_SPECIAL = "VAT_SPECIAL";
                    
    /**
     * 发票类型: 增值税普通 
     */
    String INVOICE_TYPE_VAT_NORMAL = "VAT_NORMAL";
                    
    /**
     * 发票类型: 收据 
     */
    String INVOICE_TYPE_RECEIPT = "RECEIPT";
                    
}
