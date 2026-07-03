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

    // ---- 召回事件（2.11）----

    // 召回触发类型（erp-qa/recall-trigger-type）
    int RECALL_TRIGGER_MANUAL = 10;
    int RECALL_TRIGGER_GAUGE_NCR_UPGRADE = 20;
    int RECALL_TRIGGER_BATCH_NCR_UPGRADE = 30;
    int RECALL_TRIGGER_REGULATORY = 40;

    // 召回严重程度（erp-qa/recall-severity）
    int RECALL_SEVERITY_LOW = 10;
    int RECALL_SEVERITY_MEDIUM = 20;
    int RECALL_SEVERITY_HIGH = 30;
    int RECALL_SEVERITY_CRITICAL = 40;

    // 召回状态（erp-qa/recall-status，5 态）
    int RECALL_STATUS_OPEN = 10;
    int RECALL_STATUS_APPROVED = 20;
    int RECALL_STATUS_IN_PROGRESS = 30;
    int RECALL_STATUS_CLOSED = 40;
    int RECALL_STATUS_CANCELLED = 50;

    // 召回目标退货状态（erp-qa/recall-target-return-status）
    int RECALL_TARGET_RETURN_PENDING = 10;
    int RECALL_TARGET_RETURN_NOTIFIED = 20;
    int RECALL_TARGET_RETURN_RETURNED = 30;

    // 召回配置项
    // 召回是否强制审批（默认 true：OPEN→APPROVED 须经 submit/approve）
    String CONFIG_RECALL_REQUIRE_APPROVAL = "erp-qua.recall-require-approval";
    // 召回关闭是否要求全部目标已通知（默认 true：close 前所有 target returnStatus≠PENDING 且 notifyCustomer=true）
    String CONFIG_RECALL_NOTIFY_REQUIRED_TO_CLOSE = "erp-qua.recall-notify-required-to-close";

    // 库存域追溯链开关（inventory 既有配置，召回定位消费）
    String CONFIG_INV_TRACE_CHAIN_ENABLED = "erp-inv.trace-chain-enabled";

    // 跨域业务单据类型（自由字符串，关联库存移动单 relatedBillType）
    // 镜像 ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY（sales dao 不暴露该常量，此处镜像不可变值）
    String RELATED_BILL_TYPE_SAL_DELIVERY_INV = "ERP_SAL_DELIVERY";

    // ---- 跨域字典镜像值（召回定位/退货编排消费；权威为各域 ORM 字典，此处仅镜像不可变码值）----
    // erp-inv/operation-type：出库（销售出库移动单 moveType）
    int INV_OPERATION_TYPE_OUTGOING = 20;
    // erp-inv/move-status：已完成（仅已出库的货才进入召回定位）
    int INV_MOVE_STATUS_DONE = 30;
    // erp-sal/doc-status：草稿（召回生成的退货单为草稿，召回不直接改余额，由销售标准流程审批过账）
    int SAL_DOC_STATUS_DRAFT = 10;
    // erp-sal/approve-status：未提交（召回生成的退货单初始审核状态）
    int SAL_APPROVE_STATUS_UNSUBMITTED = 10;
}
