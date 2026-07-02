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

    // ---- asset-status ----
    int ASSET_STATUS_DRAFT = 10;
    int ASSET_STATUS_IN_SERVICE = 20;
    int ASSET_STATUS_IDLE = 30;
    int ASSET_STATUS_SCRAPPED = 40;
    int ASSET_STATUS_SOLD = 50;

    // ---- depreciation-method ----
    int DEPRECIATION_METHOD_STRAIGHT_LINE = 10;
    int DEPRECIATION_METHOD_DECLINING = 20;
    int DEPRECIATION_METHOD_UNITS = 30;

    // ---- depreciation-schedule-status ----
    int SCHEDULE_STATUS_PENDING = 10;
    int SCHEDULE_STATUS_EXECUTED = 20;
    int SCHEDULE_STATUS_REVERSED = 30;
    int SCHEDULE_STATUS_CANCELLED = 40;

    // ---- disposal-type ----
    int DISPOSAL_TYPE_SCRAPPED = 10;
    int DISPOSAL_TYPE_SOLD = 20;

    // ---- doc-status ----
    int DOC_STATUS_DRAFT = 10;
    int DOC_STATUS_ACTIVE = 20;
    int DOC_STATUS_CANCELLED = 30;

    // ---- approve-status ----
    int APPROVE_STATUS_UNSUBMITTED = 10;
    int APPROVE_STATUS_SUBMITTED = 20;
    int APPROVE_STATUS_APPROVED = 30;
    int APPROVE_STATUS_REJECTED = 40;

    // ---- capitalization-source-type ----
    int SOURCE_TYPE_INVENTORY = 10;
    int SOURCE_TYPE_CIP = 20;
    int SOURCE_TYPE_DIRECT_PURCHASE = 30;

    // ---- erp-fin/period-status（折旧期间控制引用） ----
    int PERIOD_STATUS_OPEN = 10;
    int PERIOD_STATUS_CLOSED = 30;

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
}
