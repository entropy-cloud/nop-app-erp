package app.erp.qa.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpQaDaoConstants {
    
    /**
     * 检验类型: 来料检验 
     */
    String INSPECTION_TYPE_INCOMING = "INCOMING";
                    
    /**
     * 检验类型: 制程检验 
     */
    String INSPECTION_TYPE_IN_PROCESS = "IN_PROCESS";
                    
    /**
     * 检验类型: 完工检验 
     */
    String INSPECTION_TYPE_FINAL = "FINAL";
                    
    /**
     * 检验类型: 出货检验 
     */
    String INSPECTION_TYPE_OUTGOING = "OUTGOING";
                    
    /**
     * 质检结果: 待检 
     */
    String INSPECTION_RESULT_PENDING = "PENDING";
                    
    /**
     * 质检结果: 合格 
     */
    String INSPECTION_RESULT_ACCEPTED = "ACCEPTED";
                    
    /**
     * 质检结果: 让步接收 
     */
    String INSPECTION_RESULT_CONDITIONAL = "CONDITIONAL";
                    
    /**
     * 质检结果: 不合格 
     */
    String INSPECTION_RESULT_REJECTED = "REJECTED";
                    
    /**
     * 不符合项状态: 待处理 
     */
    String NCR_STATUS_OPEN = "OPEN";
                    
    /**
     * 不符合项状态: 评审中 
     */
    String NCR_STATUS_IN_REVIEW = "IN_REVIEW";
                    
    /**
     * 不符合项状态: 已解决 
     */
    String NCR_STATUS_RESOLVED = "RESOLVED";
                    
    /**
     * 不符合项状态: 已升级为召回 
     */
    String NCR_STATUS_ESCALATED_TO_RECALL = "ESCALATED_TO_RECALL";
                    
    /**
     * 不符合项状态: 已取消 
     */
    String NCR_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 纠正措施类型: 纠正 
     */
    String ACTION_TYPE_CORRECTIVE = "CORRECTIVE";
                    
    /**
     * 纠正措施类型: 预防 
     */
    String ACTION_TYPE_PREVENTIVE = "PREVENTIVE";
                    
    /**
     * 纠正措施类型: 纠正预防 
     */
    String ACTION_TYPE_CAPA = "CAPA";
                    
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
     * 不合格严重程度: 低 
     */
    String SEVERITY_LOW = "LOW";
                    
    /**
     * 不合格严重程度: 一般 
     */
    String SEVERITY_NORMAL = "NORMAL";
                    
    /**
     * 不合格严重程度: 高 
     */
    String SEVERITY_HIGH = "HIGH";
                    
    /**
     * 不合格严重程度: 严重 
     */
    String SEVERITY_CRITICAL = "CRITICAL";
                    
    /**
     * 纠正措施状态: 待执行 
     */
    String ACTION_STATUS_PENDING = "PENDING";
                    
    /**
     * 纠正措施状态: 执行中 
     */
    String ACTION_STATUS_IN_PROGRESS = "IN_PROGRESS";
                    
    /**
     * 纠正措施状态: 已完成 
     */
    String ACTION_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 纠正措施状态: 已逾期 
     */
    String ACTION_STATUS_OVERDUE = "OVERDUE";
                    
    /**
     * 风险状态: 待处理 
     */
    String RISK_STATUS_OPEN = "OPEN";
                    
    /**
     * 风险状态: 已缓解 
     */
    String RISK_STATUS_MITIGATED = "MITIGATED";
                    
    /**
     * 风险状态: 已关闭 
     */
    String RISK_STATUS_CLOSED = "CLOSED";
                    
    /**
     * 质量目标状态: 进行中 
     */
    String GOAL_STATUS_ACTIVE = "ACTIVE";
                    
    /**
     * 质量目标状态: 已达成 
     */
    String GOAL_STATUS_ACHIEVED = "ACHIEVED";
                    
    /**
     * 质量目标状态: 未达成 
     */
    String GOAL_STATUS_FAILED = "FAILED";
                    
    /**
     * 质量目标状态: 已取消 
     */
    String GOAL_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 评审类型: 内部评审 
     */
    String REVIEW_TYPE_INTERNAL = "INTERNAL";
                    
    /**
     * 评审类型: 外部评审 
     */
    String REVIEW_TYPE_EXTERNAL = "EXTERNAL";
                    
    /**
     * 评审类型: 供应商评审 
     */
    String REVIEW_TYPE_SUPPLIER = "SUPPLIER";
                    
    /**
     * 处理决定: 报废 
     */
    String DISPOSITION_TYPE_SCRAP = "SCRAP";
                    
    /**
     * 处理决定: 退货 
     */
    String DISPOSITION_TYPE_RETURN = "RETURN";
                    
    /**
     * 处理决定: 让步接收 
     */
    String DISPOSITION_TYPE_CONCESSION = "CONCESSION";
                    
    /**
     * 处理决定: 降级使用 
     */
    String DISPOSITION_TYPE_DOWNGRADE = "DOWNGRADE";
                    
    /**
     * 召回触发类型: 手动发起 
     */
    String RECALL_TRIGGER_TYPE_MANUAL = "MANUAL";
                    
    /**
     * 召回触发类型: 量具NCR升级 
     */
    String RECALL_TRIGGER_TYPE_GAUGE_NCR_UPGRADE = "GAUGE_NCR_UPGRADE";
                    
    /**
     * 召回触发类型: 批次NCR升级 
     */
    String RECALL_TRIGGER_TYPE_BATCH_NCR_UPGRADE = "BATCH_NCR_UPGRADE";
                    
    /**
     * 召回触发类型: 监管要求 
     */
    String RECALL_TRIGGER_TYPE_REGULATORY = "REGULATORY";
                    
    /**
     * 召回严重程度: 低 
     */
    String RECALL_SEVERITY_LOW = "LOW";
                    
    /**
     * 召回严重程度: 中 
     */
    String RECALL_SEVERITY_MEDIUM = "MEDIUM";
                    
    /**
     * 召回严重程度: 高 
     */
    String RECALL_SEVERITY_HIGH = "HIGH";
                    
    /**
     * 召回严重程度: 严重 
     */
    String RECALL_SEVERITY_CRITICAL = "CRITICAL";
                    
    /**
     * 召回状态: 待处理 
     */
    String RECALL_STATUS_OPEN = "OPEN";
                    
    /**
     * 召回状态: 已审批 
     */
    String RECALL_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 召回状态: 执行中 
     */
    String RECALL_STATUS_IN_PROGRESS = "IN_PROGRESS";
                    
    /**
     * 召回状态: 已关闭 
     */
    String RECALL_STATUS_CLOSED = "CLOSED";
                    
    /**
     * 召回状态: 已取消 
     */
    String RECALL_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 召回目标退货状态: 待通知 
     */
    String RECALL_TARGET_RETURN_STATUS_PENDING = "PENDING";
                    
    /**
     * 召回目标退货状态: 已通知 
     */
    String RECALL_TARGET_RETURN_STATUS_NOTIFIED = "NOTIFIED";
                    
    /**
     * 召回目标退货状态: 已退货 
     */
    String RECALL_TARGET_RETURN_STATUS_RETURNED = "RETURNED";
                    
    /**
     * SPC 控制图类型: 均值-极差图(X̄-R) 
     */
    String SPC_CHART_TYPE_X_BAR_R = "X_BAR_R";
                    
    /**
     * SPC 控制图类型: 均值-标准差图(X̄-s) 
     */
    String SPC_CHART_TYPE_X_BAR_S = "X_BAR_S";
                    
    /**
     * SPC 控制图类型: 单值-移动极差图(I-MR) 
     */
    String SPC_CHART_TYPE_X_MR = "X_MR";
                    
    /**
     * SPC 控制图类型: 不合格品率图(P) 
     */
    String SPC_CHART_TYPE_P = "P";
                    
    /**
     * SPC 控制图类型: 不合格品数图(NP) 
     */
    String SPC_CHART_TYPE_NP = "NP";
                    
    /**
     * SPC 控制图类型: 缺陷数图(C) 
     */
    String SPC_CHART_TYPE_C = "C";
                    
    /**
     * SPC 控制图类型: 单位缺陷数图(U) 
     */
    String SPC_CHART_TYPE_U = "U";
                    
    /**
     * SPC 控制限计算状态: 待计算 
     */
    String SPC_CALC_STATUS_PENDING = "PENDING";
                    
    /**
     * SPC 控制限计算状态: 已计算 
     */
    String SPC_CALC_STATUS_CALCULATED = "CALCULATED";
                    
    /**
     * SPC 控制限计算状态: 需重算 
     */
    String SPC_CALC_STATUS_STALE = "STALE";
                    
    /**
     * SPC 过程能力等级: 不足(Cpk<1.0) 
     */
    String SPC_CAPABILITY_INADEQUATE = "INADEQUATE";
                    
    /**
     * SPC 过程能力等级: 可接受(1.0≤Cpk<1.33) 
     */
    String SPC_CAPABILITY_ACCEPTABLE = "ACCEPTABLE";
                    
    /**
     * SPC 过程能力等级: 充分(1.33≤Cpk<1.67) 
     */
    String SPC_CAPABILITY_CAPABLE = "CAPABLE";
                    
    /**
     * SPC 过程能力等级: 优秀(Cpk≥1.67) 
     */
    String SPC_CAPABILITY_EXCELLENT = "EXCELLENT";
                    
    /**
     * SPC 中心线计算方式: 数据驱动 
     */
    String SPC_CL_CENTER_TYPE_AUTO_FROM_DATA = "AUTO_FROM_DATA";
                    
    /**
     * SPC 中心线计算方式: 手工录入 
     */
    String SPC_CL_CENTER_TYPE_MANUAL = "MANUAL";
                    
    /**
     * SPC 中心线计算方式: 目标值 
     */
    String SPC_CL_CENTER_TYPE_TARGET = "TARGET";
                    
}
