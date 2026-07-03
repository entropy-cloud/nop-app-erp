package app.erp.qa.service;

/**
 * 质量域常量。字典码值权威：{@code module-quality/model/app-erp-quality.orm.xml}。
 *
 * <p>权威：{@code docs/design/quality/state-machine.md}、
 * {@code docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md}。
 */
public interface ErpQaConstants {

    // 检验类型（erp-qa/inspection-type）
    int INSPECTION_TYPE_INCOMING = 10;
    int INSPECTION_TYPE_IN_PROCESS = 20;
    int INSPECTION_TYPE_FINAL = 30;
    int INSPECTION_TYPE_OUTGOING = 40;

    // 质检单结果（erp-qa/inspection-result，质检单 4 态；权威：state-machine.md §适用对象一）
    int INSPECTION_RESULT_PENDING = 10;
    int INSPECTION_RESULT_ACCEPTED = 20;
    int INSPECTION_RESULT_CONDITIONAL = 30;
    int INSPECTION_RESULT_REJECTED = 40;

    // NCR 状态（erp-qa/ncr-status，5 态；权威：state-machine.md §适用对象二）
    int NCR_STATUS_OPEN = 10;
    int NCR_STATUS_IN_REVIEW = 20;
    int NCR_STATUS_RESOLVED = 30;
    int NCR_STATUS_ESCALATED_TO_RECALL = 35;
    int NCR_STATUS_CANCELLED = 40;

    // 纠正措施状态（erp-qa/action-status）
    int ACTION_STATUS_PENDING = 10;
    int ACTION_STATUS_IN_PROGRESS = 20;
    int ACTION_STATUS_COMPLETED = 30;
    int ACTION_STATUS_OVERDUE = 40;

    // 审核状态（erp-qa/approve-status）
    int APPROVE_STATUS_UNSUBMITTED = 10;
    int APPROVE_STATUS_SUBMITTED = 20;
    int APPROVE_STATUS_APPROVED = 30;
    int APPROVE_STATUS_REJECTED = 40;

    // 单据状态（erp-qa/doc-status）
    int DOC_STATUS_DRAFT = 10;
    int DOC_STATUS_ACTIVE = 20;
    int DOC_STATUS_CANCELLED = 30;

    // 处置决定（erp-qa/disposition-type）—— NCR 评审裁决时填
    int DISPOSITION_TYPE_SCRAP = 10;
    int DISPOSITION_TYPE_RETURN = 20;
    int DISPOSITION_TYPE_CONCESSION = 30;
    int DISPOSITION_TYPE_DOWNGRADE = 40;

    // NCR 来源类型（自由字符串，写入 ErpQaNonConformance.sourceType）
    String NCR_SOURCE_TYPE_INSPECTION = "INSPECTION";

    // 业务单据类型（自由字符串，写入 ErpQaInspection.relatedBillType / mandatory-inspection-bill-types 配置）
    String RELATED_BILL_TYPE_PUR_RECEIPT = "ERP_PUR_RECEIPT";
    String RELATED_BILL_TYPE_SAL_DELIVERY = "ERP_SAL_DELIVERY";
    String RELATED_BILL_TYPE_MFG_WORK_ORDER = "ERP_MFG_WORK_ORDER";

    // 质量域配置项（经 AppConfig.var 读取；无 .env/外部服务）
    // 强制质检的业务单据类型列表（逗号分隔，如 ERP_PUR_RECEIPT,ERP_MFG_WORK_ORDER）；空=不强制
    String CONFIG_MANDATORY_INSPECTION_BILL_TYPES = "erp-qua.mandatory-inspection-bill-types";
    // 全局默认质检模板 ID（无物料匹配模板时回落；空=无默认，质检单无行人工补录）
    String CONFIG_DEFAULT_INSPECTION_TEMPLATE = "erp-qua.default-inspection-template";
    // REJECTED 时是否自动生成 NCR（默认 true）
    String CONFIG_AUTO_CREATE_NCR_ON_REJECT = "erp-qua.auto-create-ncr-on-reject";
}
