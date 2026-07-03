package app.erp.mfg.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpMfgDaoConstants {
    
    /**
     * 工单状态: 草稿 
     */
    int WORK_ORDER_STATUS_DRAFT = 10;
                    
    /**
     * 工单状态: 已提交 
     */
    int WORK_ORDER_STATUS_SUBMITTED = 20;
                    
    /**
     * 工单状态: 未开始 
     */
    int WORK_ORDER_STATUS_NOT_STARTED = 30;
                    
    /**
     * 工单状态: 生产中 
     */
    int WORK_ORDER_STATUS_IN_PROCESS = 40;
                    
    /**
     * 工单状态: 已齐套 
     */
    int WORK_ORDER_STATUS_STOCK_RESERVED = 50;
                    
    /**
     * 工单状态: 部分齐套 
     */
    int WORK_ORDER_STATUS_STOCK_PARTIAL = 60;
                    
    /**
     * 工单状态: 已完工 
     */
    int WORK_ORDER_STATUS_COMPLETED = 70;
                    
    /**
     * 工单状态: 已停工 
     */
    int WORK_ORDER_STATUS_STOPPED = 80;
                    
    /**
     * 工单状态: 已关闭 
     */
    int WORK_ORDER_STATUS_CLOSED = 90;
                    
    /**
     * 工单状态: 已取消 
     */
    int WORK_ORDER_STATUS_CANCELLED = 100;
                    
    /**
     * 作业卡状态: 待开始 
     */
    int JOB_CARD_STATUS_OPEN = 10;
                    
    /**
     * 作业卡状态: 作业中 
     */
    int JOB_CARD_STATUS_WORK_IN_PROGRESS = 20;
                    
    /**
     * 作业卡状态: 部分转序 
     */
    int JOB_CARD_STATUS_PARTIALLY_TRANSFERRED = 30;
                    
    /**
     * 作业卡状态: 已转序 
     */
    int JOB_CARD_STATUS_MATERIAL_TRANSFERRED = 40;
                    
    /**
     * 作业卡状态: 暂停 
     */
    int JOB_CARD_STATUS_ON_HOLD = 50;
                    
    /**
     * 作业卡状态: 已提交 
     */
    int JOB_CARD_STATUS_SUBMITTED = 60;
                    
    /**
     * 作业卡状态: 已完成 
     */
    int JOB_CARD_STATUS_COMPLETED = 70;
                    
    /**
     * 作业卡状态: 已取消 
     */
    int JOB_CARD_STATUS_CANCELLED = 80;
                    
    /**
     * BOM 类型: 制造 
     */
    int BOM_TYPE_NORMAL = 10;
                    
    /**
     * BOM 类型: 虚拟件/Kit 
     */
    int BOM_TYPE_PHANTOM = 20;
                    
    /**
     * 消耗控制: 允许超耗 
     */
    int CONSUMPTION_FLEXIBLE = 10;
                    
    /**
     * 消耗控制: 超耗警告 
     */
    int CONSUMPTION_WARNING = 20;
                    
    /**
     * 消耗控制: 严格按 BOM 
     */
    int CONSUMPTION_STRICT = 30;
                    
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
     * 工单优先级: 低 
     */
    int PRIORITY_LOW = 10;
                    
    /**
     * 工单优先级: 普通 
     */
    int PRIORITY_NORMAL = 20;
                    
    /**
     * 工单优先级: 高 
     */
    int PRIORITY_HIGH = 30;
                    
    /**
     * 工单优先级: 紧急 
     */
    int PRIORITY_URGENT = 40;
                    
    /**
     * MRP 计划状态: 草稿 
     */
    int MRP_STATUS_DRAFT = 10;
                    
    /**
     * MRP 计划状态: 运算中 
     */
    int MRP_STATUS_RUNNING = 20;
                    
    /**
     * MRP 计划状态: 运算完成 
     */
    int MRP_STATUS_COMPLETED = 30;
                    
    /**
     * MRP 计划状态: 已确认(转工单) 
     */
    int MRP_STATUS_FIRMED = 40;
                    
    /**
     * MRP 计划状态: 已取消 
     */
    int MRP_STATUS_CANCELLED = 50;
                    
    /**
     * MRP 计划订单类型: 计划订单 
     */
    int MRP_ORDER_TYPE_PLANNED_ORDER = 10;
                    
    /**
     * MRP 计划订单类型: 请购建议 
     */
    int MRP_ORDER_TYPE_PURCHASE_REQUEST = 20;
                    
    /**
     * MRP 计划订单类型: 工单建议 
     */
    int MRP_ORDER_TYPE_WORK_ORDER_REQUEST = 30;
                    
    /**
     * MRP 计划订单类型: 委外建议 
     */
    int MRP_ORDER_TYPE_SUBCONTRACT_REQUEST = 40;
                    
    /**
     * MRP 需求来源: 销售订单 
     */
    int MRP_DEMAND_SOURCE_SALES_ORDER = 10;
                    
    /**
     * MRP 需求来源: 预测 
     */
    int MRP_DEMAND_SOURCE_FORECAST = 20;
                    
    /**
     * MRP 需求来源: 安全库存 
     */
    int MRP_DEMAND_SOURCE_SAFETY_STOCK = 30;
                    
    /**
     * MRP 需求来源: 手工录入 
     */
    int MRP_DEMAND_SOURCE_MANUAL = 40;
                    
    /**
     * 领料状态: 草稿 
     */
    int ISSUE_STATUS_DRAFT = 10;
                    
    /**
     * 领料状态: 已确认 
     */
    int ISSUE_STATUS_CONFIRMED = 20;
                    
    /**
     * 领料状态: 已出库 
     */
    int ISSUE_STATUS_DONE = 30;
                    
    /**
     * 领料状态: 已取消 
     */
    int ISSUE_STATUS_CANCELLED = 40;
                    
    /**
     * 委外单状态: 草稿 
     */
    int SUBCONTRACT_STATUS_DRAFT = 10;
                    
    /**
     * 委外单状态: 已生效 
     */
    int SUBCONTRACT_STATUS_ACTIVE = 20;
                    
    /**
     * 委外单状态: 已作废 
     */
    int SUBCONTRACT_STATUS_CANCELLED = 30;
                    
    /**
     * 成本滚算状态: 草稿 
     */
    int COST_ROLLUP_STATUS_DRAFT = 10;
                    
    /**
     * 成本滚算状态: 已计算 
     */
    int COST_ROLLUP_STATUS_CALCULATED = 20;
                    
    /**
     * 成本滚算状态: 已发布 
     */
    int COST_ROLLUP_STATUS_FIRMED = 30;
                    
    /**
     * 成本滚算状态: 已作废 
     */
    int COST_ROLLUP_STATUS_CANCELLED = 40;
                    
    /**
     * 联副产品类型: 联产品 
     */
    int BYPRODUCT_TYPE_CO_PRODUCT = 10;
                    
    /**
     * 联副产品类型: 副产品 
     */
    int BYPRODUCT_TYPE_BY_PRODUCT = 20;
                    
    /**
     * 班次类型: 全天单班 
     */
    int SHIFT_TYPE_ONE_SHIFT = 10;
                    
    /**
     * 班次类型: 早班 
     */
    int SHIFT_TYPE_MORNING = 20;
                    
    /**
     * 班次类型: 中班 
     */
    int SHIFT_TYPE_AFTERNOON = 30;
                    
    /**
     * 班次类型: 夜班 
     */
    int SHIFT_TYPE_NIGHT = 40;
                    
    /**
     * 工作日模式: 全周 
     */
    int WORK_DATE_PATTERN_ALL_WEEK = 10;
                    
    /**
     * 工作日模式: 工作日(周一至周五) 
     */
    int WORK_DATE_PATTERN_WEEKDAYS = 20;
                    
    /**
     * 工作日模式: 仅周末 
     */
    int WORK_DATE_PATTERN_WEEKEND = 30;
                    
}
