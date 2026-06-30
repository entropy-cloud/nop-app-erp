package app.erp.aps.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpApsDaoConstants {
    
    /**
     * 工序工单状态: 草稿 
     */
    int OPERATION_ORDER_STATUS_DRAFT = 10;
                    
    /**
     * 工序工单状态: 已排程 
     */
    int OPERATION_ORDER_STATUS_PLANNED = 20;
                    
    /**
     * 工序工单状态: 执行中 
     */
    int OPERATION_ORDER_STATUS_IN_PROGRESS = 30;
                    
    /**
     * 工序工单状态: 已完成 
     */
    int OPERATION_ORDER_STATUS_FINISHED = 40;
                    
    /**
     * 工序工单状态: 已取消 
     */
    int OPERATION_ORDER_STATUS_CANCELLED = 50;
                    
    /**
     * 排产方案状态: 草稿 
     */
    int SCHEDULE_STATUS_DRAFT = 10;
                    
    /**
     * 排产方案状态: 已发布 
     */
    int SCHEDULE_STATUS_PUBLISHED = 20;
                    
    /**
     * 排产方案状态: 已归档 
     */
    int SCHEDULE_STATUS_ARCHIVED = 30;
                    
    /**
     * 约束类型: 维护停机 
     */
    int CONSTRAINT_TYPE_MAINTENANCE = 10;
                    
    /**
     * 约束类型: 刀具寿命 
     */
    int CONSTRAINT_TYPE_TOOL = 20;
                    
    /**
     * 约束类型: 人员约束 
     */
    int CONSTRAINT_TYPE_PERSONNEL = 30;
                    
    /**
     * 排产模式: 前向排产 
     */
    int SCHEDULING_MODE_FORWARD = 10;
                    
    /**
     * 排产模式: 后向排产 
     */
    int SCHEDULING_MODE_BACKWARD = 20;
                    
}
