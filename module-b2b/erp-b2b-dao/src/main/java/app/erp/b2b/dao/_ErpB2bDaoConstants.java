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
                    
    /**
     * 伙伴上线状态: 已注册 
     */
    int PARTNER_STATUS_REGISTERED = 10;
                    
    /**
     * 伙伴上线状态: 测试中 
     */
    int PARTNER_STATUS_TESTING = 20;
                    
    /**
     * 伙伴上线状态: 已认证 
     */
    int PARTNER_STATUS_CERTIFIED = 30;
                    
    /**
     * 伙伴上线状态: 生产中 
     */
    int PARTNER_STATUS_PRODUCTION = 40;
                    
    /**
     * 伙伴上线状态: 已暂停 
     */
    int PARTNER_STATUS_SUSPENDED = 50;
                    
    /**
     * 伙伴上线状态: 已终止 
     */
    int PARTNER_STATUS_TERMINATED = 60;
                    
    /**
     * 传输协议: AS2 
     */
    int PROTOCOL_AS2 = 10;
                    
    /**
     * 传输协议: SFTP 
     */
    int PROTOCOL_SFTP = 20;
                    
    /**
     * 传输协议: HTTP 
     */
    int PROTOCOL_HTTP = 30;
                    
    /**
     * 传输协议: HTTPS 
     */
    int PROTOCOL_HTTPS = 40;
                    
    /**
     * 传输协议: OFTP2 
     */
    int PROTOCOL_OFTP2 = 50;
                    
    /**
     * 认证方式: HMAC 
     */
    int AUTH_METHOD_HMAC = 10;
                    
    /**
     * 认证方式: Basic Auth 
     */
    int AUTH_METHOD_BASIC_AUTH = 20;
                    
    /**
     * 认证方式: 证书 
     */
    int AUTH_METHOD_CERTIFICATE = 30;
                    
    /**
     * 认证方式: OAuth2 
     */
    int AUTH_METHOD_OAUTH2 = 40;
                    
    /**
     * 凭证类型: API Key 
     */
    int CREDENTIAL_TYPE_API_KEY = 10;
                    
    /**
     * 凭证类型: 用户名密码 
     */
    int CREDENTIAL_TYPE_USERNAME_PASSWORD = 20;
                    
    /**
     * 凭证类型: 证书 
     */
    int CREDENTIAL_TYPE_CERTIFICATE = 30;
                    
    /**
     * 凭证类型: SSH Key 
     */
    int CREDENTIAL_TYPE_SSH_KEY = 40;
                    
    /**
     * 测试方向: 出站 
     */
    int EXCHANGE_DIRECTION_OUTBOUND = 10;
                    
    /**
     * 测试方向: 入站 
     */
    int EXCHANGE_DIRECTION_INBOUND = 20;
                    
}
