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
                    
    /**
     * 调查发送渠道: 邮件 
     */
    int SURVEY_CHANNEL_EMAIL = 10;
                    
    /**
     * 调查发送渠道: 电话 
     */
    int SURVEY_CHANNEL_PHONE = 20;
                    
    /**
     * 调查发送渠道: 门户 
     */
    int SURVEY_CHANNEL_PORTAL = 30;
                    
    /**
     * 调查发送渠道: 在线聊天 
     */
    int SURVEY_CHANNEL_CHAT = 40;
                    
    /**
     * 服务类型: 保修期权益 
     */
    int SERVICE_TYPE_WARRANTY = 10;
                    
    /**
     * 服务类型: 付费支持合同 
     */
    int SERVICE_TYPE_SUPPORT_CONTRACT = 20;
                    
    /**
     * 服务类型: 按次计费 
     */
    int SERVICE_TYPE_PAY_PER_TICKET = 30;
                    
    /**
     * 合同类型: 年度 
     */
    int CONTRACT_TYPE_ANNUAL = 10;
                    
    /**
     * 合同类型: 季度 
     */
    int CONTRACT_TYPE_QUARTERLY = 20;
                    
    /**
     * 合同类型: 一次性 
     */
    int CONTRACT_TYPE_ONE_TIME = 30;
                    
    /**
     * 计费周期: 按月 
     */
    int BILLING_CYCLE_MONTHLY = 10;
                    
    /**
     * 计费周期: 按季度 
     */
    int BILLING_CYCLE_QUARTERLY = 20;
                    
    /**
     * 计费周期: 按年 
     */
    int BILLING_CYCLE_YEARLY = 30;
                    
    /**
     * 合同状态: 草稿 
     */
    int CONTRACT_STATUS_DRAFT = 10;
                    
    /**
     * 合同状态: 生效中 
     */
    int CONTRACT_STATUS_ACTIVE = 20;
                    
    /**
     * 合同状态: 已过期 
     */
    int CONTRACT_STATUS_EXPIRED = 30;
                    
    /**
     * 合同状态: 已取消 
     */
    int CONTRACT_STATUS_CANCELLED = 40;
                    
    /**
     * 履行动作类型: 创建工单 
     */
    int FULFILLMENT_ACTION_TYPE_CREATE_TICKET = 10;
                    
    /**
     * 履行动作类型: 分配处理团队 
     */
    int FULFILLMENT_ACTION_TYPE_ASSIGN_TEAM = 20;
                    
    /**
     * 履行动作类型: 分配处理人 
     */
    int FULFILLMENT_ACTION_TYPE_ASSIGN_AGENT = 30;
                    
    /**
     * 履行动作类型: 审批流程 
     */
    int FULFILLMENT_ACTION_TYPE_REQUEST_APPROVAL = 40;
                    
    /**
     * 履行动作类型: 通知客户 
     */
    int FULFILLMENT_ACTION_TYPE_NOTIFY_CUSTOMER = 50;
                    
    /**
     * 履行动作类型: 更新工单状态 
     */
    int FULFILLMENT_ACTION_TYPE_UPDATE_STATUS = 60;
                    
    /**
     * 履行动作类型: 创建子工单 
     */
    int FULFILLMENT_ACTION_TYPE_CREATE_CHILD_TICKET = 70;
                    
    /**
     * 履行动作类型: 触发外部工作流 
     */
    int FULFILLMENT_ACTION_TYPE_INVOKE_WORKFLOW = 80;
                    
    /**
     * 履行动作类型: 自动关闭 
     */
    int FULFILLMENT_ACTION_TYPE_CLOSE_TICKET = 90;
                    
    /**
     * 计时审批状态: 待审批 
     */
    int TIME_ENTRY_APPROVE_STATUS_PENDING = 10;
                    
    /**
     * 计时审批状态: 已审批 
     */
    int TIME_ENTRY_APPROVE_STATUS_APPROVED = 20;
                    
    /**
     * 计时审批状态: 已驳回 
     */
    int TIME_ENTRY_APPROVE_STATUS_REJECTED = 30;
                    
    /**
     * 计时来源: 手动录入 
     */
    int TIME_ENTRY_SOURCE_MANUAL = 10;
                    
    /**
     * 计时来源: 计时器导入 
     */
    int TIME_ENTRY_SOURCE_TIMER_IMPORT = 20;
                    
}
