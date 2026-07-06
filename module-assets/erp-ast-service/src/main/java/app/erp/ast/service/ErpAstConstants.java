package app.erp.ast.service;

/**
 * 资产域状态码与配置键常量。权威值来自 {@code module-assets/model/app-erp-assets.orm.xml}
 * 关联字典 {@code erp-ast/asset-status}、{@code erp-ast/depreciation-method}、
 * {@code erp-ast/depreciation-schedule-status}、{@code erp-ast/disposal-type}、
 * {@code erp-ast/doc-status}、{@code erp-ast/approve-status}、{@code erp-ast/capitalization-source-type}。
 */
public interface ErpAstConstants {

    // ---- 配置项（depreciation-and-posting.md §配置），经 AppConfig.var 读取 ----
    /** 期末结账时是否自动批量折旧（供 1000-3 调用），默认 true。 */
    String CONFIG_AUTO_DEPRECIATION_ON_CLOSE = "erp-ast.auto-depreciation-on-close";
    /** 批量折旧是否按资产类别分组并行，默认 true。 */
    String CONFIG_DEPRECIATION_PARALLEL_BY_CATEGORY = "erp-ast.depreciation-parallel-by-category";
    /** 折旧后净值是否强制不低于残值，默认 true。 */
    String CONFIG_RESIDUAL_VALUE_ENFORCED = "erp-ast.residual-value-enforced";
    /** 定时批量折旧 cron（空=不调度；plan 2026-07-05-0306-1 §配置点）。 */
    String CONFIG_DEPRECIATION_CRON = "erp-ast.depreciation-cron";
    /** 重估增值后是否调整折旧基数（默认 true）。 */
    String CONFIG_REVALUATION_ADJUST_DEPRECIATION_BASE = "erp-ast.revaluation-adjust-depreciation-base";
    /** 减值/重估是否强制审批（默认 true）。 */
    String CONFIG_VALUE_ADJUSTMENT_REQUIRE_APPROVAL = "erp-ast.value-adjustment-require-approval";

    // ---- CIP 配置项（cip.md §配置 + plan 0930-1） ----
    /** 利息资本化是否启用（默认 false；自动计提引擎落地前关闭，避免业务方误用）。 */
    String CONFIG_CIP_INTEREST_CAPITALIZATION_ENABLED = "erp-ast.cip-interest-capitalization-enabled";
    /** CIP 转固是否强制审批（默认 true）。 */
    String CONFIG_CIP_REQUIRE_APPROVAL = "erp-ast.cip-require-approval";
    /** CIP 是否允许部分转固（默认 true）。 */
    String CONFIG_CIP_PARTIAL_TRANSFER_ALLOWED = "erp-ast.cip-partial-transfer-allowed";

    // ---- asset-status ----
    String ASSET_STATUS_DRAFT = "DRAFT";
    String ASSET_STATUS_IN_SERVICE = "IN_SERVICE";
    String ASSET_STATUS_IDLE = "IDLE";
    String ASSET_STATUS_SCRAPPED = "SCRAPPED";
    String ASSET_STATUS_SOLD = "SOLD";

    // ---- depreciation-method ----
    String DEPRECIATION_METHOD_STRAIGHT_LINE = "STRAIGHT_LINE";
    String DEPRECIATION_METHOD_DECLINING = "DECLINING";
    String DEPRECIATION_METHOD_UNITS = "UNITS";

    // ---- depreciation-schedule-status ----
    String SCHEDULE_STATUS_PENDING = "PENDING";
    String SCHEDULE_STATUS_EXECUTED = "EXECUTED";
    String SCHEDULE_STATUS_REVERSED = "REVERSED";
    String SCHEDULE_STATUS_CANCELLED = "CANCELLED";

    // ---- disposal-type ----
    String DISPOSAL_TYPE_SCRAPPED = "SCRAPPED";
    String DISPOSAL_TYPE_SOLD = "SOLD";

    // ---- adjustment-type ----
    String ADJUSTMENT_TYPE_IMPAIRMENT = "IMPAIRMENT";
    String ADJUSTMENT_TYPE_REVALUATION_UP = "REVALUATION_UP";
    String ADJUSTMENT_TYPE_REVALUATION_DOWN = "REVALUATION_DOWN";

    // ---- doc-status ----
    String DOC_STATUS_DRAFT = "DRAFT";
    String DOC_STATUS_ACTIVE = "ACTIVE";
    String DOC_STATUS_CANCELLED = "CANCELLED";

