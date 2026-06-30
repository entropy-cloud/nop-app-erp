package app.erp.crm.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpCrmDaoConstants {
    
    /**
     * 线索商机类型: 线索 
     */
    String LEAD_TYPE_LEAD = "LEAD";
                    
    /**
     * 线索商机类型: 商机 
     */
    String LEAD_TYPE_OPPORTUNITY = "OPPORTUNITY";
                    
    /**
     * 线索商机状态: 新建 
     */
    String LEAD_DOC_STATUS_NEW = "NEW";
                    
    /**
     * 线索商机状态: 已验证 
     */
    String LEAD_DOC_STATUS_QUALIFIED = "QUALIFIED";
                    
    /**
     * 线索商机状态: 已转化 
     */
    String LEAD_DOC_STATUS_CONVERTED = "CONVERTED";
                    
    /**
     * 线索商机状态: 已丢失 
     */
    String LEAD_DOC_STATUS_LOST = "LOST";
                    
    /**
     * 线索商机状态: 已取消 
     */
    String LEAD_DOC_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 活动状态: 已计划 
     */
    String EVENT_STATUS_PLANNED = "PLANNED";
                    
    /**
     * 活动状态: 已完成 
     */
    String EVENT_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 活动状态: 已取消 
     */
    String EVENT_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 活动优先级: 低 
     */
    String EVENT_PRIORITY_LOW = "LOW";
                    
    /**
     * 活动优先级: 普通 
     */
    String EVENT_PRIORITY_NORMAL = "NORMAL";
                    
    /**
     * 活动优先级: 高 
     */
    String EVENT_PRIORITY_HIGH = "HIGH";
                    
    /**
     * 活动优先级: 紧急 
     */
    String EVENT_PRIORITY_URGENT = "URGENT";
                    
    /**
     * 活动类型(Activity): 备注 
     */
    String ACTIVITY_TYPE_NOTE = "NOTE";
                    
    /**
     * 活动类型(Activity): 通话 
     */
    String ACTIVITY_TYPE_CALL = "CALL";
                    
    /**
     * 活动类型(Activity): 邮件 
     */
    String ACTIVITY_TYPE_EMAIL = "EMAIL";
                    
    /**
     * 活动类型(Activity): 会议 
     */
    String ACTIVITY_TYPE_MEETING = "MEETING";
                    
    /**
     * 事件类型(Event): 通话 
     */
    String EVENT_TYPE_CALL = "CALL";
                    
    /**
     * 事件类型(Event): 邮件 
     */
    String EVENT_TYPE_EMAIL = "EMAIL";
                    
    /**
     * 事件类型(Event): 会议 
     */
    String EVENT_TYPE_MEETING = "MEETING";
                    
    /**
     * 事件类型(Event): 任务 
     */
    String EVENT_TYPE_TASK = "TASK";
                    
}
