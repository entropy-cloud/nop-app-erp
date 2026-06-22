package app.erp.sal.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpSalDaoConstants {
    
    /**
     * 单据状态: 草稿 
     */
    int DOC_STATUS_DRAFT = 10;
                    
    /**
     * 单据状态: 已生效 
     */
    int DOC_STATUS_ACTIVE = 20;
                    
    /**
     * 单据状态: 已作废 
     */
    int DOC_STATUS_CANCELLED = 30;
                    
    /**
     * 审核状态: 未提交 
     */
    int APPROVE_STATUS_UNSUBMITTED = 10;
                    
    /**
     * 审核状态: 已提交 
     */
    int APPROVE_STATUS_SUBMITTED = 20;
                    
    /**
     * 审核状态: 已审核 
     */
    int APPROVE_STATUS_APPROVED = 30;
                    
    /**
     * 审核状态: 已驳回 
     */
    int APPROVE_STATUS_REJECTED = 40;
                    
    /**
     * 收款进度: 未收款 
     */
    int RECEIVED_STATUS_UNRECEIVED = 10;
                    
    /**
     * 收款进度: 部分收款 
     */
    int RECEIVED_STATUS_PARTIAL = 20;
                    
    /**
     * 收款进度: 已收清 
     */
    int RECEIVED_STATUS_RECEIVED = 30;
                    
    /**
     * 发货状态: 未发货 
     */
    int DELIVERY_STATUS_UNDELIVERED = 10;
                    
    /**
     * 发货状态: 部分发货 
     */
    int DELIVERY_STATUS_PARTIAL = 20;
                    
    /**
     * 发货状态: 已发清 
     */
    int DELIVERY_STATUS_DELIVERED = 30;
                    
    /**
     * 销售单据类型: 销售报价 
     */
    int BIZ_TYPE_QUOTATION = 10;
                    
    /**
     * 销售单据类型: 销售合同 
     */
    int BIZ_TYPE_CONTRACT = 20;
                    
    /**
     * 销售单据类型: 销售订单 
     */
    int BIZ_TYPE_ORDER = 30;
                    
    /**
     * 销售单据类型: 销售出库 
     */
    int BIZ_TYPE_DELIVERY = 40;
                    
    /**
     * 销售单据类型: 销售发票 
     */
    int BIZ_TYPE_INVOICE = 50;
                    
    /**
     * 销售单据类型: 收款单 
     */
    int BIZ_TYPE_RECEIPT = 60;
                    
    /**
     * 销售单据类型: 销售退货 
     */
    int BIZ_TYPE_RETURN = 70;
                    
}
