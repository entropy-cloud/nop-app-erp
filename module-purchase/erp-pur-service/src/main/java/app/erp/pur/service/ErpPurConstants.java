package app.erp.pur.service;

/**
 * 采购域状态码常量。权威值来自 {@code module-purchase/model/app-erp-purchase.orm.xml}
 * 关联字典 {@code erp-pur/approve-status}、{@code erp-pur/doc-status}、{@code erp-pur/receive-status}。
 *
 * <p>三轴状态分离见 {@code docs/design/purchase/state-machine.md}。
 */
public interface ErpPurConstants {

    // 审核轴 approve-status
    int APPROVE_STATUS_UNSUBMITTED = 10;
    int APPROVE_STATUS_SUBMITTED = 20;
    int APPROVE_STATUS_APPROVED = 30;
    int APPROVE_STATUS_REJECTED = 40;

    // 单据生命周期轴 doc-status
    int DOC_STATUS_DRAFT = 10;
    int DOC_STATUS_ACTIVE = 20;
    int DOC_STATUS_CANCELLED = 30;

    // 收货进度（派生）receive-status
    int RECEIVE_STATUS_UNRECEIVED = 10;
    int RECEIVE_STATUS_PARTIAL = 20;
    int RECEIVE_STATUS_RECEIVED = 30;

    // 付款进度（派生）paid-status：采购发票的付款状态，付款单的核销状态复用本字典
    int PAID_STATUS_UNPAID = 10;
    int PAID_STATUS_PARTIAL = 20;
    int PAID_STATUS_PAID = 30;

    // 主数据启用状态 erp-md/active-status
    int PARTNER_STATUS_ACTIVE = 10;

    // AVL 准入状态 erp-md/supplier-approval-status（purchase 侧副本，避免 main 代码依赖 master-data-service）
    // 权威值见 module-master-data 的 ErpMdConstants（镜像 dict 值）
    int APPROVAL_STATUS_SUSPENDED = 40;
    int APPROVAL_STATUS_REJECTED = 50;

    // 库存作业类型（对齐 erp-inv/operation-type，调用方侧副本避免 main 代码依赖 inventory-service）
    int MOVE_TYPE_INCOMING = 10;
    int MOVE_TYPE_OUTGOING = 20;

    // 入库联动标识（自由字符串，inventory 侧无字典约束）
    String RELATED_BILL_TYPE_PUR_RECEIVE = "ERP_PUR_RECEIVE";
    String RELATED_BILL_TYPE_PUR_RETURN = "ERP_PUR_RETURN";
    String RELATED_BILL_TYPE_REVERSAL = "REVERSAL";

    // 三单匹配配置项（three-way-match.md §不匹配的处理策略，缺失走默认，无需 .env）
    String CONFIG_MATCH_QTY_TOLERANCE = "erp-pur.match-qty-tolerance";
    String CONFIG_MATCH_PRICE_TOLERANCE = "erp-pur.match-price-tolerance";
    String CONFIG_MATCH_STRICT_MODE = "erp-pur.match-strict-mode";

    // 退货配置项（returns.md §配置项，缺失走默认，无需 .env）
    String CONFIG_RETURN_REASON_REQUIRED = "erp-pur.return-reason-required";
    String CONFIG_RETURN_APPROVAL_REQUIRED = "erp-pur.return-approval-required";

    // 已核销判定阈值：累计已核销 / 发票含税总额 ≥ 此比例视为 PAID，否则按是否 > 0 区分 PARTIAL/UNPAID。
    // 用 BigDecimal 比例避免浮点；1.0 即全额核销才 PAID。
    java.math.BigDecimal PAID_FULL_RATIO = java.math.BigDecimal.ONE;

    // 供应商评级 standing erp-pur/supplier-standing（finalize 后按 totalScore 落档）
    int STANDING_GREEN = 10;
    int STANDING_YELLOW = 20;
    int STANDING_RED = 30;

    // 评分卡周期状态 erp-pur/scorecard-status（DRAFT 草稿可重算 / FINALIZED 已定稿不可重算）
    int SCORECARD_STATUS_DRAFT = 10;
    int SCORECARD_STATUS_FINALIZED = 20;

    // 评分卡配置项（supplier-evaluation.md §配置点，缺失走默认，无需 .env）
    String CONFIG_SCORECARD_PREVENT_ON_RED = "erp-pur.scorecard-prevent-on-red";
    String CONFIG_SCORECARD_EVALUATION_CRON = "erp-pur.scorecard-evaluation-cron";
}
