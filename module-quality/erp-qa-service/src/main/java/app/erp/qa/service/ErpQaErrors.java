package app.erp.qa.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 质量域错误码。描述为中文，框架经 i18n 翻译；公共/GraphQL 面向错误统一 {@link ErrorCode} + {@code NopException}。
 *
 * <p>权威：{@code docs/design/quality/state-machine.md}、
 * {@code docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md}。
 */
public interface ErpQaErrors {

    String ARG_INSPECTION_ID = "inspectionId";
    String ARG_INSPECTION_CODE = "inspectionCode";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_NCR_ID = "ncrId";
    String ARG_NCR_CODE = "ncrCode";
    String ARG_ACTION_ID = "actionId";
    String ARG_BILL_TYPE = "billType";
    String ARG_BILL_CODE = "billCode";
    String ARG_MATERIAL_ID = "materialId";
    String ARG_RECALL_ID = "recallId";
    String ARG_RECALL_CODE = "recallCode";
    String ARG_BATCH_NO = "batchNo";
    String ARG_DISPOSITION_TYPE = "dispositionType";

    // --- 报表渲染作用域参数键 ---
    String ARG_REPORT_NAME = "reportName";
    String ARG_RENDER_TYPE = "renderType";

    ErrorCode ERR_INSPECTION_NOT_FOUND = ErrorCode.define(
            "erp.err.qa.inspection.not-found",
            "质检单不存在: {inspectionId}",
            ARG_INSPECTION_ID);

    ErrorCode ERR_INVALID_INSPECTION_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.qa.inspection.illegal-status-transition",
            "质检单[{inspectionCode}]当前结果[{currentStatus}]不允许此操作，期望[{expectedStatus}]",
            ARG_INSPECTION_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_INSPECTION_LINES_EMPTY = ErrorCode.define(
            "erp.err.qa.inspection.lines-empty",
            "质检单[{inspectionCode}]无质检行，无法汇总结果，请先补录检验项",
            ARG_INSPECTION_CODE);

    ErrorCode ERR_INSPECTION_MANDATORY_BLOCKED = ErrorCode.define(
            "erp.err.qa.inspection.mandatory-blocked",
            "业务单据[{billType}:{billCode}]属强制质检类型，关联质检单未得出合格/让步结论，禁止流转",
            ARG_BILL_TYPE, ARG_BILL_CODE);

    ErrorCode ERR_NCR_NOT_FOUND = ErrorCode.define(
            "erp.err.qa.ncr.not-found",
            "不符合项报告不存在: {ncrId}",
            ARG_NCR_ID);

    ErrorCode ERR_INVALID_NCR_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.qa.ncr.illegal-status-transition",
            "NCR[{ncrCode}]当前状态[{currentStatus}]不允许此操作，期望[{expectedStatus}]",
            ARG_NCR_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED = ErrorCode.define(
            "erp.err.qa.ncr.resolve-capa-not-completed",
            "NCR[{ncrCode}]存在未完成（含 PENDING/IN_PROGRESS）或未验证（缺验证人/验证日期）的 CAPA 措施，禁止解决",
            ARG_NCR_CODE);

    ErrorCode ERR_ACTION_NOT_FOUND = ErrorCode.define(
            "erp.err.qa.action.not-found",
            "纠正措施不存在: {actionId}",
            ARG_ACTION_ID);

    ErrorCode ERR_INVALID_ACTION_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.qa.action.illegal-status-transition",
            "纠正措施[{actionId}]当前状态[{currentStatus}]不允许此操作，期望[{expectedStatus}]",
            ARG_ACTION_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_ACTION_VERIFY_REQUIRES_COMPLETED = ErrorCode.define(
            "erp.err.qa.action.verify-requires-completed",
            "纠正措施[{actionId}]须先完成（COMPLETED）才能录入效果验证",
            ARG_ACTION_ID);

    ErrorCode ERR_ACTION_VERIFY_MISSING_FIELDS = ErrorCode.define(
            "erp.err.qa.action.verify-missing-fields",
            "纠正措施[{actionId}]效果验证须同时填写验证人与验证日期",
            ARG_ACTION_ID);

    // ---- 召回事件（2.11）----

    ErrorCode ERR_RECALL_NOT_FOUND = ErrorCode.define(
            "erp.err.qa.recall.not-found",
            "召回事件不存在: {recallId}",
            ARG_RECALL_ID);

