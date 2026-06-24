package app.erp.ast.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpAstDaoConstants {
    
    /**
     * 资产状态: 草稿 
     */
    int ASSET_STATUS_DRAFT = 10;
                    
    /**
     * 资产状态: 使用中 
     */
    int ASSET_STATUS_IN_SERVICE = 20;
                    
    /**
     * 资产状态: 闲置 
     */
    int ASSET_STATUS_IDLE = 30;
                    
    /**
     * 资产状态: 已报废 
     */
    int ASSET_STATUS_SCRAPPED = 40;
                    
    /**
     * 资产状态: 已出售 
     */
    int ASSET_STATUS_SOLD = 50;
                    
    /**
     * 折旧方法: 直线法 
     */
    int DEPRECIATION_METHOD_STRAIGHT_LINE = 10;
                    
    /**
     * 折旧方法: 双倍余额递减 
     */
    int DEPRECIATION_METHOD_DECLINING = 20;
                    
    /**
     * 折旧方法: 工作量法 
     */
    int DEPRECIATION_METHOD_UNITS = 30;
                    
    /**
     * 折旧计划状态: 待执行 
     */
    int DEPRECIATION_SCHEDULE_STATUS_PENDING = 10;
                    
    /**
     * 折旧计划状态: 已执行 
     */
    int DEPRECIATION_SCHEDULE_STATUS_EXECUTED = 20;
                    
    /**
     * 折旧计划状态: 已冲销 
     */
    int DEPRECIATION_SCHEDULE_STATUS_REVERSED = 30;
                    
    /**
     * 价值调整类型: 减值 
     */
    int ADJUSTMENT_TYPE_IMPAIRMENT = 10;
                    
    /**
     * 价值调整类型: 重估 
     */
    int ADJUSTMENT_TYPE_REVALUATION = 20;
                    
    /**
     * 处置类型: 报废 
     */
    int DISPOSAL_TYPE_SCRAPPED = 10;
                    
    /**
     * 处置类型: 出售 
     */
    int DISPOSAL_TYPE_SOLD = 20;
                    
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
     * 资本化来源类型: 库存 
     */
    int CAPITALIZATION_SOURCE_TYPE_INVENTORY = 10;
                    
    /**
     * 资本化来源类型: 在建工程 
     */
    int CAPITALIZATION_SOURCE_TYPE_CIP = 20;
                    
    /**
     * 处置原因: 报废 
     */
    int DISPOSAL_REASON_OBSOLETE = 10;
                    
    /**
     * 处置原因: 出售 
     */
    int DISPOSAL_REASON_SOLD = 20;
                    
    /**
     * 处置原因: 捐赠 
     */
    int DISPOSAL_REASON_DONATED = 30;
                    
    /**
     * 处置原因: 被盗 
     */
    int DISPOSAL_REASON_STOLEN = 40;
                    
    /**
     * 处置原因: 其他 
     */
    int DISPOSAL_REASON_OTHER = 50;
                    
}
