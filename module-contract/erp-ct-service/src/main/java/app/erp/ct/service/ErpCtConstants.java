package app.erp.ct.service;

/**
 * 合同域常量。状态码权威定义见 {@code module-contract/model/app-erp-contract.orm.xml}
 *（dict: erp-ct/contract-status、erp-ct/version-status、erp-ct/settlement-status、
 * erp-ct/rebate-agreement-status、erp-ct/accrual-method、erp-ct/rebate-type、erp-ct/contract-direction、erp-ct/contract-type）。
 */
public interface ErpCtConstants {

    // --- 合同状态（dict erp-ct/contract-status） ---
    String CONTRACT_STATUS_DRAFT = "DRAFT";
    String CONTRACT_STATUS_NEGOTIATION = "NEGOTIATION";
    String CONTRACT_STATUS_ACTIVE = "ACTIVE";
    String CONTRACT_STATUS_SUSPENDED = "SUSPENDED";
    String CONTRACT_STATUS_EXPIRED = "EXPIRED";
    String CONTRACT_STATUS_TERMINATED = "TERMINATED";

    // --- 版本状态（dict erp-ct/version-status） ---
    String VERSION_STATUS_DRAFT = "DRAFT";
    String VERSION_STATUS_FINALIZED = "FINALIZED";
    String VERSION_STATUS_SIGNED = "SIGNED";

    // --- 结算单状态（dict erp-ct/settlement-status） ---
    String SETTLEMENT_STATUS_DRAFT = "DRAFT";
    String SETTLEMENT_STATUS_POSTED = "POSTED";
    String SETTLEMENT_STATUS_CANCELLED = "CANCELLED";

    // --- 返利协议状态（dict erp-ct/rebate-agreement-status） ---
    String REBATE_AGREEMENT_STATUS_DRAFT = "DRAFT";
    String REBATE_AGREEMENT_STATUS_ACTIVE = "ACTIVE";
    String REBATE_AGREEMENT_STATUS_EXPIRED = "EXPIRED";
    String REBATE_AGREEMENT_STATUS_SETTLED = "SETTLED";

    // --- 计提方法（dict erp-ct/accrual-method） ---
    String ACCRUAL_METHOD_PERIOD_END = "PERIOD_END";
    String ACCRUAL_METHOD_PROGRESSIVE = "PROGRESSIVE";

    // --- 返利类型（dict erp-ct/rebate-type） ---
    String REBATE_TYPE_PURCHASE = "PURCHASE";
    String REBATE_TYPE_SALES = "SALES";

    // --- 合同类型/方向（dict erp-ct/contract-type / erp-ct/contract-direction） ---
    String CONTRACT_TYPE_PURCHASE = "PURCHASE";
    String CONTRACT_TYPE_SALES = "SALES";
    String CONTRACT_DIRECTION_INBOUND = "INBOUND";
    String CONTRACT_DIRECTION_OUTBOUND = "OUTBOUND";

    // --- 电子签章状态（dict erp-ct/sign-status，plan 2026-07-04-2200-2） ---
    String SIGNATURE_STATUS_PENDING = "PENDING_SIGNATURE";
    String SIGNATURE_STATUS_PARTIALLY = "PARTIALLY_SIGNED";
    String SIGNATURE_STATUS_FULLY = "FULLY_SIGNED";
    String SIGNATURE_STATUS_REJECTED = "REJECTED";
    String SIGNATURE_STATUS_EXPIRED = "EXPIRED";
    String SIGNATURE_STATUS_CANCELLED = "CANCELLED";

    // --- 电子签章提供商（dict erp-ct/sign-provider） ---
    String SIGNATURE_PROVIDER_MOCK = "MOCK";

    // --- 签署 webhook 回调事件（design e-signature.md §签署回调 Webhook） ---
    String SIGNATURE_EVENT_SIGNER_SIGNED = "signer.signed";
    String SIGNATURE_EVENT_COMPLETED = "signing.completed";
    /** design webhook 表同时列了 declined/rejected；本期按权威状态机 6 态收敛 declined→REJECTED。 */
    String SIGNATURE_EVENT_REJECTED = "signing.rejected";
    String SIGNATURE_EVENT_DECLINED = "signing.declined";
    String SIGNATURE_EVENT_EXPIRED = "signing.expired";

    /** 签署顺序：顺序 / 并行（SignatureInitRequest.signingOrder）。 */
    String SIGNING_ORDER_SEQUENTIAL = "SEQUENTIAL";

    // ---- 跨域镜像：发票草稿初始审核状态（对齐 ErpPurConstants/ErpSalConstants.APPROVE_STATUS_UNSUBMITTED）----
    // 合同域触发生成的 AP/AR 发票草稿为新建实体初始化，非用户审批状态迁移。
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
}
