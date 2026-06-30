package app.erp.b2b.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpB2bDaoConstants {
    
    /**
     * EDI 文档状态: 待发送 
     */
    int EDI_DOC_STATE_TO_SEND = 10;
                    
    /**
     * EDI 文档状态: 已发送 
     */
    int EDI_DOC_STATE_SENT = 20;
                    
    /**
     * EDI 文档状态: 待取消 
     */
    int EDI_DOC_STATE_TO_CANCEL = 25;
                    
    /**
     * EDI 文档状态: 已取消 
     */
    int EDI_DOC_STATE_CANCELLED = 30;
                    
    /**
     * EDI 文档状态: 错误 
     */
    int EDI_DOC_STATE_ERROR = 40;
                    
    /**
     * EDI 文档状态: 已接收 
     */
    int EDI_DOC_STATE_RECEIVED = 50;
                    
    /**
     * EDI 文档状态: 已确认 
     */
    int EDI_DOC_STATE_ACKNOWLEDGED = 60;
                    
    /**
     * EDI 文档状态: 已归档 
     */
    int EDI_DOC_STATE_ARCHIVED = 70;
                    
    /**
     * ASN 状态: 已接收 
     */
    int ASN_STATUS_RECEIVED = 10;
                    
    /**
     * ASN 状态: 已匹配 
     */
    int ASN_STATUS_MATCHED = 20;
                    
    /**
     * ASN 状态: 已入库 
     */
    int ASN_STATUS_RECEIVED_TO_STOCK = 30;
                    
    /**
     * ASN 状态: 已取消 
     */
    int ASN_STATUS_CANCELLED = 40;
                    
    /**
     * EDI 方向: 出站 
     */
    int EDI_DIRECTION_OUTBOUND = 10;
                    
    /**
     * EDI 方向: 入站 
     */
    int EDI_DIRECTION_INBOUND = 20;
                    
    /**
     * EDI 方向: 双向 
     */
    int EDI_DIRECTION_BOTH = 30;
                    
    /**
     * EDI 标准: UBL 
     */
    int EDI_STANDARD_UBL = 10;
                    
    /**
     * EDI 标准: X12 
     */
    int EDI_STANDARD_X12 = 20;
                    
    /**
     * EDI 标准: EDIFACT 
     */
    int EDI_STANDARD_EDIFACT = 30;
                    
    /**
     * EDI 标准: 自定义 
     */
    int EDI_STANDARD_CUSTOM = 40;
                    
    /**
     * 阻断级别: 信息 
     */
    int BLOCKING_LEVEL_INFO = 10;
                    
    /**
     * 阻断级别: 警告 
     */
    int BLOCKING_LEVEL_WARN = 20;
                    
    /**
     * 阻断级别: 错误 
     */
    int BLOCKING_LEVEL_ERROR = 30;
                    
    /**
     * 代码映射类型: 物料 
     */
    int MAPPING_TYPE_MATERIAL = 10;
                    
    /**
     * 代码映射类型: 伙伴 
     */
    int MAPPING_TYPE_PARTNER = 20;
                    
    /**
     * 代码映射类型: 计量单位 
     */
    int MAPPING_TYPE_UOM = 30;
                    
}
