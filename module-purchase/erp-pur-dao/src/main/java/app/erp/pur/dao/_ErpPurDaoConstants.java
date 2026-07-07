package app.erp.pur.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpPurDaoConstants {
    
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
     * 付款进度: 未付款 
     */
    String PAID_STATUS_UNPAID = "UNPAID";
                    
    /**
     * 付款进度: 部分付款 
     */
    String PAID_STATUS_PARTIAL = "PARTIAL";
                    
    /**
     * 付款进度: 已付清 
     */
    String PAID_STATUS_PAID = "PAID";
                    
    /**
     * 收货状态: 未收货 
     */
    String RECEIVE_STATUS_UNRECEIVED = "UNRECEIVED";
                    
    /**
     * 收货状态: 部分收货 
     */
    String RECEIVE_STATUS_PARTIAL = "PARTIAL";
                    
    /**
     * 收货状态: 已收清 
     */
    String RECEIVE_STATUS_RECEIVED = "RECEIVED";
                    
    /**
     * 采购单据类型: 请购单 
     */
    String BIZ_TYPE_REQUISITION = "REQUISITION";
                    
    /**
     * 采购单据类型: 询价单 
     */
    String BIZ_TYPE_RFQ = "RFQ";
                    
    /**
     * 采购单据类型: 报价单 
     */
    String BIZ_TYPE_QUOTATION = "QUOTATION";
                    
    /**
     * 采购单据类型: 采购订单 
     */
    String BIZ_TYPE_ORDER = "ORDER";
                    
    /**
     * 采购单据类型: 采购入库 
     */
    String BIZ_TYPE_RECEIVE = "RECEIVE";
                    
    /**
     * 采购单据类型: 采购发票 
     */
    String BIZ_TYPE_INVOICE = "INVOICE";
                    
    /**
     * 采购单据类型: 付款单 
     */
    String BIZ_TYPE_PAYMENT = "PAYMENT";
                    
    /**
     * 采购单据类型: 采购退货 
     */
    String BIZ_TYPE_RETURN = "RETURN";
                    
    /**
     * 单据方向: 入库 
     */
    String DOC_DIRECTION_INBOUND = "INBOUND";
                    
    /**
     * 单据方向: 出库 
     */
    String DOC_DIRECTION_OUTBOUND = "OUTBOUND";
                    
    /**
     * 入库类型: 正常入库 
     */
    String RECEIVE_TYPE_NORMAL = "NORMAL";
                    
    /**
     * 入库类型: 退货入库 
     */
    String RECEIVE_TYPE_RETURN = "RETURN";
                    
    /**
     * 入库类型: 赠品入库 
     */
    String RECEIVE_TYPE_GIFT = "GIFT";
                    
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
                    
    /**
     * 供应商评级: 优秀 
     */
    String SUPPLIER_STANDING_GREEN = "GREEN";
                    
    /**
     * 供应商评级: 待改进 
     */
    String SUPPLIER_STANDING_YELLOW = "YELLOW";
                    
    /**
     * 供应商评级: 不合格 
     */
    String SUPPLIER_STANDING_RED = "RED";
                    
    /**
     * 评分卡状态: 草稿 
     */
    String SCORECARD_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 评分卡状态: 已定稿 
     */
    String SCORECARD_STATUS_FINALIZED = "FINALIZED";
                    
}
