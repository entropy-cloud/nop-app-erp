package app.erp.mnt.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpMntDaoConstants {
    
    /**
     * 维护计划类型: 预防性维护 
     */
    int SCHEDULE_TYPE_PREVENTIVE = 10;
                    
    /**
     * 维护计划类型: 预测性维护 
     */
    int SCHEDULE_TYPE_PREDICTIVE = 20;
                    
    /**
     * 维护计划类型: 校准 
     */
    int SCHEDULE_TYPE_CALIBRATION = 30;
                    
    /**
     * 维护访问状态: 草稿 
     */
    int VISIT_STATUS_DRAFT = 10;
                    
    /**
     * 维护访问状态: 已排程 
     */
    int VISIT_STATUS_SCHEDULED = 20;
                    
    /**
     * 维护访问状态: 执行中 
     */
    int VISIT_STATUS_IN_PROGRESS = 30;
                    
    /**
     * 维护访问状态: 已完成 
     */
    int VISIT_STATUS_COMPLETED = 40;
                    
    /**
     * 维护访问状态: 已取消 
     */
    int VISIT_STATUS_CANCELLED = 50;
                    
    /**
     * 维护请求状态: 待受理 
     */
    int REQUEST_STATUS_OPEN = 10;
                    
    /**
     * 维护请求状态: 已受理 
     */
    int REQUEST_STATUS_ACCEPTED = 20;
                    
    /**
     * 维护请求状态: 维修中 
     */
    int REQUEST_STATUS_IN_PROGRESS = 30;
                    
    /**
     * 维护请求状态: 已完成 
     */
    int REQUEST_STATUS_COMPLETED = 40;
                    
    /**
     * 维护请求状态: 已拒绝 
     */
    int REQUEST_STATUS_REJECTED = 50;
                    
    /**
     * 维护请求状态: 已取消 
     */
    int REQUEST_STATUS_CANCELLED = 60;
                    
    /**
     * 设备状态: 运行中 
     */
    int EQUIPMENT_STATUS_RUNNING = 10;
                    
    /**
     * 设备状态: 闲置 
     */
    int EQUIPMENT_STATUS_IDLE = 20;
                    
    /**
     * 设备状态: 维护中 
     */
    int EQUIPMENT_STATUS_UNDER_MAINTENANCE = 30;
                    
    /**
     * 设备状态: 故障停机 
     */
    int EQUIPMENT_STATUS_DOWN = 40;
                    
    /**
     * 设备状态: 已停用 
     */
    int EQUIPMENT_STATUS_DECOMMISSIONED = 50;
                    
    /**
     * 重复类型: 每日 
     */
    int RECURRENCE_TYPE_DAILY = 10;
                    
    /**
     * 重复类型: 每周 
     */
    int RECURRENCE_TYPE_WEEKLY = 20;
                    
    /**
     * 重复类型: 每月 
     */
    int RECURRENCE_TYPE_MONTHLY = 30;
                    
    /**
     * 重复类型: 每年 
     */
    int RECURRENCE_TYPE_YEARLY = 40;
                    
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
                    
    /**
     * 维护任务状态: 待执行 
     */
    int VISIT_TASK_STATUS_PENDING = 10;
                    
    /**
     * 维护任务状态: 执行中 
     */
    int VISIT_TASK_STATUS_IN_PROGRESS = 20;
                    
    /**
     * 维护任务状态: 已完成 
     */
    int VISIT_TASK_STATUS_COMPLETED = 30;
                    
    /**
     * 维护任务状态: 已跳过 
     */
    int VISIT_TASK_STATUS_SKIPPED = 40;
                    
    /**
     * 维护任务状态: 失败 
     */
    int VISIT_TASK_STATUS_FAILED = 50;
                    
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
     * 校准结果: 合格 
     */
    int CALIBRATION_RESULT_PASS = 10;
                    
    /**
     * 校准结果: 不合格 
     */
    int CALIBRATION_RESULT_FAIL = 20;
                    
    /**
     * 校准结果: 调整后合格 
     */
    int CALIBRATION_RESULT_ADJUSTED = 30;
                    
}
