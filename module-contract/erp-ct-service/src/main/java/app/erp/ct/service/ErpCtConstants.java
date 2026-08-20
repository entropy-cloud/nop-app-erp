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

    // --- 消耗计费超量通知事件（RC-R1.33，P1-RC-075，UC-CT-04 异常路径，D5 契约） ---
    // 无 ACTIVE 模板时经 IErpSysNotificationBiz.notify best-effort 静默跳过（R1.4 范式）。
    String NOTIFY_EVENT_CONSUMPTION_OVER_120 = "ct.consumption-over-120-percent";

    // --- 审批状态（dict erp-ct/approval-status，RC-R1.34 UC-CT-07） ---
    String APPROVAL_STATUS_WAITING = "WAITING";
    String APPROVAL_STATUS_PENDING = "PENDING";
    String APPROVAL_STATUS_APPROVED = "APPROVED";
    String APPROVAL_STATUS_REJECTED = "REJECTED";
    String APPROVAL_STATUS_SKIPPED = "SKIPPED";

    // --- 审批工作流通知事件（RC-R1.34，UC-CT-06/07；无 ACTIVE 模板时 notify best-effort 静默跳过） ---
    /** 审批待办通知（新节点激活 / 终止申请发起，接收人 = 当前 PENDING 审批人）。 */
    String NOTIFY_EVENT_APPROVAL_TASK = "ct.approval-task";
    /** 审批驳回通知（接收人 = 合同经办人 createdBy）。 */
    String NOTIFY_EVENT_APPROVAL_REJECTED = "ct.approval-rejected";
    /** 驳回超限锁定强制升级通知（D3，接收人 = 合同经办人）。 */
    String NOTIFY_EVENT_APPROVAL_LOCKED = "ct.approval-locked";
    /** 72h 审批超时升级通知（D6 job，接收人 = 上一节点审批人或合同经办人）。 */
    String NOTIFY_EVENT_APPROVAL_TIMEOUT_ESCALATION = "ct.approval-timeout-escalation";
    /** 终止善后 TODO 通知（D5，接收人 = 合同经办人；TODO 语义由通知承载）。 */
    String NOTIFY_EVENT_TERMINATE_WINDDOWN = "ct.terminate-winddown";
    /** 终止申请驳回通知（接收人 = 合同经办人）。 */
    String NOTIFY_EVENT_TERMINATE_REJECTED = "ct.terminate-rejected";

    // --- 合同到期自动化通知事件（RC-R1.35，UC-CT-05；D1-D5 裁决见 plan 2026-08-15-1023-2 Phase 1） ---
    // 无 ACTIVE 模板时 notify best-effort 静默跳过（R1.4 范式）。
    /** 到期前 30 天提醒（接收人 = 合同经办人 createdBy）。 */
    String NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_30 = "ct.contract-expiry-warning-30";
    /** 到期前 15 天再次提醒「即将到期」（接收人 = 合同经办人 createdBy）。 */
    String NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_15 = "ct.contract-expiry-warning-15";
    /** 到期前 7 天升级通知经办人上级（D2：NopAuthUser.managerId → 兜底 NopAuthDept.managerId）。 */
    String NOTIFY_EVENT_CONTRACT_EXPIRY_ESCALATION_7 = "ct.contract-expiry-escalation-7";

    // --- 合同文档仓库（RC-R1.80，P1-RC-079，UC-CT-10；docs/design/contract/contract-repository.md） ---

    /** Legal Hold 设置角色（owner doc §合规规则「admin 手动设置」；roleId 与 nop_auth_role.csv 种子「admin」一致）。 */
    String LEGAL_HOLD_ROLE_ID = "admin";

    /** OCR 引擎编码（config erp-ct.ocr-engine 匹配值）：零依赖手动/无操作识别器（默认）。 */
    String OCR_ENGINE_MANUAL = "manual";

    /** 文档销毁审计通知事件（D4 逻辑删除销毁事件记录；无 ACTIVE 模板时 notify 静默跳过）。 */
    String NOTIFY_EVENT_DOCUMENT_PURGED = "ct.document-purged";
}
