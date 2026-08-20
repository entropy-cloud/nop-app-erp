package app.erp.pur.service;

import app.erp.pur.dao.constants.ErpPurDocStatus;

/**
 * 采购域状态码常量。权威值来自 {@code module-purchase/model/app-erp-purchase.orm.xml}
 * 关联字典 {@code wf/approve-status}、{@code erp-pur/doc-status}、{@code erp-pur/receive-status}。
 *
 * <p>三轴状态分离见 {@code docs/design/purchase/state-machine.md}。
 *
 * <p>{@code extends ErpPurDocStatus} 复用 dao 层常量定义，保持 approve-status / doc-status 单一真相源；
 * 本接口仅追加 service 层独有的派生状态与配置项。
 */
public interface ErpPurConstants extends ErpPurDocStatus {

    // 收货进度（派生）receive-status
    String RECEIVE_STATUS_UNRECEIVED = "UNRECEIVED";
    String RECEIVE_STATUS_PARTIAL = "PARTIAL";
    String RECEIVE_STATUS_RECEIVED = "RECEIVED";

    // 付款进度（派生）paid-status：采购发票的付款状态，付款单的核销状态复用本字典
    String PAID_STATUS_UNPAID = "UNPAID";
    String PAID_STATUS_PARTIAL = "PARTIAL";
    String PAID_STATUS_PAID = "PAID";

    // 主数据启用状态 erp-md/active-status
    String PARTNER_STATUS_ACTIVE = "ACTIVE";

    // AVL 准入状态 erp-md/supplier-approval-status（purchase 侧副本，避免 main 代码依赖 master-data-service）
    // 权威值见 module-master-data 的 ErpMdConstants（镜像 dict 值）
    String APPROVAL_STATUS_SUSPENDED = "SUSPENDED";
    String APPROVAL_STATUS_REJECTED = "REJECTED";

    // 库存作业类型（对齐 erp-inv/operation-type，调用方侧副本避免 main 代码依赖 inventory-service）
    String MOVE_TYPE_INCOMING = "INCOMING";
    String MOVE_TYPE_OUTGOING = "OUTGOING";

    // 入库联动标识（自由字符串，inventory 侧无字典约束）
    String RELATED_BILL_TYPE_PUR_RECEIVE = "ERP_PUR_RECEIVE";
    String RELATED_BILL_TYPE_PUR_RETURN = "ERP_PUR_RETURN";
    String RELATED_BILL_TYPE_REVERSAL = "REVERSAL";

    // 三单匹配配置项（three-way-match.md §不匹配的处理策略，缺失走默认，无需 .env）
    String CONFIG_MATCH_QTY_TOLERANCE = "erp-pur.match-qty-tolerance";
    String CONFIG_MATCH_PRICE_TOLERANCE = "erp-pur.match-price-tolerance";
    String CONFIG_MATCH_STRICT_MODE = "erp-pur.match-strict-mode";

    // 价格差异处理策略（RC-R1.50 / P1-RC-018，three-way-match.md §不匹配的处理策略）。
    // 空/其他值 = 拒绝族（既有 strict 抛错 / 非 strict warn 放行，零 PPV）；POST_DIFFERENCE = 「接收并过账差异」
    // （价格超容差 warn 放行 + buildEvent 写差异键 → AP_INVOICE 增 PPV 行，1403 按差异拆分）。
    String CONFIG_PRICE_DIFF_STRATEGY = "erp-pur.price-diff-strategy";
    String PRICE_DIFF_STRATEGY_POST_DIFFERENCE = "POST_DIFFERENCE";

    // 合同量折扣消费门控（RC-R1.79 / P1-RC-078，UC-CT-08 A，volume-discount.md §折扣应用）。
    // 默认 true 对齐 erp-ct.volume-discount-enabled 既有默认；false 时 ctContractLineId 仅存储不应用折扣。
    String CONFIG_CT_DISCOUNT_ENABLED = "erp-pur.ct-discount-enabled";

    // 量折扣来源标记（订单行 remark 前缀，幂等覆盖；purchase 行无 pricingSource 列故以 remark 载体）
    String CT_DISCOUNT_REMARK_TAG = "[CT_VOLUME_DISCOUNT]";

    // 付款核销三单匹配二次门控（R1.8 P1-MA2-003 方案 A，three-way-match.md §匹配时机「付款前最终校验」）。
    // 默认 false 保护既有测试基线；启用后 settle 路径强制 strict 复核 invoice 三单匹配完成态。
    String CONFIG_SETTLE_RECHECK_THREE_WAY_MATCH = "erp-pur.settle-recheck-three-way-match";

    // 退货配置项（returns.md §配置项，缺失走默认，无需 .env）
    String CONFIG_RETURN_REASON_REQUIRED = "erp-pur.return-reason-required";
    String CONFIG_RETURN_APPROVAL_REQUIRED = "erp-pur.return-approval-required";

    // 已核销判定阈值：累计已核销 / 发票含税总额 ≥ 此比例视为 PAID，否则按是否 > 0 区分 PARTIAL/UNPAID。
    // 用 BigDecimal 比例避免浮点；1.0 即全额核销才 PAID。
    java.math.BigDecimal PAID_FULL_RATIO = java.math.BigDecimal.ONE;

    // 供应商评级 standing erp-pur/supplier-standing（finalize 后按 totalScore 落档）
    String STANDING_GREEN = "GREEN";
    String STANDING_YELLOW = "YELLOW";
    String STANDING_RED = "RED";

    // 评分卡周期状态 erp-pur/scorecard-status（DRAFT 草稿可重算 / FINALIZED 已定稿不可重算）
    String SCORECARD_STATUS_DRAFT = "DRAFT";
    String SCORECARD_STATUS_FINALIZED = "FINALIZED";

    // 评分卡配置项（supplier-evaluation.md §配置点，缺失走默认，无需 .env）
    String CONFIG_SCORECARD_PREVENT_ON_RED = "erp-pur.scorecard-prevent-on-red";
    String CONFIG_SCORECARD_EVALUATION_CRON = "erp-pur.scorecard-evaluation-cron";

    // ---- 看板预警阈值配置项（dashboards.md §实现约定 §5，经 AppConfig.var 读取，NopSysVariable 可运行时覆盖）----
    /** 应付超期预警天数（账龄 > 此值触发）；默认 0=关闭预警。 */
    String CONFIG_DASH_PUR_AP_OVERDUE_DAYS = "erp-dash.pur-ap-overdue-days";
    int DEFAULT_DASH_PUR_AP_OVERDUE_DAYS = 0;
}
