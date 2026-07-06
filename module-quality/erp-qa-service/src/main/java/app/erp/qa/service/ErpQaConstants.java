package app.erp.qa.service;

/**
 * 质量域常量。字典码值权威：{@code module-quality/model/app-erp-quality.orm.xml}。
 *
 * <p>权威：{@code docs/design/quality/state-machine.md}、
 * {@code docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md}。
 */
public interface ErpQaConstants {

    // 检验类型（erp-qa/inspection-type）
    String INSPECTION_TYPE_INCOMING = "INCOMING";
    String INSPECTION_TYPE_IN_PROCESS = "IN_PROCESS";
    String INSPECTION_TYPE_FINAL = "FINAL";
    String INSPECTION_TYPE_OUTGOING = "OUTGOING";

    // 质检单结果（erp-qa/inspection-result，质检单 4 态；权威：state-machine.md §适用对象一）
    String INSPECTION_RESULT_PENDING = "PENDING";
    String INSPECTION_RESULT_ACCEPTED = "ACCEPTED";
    String INSPECTION_RESULT_CONDITIONAL = "CONDITIONAL";
    String INSPECTION_RESULT_REJECTED = "REJECTED";

    // NCR 状态（erp-qa/ncr-status，5 态；权威：state-machine.md §适用对象二）
    String NCR_STATUS_OPEN = "OPEN";
    String NCR_STATUS_IN_REVIEW = "IN_REVIEW";
    String NCR_STATUS_RESOLVED = "RESOLVED";
    String NCR_STATUS_ESCALATED_TO_RECALL = "ESCALATED_TO_RECALL";
    String NCR_STATUS_CANCELLED = "CANCELLED";

    // 纠正措施状态（erp-qa/action-status）
    String ACTION_STATUS_PENDING = "PENDING";
    String ACTION_STATUS_IN_PROGRESS = "IN_PROGRESS";
    String ACTION_STATUS_COMPLETED = "COMPLETED";
    String ACTION_STATUS_OVERDUE = "OVERDUE";

    // 审核状态（erp-qa/approve-status）
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";

    // 单据状态（erp-qa/doc-status）
    String DOC_STATUS_DRAFT = "DRAFT";
    String DOC_STATUS_ACTIVE = "ACTIVE";
    String DOC_STATUS_CANCELLED = "CANCELLED";

    // 处置决定（erp-qa/disposition-type）—— NCR 评审裁决时填
    String DISPOSITION_TYPE_SCRAP = "SCRAP";
    String DISPOSITION_TYPE_RETURN = "RETURN";
    String DISPOSITION_TYPE_CONCESSION = "CONCESSION";
    String DISPOSITION_TYPE_DOWNGRADE = "DOWNGRADE";

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
    // NCR 过账模式（plan 2026-07-05-2352-2）：AUTO_POST（resolve 时自动过账）/ MANUAL_POST（人工 postNcr 触发）
    String CONFIG_NCR_POSTING_MODE = "erp-qua.ncr-posting-mode";
    String NCR_POSTING_MODE_AUTO = "AUTO_POST";
    String NCR_POSTING_MODE_MANUAL = "MANUAL_POST";
    // NCR 过账默认账套（plan 2026-07-05-2352-2）：NCR 过账凭证所属账套 ID（NCR 无账套字段，经配置指定）
    String CONFIG_NCR_DEFAULT_ACCT_SCHEMA = "erp-qua.ncr-default-acct-schema";

    // ---- 召回事件（2.11）----

    // 召回触发类型（erp-qa/recall-trigger-type）
    String RECALL_TRIGGER_MANUAL = "MANUAL";
    String RECALL_TRIGGER_GAUGE_NCR_UPGRADE = "GAUGE_NCR_UPGRADE";
    String RECALL_TRIGGER_BATCH_NCR_UPGRADE = "BATCH_NCR_UPGRADE";
    String RECALL_TRIGGER_REGULATORY = "REGULATORY";

    // 召回严重程度（erp-qa/recall-severity）
    String RECALL_SEVERITY_LOW = "LOW";
    String RECALL_SEVERITY_MEDIUM = "MEDIUM";
    String RECALL_SEVERITY_HIGH = "HIGH";
    String RECALL_SEVERITY_CRITICAL = "CRITICAL";

    // 召回状态（erp-qa/recall-status，5 态）
    String RECALL_STATUS_OPEN = "OPEN";
    String RECALL_STATUS_APPROVED = "APPROVED";
    String RECALL_STATUS_IN_PROGRESS = "IN_PROGRESS";
    String RECALL_STATUS_CLOSED = "CLOSED";
    String RECALL_STATUS_CANCELLED = "CANCELLED";

    // 召回目标退货状态（erp-qa/recall-target-return-status）
    String RECALL_TARGET_RETURN_PENDING = "PENDING";
    String RECALL_TARGET_RETURN_NOTIFIED = "NOTIFIED";
    String RECALL_TARGET_RETURN_RETURNED = "RETURNED";

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
    String INV_OPERATION_TYPE_OUTGOING = "OUTGOING";
    // erp-inv/move-status：已完成（仅已出库的货才进入召回定位）
    String INV_MOVE_STATUS_DONE = "DONE";
    // erp-sal/doc-status：草稿（召回生成的退货单为草稿，召回不直接改余额，由销售标准流程审批过账）
    String SAL_DOC_STATUS_DRAFT = "DRAFT";
    // erp-sal/approve-status：未提交（召回生成的退货单初始审核状态）
    String SAL_APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";

    // ---- 看板预警阈值配置项（dashboards.md §实现约定 §5，经 AppConfig.var 读取，NopSysVariable 可运行时覆盖）----
    /** CAPA 逾期预警窗口天数（Action.dueDate 早于 today-minus-overdueDays 触发预警）；默认 0=直接 < today 比对。 */
    String CONFIG_DASH_QA_CAPA_OVERDUE_DAYS = "erp-dash.qa-capa-overdue-days";
    int DEFAULT_DASH_QA_CAPA_OVERDUE_DAYS = 0;
}
