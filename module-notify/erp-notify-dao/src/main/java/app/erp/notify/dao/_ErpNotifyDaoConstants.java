package app.erp.notify.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpNotifyDaoConstants {
    
    /**
     * 通知渠道: 站内消息 
     */
    String NOTIFICATION_CHANNEL_IN_APP = "IN_APP";
                    
    /**
     * 通知渠道: 邮件 
     */
    String NOTIFICATION_CHANNEL_EMAIL = "EMAIL";
                    
    /**
     * 通知渠道: 短信 
     */
    String NOTIFICATION_CHANNEL_SMS = "SMS";
                    
    /**
     * 通知实例状态: 待发送 
     */
    String NOTIFICATION_STATUS_PENDING = "PENDING";
                    
    /**
     * 通知实例状态: 已发送 
     */
    String NOTIFICATION_STATUS_SENT = "SENT";
                    
    /**
     * 通知实例状态: 已合并 
     */
    String NOTIFICATION_STATUS_MERGED = "MERGED";
                    
    /**
     * 通知实例状态: 发送失败 
     */
    String NOTIFICATION_STATUS_FAILED = "FAILED";
                    
    /**
     * 接收人解析器: 按角色 
     */
    String RECIPIENT_RESOLVER_ROLE = "ROLE";
                    
    /**
     * 接收人解析器: 按部门 
     */
    String RECIPIENT_RESOLVER_ORG = "ORG";
                    
    /**
     * 接收人解析器: 按业务伙伴 
     */
    String RECIPIENT_RESOLVER_PARTNER = "PARTNER";
                    
    /**
     * 接收人解析器: 用户列表 
     */
    String RECIPIENT_RESOLVER_USER_LIST = "USER_LIST";
                    
    /**
     * 频控合并策略: 不合并 
     */
    String MERGE_STRATEGY_NONE = "NONE";
                    
    /**
     * 频控合并策略: 按用户与类型合并 
     */
    String MERGE_STRATEGY_MERGE_BY_USER_TYPE = "MERGE_BY_USER_TYPE";
                    
    /**
     * 模板状态: 草稿 
     */
    String TEMPLATE_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 模板状态: 启用 
     */
    String TEMPLATE_STATUS_ACTIVE = "ACTIVE";
                    
}
