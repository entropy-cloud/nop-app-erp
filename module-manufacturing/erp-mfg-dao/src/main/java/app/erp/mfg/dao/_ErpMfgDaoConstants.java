package app.erp.mfg.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpMfgDaoConstants {
    
    /**
     * 工单状态: 草稿 
     */
    String WORK_ORDER_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 工单状态: 已提交 
     */
    String WORK_ORDER_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 工单状态: 未开始 
     */
    String WORK_ORDER_STATUS_NOT_STARTED = "NOT_STARTED";
                    
    /**
     * 工单状态: 生产中 
     */
    String WORK_ORDER_STATUS_IN_PROCESS = "IN_PROCESS";
                    
    /**
     * 工单状态: 已齐套 
     */
    String WORK_ORDER_STATUS_STOCK_RESERVED = "STOCK_RESERVED";
                    
    /**
     * 工单状态: 部分齐套 
     */
    String WORK_ORDER_STATUS_STOCK_PARTIAL = "STOCK_PARTIAL";
                    
    /**
     * 工单状态: 已完工 
     */
    String WORK_ORDER_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 工单状态: 已停工 
     */
    String WORK_ORDER_STATUS_STOPPED = "STOPPED";
                    
    /**
     * 工单状态: 已关闭 
     */
    String WORK_ORDER_STATUS_CLOSED = "CLOSED";
                    
    /**
     * 工单状态: 已取消 
     */
    String WORK_ORDER_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 作业卡状态: 待开始 
     */
    String JOB_CARD_STATUS_OPEN = "OPEN";
                    
    /**
     * 作业卡状态: 作业中 
     */
    String JOB_CARD_STATUS_WORK_IN_PROGRESS = "WORK_IN_PROGRESS";
                    
    /**
     * 作业卡状态: 部分转序 
     */
    String JOB_CARD_STATUS_PARTIALLY_TRANSFERRED = "PARTIALLY_TRANSFERRED";
                    
    /**
     * 作业卡状态: 已转序 
     */
    String JOB_CARD_STATUS_MATERIAL_TRANSFERRED = "MATERIAL_TRANSFERRED";
                    
    /**
     * 作业卡状态: 暂停 
     */
    String JOB_CARD_STATUS_ON_HOLD = "ON_HOLD";
                    
    /**
     * 作业卡状态: 已提交 
     */
    String JOB_CARD_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 作业卡状态: 已完成 
     */
    String JOB_CARD_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 作业卡状态: 已取消 
     */
    String JOB_CARD_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * BOM 类型: 制造 
     */
    String BOM_TYPE_NORMAL = "NORMAL";
                    
    /**
     * BOM 类型: 虚拟件/Kit 
     */
    String BOM_TYPE_PHANTOM = "PHANTOM";
                    
    /**
     * 消耗控制: 允许超耗 
     */
    String CONSUMPTION_FLEXIBLE = "FLEXIBLE";
                    
    /**
     * 消耗控制: 超耗警告 
     */
    String CONSUMPTION_WARNING = "WARNING";
                    
    /**
     * 消耗控制: 严格按 BOM 
     */
    String CONSUMPTION_STRICT = "STRICT";
                    
    /**
     * 工单优先级: 低 
     */
    String PRIORITY_LOW = "LOW";
                    
    /**
     * 工单优先级: 普通 
     */
    String PRIORITY_NORMAL = "NORMAL";
                    
    /**
     * 工单优先级: 高 
     */
    String PRIORITY_HIGH = "HIGH";
                    
    /**
     * 工单优先级: 紧急 
     */
    String PRIORITY_URGENT = "URGENT";
                    
    /**
     * MRP 计划状态: 草稿 
     */
    String MRP_STATUS_DRAFT = "DRAFT";
                    
    /**
     * MRP 计划状态: 运算中 
     */
    String MRP_STATUS_RUNNING = "RUNNING";
                    
    /**
     * MRP 计划状态: 运算完成 
     */
    String MRP_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * MRP 计划状态: 已确认(转工单) 
     */
    String MRP_STATUS_FIRMED = "FIRMED";
                    
    /**
     * MRP 计划状态: 已取消 
     */
    String MRP_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * MRP 计划订单类型: 计划订单 
     */
    String MRP_ORDER_TYPE_PLANNED_ORDER = "PLANNED_ORDER";
                    
    /**
     * MRP 计划订单类型: 请购建议 
     */
    String MRP_ORDER_TYPE_PURCHASE_REQUEST = "PURCHASE_REQUEST";
                    
    /**
     * MRP 计划订单类型: 工单建议 
     */
    String MRP_ORDER_TYPE_WORK_ORDER_REQUEST = "WORK_ORDER_REQUEST";
                    
    /**
     * MRP 计划订单类型: 委外建议 
     */
    String MRP_ORDER_TYPE_SUBCONTRACT_REQUEST = "SUBCONTRACT_REQUEST";
                    
    /**
     * MRP 需求来源: 销售订单 
     */
    String MRP_DEMAND_SOURCE_SALES_ORDER = "SALES_ORDER";
                    
    /**
     * MRP 需求来源: 预测 
     */
    String MRP_DEMAND_SOURCE_FORECAST = "FORECAST";
                    
    /**
     * MRP 需求来源: 安全库存 
     */
    String MRP_DEMAND_SOURCE_SAFETY_STOCK = "SAFETY_STOCK";
                    
    /**
     * MRP 需求来源: 手工录入 
     */
    String MRP_DEMAND_SOURCE_MANUAL = "MANUAL";
                    
    /**
     * 领料状态: 草稿 
     */
    String ISSUE_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 领料状态: 已确认 
     */
    String ISSUE_STATUS_CONFIRMED = "CONFIRMED";
                    
    /**
     * 领料状态: 已出库 
     */
    String ISSUE_STATUS_DONE = "DONE";
                    
    /**
     * 领料状态: 已取消 
     */
    String ISSUE_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 委外单状态: 草稿 
     */
    String SUBCONTRACT_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 委外单状态: 已提交 
     */
    String SUBCONTRACT_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 委外单状态: 已审核 
     */
    String SUBCONTRACT_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 委外单状态: 已发料 
     */
    String SUBCONTRACT_STATUS_ISSUED = "ISSUED";
                    
    /**
     * 委外单状态: 已收货 
     */
    String SUBCONTRACT_STATUS_RECEIVED = "RECEIVED";
                    
    /**
     * 委外单状态: 已完成 
     */
    String SUBCONTRACT_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 委外单状态: 已取消 
     */
    String SUBCONTRACT_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 委外单状态: 已驳回 
     */
    String SUBCONTRACT_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 成本滚算状态: 草稿 
     */
    String COST_ROLLUP_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 成本滚算状态: 已计算 
     */
    String COST_ROLLUP_STATUS_CALCULATED = "CALCULATED";
                    
    /**
     * 成本滚算状态: 已发布 
     */
    String COST_ROLLUP_STATUS_FIRMED = "FIRMED";
                    
    /**
     * 成本滚算状态: 已作废 
     */
    String COST_ROLLUP_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 联副产品类型: 联产品 
     */
    String BYPRODUCT_TYPE_CO_PRODUCT = "CO_PRODUCT";
                    
    /**
     * 联副产品类型: 副产品 
     */
    String BYPRODUCT_TYPE_BY_PRODUCT = "BY_PRODUCT";
                    
    /**
     * 班次类型: 全天单班 
     */
    String SHIFT_TYPE_ONE_SHIFT = "ONE_SHIFT";
                    
    /**
     * 班次类型: 早班 
     */
    String SHIFT_TYPE_MORNING = "MORNING";
                    
    /**
     * 班次类型: 中班 
     */
    String SHIFT_TYPE_AFTERNOON = "AFTERNOON";
                    
    /**
     * 班次类型: 夜班 
     */
    String SHIFT_TYPE_NIGHT = "NIGHT";
                    
    /**
     * 需求预测状态: 草稿 
     */
    String FORECAST_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 需求预测状态: 已审批 
     */
    String FORECAST_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 需求预测状态: 已消费 
     */
    String FORECAST_STATUS_CONSUMED = "CONSUMED";
                    
    /**
     * 需求预测状态: 已取消 
     */
    String FORECAST_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 工作日模式: 全周 
     */
    String WORK_DATE_PATTERN_ALL_WEEK = "ALL_WEEK";
                    
    /**
     * 工作日模式: 工作日(周一至周五) 
     */
    String WORK_DATE_PATTERN_WEEKDAYS = "WEEKDAYS";
                    
    /**
     * 工作日模式: 仅周末 
     */
    String WORK_DATE_PATTERN_WEEKEND = "WEEKEND";
                    
    /**
     * 工单来源单据类型: 销售订单 
     */
    String SOURCE_ORDER_TYPE_SALES_ORDER = "SALES_ORDER";
                    
    /**
     * 工单来源单据类型: 预测 
     */
    String SOURCE_ORDER_TYPE_FORECAST = "FORECAST";
                    
    /**
     * 工单来源单据类型: 手工录入 
     */
    String SOURCE_ORDER_TYPE_MANUAL = "MANUAL";
                    
    /**
     * 工单来源单据类型: APS排程生成 
     */
    String SOURCE_ORDER_TYPE_APS_SCHEDULE = "APS_SCHEDULE";
                    
    /**
     * 差异类型: 材料用量差异 
     */
    String VARIANCE_TYPE_MATERIAL_USAGE = "MATERIAL_USAGE";
                    
    /**
     * 差异类型: 人工效率差异 
     */
    String VARIANCE_TYPE_LABOR_EFFICIENCY = "LABOR_EFFICIENCY";
                    
    /**
     * 差异类型: 人工费率差异 
     */
    String VARIANCE_TYPE_LABOR_RATE = "LABOR_RATE";
                    
    /**
     * 差异类型: 制造费用差异 
     */
    String VARIANCE_TYPE_OVERHEAD = "OVERHEAD";
                    
    /**
     * 差异类型: 产量差异 
     */
    String VARIANCE_TYPE_VOLUME = "VOLUME";
                    
    /**
     * 差异类型: 委外费差异 
     */
    String VARIANCE_TYPE_SUBCONTRACT = "SUBCONTRACT";
                    
    /**
     * 成本要素: 材料 
     */
    String COST_ELEMENT_MATERIAL = "MATERIAL";
                    
    /**
     * 成本要素: 人工 
     */
    String COST_ELEMENT_LABOR = "LABOR";
                    
    /**
     * 成本要素: 制造费用 
     */
    String COST_ELEMENT_OVERHEAD = "OVERHEAD";
                    
    /**
     * 成本要素: 委外 
     */
    String COST_ELEMENT_SUBCONTRACT = "SUBCONTRACT";
                    
}
