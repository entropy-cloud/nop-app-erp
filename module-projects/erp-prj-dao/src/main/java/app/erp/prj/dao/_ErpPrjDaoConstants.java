package app.erp.prj.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpPrjDaoConstants {
    
    /**
     * 项目状态: 草稿 
     */
    int PROJECT_STATUS_DRAFT = 10;
                    
    /**
     * 项目状态: 进行中 
     */
    int PROJECT_STATUS_OPEN = 20;
                    
    /**
     * 项目状态: 暂停 
     */
    int PROJECT_STATUS_ON_HOLD = 30;
                    
    /**
     * 项目状态: 已完成 
     */
    int PROJECT_STATUS_COMPLETED = 40;
                    
    /**
     * 项目状态: 已取消 
     */
    int PROJECT_STATUS_CANCELLED = 50;
                    
    /**
     * 任务状态: 待开始 
     */
    int TASK_STATUS_TODO = 10;
                    
    /**
     * 任务状态: 进行中 
     */
    int TASK_STATUS_IN_PROGRESS = 20;
                    
    /**
     * 任务状态: 已完成 
     */
    int TASK_STATUS_DONE = 30;
                    
    /**
     * 任务状态: 阻塞 
     */
    int TASK_STATUS_BLOCKED = 40;
                    
    /**
     * 工时状态: 草稿 
     */
    int TIMESHEET_STATUS_DRAFT = 10;
                    
    /**
     * 工时状态: 已提交 
     */
    int TIMESHEET_STATUS_SUBMITTED = 20;
                    
    /**
     * 工时状态: 已审批 
     */
    int TIMESHEET_STATUS_APPROVED = 30;
                    
    /**
     * 优先级: 低 
     */
    int PRIORITY_LOW = 10;
                    
    /**
     * 优先级: 普通 
     */
    int PRIORITY_NORMAL = 20;
                    
    /**
     * 优先级: 高 
     */
    int PRIORITY_HIGH = 30;
                    
    /**
     * 优先级: 紧急 
     */
    int PRIORITY_URGENT = 40;
                    
}
