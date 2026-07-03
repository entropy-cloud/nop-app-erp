package app.erp.qa.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpQaDaoConstants {
    
    /**
     * 检验类型: 来料检验 
     */
    int INSPECTION_TYPE_INCOMING = 10;
                    
    /**
     * 检验类型: 制程检验 
     */
    int INSPECTION_TYPE_IN_PROCESS = 20;
                    
    /**
     * 检验类型: 完工检验 
     */
    int INSPECTION_TYPE_FINAL = 30;
                    
    /**
     * 检验类型: 出货检验 
     */
    int INSPECTION_TYPE_OUTGOING = 40;
                    
    /**
     * 质检结果: 待检 
     */
    int INSPECTION_RESULT_PENDING = 10;
                    
    /**
     * 质检结果: 合格 
     */
    int INSPECTION_RESULT_ACCEPTED = 20;
                    
    /**
     * 质检结果: 让步接收 
     */
    int INSPECTION_RESULT_CONDITIONAL = 30;
                    
    /**
     * 质检结果: 不合格 
     */
    int INSPECTION_RESULT_REJECTED = 40;
                    
    /**
     * 不符合项状态: 待处理 
     */
    int NCR_STATUS_OPEN = 10;
                    
    /**
     * 不符合项状态: 评审中 
     */
    int NCR_STATUS_IN_REVIEW = 20;
                    
    /**
     * 不符合项状态: 已解决 
     */
    int NCR_STATUS_RESOLVED = 30;
                    
    /**
     * 不符合项状态: 已升级为召回 
     */
    int NCR_STATUS_ESCALATED_TO_RECALL = 35;
                    
    /**
     * 不符合项状态: 已取消 
     */
    int NCR_STATUS_CANCELLED = 40;
                    
    /**
     * 纠正措施类型: 纠正 
     */
    int ACTION_TYPE_CORRECTIVE = 10;
                    
    /**
     * 纠正措施类型: 预防 
     */
    int ACTION_TYPE_PREVENTIVE = 20;
                    
    /**
     * 纠正措施类型: 纠正预防 
     */
    int ACTION_TYPE_CAPA = 30;
                    
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
     * 不合格严重程度: 低 
     */
    int SEVERITY_LOW = 10;
                    
    /**
     * 不合格严重程度: 一般 
     */
    int SEVERITY_NORMAL = 20;
                    
    /**
     * 不合格严重程度: 高 
     */
    int SEVERITY_HIGH = 30;
                    
    /**
     * 不合格严重程度: 严重 
     */
    int SEVERITY_CRITICAL = 40;
                    
    /**
     * 纠正措施状态: 待执行 
     */
    int ACTION_STATUS_PENDING = 10;
                    
    /**
     * 纠正措施状态: 执行中 
     */
    int ACTION_STATUS_IN_PROGRESS = 20;
                    
    /**
     * 纠正措施状态: 已完成 
     */
    int ACTION_STATUS_COMPLETED = 30;
                    
    /**
     * 纠正措施状态: 已逾期 
     */
    int ACTION_STATUS_OVERDUE = 40;
                    
    /**
     * 风险状态: 待处理 
     */
    int RISK_STATUS_OPEN = 10;
                    
    /**
     * 风险状态: 已缓解 
     */
    int RISK_STATUS_MITIGATED = 20;
                    
    /**
     * 风险状态: 已关闭 
     */
    int RISK_STATUS_CLOSED = 30;
                    
    /**
     * 质量目标状态: 进行中 
     */
    int GOAL_STATUS_ACTIVE = 10;
                    
    /**
     * 质量目标状态: 已达成 
     */
    int GOAL_STATUS_ACHIEVED = 20;
                    
    /**
     * 质量目标状态: 未达成 
     */
    int GOAL_STATUS_FAILED = 30;
                    
    /**
     * 质量目标状态: 已取消 
     */
    int GOAL_STATUS_CANCELLED = 40;
                    
    /**
     * 评审类型: 内部评审 
     */
    int REVIEW_TYPE_INTERNAL = 10;
                    
    /**
     * 评审类型: 外部评审 
     */
    int REVIEW_TYPE_EXTERNAL = 20;
                    
    /**
     * 评审类型: 供应商评审 
     */
    int REVIEW_TYPE_SUPPLIER = 30;
                    
    /**
     * 处理决定: 报废 
     */
    int DISPOSITION_TYPE_SCRAP = 10;
                    
    /**
     * 处理决定: 退货 
     */
    int DISPOSITION_TYPE_RETURN = 20;
                    
    /**
     * 处理决定: 让步接收 
     */
    int DISPOSITION_TYPE_CONCESSION = 30;
                    
    /**
     * 处理决定: 降级使用 
     */
    int DISPOSITION_TYPE_DOWNGRADE = 40;
                    
    /**
     * 召回触发类型: 手动发起 
     */
    int RECALL_TRIGGER_TYPE_MANUAL = 10;
                    
    /**
     * 召回触发类型: 量具NCR升级 
     */
    int RECALL_TRIGGER_TYPE_GAUGE_NCR_UPGRADE = 20;
                    
    /**
     * 召回触发类型: 批次NCR升级 
     */
    int RECALL_TRIGGER_TYPE_BATCH_NCR_UPGRADE = 30;
                    
    /**
     * 召回触发类型: 监管要求 
     */
    int RECALL_TRIGGER_TYPE_REGULATORY = 40;
                    
    /**
     * 召回严重程度: 低 
     */
    int RECALL_SEVERITY_LOW = 10;
                    
    /**
     * 召回严重程度: 中 
     */
    int RECALL_SEVERITY_MEDIUM = 20;
                    
    /**
     * 召回严重程度: 高 
     */
    int RECALL_SEVERITY_HIGH = 30;
                    
    /**
     * 召回严重程度: 严重 
     */
    int RECALL_SEVERITY_CRITICAL = 40;
                    
    /**
     * 召回状态: 待处理 
     */
    int RECALL_STATUS_OPEN = 10;
                    
    /**
     * 召回状态: 已审批 
     */
    int RECALL_STATUS_APPROVED = 20;
                    
    /**
     * 召回状态: 执行中 
     */
    int RECALL_STATUS_IN_PROGRESS = 30;
                    
    /**
     * 召回状态: 已关闭 
     */
    int RECALL_STATUS_CLOSED = 40;
                    
    /**
     * 召回状态: 已取消 
     */
    int RECALL_STATUS_CANCELLED = 50;
                    
    /**
     * 召回目标退货状态: 待通知 
     */
    int RECALL_TARGET_RETURN_STATUS_PENDING = 10;
                    
    /**
     * 召回目标退货状态: 已通知 
     */
    int RECALL_TARGET_RETURN_STATUS_NOTIFIED = 20;
                    
    /**
     * 召回目标退货状态: 已退货 
     */
    int RECALL_TARGET_RETURN_STATUS_RETURNED = 30;
                    
}
