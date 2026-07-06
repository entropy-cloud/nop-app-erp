package app.erp.prj.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpPrjDaoConstants {
    
    /**
     * 项目状态: 草稿 
     */
    String PROJECT_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 项目状态: 进行中 
     */
    String PROJECT_STATUS_OPEN = "OPEN";
                    
    /**
     * 项目状态: 暂停 
     */
    String PROJECT_STATUS_ON_HOLD = "ON_HOLD";
                    
    /**
     * 项目状态: 已完成 
     */
    String PROJECT_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 项目状态: 已取消 
     */
    String PROJECT_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 任务状态: 待开始 
     */
    String TASK_STATUS_TODO = "TODO";
                    
    /**
     * 任务状态: 进行中 
     */
    String TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
                    
    /**
     * 任务状态: 已完成 
     */
    String TASK_STATUS_DONE = "DONE";
                    
    /**
     * 任务状态: 阻塞 
     */
    String TASK_STATUS_BLOCKED = "BLOCKED";
                    
    /**
     * 工时状态: 草稿 
     */
    String TIMESHEET_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 工时状态: 已提交 
     */
    String TIMESHEET_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 工时状态: 已审批 
     */
    String TIMESHEET_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 优先级: 低 
     */
    String PRIORITY_LOW = "LOW";
                    
    /**
     * 优先级: 普通 
     */
    String PRIORITY_NORMAL = "NORMAL";
                    
    /**
     * 优先级: 高 
     */
    String PRIORITY_HIGH = "HIGH";
                    
    /**
     * 优先级: 紧急 
     */
    String PRIORITY_URGENT = "URGENT";
                    
    /**
     * 损益计算状态: 待计算 
     */
    String PNL_CALC_STATUS_PENDING = "PENDING";
                    
    /**
     * 损益计算状态: 已计算 
     */
    String PNL_CALC_STATUS_CALCULATED = "CALCULATED";
                    
    /**
     * 结算类型: 竣工结算 
     */
    String SETTLEMENT_TYPE_FINAL = "FINAL";
                    
    /**
     * 结算类型: 阶段结算 
     */
    String SETTLEMENT_TYPE_INTERIM = "INTERIM";
                    
    /**
     * 结算类型: 关闭转固 
     */
    String SETTLEMENT_TYPE_CLOSE = "CLOSE";
                    
}
