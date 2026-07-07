package app.erp.cs.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpCsDaoConstants {
    
    /**
     * 工单状态: 新建 
     */
    String TICKET_STATUS_NEW = "NEW";
                    
    /**
     * 工单状态: 已分配 
     */
    String TICKET_STATUS_ASSIGNED = "ASSIGNED";
                    
    /**
     * 工单状态: 处理中 
     */
    String TICKET_STATUS_IN_PROGRESS = "IN_PROGRESS";
                    
    /**
     * 工单状态: 已解决 
     */
    String TICKET_STATUS_RESOLVED = "RESOLVED";
                    
    /**
     * 工单状态: 已关闭 
     */
    String TICKET_STATUS_CLOSED = "CLOSED";
                    
    /**
     * 工单状态: 已取消 
     */
    String TICKET_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 工单优先级: 低 
     */
    String TICKET_PRIORITY_LOW = "LOW";
                    
    /**
     * 工单优先级: 普通 
     */
    String TICKET_PRIORITY_NORMAL = "NORMAL";
                    
    /**
     * 工单优先级: 高 
     */
    String TICKET_PRIORITY_HIGH = "HIGH";
                    
    /**
     * 工单优先级: 紧急 
     */
    String TICKET_PRIORITY_URGENT = "URGENT";
                    
    /**
     * 工单来源: 电话 
     */
    String TICKET_SOURCE_PHONE = "PHONE";
                    
    /**
     * 工单来源: 邮件 
     */
    String TICKET_SOURCE_EMAIL = "EMAIL";
                    
    /**
     * 工单来源: 门户 
     */
    String TICKET_SOURCE_PORTAL = "PORTAL";
                    
    /**
     * 工单来源: 在线聊天 
     */
    String TICKET_SOURCE_CHAT = "CHAT";
                    
    /**
     * 工单来源: 社交媒体 
     */
    String TICKET_SOURCE_SOCIAL = "SOCIAL";
                    
    /**
     * 工单类型: 投诉 
     */
    String TICKET_TYPE_COMPLAINT = "COMPLAINT";
                    
    /**
     * 工单类型: 咨询 
     */
    String TICKET_TYPE_INQUIRY = "INQUIRY";
                    
    /**
     * 工单类型: 维修 
     */
    String TICKET_TYPE_REPAIR = "REPAIR";
                    
    /**
     * 工单类型: 退换货 
     */
    String TICKET_TYPE_RETURN = "RETURN";
                    
    /**
     * 工单类型: 其他 
     */
    String TICKET_TYPE_OTHER = "OTHER";
                    
    /**
     * SLA 策略: 紧急_4小时 
     */
    String SLA_POLICY_URGENT_4H = "URGENT_4H";
                    
    /**
     * SLA 策略: 高_8小时 
     */
    String SLA_POLICY_HIGH_8H = "HIGH_8H";
                    
    /**
     * SLA 策略: 普通_24小时 
     */
    String SLA_POLICY_NORMAL_24H = "NORMAL_24H";
                    
    /**
     * SLA 策略: 低_48小时 
     */
    String SLA_POLICY_LOW_48H = "LOW_48H";
                    
    /**
     * 操作类型: 分派 
     */
    String ACTION_TYPE_ASSIGN = "ASSIGN";
                    
    /**
     * 操作类型: 备注 
     */
    String ACTION_TYPE_NOTE = "NOTE";
                    
    /**
     * 操作类型: 附件 
     */
    String ACTION_TYPE_ATTACH = "ATTACH";
                    
    /**
     * 操作类型: 升级 
     */
    String ACTION_TYPE_ESCALATE = "ESCALATE";
                    
    /**
     * 操作类型: 关闭 
     */
    String ACTION_TYPE_CLOSE = "CLOSE";
                    
    /**
     * 操作类型: 取消 
     */
    String ACTION_TYPE_CANCEL = "CANCEL";
                    
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
     * 调查发送渠道: 邮件 
     */
    String SURVEY_CHANNEL_EMAIL = "EMAIL";
                    
    /**
     * 调查发送渠道: 电话 
     */
    String SURVEY_CHANNEL_PHONE = "PHONE";
                    
    /**
     * 调查发送渠道: 门户 
     */
    String SURVEY_CHANNEL_PORTAL = "PORTAL";
                    
    /**
     * 调查发送渠道: 在线聊天 
     */
    String SURVEY_CHANNEL_CHAT = "CHAT";
                    
    /**
     * 服务类型: 保修期权益 
     */
    String SERVICE_TYPE_WARRANTY = "WARRANTY";
                    
    /**
     * 服务类型: 付费支持合同 
     */
    String SERVICE_TYPE_SUPPORT_CONTRACT = "SUPPORT_CONTRACT";
                    
    /**
     * 服务类型: 按次计费 
     */
    String SERVICE_TYPE_PAY_PER_TICKET = "PAY_PER_TICKET";
                    
    /**
     * 合同类型: 年度 
     */
    String CONTRACT_TYPE_ANNUAL = "ANNUAL";
                    
    /**
     * 合同类型: 季度 
     */
    String CONTRACT_TYPE_QUARTERLY = "QUARTERLY";
                    
    /**
     * 合同类型: 一次性 
     */
    String CONTRACT_TYPE_ONE_TIME = "ONE_TIME";
                    
    /**
     * 计费周期: 按月 
     */
    String BILLING_CYCLE_MONTHLY = "MONTHLY";
                    
    /**
     * 计费周期: 按季度 
     */
    String BILLING_CYCLE_QUARTERLY = "QUARTERLY";
                    
    /**
     * 计费周期: 按年 
     */
    String BILLING_CYCLE_YEARLY = "YEARLY";
                    
    /**
     * 合同状态: 草稿 
     */
    String CONTRACT_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 合同状态: 生效中 
     */
    String CONTRACT_STATUS_ACTIVE = "ACTIVE";
                    
    /**
     * 合同状态: 已过期 
     */
    String CONTRACT_STATUS_EXPIRED = "EXPIRED";
                    
    /**
     * 合同状态: 已取消 
     */
    String CONTRACT_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 履行动作类型: 创建工单 
     */
    String FULFILLMENT_ACTION_TYPE_CREATE_TICKET = "CREATE_TICKET";
                    
    /**
     * 履行动作类型: 分配处理团队 
     */
    String FULFILLMENT_ACTION_TYPE_ASSIGN_TEAM = "ASSIGN_TEAM";
                    
    /**
     * 履行动作类型: 分配处理人 
     */
    String FULFILLMENT_ACTION_TYPE_ASSIGN_AGENT = "ASSIGN_AGENT";
                    
    /**
     * 履行动作类型: 审批流程 
     */
    String FULFILLMENT_ACTION_TYPE_REQUEST_APPROVAL = "REQUEST_APPROVAL";
                    
    /**
     * 履行动作类型: 通知客户 
     */
    String FULFILLMENT_ACTION_TYPE_NOTIFY_CUSTOMER = "NOTIFY_CUSTOMER";
                    
    /**
     * 履行动作类型: 更新工单状态 
     */
    String FULFILLMENT_ACTION_TYPE_UPDATE_STATUS = "UPDATE_STATUS";
                    
    /**
     * 履行动作类型: 创建子工单 
     */
    String FULFILLMENT_ACTION_TYPE_CREATE_CHILD_TICKET = "CREATE_CHILD_TICKET";
                    
    /**
     * 履行动作类型: 触发外部工作流 
     */
    String FULFILLMENT_ACTION_TYPE_INVOKE_WORKFLOW = "INVOKE_WORKFLOW";
                    
    /**
     * 履行动作类型: 自动关闭 
     */
    String FULFILLMENT_ACTION_TYPE_CLOSE_TICKET = "CLOSE_TICKET";
                    
    /**
     * 计时审批状态: 待审批 
     */
    String TIME_ENTRY_APPROVE_STATUS_PENDING = "PENDING";
                    
    /**
     * 计时审批状态: 已审批 
     */
    String TIME_ENTRY_APPROVE_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 计时审批状态: 已驳回 
     */
    String TIME_ENTRY_APPROVE_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 计时来源: 手动录入 
     */
    String TIME_ENTRY_SOURCE_MANUAL = "MANUAL";
                    
    /**
     * 计时来源: 计时器导入 
     */
    String TIME_ENTRY_SOURCE_TIMER_IMPORT = "TIMER_IMPORT";
                    
}
