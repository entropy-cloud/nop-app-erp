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
                    
    /**
     * 价格清单状态: 启用 
     */
    String PRICE_LIST_STATUS_ACTIVE = "ACTIVE";
                    
    /**
     * 价格清单状态: 停用 
     */
    String PRICE_LIST_STATUS_INACTIVE = "INACTIVE";
                    
    /**
     * 促销规则类型: 百分比折扣 
     */
    String PRICING_RULE_TYPE_PERCENT_DISCOUNT = "PERCENT_DISCOUNT";
                    
    /**
     * 促销规则类型: 满减 
     */
    String PRICING_RULE_TYPE_AMOUNT_OFF = "AMOUNT_OFF";
                    
    /**
     * 促销规则类型: 买赠 
     */
    String PRICING_RULE_TYPE_GIFT = "GIFT";
                    
    /**
     * 促销规则类型: 价格覆盖 
     */
    String PRICING_RULE_TYPE_PRICE_OVERRIDE = "PRICE_OVERRIDE";
                    
    /**
     * 促销规则目标: 行级 
     */
    String PRICING_TARGET_LINE = "LINE";
                    
    /**
     * 促销规则目标: 单级 
     */
    String PRICING_TARGET_ORDER = "ORDER";
                    
}
