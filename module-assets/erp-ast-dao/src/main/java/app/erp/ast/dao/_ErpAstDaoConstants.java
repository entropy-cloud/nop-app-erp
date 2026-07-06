package app.erp.ast.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpAstDaoConstants {
    
    /**
     * 资产状态: 草稿 
     */
    String ASSET_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 资产状态: 使用中 
     */
    String ASSET_STATUS_IN_SERVICE = "IN_SERVICE";
                    
    /**
     * 资产状态: 闲置 
     */
    String ASSET_STATUS_IDLE = "IDLE";
                    
    /**
     * 资产状态: 已报废 
     */
    String ASSET_STATUS_SCRAPPED = "SCRAPPED";
                    
    /**
     * 资产状态: 已出售 
     */
    String ASSET_STATUS_SOLD = "SOLD";
                    
    /**
     * 折旧方法: 直线法 
     */
    String DEPRECIATION_METHOD_STRAIGHT_LINE = "STRAIGHT_LINE";
                    
    /**
     * 折旧方法: 双倍余额递减 
     */
    String DEPRECIATION_METHOD_DECLINING = "DECLINING";
                    
    /**
     * 折旧方法: 工作量法 
     */
    String DEPRECIATION_METHOD_UNITS = "UNITS";
                    
    /**
     * 折旧计划状态: 待执行 
     */
    String DEPRECIATION_SCHEDULE_STATUS_PENDING = "PENDING";
                    
    /**
     * 折旧计划状态: 已执行 
     */
    String DEPRECIATION_SCHEDULE_STATUS_EXECUTED = "EXECUTED";
                    
    /**
     * 折旧计划状态: 已冲销 
     */
    String DEPRECIATION_SCHEDULE_STATUS_REVERSED = "REVERSED";
                    
    /**
     * 折旧计划状态: 已取消 
     */
    String DEPRECIATION_SCHEDULE_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 价值调整类型: 减值 
     */
    String ADJUSTMENT_TYPE_IMPAIRMENT = "IMPAIRMENT";
                    
    /**
     * 价值调整类型: 重估增值 
     */
    String ADJUSTMENT_TYPE_REVALUATION_UP = "REVALUATION_UP";
                    
    /**
     * 价值调整类型: 重估减值 
     */
    String ADJUSTMENT_TYPE_REVALUATION_DOWN = "REVALUATION_DOWN";
                    
    /**
     * 处置类型: 报废 
     */
    String DISPOSAL_TYPE_SCRAPPED = "SCRAPPED";
                    
    /**
     * 处置类型: 出售 
     */
    String DISPOSAL_TYPE_SOLD = "SOLD";
                    
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
     * 审核状态: 未提交 
     */
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
                    
    /**
     * 审核状态: 已提交 
     */
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 审核状态: 已审核 
     */
    String APPROVE_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 审核状态: 已驳回 
     */
    String APPROVE_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 资本化来源类型: 库存 
     */
    String CAPITALIZATION_SOURCE_TYPE_INVENTORY = "INVENTORY";
                    
    /**
     * 资本化来源类型: 在建工程 
     */
    String CAPITALIZATION_SOURCE_TYPE_CIP = "CIP";
                    
    /**
     * 资本化来源类型: 直接购置 
     */
    String CAPITALIZATION_SOURCE_TYPE_DIRECT_PURCHASE = "DIRECT_PURCHASE";
                    
    /**
     * 处置原因: 报废 
     */
    String DISPOSAL_REASON_OBSOLETE = "OBSOLETE";
                    
    /**
     * 处置原因: 出售 
     */
    String DISPOSAL_REASON_SOLD = "SOLD";
                    
    /**
     * 处置原因: 捐赠 
     */
    String DISPOSAL_REASON_DONATED = "DONATED";
                    
    /**
     * 处置原因: 被盗 
     */
    String DISPOSAL_REASON_STOLEN = "STOLEN";
                    
    /**
     * 处置原因: 其他 
     */
    String DISPOSAL_REASON_OTHER = "OTHER";
                    
    /**
     * 在建工程状态: 草稿 
     */
    String CIP_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 在建工程状态: 建设中 
     */
    String CIP_STATUS_IN_CONSTRUCTION = "IN_CONSTRUCTION";
                    
    /**
     * 在建工程状态: 已完工转固 
     */
    String CIP_STATUS_TRANSFERRED = "TRANSFERRED";
                    
    /**
     * CIP成本类型: 采购 
     */
    String CIP_COST_TYPE_PURCHASE = "PURCHASE";
                    
    /**
     * CIP成本类型: 服务 
     */
    String CIP_COST_TYPE_SERVICE = "SERVICE";
                    
    /**
     * CIP成本类型: 人工 
     */
    String CIP_COST_TYPE_LABOR = "LABOR";
                    
    /**
     * CIP成本类型: 利息资本化 
     */
    String CIP_COST_TYPE_INTEREST_CAPITALIZATION = "INTEREST_CAPITALIZATION";
                    
    /**
     * CIP成本类型: 其他 
     */
    String CIP_COST_TYPE_OTHER = "OTHER";
                    
}
