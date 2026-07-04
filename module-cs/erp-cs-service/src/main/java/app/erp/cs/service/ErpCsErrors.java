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

    ErrorCode ERR_TICKET_NOT_FOUND = ErrorCode.define(
            "nop.err.cs.ticket.not-found",
            "客服工单不存在: {ticketId}",
            ARG_TICKET_ID);

    ErrorCode ERR_INVALID_TICKET_STATUS_TRANSITION = ErrorCode.define(
            "nop.err.cs.ticket.illegal-status-transition",
            "客服工单[{ticketCode}]当前状态[{currentStatus}]不允许此操作，期望[{expectedStatus}]",
            ARG_TICKET_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_TICKET_CLOSE_BREACHED_NO_REASON = ErrorCode.define(
            "nop.err.cs.ticket.close-breached-no-reason",
            "客服工单[{ticketCode}]SLA 已超时（isSlaCompleted=false），关闭前必须在 remark 中注明超时原因",
            ARG_TICKET_CODE);

    ErrorCode ERR_TICKET_ALREADY_TERMINAL = ErrorCode.define(
            "nop.err.cs.ticket.already-terminal",
            "客服工单[{ticketCode}]已处于终态[{currentStatus}]，禁止任何状态迁移",
            ARG_TICKET_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_SURVEY_ALREADY_EXISTS = ErrorCode.define(
            "nop.err.cs.survey.already-exists",
            "工单[{ticketId}]的调查已存在，禁止重复创建",
            ARG_TICKET_ID);

    ErrorCode ERR_SURVEY_TOKEN_INVALID = ErrorCode.define(
            "nop.err.cs.survey.token-invalid",
            "调查令牌无效或已过期: {surveyToken}",
            ARG_SURVEY_TOKEN);

    ErrorCode ERR_SURVEY_SCORE_OUT_OF_RANGE = ErrorCode.define(
            "nop.err.cs.survey.score-out-of-range",
            "调查评分字段[{field}]值[{value}]超出允许区间[{min}~{max}]",
            ARG_FIELD, ARG_VALUE, ARG_MIN, ARG_MAX);

    ErrorCode ERR_SURVEY_ALREADY_RESPONDED = ErrorCode.define(
            "nop.err.cs.survey.already-responded",
            "调查[{surveyId}]已被客户提交，禁止重复提交",
            ARG_SURVEY_ID);
}
