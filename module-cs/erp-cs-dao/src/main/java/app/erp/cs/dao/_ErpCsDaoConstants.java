package app.erp.cs.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpCsDaoConstants {
    
    /**
     * 工单状态: 新建 
     */
    int TICKET_STATUS_NEW = 10;
                    
    /**
     * 工单状态: 已分配 
     */
    int TICKET_STATUS_ASSIGNED = 20;
                    
    /**
     * 工单状态: 处理中 
     */
    int TICKET_STATUS_IN_PROGRESS = 30;
                    
    /**
     * 工单状态: 已解决 
     */
    int TICKET_STATUS_RESOLVED = 40;
                    
    /**
     * 工单状态: 已关闭 
     */
    int TICKET_STATUS_CLOSED = 50;
                    
    /**
     * 工单状态: 已取消 
     */
    int TICKET_STATUS_CANCELLED = 60;
                    
    /**
     * 工单优先级: 低 
     */
    int TICKET_PRIORITY_LOW = 10;
                    
    /**
     * 工单优先级: 普通 
     */
    int TICKET_PRIORITY_NORMAL = 20;
                    
    /**
     * 工单优先级: 高 
     */
    int TICKET_PRIORITY_HIGH = 30;
                    
    /**
     * 工单优先级: 紧急 
     */
    int TICKET_PRIORITY_URGENT = 40;
                    
    /**
     * 工单来源: 电话 
     */
    int TICKET_SOURCE_PHONE = 10;
                    
    /**
     * 工单来源: 邮件 
     */
    int TICKET_SOURCE_EMAIL = 20;
                    
    /**
     * 工单来源: 门户 
     */
    int TICKET_SOURCE_PORTAL = 30;
                    
    /**
     * 工单来源: 在线聊天 
     */
    int TICKET_SOURCE_CHAT = 40;
                    
    /**
     * 工单来源: 社交媒体 
     */
    int TICKET_SOURCE_SOCIAL = 50;
                    
    /**
     * 工单类型: 投诉 
     */
    int TICKET_TYPE_COMPLAINT = 10;
                    
    /**
     * 工单类型: 咨询 
     */
    int TICKET_TYPE_INQUIRY = 20;
                    
    /**
     * 工单类型: 维修 
     */
    int TICKET_TYPE_REPAIR = 30;
                    
    /**
     * 工单类型: 退换货 
     */
    int TICKET_TYPE_RETURN = 40;
                    
    /**
     * 工单类型: 其他 
     */
    int TICKET_TYPE_OTHER = 99;
                    
    /**
     * SLA 策略: 紧急_4小时 
     */
    int SLA_POLICY_URGENT_4H = 10;
                    
    /**
     * SLA 策略: 高_8小时 
     */
    int SLA_POLICY_HIGH_8H = 20;
                    
    /**
     * SLA 策略: 普通_24小时 
     */
    int SLA_POLICY_NORMAL_24H = 30;
                    
    /**
     * SLA 策略: 低_48小时 
     */
    int SLA_POLICY_LOW_48H = 40;
                    
    /**
     * 操作类型: 分派 
     */
    int ACTION_TYPE_ASSIGN = 10;
                    
    /**
     * 操作类型: 备注 
     */
    int ACTION_TYPE_NOTE = 20;
                    
    /**
     * 操作类型: 附件 
     */
    int ACTION_TYPE_ATTACH = 30;
                    
    /**
     * 操作类型: 升级 
     */
    int ACTION_TYPE_ESCALATE = 40;
                    
    /**
     * 操作类型: 关闭 
     */
    int ACTION_TYPE_CLOSE = 50;
                    
    /**
     * 操作类型: 取消 
     */
    int ACTION_TYPE_CANCEL = 60;
                    
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
                    
}
