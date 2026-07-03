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
}
