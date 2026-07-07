package app.erp.crm.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * CRM 域业务异常错误码。所有 CRM 流程中的业务异常使用 {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 */
public interface ErpCrmErrors {

    String ARG_LEAD_ID = "leadId";
    String ARG_LEAD_CODE = "leadCode";
    String ARG_LEAD_TYPE = "leadType";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_FROM_STAGE_ID = "fromStageId";
    String ARG_TO_STAGE_ID = "toStageId";
    String ARG_STAGE_ID = "stageId";
    String ARG_PARTNER_ID = "partnerId";
    String ARG_QUOTATION_CODE = "quotationCode";
    String ARG_DUPLICATE_COUNT = "duplicateCount";
    String ARG_EVENT_ID = "eventId";
    String ARG_EVENT_CODE = "eventCode";
    String ARG_PERIOD_ID = "periodId";
    String ARG_CONFIG_ID = "configId";
    String ARG_ACTIVE_COUNT = "activeCount";

    // --- 报表渲染作用域参数键 ---
    String ARG_REPORT_NAME = "reportName";
    String ARG_RENDER_TYPE = "renderType";

    // --- 区域管理 / 配额 参数键 ---
    String ARG_TERRITORY_ID = "territoryId";
    String ARG_TERRITORY_CODE = "territoryCode";
    String ARG_PARENT_ID = "parentId";
    String ARG_NEW_PARENT_ID = "newParentId";
    String ARG_CURRENT_LEVEL = "currentLevel";
    String ARG_MAX_LEVEL = "maxLevel";
    String ARG_QUOTA_ID = "quotaId";
    String ARG_PERIOD_TYPE = "periodType";
    String ARG_PERIOD_LABEL = "periodLabel";
    String ARG_CONDITION_TYPE = "conditionType";

    // --- CPQ 参数键 ---
    String ARG_CONFIGURATOR_ID = "configuratorId";
    String ARG_RULE_COUNT = "ruleCount";
    String ARG_MAX_RULES = "maxRules";
    String ARG_DISCOUNT_TYPE = "discountType";
    String ARG_PRODUCT_ID = "productId";
    String ARG_CUSTOMER_ID = "customerId";
    String ARG_QUANTITY = "quantity";
    String ARG_MIN_QTY = "minQuantity";
    String ARG_MAX_QTY = "maxQuantity";
    String ARG_EFFECTIVE_FROM = "effectiveFrom";
    String ARG_EFFECTIVE_TO = "effectiveTo";

    // --- 序列 / 漏斗 参数键（plan 2026-07-07-1430-3） ---
    String ARG_SEQUENCE_ID = "sequenceId";
    String ARG_PROGRESS_ID = "progressId";
    String ARG_STEP_INDEX = "stepIndex";
    String ARG_TEMPLATE_TYPE = "templateType";
    String ARG_PERIOD_START = "periodStart";
    String ARG_PERIOD_END = "periodEnd";
    String ARG_FUNNEL_ID = "funnelId";

    ErrorCode ERR_LEAD_NOT_FOUND = ErrorCode.define("erp.err.crm.lead-not-found",
            "线索/商机 {leadId} 不存在", ARG_LEAD_ID);

    ErrorCode ERR_LEAD_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.crm.lead-illegal-status-transition",
            "线索/商机 {leadCode} 当前单据状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_LEAD_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_LOST_REASON_REQUIRED = ErrorCode.define("erp.err.crm.lost-reason-required",
            "线索/商机 {leadCode} 标记丢单必须填写丢单原因", ARG_LEAD_CODE);

    ErrorCode ERR_STAGE_NOT_FOUND = ErrorCode.define("erp.err.crm.stage-not-found",
            "漏斗阶段 {stageId} 不存在", ARG_STAGE_ID);

    ErrorCode ERR_LEAD_STAGE_MISMATCH = ErrorCode.define("erp.err.crm.lead-stage-mismatch",
            "线索/商机 {leadCode} 当前阶段不匹配（期望 fromStageId={fromStageId}，实际 stageId={toStageId}）",
            ARG_LEAD_CODE, ARG_FROM_STAGE_ID, ARG_TO_STAGE_ID);

    ErrorCode ERR_LEAD_TYPE_MISMATCH = ErrorCode.define("erp.err.crm.lead-type-mismatch",
            "线索/商机 {leadCode} 类型不匹配（期望 {leadType}）", ARG_LEAD_CODE, ARG_LEAD_TYPE);

    ErrorCode ERR_OPPORTUNITY_PARTNER_REQUIRED = ErrorCode.define("erp.err.crm.opportunity-partner-required",
            "商机 {leadCode} 缺少客户，不可转报价单（须先转客户或关联 partnerId）", ARG_LEAD_CODE);

    ErrorCode ERR_LEAD_ALREADY_CONVERTED = ErrorCode.define("erp.err.crm.lead-already-converted",
            "线索/商机 {leadCode} 已转化，不可重复转化", ARG_LEAD_CODE);

    ErrorCode ERR_DUPLICATE_LEAD_FOUND = ErrorCode.define("erp.err.crm.duplicate-lead-found",
            "发现 {duplicateCount} 条疑似重复线索（companyName/contactEmail/contactPhone 命中既有非终态线索）",
            ARG_DUPLICATE_COUNT);

    ErrorCode ERR_EVENT_NOT_FOUND = ErrorCode.define("erp.err.crm.event-not-found",
            "活动/事件 {eventId} 不存在", ARG_EVENT_ID);

    ErrorCode ERR_EVENT_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.crm.event-illegal-status-transition",
            "活动/事件 {eventCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_EVENT_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_MULTIPLE_ACTIVE_SCORE_CONFIG = ErrorCode.define("erp.err.crm.multiple-active-score-config",
            "存在 {activeCount} 个 isActive=true 的评分规则配置（同一时间仅允许一个生效）", ARG_ACTIVE_COUNT);

    ErrorCode ERR_FORECAST_PERIOD_NOT_FOUND = ErrorCode.define("erp.err.crm.forecast-period-not-found",
            "预测期间 {periodId} 不存在", ARG_PERIOD_ID);

    ErrorCode ERR_FORECAST_PERIOD_NOT_OPEN = ErrorCode.define("erp.err.crm.forecast-period-not-open",
            "预测期间 {periodId} 当前状态={currentStatus}，仅 OPEN 期间可刷新预测（期望状态={expectedStatus}）",
            ARG_PERIOD_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    // --- 报表渲染作用域（镜像 ErpMfgErrors.ERR_REPORT_*，CRM 域独立错误码，不跨域 import） ---

    ErrorCode ERR_REPORT_NAME_INVALID = ErrorCode.define(
            "erp.err.crm.report.name-invalid",
            "报表名[{reportName}]非法（含路径注入字符或不合规段），拒绝渲染",
            ARG_REPORT_NAME);

    ErrorCode ERR_REPORT_RENDER_TYPE_INVALID = ErrorCode.define(
            "erp.err.crm.report.render-type-invalid",
            "渲染类型[{renderType}]非法（仅允许 html/xlsx/pdf）",
            ARG_RENDER_TYPE);

    // --- 区域树维护 ---

    ErrorCode ERR_TERRITORY_MAX_DEPTH_EXCEEDED = ErrorCode.define(
            "erp.err.crm.territory.max-depth-exceeded",
            "销售区域层级深度超限（当前 level={currentLevel}，最大允许={maxLevel}）",
            ARG_CURRENT_LEVEL, ARG_MAX_LEVEL);

    ErrorCode ERR_TERRITORY_CYCLE = ErrorCode.define(
            "erp.err.crm.territory.cycle",
            "销售区域 parentId 成环（territoryId={territoryId}，attempted parentId={parentId}）",
            ARG_TERRITORY_ID, ARG_PARENT_ID);

    ErrorCode ERR_TERRITORY_HAS_CHILDREN = ErrorCode.define(
            "erp.err.crm.territory.has-children",
            "销售区域 {territoryId} 存在子节点，禁止删除（须先迁移或删除子节点）",
            ARG_TERRITORY_ID);

    ErrorCode ERR_TERRITORY_NOT_ACTIVE = ErrorCode.define(
            "erp.err.crm.territory.not-active",
            "销售区域 {territoryId} 已停用，不参与分配匹配",
            ARG_TERRITORY_ID);

    // --- 配额管理 ---

    ErrorCode ERR_QUOTA_FINALIZED = ErrorCode.define(
            "erp.err.crm.quota.finalized",
            "销售配额 {quotaId} 已定稿，不可修改（须先 unfinalizeQuota 解冻）",
            ARG_QUOTA_ID);

    ErrorCode ERR_QUOTA_NO_MATCH = ErrorCode.define(
            "erp.err.crm.quota.no-match",
            "未找到匹配的配额行（territoryId={territoryId}, periodType={periodType}, periodLabel={periodLabel}）",
            ARG_TERRITORY_ID, ARG_PERIOD_TYPE, ARG_PERIOD_LABEL);

    // --- CPQ（plan 2026-07-07-1430-2） ---

    ErrorCode ERR_CPQ_RULE_LIMIT_EXCEEDED = ErrorCode.define(
            "erp.err.crm.cpq.rule-limit-exceeded",
            "配置器 {configuratorId} 的规则数 {ruleCount} 超过上限 {maxRules}",
            ARG_CONFIGURATOR_ID, ARG_RULE_COUNT, ARG_MAX_RULES);

    ErrorCode ERR_CPQ_CONFIGURATOR_INACTIVE = ErrorCode.define(
            "erp.err.crm.cpq.configurator-inactive",
            "产品配置器 {configuratorId} 未启用或不在生效期间内",
            ARG_CONFIGURATOR_ID);

    ErrorCode ERR_CPQ_DISCOUNT_INCONSISTENT = ErrorCode.define(
            "erp.err.crm.cpq.discount-inconsistent",
            "折扣类型 {discountType} 与折扣值不一致（PERCENTAGE 须 0-100；FIXED 须非负）",
            ARG_DISCOUNT_TYPE);

    ErrorCode ERR_CPQ_QTY_RANGE_INVALID = ErrorCode.define(
            "erp.err.crm.cpq.qty-range-invalid",
            "数量区间非法（minQuantity={minQuantity} 大于 maxQuantity={maxQuantity}）",
            ARG_MIN_QTY, ARG_MAX_QTY);

    ErrorCode ERR_CPQ_EFFECTIVE_DATE_INVALID = ErrorCode.define(
            "erp.err.crm.cpq.effective-date-invalid",
            "生效期间非法（effectiveFrom={effectiveFrom} 晚于 effectiveTo={effectiveTo}）",
            ARG_EFFECTIVE_FROM, ARG_EFFECTIVE_TO);

    ErrorCode ERR_CPQ_NO_PRICE_MATCHED = ErrorCode.define(
            "erp.err.crm.cpq.no-price-matched",
            "未匹配到价格规则（productId={productId}, customerId={customerId}, quantity={quantity}）",
            ARG_PRODUCT_ID, ARG_CUSTOMER_ID, ARG_QUANTITY);

    // --- 序列管理（plan 2026-07-07-1430-3） ---

    ErrorCode ERR_SEQUENCE_NO_MATCH = ErrorCode.define(
            "erp.err.crm.sequence.no-match",
            "线索/商机 {leadId} 未匹配到任何启用的序列分配规则（也无 default 序列）",
            ARG_LEAD_ID);

    ErrorCode ERR_SEQUENCE_STEP_NOT_DUE = ErrorCode.define(
            "erp.err.crm.sequence.step-not-due",
            "序列进度 {progressId} 当前步骤 stepIndex={stepIndex} 未达完成条件（活动类型/事件状态不匹配）",
            ARG_PROGRESS_ID, ARG_STEP_INDEX);

    ErrorCode ERR_SEQUENCE_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.crm.sequence.illegal-status-transition",
            "序列进度 {progressId} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_PROGRESS_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_SEQUENCE_ALREADY_ASSIGNED = ErrorCode.define(
            "erp.err.crm.sequence.already-assigned",
            "线索/商机 {leadId} 已存在活跃序列进度（progressId={progressId}），须先 switchSequence 切换",
            ARG_LEAD_ID, ARG_PROGRESS_ID);

    ErrorCode ERR_FUNNEL_PERIOD_INVALID = ErrorCode.define(
            "erp.err.crm.funnel.period-invalid",
            "漏斗期间非法（periodStart={periodStart} 晚于 periodEnd={periodEnd}）",
            ARG_PERIOD_START, ARG_PERIOD_END);
}
