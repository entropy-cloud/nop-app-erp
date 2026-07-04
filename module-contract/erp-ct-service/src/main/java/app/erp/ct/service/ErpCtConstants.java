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
}
