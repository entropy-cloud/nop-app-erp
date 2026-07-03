package app.erp.pur.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpPurDaoConstants {
    
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
     * 付款进度: 未付款 
     */
    int PAID_STATUS_UNPAID = 10;
                    
    /**
     * 付款进度: 部分付款 
     */
    int PAID_STATUS_PARTIAL = 20;
                    
    /**
     * 付款进度: 已付清 
     */
    int PAID_STATUS_PAID = 30;
                    
    /**
     * 收货状态: 未收货 
     */
    int RECEIVE_STATUS_UNRECEIVED = 10;
                    
    /**
     * 收货状态: 部分收货 
     */
    int RECEIVE_STATUS_PARTIAL = 20;
                    
    /**
     * 收货状态: 已收清 
     */
    int RECEIVE_STATUS_RECEIVED = 30;
                    
    /**
     * 采购单据类型: 请购单 
     */
    int BIZ_TYPE_REQUISITION = 10;
                    
    /**
     * 采购单据类型: 询价单 
     */
    int BIZ_TYPE_RFQ = 20;
                    
    /**
     * 采购单据类型: 报价单 
     */
    int BIZ_TYPE_QUOTATION = 30;
                    
    /**
     * 采购单据类型: 采购订单 
     */
    int BIZ_TYPE_ORDER = 40;
                    
    /**
     * 采购单据类型: 采购入库 
     */
    int BIZ_TYPE_RECEIVE = 50;
                    
    /**
     * 采购单据类型: 采购发票 
     */
    int BIZ_TYPE_INVOICE = 60;
                    
    /**
     * 采购单据类型: 付款单 
     */
    int BIZ_TYPE_PAYMENT = 70;
                    
    /**
     * 采购单据类型: 采购退货 
     */
    int BIZ_TYPE_RETURN = 80;
                    
    /**
     * 单据方向: 入库 
     */
    int DOC_DIRECTION_INBOUND = 10;
                    
    /**
     * 单据方向: 出库 
     */
    int DOC_DIRECTION_OUTBOUND = 20;
                    
    /**
     * 入库类型: 正常入库 
     */
    int RECEIVE_TYPE_NORMAL = 10;
                    
    /**
     * 入库类型: 退货入库 
     */
    int RECEIVE_TYPE_RETURN = 20;
                    
    /**
     * 入库类型: 赠品入库 
     */
    int RECEIVE_TYPE_GIFT = 30;
                    
    /**
     * 发票类型: 增值税专用 
     */
    int INVOICE_TYPE_VAT_SPECIAL = 10;
                    
    /**
     * 发票类型: 增值税普通 
     */
    int INVOICE_TYPE_VAT_NORMAL = 20;
                    
    /**
     * 发票类型: 收据 
     */
    int INVOICE_TYPE_RECEIPT = 30;
                    
    /**
     * 供应商评级: 优秀 
     */
    int SUPPLIER_STANDING_GREEN = 10;
                    
    /**
     * 供应商评级: 待改进 
     */
    int SUPPLIER_STANDING_YELLOW = 20;
                    
    /**
     * 供应商评级: 不合格 
     */
    int SUPPLIER_STANDING_RED = 30;
                    
    /**
     * 评分卡状态: 草稿 
     */
    int SCORECARD_STATUS_DRAFT = 10;
                    
    /**
     * 评分卡状态: 已定稿 
     */
    int SCORECARD_STATUS_FINALIZED = 20;
                    
}
