package app.erp.mnt.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpMntDaoConstants {
    
    /**
     * 维护计划类型: 预防性维护 
     */
    String SCHEDULE_TYPE_PREVENTIVE = "PREVENTIVE";
                    
    /**
     * 维护计划类型: 预测性维护 
     */
    String SCHEDULE_TYPE_PREDICTIVE = "PREDICTIVE";
                    
    /**
     * 维护计划类型: 校准 
     */
    String SCHEDULE_TYPE_CALIBRATION = "CALIBRATION";
                    
    /**
     * 维护访问状态: 草稿 
     */
    String VISIT_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 维护访问状态: 已排程 
     */
    String VISIT_STATUS_SCHEDULED = "SCHEDULED";
                    
    /**
     * 维护访问状态: 执行中 
     */
    String VISIT_STATUS_IN_PROGRESS = "IN_PROGRESS";
                    
    /**
     * 维护访问状态: 已完成 
     */
    String VISIT_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 维护访问状态: 已取消 
     */
    String VISIT_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 维护请求状态: 待受理 
     */
    String REQUEST_STATUS_OPEN = "OPEN";
                    
    /**
     * 维护请求状态: 已受理 
     */
    String REQUEST_STATUS_ACCEPTED = "ACCEPTED";
                    
    /**
     * 维护请求状态: 维修中 
     */
    String REQUEST_STATUS_IN_PROGRESS = "IN_PROGRESS";
                    
    /**
     * 维护请求状态: 已完成 
     */
    String REQUEST_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 维护请求状态: 已拒绝 
     */
    String REQUEST_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 维护请求状态: 已取消 
     */
    String REQUEST_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 设备状态: 运行中 
     */
    String EQUIPMENT_STATUS_RUNNING = "RUNNING";
                    
    /**
     * 设备状态: 闲置 
     */
    String EQUIPMENT_STATUS_IDLE = "IDLE";
                    
    /**
     * 设备状态: 维护中 
     */
    String EQUIPMENT_STATUS_UNDER_MAINTENANCE = "UNDER_MAINTENANCE";
                    
    /**
     * 设备状态: 故障停机 
     */
    String EQUIPMENT_STATUS_DOWN = "DOWN";
                    
    /**
     * 设备状态: 已停用 
     */
    String EQUIPMENT_STATUS_DECOMMISSIONED = "DECOMMISSIONED";
                    
    /**
     * 重复类型: 每日 
     */
    String RECURRENCE_TYPE_DAILY = "DAILY";
                    
    /**
     * 重复类型: 每周 
     */
    String RECURRENCE_TYPE_WEEKLY = "WEEKLY";
                    
    /**
     * 重复类型: 每月 
     */
    String RECURRENCE_TYPE_MONTHLY = "MONTHLY";
                    
    /**
     * 重复类型: 每年 
     */
    String RECURRENCE_TYPE_YEARLY = "YEARLY";
                    
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
     * 维护任务状态: 待执行 
     */
    String VISIT_TASK_STATUS_PENDING = "PENDING";
                    
    /**
     * 维护任务状态: 执行中 
     */
    String VISIT_TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
                    
    /**
     * 维护任务状态: 已完成 
     */
    String VISIT_TASK_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 维护任务状态: 已跳过 
     */
    String VISIT_TASK_STATUS_SKIPPED = "SKIPPED";
                    
    /**
     * 维护任务状态: 失败 
     */
    String VISIT_TASK_STATUS_FAILED = "FAILED";
                    
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
     * 校准结果: 合格 
     */
    String CALIBRATION_RESULT_PASS = "PASS";
                    
    /**
     * 校准结果: 不合格 
     */
    String CALIBRATION_RESULT_FAIL = "FAIL";
                    
    /**
     * 校准结果: 调整后合格 
     */
    String CALIBRATION_RESULT_ADJUSTED = "ADJUSTED";
                    
    /**
     * 维护类型: 计划性 
     */
    String VISIT_TYPE_PLANNED = "PLANNED";
                    
    /**
     * 维护类型: 响应性 
     */
    String VISIT_TYPE_RESPONSIVE = "RESPONSIVE";
                    
    /**
     * 执行结果: 正常 
     */
    String VISIT_RESULT_NORMAL = "NORMAL";
                    
    /**
     * 执行结果: 异常 
     */
    String VISIT_RESULT_ABNORMAL = "ABNORMAL";
                    
    /**
     * 执行结果: 部分完成 
     */
    String VISIT_RESULT_PARTIAL = "PARTIAL";
                    
}
