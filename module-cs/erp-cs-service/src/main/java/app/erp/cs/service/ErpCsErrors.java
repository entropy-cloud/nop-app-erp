package app.erp.cs.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 客服域错误码。描述为中文，框架经 i18n 翻译；公共/GraphQL 面向错误统一 {@link ErrorCode} + {@code NopException}。
 *
 * <p>权威：{@code docs/design/customer-service/state-machine.md}、
 * {@code docs/design/customer-service/sla.md}、{@code docs/design/customer-service/csat.md}、
 * {@code docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md}。
 */
public interface ErpCsErrors {

    String ARG_TICKET_ID = "ticketId";
    String ARG_TICKET_CODE = "ticketCode";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_SURVEY_ID = "surveyId";
    String ARG_SURVEY_TOKEN = "surveyToken";
    String ARG_FIELD = "field";
    String ARG_VALUE = "value";
    String ARG_MIN = "min";
    String ARG_MAX = "max";

    // --- 权益/目录参数键（plan 2026-07-07-1430-1） ---
    String ARG_ENTITLEMENT_ID = "entitlementId";
    String ARG_ENTITLEMENT_CODE = "entitlementCode";
    String ARG_PARTNER_ID = "partnerId";
    String ARG_USED_TICKETS = "usedTickets";
    String ARG_MAX_TICKETS = "maxTickets";
    String ARG_CATEGORY_ID = "categoryId";
    String ARG_CATEGORY_NAME = "categoryName";
    String ARG_CURRENT_DEPTH = "currentDepth";
    String ARG_MAX_DEPTH = "maxDepth";
    String ARG_CATALOG_ITEM_ID = "catalogItemId";
    String ARG_CATALOG_ITEM_NAME = "catalogItemName";

    // --- 报表渲染作用域参数键 ---
    String ARG_REPORT_NAME = "reportName";
    String ARG_RENDER_TYPE = "renderType";

    // --- 知识库搜索参数键（plan 2026-07-08-0056-2） ---
    String ARG_KEYWORD = "keyword";
    String ARG_LIMIT = "limit";
    String ARG_MAX_LIMIT = "maxLimit";

    // --- 预设应答参数键（plan 2026-07-11-1234-2） ---
    String ARG_CANNED_RESPONSE_ID = "cannedResponseId";
    String ARG_VARIABLE_KEY = "variableKey";

    ErrorCode ERR_TICKET_NOT_FOUND = ErrorCode.define(
            "erp.err.cs.ticket.not-found",
            "客服工单不存在: {ticketId}",
            ARG_TICKET_ID);

    ErrorCode ERR_INVALID_TICKET_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.cs.ticket.illegal-status-transition",
            "客服工单[{ticketCode}]当前状态[{currentStatus}]不允许此操作，期望[{expectedStatus}]",
            ARG_TICKET_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_TICKET_CLOSE_BREACHED_NO_REASON = ErrorCode.define(
            "erp.err.cs.ticket.close-breached-no-reason",
            "客服工单[{ticketCode}]SLA 已超时（isSlaCompleted=false），关闭前必须在 remark 中注明超时原因",
            ARG_TICKET_CODE);

