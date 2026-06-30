package app.erp.crm.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpCrmDaoConstants {
    
    /**
     * 线索商机类型: 线索 
     */
    String LEAD_TYPE_LEAD = "LEAD";
                    
    /**
     * 线索商机类型: 商机 
     */
    String LEAD_TYPE_OPPORTUNITY = "OPPORTUNITY";
                    
    /**
     * 线索商机状态: 新建 
     */
    String LEAD_DOC_STATUS_NEW = "NEW";
                    
    /**
     * 线索商机状态: 已验证 
     */
    String LEAD_DOC_STATUS_QUALIFIED = "QUALIFIED";
                    
    /**
     * 线索商机状态: 已转化 
     */
    String LEAD_DOC_STATUS_CONVERTED = "CONVERTED";
                    
    /**
     * 线索商机状态: 已丢失 
     */
    String LEAD_DOC_STATUS_LOST = "LOST";
                    
    /**
     * 线索商机状态: 已取消 
     */
    String LEAD_DOC_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 活动状态: 已计划 
     */
    String EVENT_STATUS_PLANNED = "PLANNED";
                    
    /**
     * 活动状态: 已完成 
     */
    String EVENT_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 活动状态: 已取消 
     */
    String EVENT_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 活动优先级: 低 
     */
    String EVENT_PRIORITY_LOW = "LOW";
                    
    /**
     * 活动优先级: 普通 
     */
    String EVENT_PRIORITY_NORMAL = "NORMAL";
                    
    /**
     * 活动优先级: 高 
     */
    String EVENT_PRIORITY_HIGH = "HIGH";
                    
    /**
     * 活动优先级: 紧急 
     */
    String EVENT_PRIORITY_URGENT = "URGENT";
                    
    /**
     * 活动类型(Activity): 备注 
     */
    String ACTIVITY_TYPE_NOTE = "NOTE";
                    
    /**
     * 活动类型(Activity): 通话 
     */
    String ACTIVITY_TYPE_CALL = "CALL";
                    
    /**
     * 活动类型(Activity): 邮件 
     */
    String ACTIVITY_TYPE_EMAIL = "EMAIL";
                    
    /**
     * 活动类型(Activity): 会议 
     */
    String ACTIVITY_TYPE_MEETING = "MEETING";
                    
    /**
     * 事件类型(Event): 通话 
     */
    String EVENT_TYPE_CALL = "CALL";
                    
    /**
     * 事件类型(Event): 邮件 
     */
    String EVENT_TYPE_EMAIL = "EMAIL";
                    
    /**
     * 事件类型(Event): 会议 
     */
    String EVENT_TYPE_MEETING = "MEETING";
                    
    /**
     * 事件类型(Event): 任务 
     */
    String EVENT_TYPE_TASK = "TASK";
                    
    /**
     * 评分方法: 查表映射 
     */
    String SCORING_METHOD_LOOKUP = "LOOKUP";
                    
    /**
     * 评分方法: 公式计算 
     */
    String SCORING_METHOD_FORMULA = "FORMULA";
                    
    /**
     * 评分方法: 是否匹配 
     */
    String SCORING_METHOD_BOOLEAN = "BOOLEAN";
                    
    /**
     * 评分触发事件: 手动 
     */
    String SCORING_TRIGGER_EVENT_MANUAL = "MANUAL";
                    
    /**
     * 评分触发事件: 线索变更 
     */
    String SCORING_TRIGGER_EVENT_LEAD_UPDATE = "LEAD_UPDATE";
                    
    /**
     * 评分触发事件: 定时批量 
     */
    String SCORING_TRIGGER_EVENT_SCHEDULED = "SCHEDULED";
                    
    /**
     * 评分触发动作: 无 
     */
    String SCORING_TRIGGERED_ACTION_NONE = "NONE";
                    
    /**
     * 评分触发动作: 自动转商机 
     */
    String SCORING_TRIGGERED_ACTION_AUTO_QUALIFY = "AUTO_QUALIFY";
                    
    /**
     * 评分触发动作: 通知负责人 
     */
    String SCORING_TRIGGERED_ACTION_NOTIFY_OWNER = "NOTIFY_OWNER";
                    
    /**
     * 预测期间类型: 月度 
     */
    String FORECAST_PERIOD_TYPE_MONTHLY = "MONTHLY";
                    
    /**
     * 预测期间类型: 季度 
     */
    String FORECAST_PERIOD_TYPE_QUARTERLY = "QUARTERLY";
                    
    /**
     * 预测期间类型: 年度 
     */
    String FORECAST_PERIOD_TYPE_ANNUAL = "ANNUAL";
                    
    /**
     * 预测期间状态: 进行中 
     */
    String FORECAST_PERIOD_STATUS_OPEN = "OPEN";
                    
    /**
     * 预测期间状态: 已结束 
     */
    String FORECAST_PERIOD_STATUS_CLOSED = "CLOSED";
                    
    /**
     * 预测期间状态: 已冻结 
     */
    String FORECAST_PERIOD_STATUS_FROZEN = "FROZEN";
                    
    /**
     * 预测分类: 承诺 
     */
    String FORECAST_CATEGORY_COMMIT = "COMMIT";
                    
    /**
     * 预测分类: 乐观 
     */
    String FORECAST_CATEGORY_UPSIDE = "UPSIDE";
                    
    /**
     * 预测分类: 最佳 
     */
    String FORECAST_CATEGORY_BEST_CASE = "BEST_CASE";
                    
    /**
     * 区域类型: 大区 
     */
    String TERRITORY_TYPE_REGION = "REGION";
                    
    /**
     * 区域类型: 地区 
     */
    String TERRITORY_TYPE_AREA = "AREA";
                    
    /**
     * 区域类型: 分公司 
     */
    String TERRITORY_TYPE_BRANCH = "BRANCH";
                    
    /**
     * 区域类型: 团队 
     */
    String TERRITORY_TYPE_TEAM = "TEAM";
                    
    /**
     * 分配条件类型: 按地理 
     */
    String ASSIGNMENT_CONDITION_TYPE_GEOGRAPHY = "GEOGRAPHY";
                    
    /**
     * 分配条件类型: 按行业 
     */
    String ASSIGNMENT_CONDITION_TYPE_INDUSTRY = "INDUSTRY";
                    
    /**
     * 分配条件类型: 按客户规模 
     */
    String ASSIGNMENT_CONDITION_TYPE_CUSTOMER_SIZE = "CUSTOMER_SIZE";
                    
    /**
     * 分配条件类型: 按自定义字段 
     */
    String ASSIGNMENT_CONDITION_TYPE_CUSTOM_FIELD = "CUSTOM_FIELD";
                    
    /**
     * 分配方法: 轮流分配 
     */
    String ASSIGNMENT_METHOD_ROUND_ROBIN = "ROUND_ROBIN";
                    
    /**
     * 分配方法: 按负载 
     */
    String ASSIGNMENT_METHOD_LOAD_BALANCED = "LOAD_BALANCED";
                    
    /**
     * 分配方法: 手动 
     */
    String ASSIGNMENT_METHOD_MANUAL = "MANUAL";
                    
    /**
     * 配额期间类型: 年度 
     */
    String QUOTA_PERIOD_TYPE_ANNUAL = "ANNUAL";
                    
    /**
     * 配额期间类型: 季度 
     */
    String QUOTA_PERIOD_TYPE_QUARTERLY = "QUARTERLY";
                    
    /**
     * 配额期间类型: 月度 
     */
    String QUOTA_PERIOD_TYPE_MONTHLY = "MONTHLY";
                    
    /**
     * 配置规则类型: 必选 
     */
    String CONFIG_RULE_TYPE_REQUIRED = "REQUIRED";
                    
    /**
     * 配置规则类型: 可选 
     */
    String CONFIG_RULE_TYPE_OPTIONAL = "OPTIONAL";
                    
    /**
     * 配置规则类型: 互斥 
     */
    String CONFIG_RULE_TYPE_EXCLUDED = "EXCLUDED";
                    
    /**
     * 配置规则类型: 推荐 
     */
    String CONFIG_RULE_TYPE_RECOMMENDED = "RECOMMENDED";
                    
    /**
     * 价格规则类型: 数量阶梯折扣 
     */
    String PRICE_RULE_TYPE_VOLUME = "VOLUME";
                    
    /**
     * 价格规则类型: 促销定价 
     */
    String PRICE_RULE_TYPE_PROMOTIONAL = "PROMOTIONAL";
                    
    /**
     * 价格规则类型: 客户特定价格 
     */
    String PRICE_RULE_TYPE_CUSTOMER_SPECIFIC = "CUSTOMER_SPECIFIC";
                    
    /**
     * 捆绑折扣类型: 百分比折扣 
     */
    String BUNDLE_DISCOUNT_TYPE_PERCENTAGE = "PERCENTAGE";
                    
    /**
     * 捆绑折扣类型: 固定金额折扣 
     */
    String BUNDLE_DISCOUNT_TYPE_FIXED = "FIXED";
                    
    /**
     * 序列模板类型: 新线索跟进 
     */
    String SEQUENCE_TEMPLATE_TYPE_NEW_LEAD = "NEW_LEAD";
                    
    /**
     * 序列模板类型: 验证跟进 
     */
    String SEQUENCE_TEMPLATE_TYPE_QUALIFICATION = "QUALIFICATION";
                    
    /**
     * 序列模板类型: 谈判跟进 
     */
    String SEQUENCE_TEMPLATE_TYPE_NEGOTIATION = "NEGOTIATION";
                    
    /**
     * 序列模板类型: 重新激活 
     */
    String SEQUENCE_TEMPLATE_TYPE_RE_ENGAGEMENT = "RE_ENGAGEMENT";
                    
    /**
     * 步骤完成条件: 通话完成 
     */
    String STEP_COMPLETION_CONDITION_CALL_COMPLETED = "CALL_COMPLETED";
                    
    /**
     * 步骤完成条件: 邮件打开 
     */
    String STEP_COMPLETION_CONDITION_EMAIL_OPENED = "EMAIL_OPENED";
                    
    /**
     * 步骤完成条件: 邮件回复 
     */
    String STEP_COMPLETION_CONDITION_EMAIL_REPLIED = "EMAIL_REPLIED";
                    
    /**
     * 步骤完成条件: 会议举行 
     */
    String STEP_COMPLETION_CONDITION_MEETING_HELD = "MEETING_HELD";
                    
    /**
     * 步骤完成条件: 任务完成 
     */
    String STEP_COMPLETION_CONDITION_TASK_DONE = "TASK_DONE";
                    
    /**
     * 序列进度状态: 进行中 
     */
    String SEQUENCE_PROGRESS_STATUS_IN_PROGRESS = "IN_PROGRESS";
                    
    /**
     * 序列进度状态: 已完成 
     */
    String SEQUENCE_PROGRESS_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 序列进度状态: 已跳过 
     */
    String SEQUENCE_PROGRESS_STATUS_SKIPPED = "SKIPPED";
                    
    /**
     * 序列分配条件类型: 按线索来源 
     */
    String SEQ_ASSIGNMENT_CONDITION_TYPE_LEAD_SOURCE = "LEAD_SOURCE";
                    
    /**
     * 序列分配条件类型: 按区域 
     */
    String SEQ_ASSIGNMENT_CONDITION_TYPE_TERRITORY = "TERRITORY";
                    
    /**
     * 序列分配条件类型: 按产品线 
     */
    String SEQ_ASSIGNMENT_CONDITION_TYPE_PRODUCT_LINE = "PRODUCT_LINE";
                    
    /**
     * 序列分配条件类型: 按自定义字段 
     */
    String SEQ_ASSIGNMENT_CONDITION_TYPE_CUSTOM_FIELD = "CUSTOM_FIELD";
                    
}