    ErrorCode ERR_INVALID_RECALL_STATUS_TRANSITION = ErrorCode.define(
            "erp.err.qa.recall.illegal-status-transition",
            "召回事件[{recallCode}]当前状态[{currentStatus}]不允许此操作，期望[{expectedStatus}]",
            ARG_RECALL_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_RECALL_APPROVAL_REQUIRED = ErrorCode.define(
            "erp.err.qa.recall.approval-required",
            "召回事件[{recallCode}]配置强制审批（erp-qua.recall-require-approval=true），须先 submit 再 approve",
            ARG_RECALL_CODE);

    ErrorCode ERR_TRACE_CHAIN_DISABLED = ErrorCode.define(
            "erp.err.qa.recall.trace-chain-disabled",
            "库存追溯链未启用（erp-inv.trace-chain-enabled=false），召回目标定位不可用",
            ARG_RECALL_CODE, ARG_BATCH_NO);

    ErrorCode ERR_RECALL_LOCATE_NO_BATCH = ErrorCode.define(
            "erp.err.qa.recall.locate-no-batch",
            "召回事件[{recallCode}]未指定召回批次（batchId/batchNo），无法定位销售出库目标",
            ARG_RECALL_CODE);

    ErrorCode ERR_RECALL_NOTIFY_INCOMPLETE = ErrorCode.define(
            "erp.err.qa.recall.notify-incomplete",
            "召回事件[{recallCode}]存在未通知目标或未标记客户通知，禁止关闭（erp-qua.recall-notify-required-to-close=true）",
            ARG_RECALL_CODE);

    // ---- NCR 财务过账（plan 2026-07-05-2352-2）----

    ErrorCode ERR_NCR_ALREADY_POSTED = ErrorCode.define(
            "erp.err.qa.ncr.already-posted",
            "NCR[{ncrCode}]已过账，禁止重复过账",
            ARG_NCR_CODE);

    ErrorCode ERR_NCR_NOT_RESOLVED = ErrorCode.define(
            "erp.err.qa.ncr.not-resolved",
            "NCR[{ncrCode}]当前状态[{currentStatus}]未解决（RESOLVED），禁止过账",
            ARG_NCR_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_NCR_NOT_POSTED = ErrorCode.define(
            "erp.err.qa.ncr.not-posted",
            "NCR[{ncrCode}]未过账，禁止红冲",
            ARG_NCR_CODE);

    ErrorCode ERR_NCR_NO_QUANTITY = ErrorCode.define(
            "erp.err.qa.ncr.no-quantity",
            "NCR[{ncrCode}]不合格数量为空或为零，无法过账",
            ARG_NCR_CODE);

    ErrorCode ERR_NCR_DISPOSITION_NOT_POSTABLE = ErrorCode.define(
            "erp.err.qa.ncr.disposition-not-postable",
            "NCR[{ncrCode}]处置方式[{dispositionType}]无财务影响（CONCESSION/DOWNGRADE），不需过账",
            ARG_NCR_CODE, ARG_DISPOSITION_TYPE);

    // --- 报表渲染作用域（镜像 ErpMfgErrors.ERR_REPORT_*，质量域独立错误码，不跨域 import） ---

    ErrorCode ERR_REPORT_NAME_INVALID = ErrorCode.define(
            "erp.err.qa.report.name-invalid",
            "报表名[{reportName}]非法（含路径注入字符或不合规段），拒绝渲染",
            ARG_REPORT_NAME);

    ErrorCode ERR_REPORT_RENDER_TYPE_INVALID = ErrorCode.define(
            "erp.err.qa.report.render-type-invalid",
            "渲染类型[{renderType}]非法（仅允许 html/xlsx/pdf）",
            ARG_RENDER_TYPE);

    // ---- SPC 统计过程控制（2.4b，spc.md）----

    String ARG_CHART_ID = "chartId";
    String ARG_CHART_CODE = "chartCode";
    String ARG_SUBGROUP_NO = "subgroupNo";
    String ARG_PARAMETER_ID = "parameterId";

    ErrorCode ERR_QA_SPC_CHART_NOT_FOUND = ErrorCode.define(
            "erp.err.qa.spc.chart-not-found",
            "SPC 控制图不存在: {chartId}",
            ARG_CHART_ID);

    ErrorCode ERR_QA_SPC_PARAMETER_NOT_FOUND = ErrorCode.define(
            "erp.err.qa.spc.parameter-not-found",
            "SPC 控制图[{chartCode}]未关联关键检验参数(parameterId)，无法采样",
            ARG_CHART_CODE, ARG_PARAMETER_ID);

    ErrorCode ERR_QA_SPC_INSUFFICIENT_SAMPLES = ErrorCode.define(
            "erp.err.qa.spc.insufficient-samples",
            "SPC 控制图[{chartCode}]子组样本量 subgroupSize={subgroupNo}<2，无法计算控制限",
            ARG_CHART_CODE, ARG_SUBGROUP_NO);

    ErrorCode ERR_QA_SPC_MEASURED_VALUE_INVALID = ErrorCode.define(
            "erp.err.qa.spc.measured-value-invalid",
            "SPC 采样：实测值非数值或为空，跳过该样本点（chartId={chartId}, parameterId={parameterId}）",
            ARG_CHART_ID, ARG_PARAMETER_ID);

    ErrorCode ERR_QA_SPC_SUBGROUP_SIZE_INVALID = ErrorCode.define(
            "erp.err.qa.spc.subgroup-size-invalid",
            "SPC 控制图[{chartCode}]subgroupSize<2，无法聚合子组",
            ARG_CHART_CODE);
}
