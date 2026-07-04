package app.erp.b2b.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpB2bDaoConstants {
    
    /**
     * EDI 文档状态: 待发送 
     */
    String EDI_DOC_STATE_TO_SEND = "TO_SEND";
                    
    /**
     * EDI 文档状态: 已发送 
     */
    String EDI_DOC_STATE_SENT = "SENT";
                    
    /**
     * EDI 文档状态: 待取消 
     */
    String EDI_DOC_STATE_TO_CANCEL = "TO_CANCEL";
                    
    /**
     * EDI 文档状态: 已取消 
     */
    String EDI_DOC_STATE_CANCELLED = "CANCELLED";
                    
    /**
     * EDI 文档状态: 错误 
     */
    String EDI_DOC_STATE_ERROR = "ERROR";
                    
    /**
     * EDI 文档状态: 已接收 
     */
    String EDI_DOC_STATE_RECEIVED = "RECEIVED";
                    
    /**
     * EDI 文档状态: 已确认 
     */
    String EDI_DOC_STATE_ACKNOWLEDGED = "ACKNOWLEDGED";
                    
    /**
     * EDI 文档状态: 已归档 
     */
    String EDI_DOC_STATE_ARCHIVED = "ARCHIVED";
                    
    /**
     * ASN 状态: 已接收 
     */
    String ASN_STATUS_RECEIVED = "RECEIVED";
                    
    /**
     * ASN 状态: 已匹配 
     */
    String ASN_STATUS_MATCHED = "MATCHED";
                    
    /**
     * ASN 状态: 已入库 
     */
    String ASN_STATUS_RECEIVED_TO_STOCK = "RECEIVED_TO_STOCK";
                    
    /**
     * ASN 状态: 已取消 
     */
    String ASN_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * EDI 方向: 出站 
     */
    String EDI_DIRECTION_OUTBOUND = "OUTBOUND";
                    
    /**
     * EDI 方向: 入站 
     */
    String EDI_DIRECTION_INBOUND = "INBOUND";
                    
    /**
     * EDI 方向: 双向 
     */
    String EDI_DIRECTION_BOTH = "BOTH";
                    
    /**
     * EDI 标准: UBL 
     */
    String EDI_STANDARD_UBL = "UBL";
                    
    /**
     * EDI 标准: X12 
     */
    String EDI_STANDARD_X12 = "X12";
                    
    /**
     * EDI 标准: EDIFACT 
     */
    String EDI_STANDARD_EDIFACT = "EDIFACT";
                    
    /**
     * EDI 标准: 自定义 
     */
    String EDI_STANDARD_CUSTOM = "CUSTOM";
                    
    /**
     * 阻断级别: 信息 
     */
    String BLOCKING_LEVEL_INFO = "INFO";
                    
    /**
     * 阻断级别: 警告 
     */
    String BLOCKING_LEVEL_WARN = "WARN";
                    
    /**
     * 阻断级别: 错误 
     */
    String BLOCKING_LEVEL_ERROR = "ERROR";
                    
    /**
     * 代码映射类型: 物料 
     */
    String MAPPING_TYPE_MATERIAL = "MATERIAL";
                    
    /**
     * 代码映射类型: 伙伴 
     */
    String MAPPING_TYPE_PARTNER = "PARTNER";
                    
    /**
     * 代码映射类型: 计量单位 
     */
    String MAPPING_TYPE_UOM = "UOM";
                    
    /**
     * 伙伴上线状态: 已注册 
     */
    String PARTNER_STATUS_REGISTERED = "REGISTERED";
                    
    /**
     * 伙伴上线状态: 测试中 
     */
    String PARTNER_STATUS_TESTING = "TESTING";
                    
    /**
     * 伙伴上线状态: 已认证 
     */
    String PARTNER_STATUS_CERTIFIED = "CERTIFIED";
                    
    /**
     * 伙伴上线状态: 生产中 
     */
    String PARTNER_STATUS_PRODUCTION = "PRODUCTION";
                    
    /**
     * 伙伴上线状态: 已暂停 
     */
    String PARTNER_STATUS_SUSPENDED = "SUSPENDED";
                    
    /**
     * 伙伴上线状态: 已终止 
     */
    String PARTNER_STATUS_TERMINATED = "TERMINATED";
                    
    /**
     * 传输协议: AS2 
     */
    String PROTOCOL_AS2 = "AS2";
                    
    /**
     * 传输协议: SFTP 
     */
    String PROTOCOL_SFTP = "SFTP";
                    
    /**
     * 传输协议: HTTP 
     */
    String PROTOCOL_HTTP = "HTTP";
                    
    /**
     * 传输协议: HTTPS 
     */
    String PROTOCOL_HTTPS = "HTTPS";
                    
    /**
     * 传输协议: OFTP2 
     */
    String PROTOCOL_OFTP2 = "OFTP2";
                    
    /**
     * 认证方式: HMAC 
     */
    String AUTH_METHOD_HMAC = "HMAC";
                    
    /**
     * 认证方式: Basic Auth 
     */
    String AUTH_METHOD_BASIC_AUTH = "BASIC_AUTH";
                    
    /**
     * 认证方式: 证书 
     */
    String AUTH_METHOD_CERTIFICATE = "CERTIFICATE";
                    
    /**
     * 认证方式: OAuth2 
     */
    String AUTH_METHOD_OAUTH2 = "OAUTH2";
                    
    /**
     * 凭证类型: API Key 
     */
    String CREDENTIAL_TYPE_API_KEY = "API_KEY";
                    
    /**
     * 凭证类型: 用户名密码 
     */
    String CREDENTIAL_TYPE_USERNAME_PASSWORD = "USERNAME_PASSWORD";
                    
    /**
     * 凭证类型: 证书 
     */
    String CREDENTIAL_TYPE_CERTIFICATE = "CERTIFICATE";
                    
    /**
     * 凭证类型: SSH Key 
     */
    String CREDENTIAL_TYPE_SSH_KEY = "SSH_KEY";
                    
    /**
     * 测试方向: 出站 
     */
    String EXCHANGE_DIRECTION_OUTBOUND = "OUTBOUND";
                    
    /**
     * 测试方向: 入站 
     */
    String EXCHANGE_DIRECTION_INBOUND = "INBOUND";
                    
    /**
     * MFT 协议: AS2 
     */
    String MFT_PROTOCOL_AS2 = "AS2";
                    
    /**
     * MFT 协议: SFTP 
     */
    String MFT_PROTOCOL_SFTP = "SFTP";
                    
    /**
     * MFT 协议: FTPS 
     */
    String MFT_PROTOCOL_FTPS = "FTPS";
                    
    /**
     * MFT 协议: HTTP 
     */
    String MFT_PROTOCOL_HTTP = "HTTP";
                    
    /**
     * MFT 协议: HTTPS 
     */
    String MFT_PROTOCOL_HTTPS = "HTTPS";
                    
    /**
     * MFT 状态: 待传输 
     */
    String MFT_STATUS_PENDING = "PENDING";
                    
    /**
     * MFT 状态: 已发送 
     */
    String MFT_STATUS_SENT = "SENT";
                    
    /**
     * MFT 状态: 已接收 
     */
    String MFT_STATUS_RECEIVED = "RECEIVED";
                    
    /**
     * MFT 状态: 失败 
     */
    String MFT_STATUS_FAILED = "FAILED";
                    
    /**
     * MFT 状态: 重试中 
     */
    String MFT_STATUS_RETRYING = "RETRYING";
                    
    /**
     * MFT 状态: 死信 
     */
    String MFT_STATUS_DEAD_LETTER = "DEAD_LETTER";
                    
}