    ErrorCode ERR_TICKET_ALREADY_TERMINAL = ErrorCode.define(
            "erp.err.cs.ticket.already-terminal",
            "客服工单[{ticketCode}]已处于终态[{currentStatus}]，禁止任何状态迁移",
            ARG_TICKET_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_SURVEY_ALREADY_EXISTS = ErrorCode.define(
            "erp.err.cs.survey.already-exists",
            "工单[{ticketId}]的调查已存在，禁止重复创建",
            ARG_TICKET_ID);

    ErrorCode ERR_SURVEY_TOKEN_INVALID = ErrorCode.define(
            "erp.err.cs.survey.token-invalid",
            "调查令牌无效或已过期: {surveyToken}",
            ARG_SURVEY_TOKEN);

    ErrorCode ERR_SURVEY_SCORE_OUT_OF_RANGE = ErrorCode.define(
            "erp.err.cs.survey.score-out-of-range",
            "调查评分字段[{field}]值[{value}]超出允许区间[{min}~{max}]",
            ARG_FIELD, ARG_VALUE, ARG_MIN, ARG_MAX);

    ErrorCode ERR_SURVEY_ALREADY_RESPONDED = ErrorCode.define(
            "erp.err.cs.survey.already-responded",
            "调查[{surveyId}]已被客户提交，禁止重复提交",
            ARG_SURVEY_ID);

    // --- 报表渲染作用域（镜像 ErpMfgErrors.ERR_REPORT_*，客服域独立错误码，不跨域 import） ---

    ErrorCode ERR_REPORT_NAME_INVALID = ErrorCode.define(
            "erp.err.cs.report.name-invalid",
            "报表名[{reportName}]非法（含路径注入字符或不合规段），拒绝渲染",
            ARG_REPORT_NAME);

    ErrorCode ERR_REPORT_RENDER_TYPE_INVALID = ErrorCode.define(
            "erp.err.cs.report.render-type-invalid",
            "渲染类型[{renderType}]非法（仅允许 html/xlsx/pdf）",
            ARG_RENDER_TYPE);

    // --- 权益/目录错误码（plan 2026-07-07-1430-1 §Phase 1/2） ---

    ErrorCode ERR_ENTITLEMENT_EXHAUSTED = ErrorCode.define(
            "erp.err.cs.entitlement.exhausted",
            "服务权益[{entitlementCode}]按次计费余量已耗尽（已用 {usedTickets}/{maxTickets}），禁止新建工单",
            ARG_ENTITLEMENT_CODE, ARG_USED_TICKETS, ARG_MAX_TICKETS);

    ErrorCode ERR_ENTITLEMENT_NONE_ACTIVE = ErrorCode.define(
            "erp.err.cs.entitlement.none-active",
            "客户[{partnerId}]无有效服务权益，且 erp-cs.entitlement-allow-no-entitlement=false，禁止新建工单",
            ARG_PARTNER_ID);

    ErrorCode ERR_ENTITLEMENT_EXPIRED = ErrorCode.define(
            "erp.err.cs.entitlement.expired",
            "服务权益[{entitlementCode}]已过期或未激活，禁止扣减",
            ARG_ENTITLEMENT_CODE);

    ErrorCode ERR_ENTITLEMENT_NOT_FOUND = ErrorCode.define(
            "erp.err.cs.entitlement.not-found",
            "服务权益不存在: {entitlementId}",
            ARG_ENTITLEMENT_ID);

    ErrorCode ERR_CATALOG_CATEGORY_MAX_DEPTH_EXCEEDED = ErrorCode.define(
            "erp.err.cs.catalog-category.max-depth-exceeded",
            "目录分类[{categoryName}]层级深度[{currentDepth}]超过最大深度[{maxDepth}]",
            ARG_CATEGORY_NAME, ARG_CURRENT_DEPTH, ARG_MAX_DEPTH);

    ErrorCode ERR_CATALOG_CATEGORY_CYCLE = ErrorCode.define(
            "erp.err.cs.catalog-category.cycle",
            "目录分类[{categoryId}]设置 parentId 将形成环路，禁止操作",
            ARG_CATEGORY_ID);

    ErrorCode ERR_CATALOG_CATEGORY_HAS_CHILDREN = ErrorCode.define(
            "erp.err.cs.catalog-category.has-children",
            "目录分类[{categoryId}]存在子节点，禁止删除",
            ARG_CATEGORY_ID);

    ErrorCode ERR_CATALOG_ITEM_INACTIVE = ErrorCode.define(
            "erp.err.cs.catalog-item.inactive",
            "服务目录项[{catalogItemName}]未上架（isActive=false），禁止建单",
            ARG_CATALOG_ITEM_NAME);

    ErrorCode ERR_CATALOG_ITEM_NOT_FOUND = ErrorCode.define(
            "erp.err.cs.catalog-item.not-found",
            "服务目录项不存在: {catalogItemId}",
            ARG_CATALOG_ITEM_ID);

    // --- 知识库搜索错误码（plan 2026-07-08-0056-2） ---

    ErrorCode ERR_KNOWLEDGE_SEARCH_KEYWORD_TOO_LONG = ErrorCode.define(
            "erp.err.cs.knowledge-search.keyword-too-long",
            "知识库搜索关键词过长（最大 {maxLimit} 字符）",
            ARG_KEYWORD, ARG_MAX_LIMIT);

    ErrorCode ERR_KNOWLEDGE_SEARCH_LIMIT_EXCEEDED = ErrorCode.define(
            "erp.err.cs.knowledge-search.limit-exceeded",
            "知识库搜索 limit[{limit}]超过最大值[{maxLimit}]",
            ARG_LIMIT, ARG_MAX_LIMIT);

    // --- 预设应答错误码（plan 2026-07-11-1234-2 §Phase 1） ---

    ErrorCode ERR_CANNED_RESPONSE_NOT_FOUND = ErrorCode.define(
            "erp.err.cs.canned-response.not-found",
            "预设应答不存在: {cannedResponseId}",
            ARG_CANNED_RESPONSE_ID);

    ErrorCode ERR_CANNED_RESPONSE_INACTIVE = ErrorCode.define(
            "erp.err.cs.canned-response.inactive",
            "预设应答[{cannedResponseId}]未启用（isActive=false），禁止渲染/插入",
            ARG_CANNED_RESPONSE_ID);

    ErrorCode ERR_CANNED_RESPONSE_REQUIRED_VAR_MISSING = ErrorCode.define(
            "erp.err.cs.canned-response.required-var-missing",
            "预设应答必填变量[{variableKey}]缺失",
            ARG_VARIABLE_KEY);
}
