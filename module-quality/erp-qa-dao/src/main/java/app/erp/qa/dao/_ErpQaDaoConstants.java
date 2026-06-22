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
                    
}
