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

    // ---- 拆分/合并 配置项（split-merge.md §配置 + plan 0930-2） ----
    /** 拆分价值舍入策略：DOWN_UP（最大项补差，默认）/ ROUND_ALL（全部四舍五入）。 */
    String CONFIG_SPLIT_ROUNDING_MODE = "erp-ast.split-rounding-mode";
    /** 合并折旧方法继承：WEIGHTED（按净值加权，默认）/ MAX（取最大价值项）。 */
    String CONFIG_MERGE_DEPRECIATION_INHERIT = "erp-ast.merge-depreciation-inherit";
    /** 拆分/合并是否强制审批（默认 true）。 */
    String CONFIG_SPLIT_MERGE_REQUIRE_APPROVAL = "erp-ast.split-merge-require-approval";
    /** 拆分/合并是否允许跨资产类别（默认 false，本期 Non-Goal 锁定）。 */
    String CONFIG_SPLIT_MERGE_ALLOW_CROSS_CATEGORY = "erp-ast.split-merge-allow-cross-category";

    // ---- CIP 配置项（cip.md §配置 + plan 0930-1） ----
    /** 利息资本化是否启用（默认 false；自动计提引擎落地前关闭，避免业务方误用）。 */
    String CONFIG_CIP_INTEREST_CAPITALIZATION_ENABLED = "erp-ast.cip-interest-capitalization-enabled";
    /** CIP 转固是否强制审批（默认 true）。 */
    String CONFIG_CIP_REQUIRE_APPROVAL = "erp-ast.cip-require-approval";
    /** CIP 是否允许部分转固（默认 true）。 */
    String CONFIG_CIP_PARTIAL_TRANSFER_ALLOWED = "erp-ast.cip-partial-transfer-allowed";

    // ---- 盘点 配置项（inventory.md §六 + plan 0842-1） ----
    /** 盘点过账是否强制先 approve 复核（默认 true）。 */
    String CONFIG_INVENTORY_REQUIRE_APPROVAL = "erp-ast.inventory-require-approval";
    /** 盘亏是否阻塞 post 待调查（true=所有盘亏须先 INVESTIGATE；默认 false=允许盘亏直接触发处置）。 */
    String CONFIG_INVENTORY_NEGATIVE_SHORTAGE_BLOCKS = "erp-ast.inventory-negative-shortage-blocks";

    // ---- 维修 配置项（maintenance.md §七 + plan 0842-2） ----
    /** 维修过账是否强制审批（默认 true）。 */
    String CONFIG_MAINTENANCE_REQUIRE_APPROVAL = "erp-ast.maintenance-require-approval";
    /** 资本化金额阈值，低于则强制费用化（0=不设阈值按 treatment 字段判定）。 */
    String CONFIG_MAINTENANCE_CAPITALIZE_THRESHOLD = "erp-ast.maintenance-capitalize-threshold";
    /** 资本化是否重算折旧基数（默认 true）。 */
    String CONFIG_MAINTENANCE_CAP_ADJUST_DEPRECIATION_BASE = "erp-ast.maintenance-cap-adjust-depreciation-base";
    /** 关联维护工单时贷中转科目防双重扣减（默认 true）。 */
    String CONFIG_MAINTENANCE_LINKED_CREDIT_CLEARING = "erp-ast.maintenance-linked-credit-clearing";

    // ---- asset-status ----
    String ASSET_STATUS_DRAFT = "DRAFT";
    String ASSET_STATUS_IN_SERVICE = "IN_SERVICE";
    String ASSET_STATUS_IDLE = "IDLE";
    String ASSET_STATUS_SCRAPPED = "SCRAPPED";
    String ASSET_STATUS_SOLD = "SOLD";
    /** 已内部处置：拆分/合并处置后账面净值归零的终态（与 SCRAPPED/SOLD 对外有损处置相对）。 */
    String ASSET_STATUS_DISPOSED = "DISPOSED";

    // ---- allocation-method（拆分分摊方式） ----
    String ALLOCATION_METHOD_PROPORTIONAL = "PROPORTIONAL";
    String ALLOCATION_METHOD_FIXED_AMOUNT = "FIXED_AMOUNT";

    // ---- 拆分/合并舍入策略 ----
    String SPLIT_ROUNDING_MODE_DOWN_UP = "DOWN_UP";
    String SPLIT_ROUNDING_MODE_ROUND_ALL = "ROUND_ALL";

    // ---- 合并折旧方法继承 ----
    String MERGE_DEPRECIATION_INHERIT_WEIGHTED = "WEIGHTED";
    String MERGE_DEPRECIATION_INHERIT_MAX = "MAX";

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

    // ---- inventory-status（盘点单状态机，inventory.md §一） ----
    String INVENTORY_STATUS_DRAFT = "DRAFT";
    String INVENTORY_STATUS_COUNTING = "COUNTING";
    String INVENTORY_STATUS_RECONCILING = "RECONCILING";
    String INVENTORY_STATUS_POSTED = "POSTED";
    String INVENTORY_STATUS_CANCELLED = "CANCELLED";

    // ---- variance-type（盘点差异类型） ----
    String VARIANCE_TYPE_SURPLUS = "SURPLUS";
    String VARIANCE_TYPE_SHORTAGE = "SHORTAGE";
    String VARIANCE_TYPE_MATCHED = "MATCHED";

    // ---- inventory-line-disposition（盘点差异处置） ----
    String INVENTORY_DISPOSITION_NONE = "NONE";
    String INVENTORY_DISPOSITION_NEW_CARD = "NEW_CARD";
    String INVENTORY_DISPOSITION_DISPOSAL = "DISPOSAL";
    String INVENTORY_DISPOSITION_INVESTIGATE = "INVESTIGATE";

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

    // ---- PostingEvent.billData 键（拆分/合并过账专用） ----
    /** 借方明细行列表（每行 {subjectCode, subjectName, amount, memo}）。 */
    String BILL_DATA_DEBIT_LINES = "DEBIT_LINES";
    /** 贷方明细行列表（每行 {subjectCode, subjectName, amount, memo}）。 */
    String BILL_DATA_CREDIT_LINES = "CREDIT_LINES";
    /** 行内科目编码键。 */
    String BILL_DATA_LINE_SUBJECT_CODE = "subjectCode";
    /** 行内科目名称键。 */
    String BILL_DATA_LINE_SUBJECT_NAME = "subjectName";
    /** 行内金额键。 */
    String BILL_DATA_LINE_AMOUNT = "amount";
    /** 行内摘要键。 */
    String BILL_DATA_LINE_MEMO = "memo";

    // ---- PostingEvent.billData 键（盘点差异过账专用） ----
    /** 盘盈合计金额（正数）。 */
    String BILL_DATA_INVENTORY_SURPLUS_AMOUNT = "INVENTORY_SURPLUS_AMOUNT";
    /** 盘亏合计金额（正数）。 */
    String BILL_DATA_INVENTORY_SHORTAGE_AMOUNT = "INVENTORY_SHORTAGE_AMOUNT";
    /** 营业外收入科目编码（贷方，盘盈）。 */
    String BILL_DATA_NON_OPERATING_INCOME_SUBJECT_CODE = "NON_OPERATING_INCOME_SUBJECT_CODE";
    /** 营业外支出科目编码（借方，盘亏）。 */
    String BILL_DATA_NON_OPERATING_EXPENSE_SUBJECT_CODE = "NON_OPERATING_EXPENSE_SUBJECT_CODE";

    // ---- maintenance-status（维修工单状态机，maintenance.md §一） ----
    String MAINTENANCE_STATUS_DRAFT = "DRAFT";
    String MAINTENANCE_STATUS_SUBMITTED = "SUBMITTED";
    String MAINTENANCE_STATUS_IN_PROGRESS = "IN_PROGRESS";
    String MAINTENANCE_STATUS_COMPLETED = "COMPLETED";
    String MAINTENANCE_STATUS_POSTED = "POSTED";
    String MAINTENANCE_STATUS_CANCELLED = "CANCELLED";

    // ---- maintenance-treatment（维修处置裁决） ----
    String MAINTENANCE_TREATMENT_CAPITALIZE = "CAPITALIZE";
    String MAINTENANCE_TREATMENT_EXPENSE = "EXPENSE";

    // ---- maintenance-cost-type（维修费用行类型） ----
    String MAINTENANCE_COST_TYPE_LABOR = "LABOR";
    String MAINTENANCE_COST_TYPE_SPARE_PART = "SPARE_PART";
    String MAINTENANCE_COST_TYPE_SUBCONTRACT = "SUBCONTRACT";

    // ---- PostingEvent.billData 键（维修过账专用） ----
    /** 维修处置裁决（CAPITALIZE/EXPENSE）。 */
    String BILL_DATA_MAINTENANCE_TREATMENT = "MAINTENANCE_TREATMENT";
    /** 维修资本化金额（正数）。 */
    String BILL_DATA_MAINTENANCE_CAPITALIZED_AMOUNT = "MAINTENANCE_CAPITALIZED_AMOUNT";
    /** 维修费用合计（正数）。 */
    String BILL_DATA_MAINTENANCE_TOTAL_COST = "MAINTENANCE_TOTAL_COST";
    /** 是否关联维护工单（true=贷中转科目防双重扣减）。 */
    String BILL_DATA_MAINTENANCE_LINKED_VISIT = "MAINTENANCE_LINKED_VISIT";
    /** 维修费用科目编码（借方，费用化路径）。 */
    String BILL_DATA_MAINTENANCE_EXPENSE_SUBJECT_CODE = "MAINTENANCE_EXPENSE_SUBJECT_CODE";
    /** 维修中转清算科目编码（贷方，关联维护工单时）。 */
    String BILL_DATA_MAINTENANCE_CLEARING_SUBJECT_CODE = "MAINTENANCE_CLEARING_SUBJECT_CODE";
    /** 银行存款科目编码（贷方，外协/人工）。 */
    String BILL_DATA_MAINTENANCE_BANK_SUBJECT_CODE = "MAINTENANCE_BANK_SUBJECT_CODE";
    /** 存货科目编码（贷方，备件消耗，独立维修时）。 */
    String BILL_DATA_MAINTENANCE_INVENTORY_SUBJECT_CODE = "MAINTENANCE_INVENTORY_SUBJECT_CODE";
}
