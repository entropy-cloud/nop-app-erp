package app.erp.ct.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 合同域业务异常错误码。所有合同流程中的业务异常使用
 * {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 *
 * <p>状态字典权威定义见 {@code module-contract/model/app-erp-contract.orm.xml}
 *（dict: erp-ct/contract-status、erp-ct/version-status、erp-ct/settlement-status、
 * erp-ct/rebate-agreement-status）。
 */
public interface ErpCtErrors {

    String ARG_CONTRACT_ID = "contractId";
    String ARG_CONTRACT_CODE = "contractCode";
    String ARG_VERSION_NO = "versionNo";
    String ARG_INVOICE_PLAN_ID = "invoicePlanId";
    String ARG_CONTRACT_LINE_ID = "contractLineId";
    String ARG_REBATE_AGREEMENT_ID = "rebateAgreementId";
    String ARG_SETTLEMENT_ID = "settlementId";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_FROM_QTY = "fromQty";
    String ARG_TO_QTY = "toQty";
    String ARG_INVOICE_TERM = "invoiceTerm";
    String ARG_PLAN_DATE = "planDate";
    String ARG_FROM_DATE = "fromDate";
    String ARG_TO_DATE = "toDate";

    // --- 电子签章（plan 2026-07-04-2200-2，dict erp-ct/sign-status / erp-ct/sign-provider） ---

    String ARG_PROVIDER_CODE = "providerCode";
    String ARG_SIGNATURE_REQUEST_ID = "signatureRequestId";
    String ARG_VERSION_ID = "versionId";
    String ARG_EVENT_ID = "eventId";

    // --- 审批工作流（RC-R1.34，UC-CT-06/07） ---

    String ARG_APPROVAL_RECORD_ID = "recordId";
    String ARG_APPROVAL_ORDER = "approvalOrder";
    String ARG_MAX_RETRIES = "maxRetries";
    String ARG_APPROVER_ID = "approverId";
    String ARG_USER_ID = "userId";

    // --- 合同头状态机（dict erp-ct/contract-status） ---

    ErrorCode ERR_CT_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.ct.illegal-status-transition",
            "合同 {contractCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_CONTRACT_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_CT_CONTRACT_NOT_ACTIVE = ErrorCode.define("erp.err.ct.contract-not-active",
            "合同 {contractCode} 非执行中（当前状态={currentStatus}），开票计划仅可由 ACTIVE 合同触发",
            ARG_CONTRACT_CODE, ARG_CURRENT_STATUS);

    ErrorCode ERR_CT_CONTRACT_SUSPENDED = ErrorCode.define("erp.err.ct.contract-suspended",
            "合同 {contractCode} 已中止，期间不可触发生成新发票",
            ARG_CONTRACT_CODE);

    // --- 创建校验（RC-R1.32，UC-CT-01 跨字段校验） ---

    ErrorCode ERR_CT_AMOUNT_MISMATCH = ErrorCode.define("erp.err.ct.amount-mismatch",
            "合同 {contractCode} 总金额 {totalAmount} 与行金额合计 {sumLineAmount} 不一致",
            ARG_CONTRACT_CODE, "totalAmount", "sumLineAmount");

    ErrorCode ERR_CT_DATE_RANGE_INVALID = ErrorCode.define("erp.err.ct.date-range-invalid",
            "合同 {contractCode} 生效日期 {startDate} 必须早于到期日期 {endDate}",
            ARG_CONTRACT_CODE, "startDate", "endDate");

    // --- 版本管理（dict erp-ct/version-status） ---

    ErrorCode ERR_CT_VERSION_NOT_CURRENT = ErrorCode.define("erp.err.ct.version-not-current",
            "合同 {contractCode} 版本 {versionNo} 非当前版本，仅当前版本可签署/定稿",
            ARG_CONTRACT_CODE, ARG_VERSION_NO);

    // --- InvoicePlan 触发 ---

    ErrorCode ERR_CT_INVOICE_PLAN_ALREADY_INVOICED = ErrorCode.define("erp.err.ct.invoice-plan-already-invoiced",
            "开票计划 {invoicePlanId} 已生成发票，不可重复触发",
            ARG_INVOICE_PLAN_ID);

    // --- 开票计划批量生成 + 已开票锁（RC-R1.33，P1-RC-074，UC-CT-03） ---

    ErrorCode ERR_CT_INVOICE_PLAN_INVOICED_IMMUTABLE = ErrorCode.define("erp.err.ct.invoice-plan-invoiced-immutable",
            "开票计划 {invoicePlanId} 已开票，金额/计划开票日期/开票条款不可修改",
            ARG_INVOICE_PLAN_ID);

    ErrorCode ERR_CT_INVOICE_PLAN_DUPLICATE = ErrorCode.define("erp.err.ct.invoice-plan-duplicate",
            "开票计划重复生成：合同行 {contractLineId} 条款 {invoiceTerm} 计划日期 {planDate} 已存在",
            ARG_CONTRACT_LINE_ID, ARG_INVOICE_TERM, ARG_PLAN_DATE);

    ErrorCode ERR_CT_INVOICE_PLAN_LINE_NOT_IN_CONTRACT = ErrorCode.define("erp.err.ct.invoice-plan-line-not-in-contract",
            "合同行 {contractLineId} 不属于合同 {contractId}",
            ARG_CONTRACT_LINE_ID, ARG_CONTRACT_ID);

    // --- 消耗计费周期汇总（RC-R1.33，P1-RC-075，UC-CT-04） ---

    ErrorCode ERR_CT_CONSUMPTION_LINE_NOT_FOUND = ErrorCode.define("erp.err.ct.consumption-line-not-found",
            "合同行 {contractLineId} 不存在，无法汇总消耗计费",
            ARG_CONTRACT_LINE_ID);

    ErrorCode ERR_CT_CONSUMPTION_DATE_RANGE_INVALID = ErrorCode.define("erp.err.ct.consumption-date-range-invalid",
            "消耗汇总期间非法：起始日期 {fromDate} 必须不晚于截止日期 {toDate}",
            ARG_FROM_DATE, ARG_TO_DATE);

    // --- VolumeDiscount 区间带（docs/design/contract/volume-discount.md §ErpCtVolumeDiscount） ---

    ErrorCode ERR_CT_DISCOUNT_BAND_OVERLAP = ErrorCode.define("erp.err.ct.discount-band-overlap",
            "合同行 {contractLineId} 批量折扣区间 [{fromQty}, {toQty}) 与既有区间重叠",
            ARG_CONTRACT_LINE_ID, ARG_FROM_QTY, ARG_TO_QTY);

    // --- 返利（dict erp-ct/rebate-agreement-status / erp-ct/settlement-status） ---

    ErrorCode ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE = ErrorCode.define("erp.err.ct.rebate-agreement-not-active",
            "返利协议 {rebateAgreementId} 非生效中（当前状态={currentStatus}），不可计提",
            ARG_REBATE_AGREEMENT_ID, ARG_CURRENT_STATUS);

    ErrorCode ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION = ErrorCode.define("erp.err.ct.settlement-illegal-transition",
            "返利结算单 {settlementId} 当前状态={currentStatus}，不允许过账（仅 DRAFT 可过账）",
            ARG_SETTLEMENT_ID, ARG_CURRENT_STATUS);

    // --- 电子签章（plan 2026-07-04-2200-2，design e-signature.md） ---

    ErrorCode ERR_CT_SIGNATURE_PROVIDER_NOT_REGISTERED = ErrorCode.define("erp.err.ct.signature-provider-not-registered",
            "签章提供商 {providerCode} 未注册（无对应 IErpCtSignatureProvider Bean）",
            ARG_PROVIDER_CODE);

    ErrorCode ERR_CT_SIGNATURE_ILLEGAL_TRANSITION = ErrorCode.define("erp.err.ct.signature-illegal-transition",
            "签章请求 {signatureRequestId} 当前状态={currentStatus}，不允许该操作（期望状态={expectedStatus}）",
            ARG_SIGNATURE_REQUEST_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_CT_SIGNATURE_VERSION_NOT_FINALIZED = ErrorCode.define("erp.err.ct.signature-version-not-finalized",
            "合同版本 {versionId} 非定稿状态（仅 FINALIZED 版本可发起电子签章）",
            ARG_VERSION_ID);

    ErrorCode ERR_CT_SIGNATURE_ALREADY_COMPLETED = ErrorCode.define("erp.err.ct.signature-already-completed",
            "签章请求 {signatureRequestId} 已完成签署，不可重复完成",
            ARG_SIGNATURE_REQUEST_ID);

    ErrorCode ERR_CT_SIGNATURE_CALLBACK_SIGNATURE_INVALID = ErrorCode.define("erp.err.ct.signature-callback-signature-invalid",
            "签章回调签名校验失败（providerCode={providerCode}）",
            ARG_PROVIDER_CODE);

    ErrorCode ERR_CT_SIGNATURE_CALLBACK_DUPLICATE_EVENT = ErrorCode.define("erp.err.ct.signature-callback-duplicate-event",
            "签章回调事件 {eventId} 已处理（幂等拒绝，providerCode={providerCode}）",
            ARG_EVENT_ID, ARG_PROVIDER_CODE);

    ErrorCode ERR_CT_SIGNATURE_INIT_FAILED = ErrorCode.define("erp.err.ct.signature-init-failed",
            "签章请求初始化失败（providerCode={providerCode}）：{errorMsg}",
            ARG_PROVIDER_CODE, "errorMsg");

    // --- 审批工作流（RC-R1.34，UC-CT-07 ApprovalWorkflowEngine） ---

    ErrorCode ERR_CT_APPROVAL_RECORD_NOT_FOUND = ErrorCode.define("erp.err.ct.approval-record-not-found",
            "审批记录 {recordId} 不存在",
            ARG_APPROVAL_RECORD_ID);

    ErrorCode ERR_CT_APPROVAL_ILLEGAL_STATUS = ErrorCode.define("erp.err.ct.approval-illegal-status",
            "审批记录 {recordId} 当前状态={currentStatus}，不允许该操作（期望状态={expectedStatus}）",
            ARG_APPROVAL_RECORD_ID, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_CT_APPROVAL_APPROVER_MISMATCH = ErrorCode.define("erp.err.ct.approval-approver-mismatch",
            "审批记录 {recordId} 审批人为 {approverId}，当前用户 {userId} 无权操作",
            ARG_APPROVAL_RECORD_ID, ARG_APPROVER_ID, ARG_USER_ID);

    ErrorCode ERR_CT_APPROVAL_LOCKED = ErrorCode.define("erp.err.ct.approval-locked",
            "合同 {contractId} 审批节点 {approvalOrder} 驳回次数已达上限 {maxRetries}，已锁定需强制升级",
            ARG_CONTRACT_ID, ARG_APPROVAL_ORDER, ARG_MAX_RETRIES);

    ErrorCode ERR_CT_APPROVAL_NOT_COMPLETE = ErrorCode.define("erp.err.ct.approval-not-complete",
            "合同 {contractCode} 审批链未全部通过，不可激活",
            ARG_CONTRACT_CODE);

    ErrorCode ERR_CT_APPROVAL_NO_REJECTED = ErrorCode.define("erp.err.ct.approval-no-rejected",
            "合同 {contractId} 无被驳回的审批节点可重新提交",
            ARG_CONTRACT_ID);

    ErrorCode ERR_CT_TERMINATE_ALREADY_PENDING = ErrorCode.define("erp.err.ct.terminate-already-pending",
            "合同 {contractCode} 已有待法务审批的终止申请，不可重复发起",
            ARG_CONTRACT_CODE);
}
