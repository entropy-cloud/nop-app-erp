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

    // --- 版本管理（dict erp-ct/version-status） ---

    ErrorCode ERR_CT_VERSION_NOT_CURRENT = ErrorCode.define("erp.err.ct.version-not-current",
            "合同 {contractCode} 版本 {versionNo} 非当前版本，仅当前版本可签署/定稿",
            ARG_CONTRACT_CODE, ARG_VERSION_NO);

    // --- InvoicePlan 触发 ---

    ErrorCode ERR_CT_INVOICE_PLAN_ALREADY_INVOICED = ErrorCode.define("erp.err.ct.invoice-plan-already-invoiced",
            "开票计划 {invoicePlanId} 已生成发票，不可重复触发",
            ARG_INVOICE_PLAN_ID);

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
}
