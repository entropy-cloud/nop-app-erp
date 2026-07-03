package app.erp.aps.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpApsDaoConstants {
    
    /**
     * 工序工单状态: 草稿 
     */
    String OPERATION_ORDER_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 工序工单状态: 已排程 
     */
    String OPERATION_ORDER_STATUS_PLANNED = "PLANNED";
                    
    /**
     * 工序工单状态: 执行中 
     */
    String OPERATION_ORDER_STATUS_IN_PROGRESS = "IN_PROGRESS";
                    
    /**
     * 工序工单状态: 已完成 
     */
    String OPERATION_ORDER_STATUS_FINISHED = "FINISHED";
                    
    /**
     * 工序工单状态: 已取消 
     */
    String OPERATION_ORDER_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 排产方案状态: 草稿 
     */
    String SCHEDULE_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 排产方案状态: 已发布 
     */
    String SCHEDULE_STATUS_PUBLISHED = "PUBLISHED";
                    
    /**
     * 排产方案状态: 已归档 
     */
    String SCHEDULE_STATUS_ARCHIVED = "ARCHIVED";
                    
    /**
     * 约束类型: 维护停机 
     */
    String CONSTRAINT_TYPE_MAINTENANCE = "MAINTENANCE";
                    
    /**
     * 约束类型: 刀具寿命 
     */
    String CONSTRAINT_TYPE_TOOL = "TOOL";
                    
    /**
     * 约束类型: 人员约束 
     */
    String CONSTRAINT_TYPE_PERSONNEL = "PERSONNEL";
                    
    /**
     * 排产模式: 前向排产 
     */
    String SCHEDULING_MODE_FORWARD = "FORWARD";
                    
    /**
     * 排产模式: 后向排产 
     */
    String SCHEDULING_MODE_BACKWARD = "BACKWARD";
                    
    /**
     * 派工类型: 自动派工 
     */
    String DISPATCH_TYPE_AUTO = "AUTO";
                    
    /**
     * 派工类型: 手动派工 
     */
    String DISPATCH_TYPE_MANUAL = "MANUAL";
                    
    /**
     * 派工类型: 保持 
     */
    String DISPATCH_TYPE_HOLD = "HOLD";
                    
    /**
     * 派工类型: 解除保持 
     */
    String DISPATCH_TYPE_UNHOLD = "UNHOLD";
                    
}