    // ---- approve-status ----
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";

    // ---- capitalization-source-type ----
    String SOURCE_TYPE_INVENTORY = "INVENTORY";
    String SOURCE_TYPE_CIP = "CIP";
    String SOURCE_TYPE_DIRECT_PURCHASE = "DIRECT_PURCHASE";

    // ---- cip-status（CIP 三态状态机） ----
    String CIP_STATUS_DRAFT = "DRAFT";
    String CIP_STATUS_IN_CONSTRUCTION = "IN_CONSTRUCTION";
    String CIP_STATUS_TRANSFERRED = "TRANSFERRED";

    // ---- cip-cost-type（CIP 成本归集行类型） ----
    String CIP_COST_TYPE_PURCHASE = "PURCHASE";
    String CIP_COST_TYPE_SERVICE = "SERVICE";
    String CIP_COST_TYPE_LABOR = "LABOR";
    String CIP_COST_TYPE_INTEREST_CAPITALIZATION = "INTEREST_CAPITALIZATION";
    String CIP_COST_TYPE_OTHER = "OTHER";

    // ---- erp-fin/period-status（折旧期间控制引用） ----
    String PERIOD_STATUS_OPEN = "OPEN";
    String PERIOD_STATUS_CLOSED = "CLOSED";

    // ---- PostingEvent.billData 键（资产过账派发器填入，Provider 读取） ----
    String BILL_DATA_ASSET_ID = "ASSET_ID";
    String BILL_DATA_CATEGORY_ID = "CATEGORY_ID";
    String BILL_DATA_ORIGINAL_VALUE = "ORIGINAL_VALUE";
    String BILL_DATA_SOURCE_TYPE = "SOURCE_TYPE";
    String BILL_DATA_DEPARTMENT_ID = "DEPARTMENT_ID";
    String BILL_DATA_DISPOSAL_TYPE = "DISPOSAL_TYPE";
    String BILL_DATA_DISPOSAL_AMOUNT = "DISPOSAL_AMOUNT";
    String BILL_DATA_GAIN_LOSS = "GAIN_LOSS";
    String BILL_DATA_DEPRECIATION_AMOUNT = "DEPRECIATION_AMOUNT";
    String BILL_DATA_ACCUMULATED_DEPRECIATION = "ACCUMULATED_DEPRECIATION";
    String BILL_DATA_NET_BOOK_VALUE = "NET_BOOK_VALUE";
    String BILL_DATA_PERIOD = "PERIOD";
    String BILL_DATA_FIXED_ASSET_SUBJECT_CODE = "FIXED_ASSET_SUBJECT_CODE";
    String BILL_DATA_ACCUM_DEPRE_SUBJECT_CODE = "ACCUM_DEPRE_SUBJECT_CODE";
    String BILL_DATA_EXPENSE_SUBJECT_CODE = "EXPENSE_SUBJECT_CODE";
    String BILL_DATA_DISPOSAL_GAINLOSS_SUBJECT_CODE = "DISPOSAL_GAINLOSS_SUBJECT_CODE";
    String BILL_DATA_CREDIT_SUBJECT_CODE = "CREDIT_SUBJECT_CODE";

    // ---- PostingEvent.billData 键（价值调整过账专用） ----
    /** 调整类型（IMPAIRMENT/REVALUATION_UP/REVALUATION_DOWN）。 */
    String BILL_DATA_ADJUSTMENT_TYPE = "ADJUSTMENT_TYPE";
    /** 调整金额（正数）。 */
    String BILL_DATA_ADJUSTMENT_AMOUNT = "ADJUSTMENT_AMOUNT";
    /** 资产减值损失科目编码（借方，减值/重估减值）。 */
    String BILL_DATA_IMPAIRMENT_LOSS_SUBJECT_CODE = "IMPAIRMENT_LOSS_SUBJECT_CODE";
    /** 固定资产减值准备科目编码（贷方，减值）。 */
    String BILL_DATA_IMPAIRMENT_PROVISION_SUBJECT_CODE = "IMPAIRMENT_PROVISION_SUBJECT_CODE";
    /** 资本公积科目编码（贷方，重估增值）。 */
    String BILL_DATA_CAPITAL_RESERVE_SUBJECT_CODE = "CAPITAL_RESERVE_SUBJECT_CODE";
}
