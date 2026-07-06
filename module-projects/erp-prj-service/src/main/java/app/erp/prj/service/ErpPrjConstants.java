package app.erp.prj.service;

/**
 * 项目域状态码与配置键常量。权威值来自 {@code module-projects/model/app-erp-projects.orm.xml}
 * 关联字典 {@code erp-prj/project-status}、{@code erp-prj/task-status}、{@code erp-prj/timesheet-status}。
 */
public interface ErpPrjConstants {

    // ---- 配置项（cost-collection.md §配置），经 AppConfig.var 读取 ----
    /** 预算控制模式：WARNING（默认，超预算警告放行）/ STRICT（超预算拦截）。 */
    String CONFIG_BUDGET_CONTROL_MODE = "erp-prj.budget-control-mode";
    /** 全局默认人工成本率（元/小时）；为空表示无全局默认，工时无费率且活动类型无费率时抛 ERR_COST_RATE_NOT_AVAILABLE。 */
    String CONFIG_DEFAULT_LABOR_COST_RATE = "erp-prj.default-labor-cost-rate";
    /** 应付职工薪酬贷方科目编码；为空时抛 ERR_PAYROLL_SUBJECT_NOT_CONFIGURED。 */
    String CONFIG_DEFAULT_PAYROLL_SUBJECT_ID = "erp-prj.default-payroll-subject-id";
    /** 费用报销归集开关（默认 true）。关闭时 closeProject 跳过费用刷新。 */
    String CONFIG_EXPENSE_AGGREGATION_ENABLED = "erp-prj.expense-aggregation-enabled";

    /** 损益汇总计算 cron（空=不调度；plan 2026-07-07-0305-1 §配置点）。 */
    String CONFIG_PNL_CALC_CRON = "erp-prj.pnl-calc-cron";
    /** 损益汇总自动计算总开关（默认 false，双层门控第二层）。 */
    String CONFIG_PNL_AUTO_CALC_ENABLED = "erp-prj.pnl-auto-calc-enabled";
    /** 项目结算强制审批开关（默认 true；关闭时 approve 跳过 SUBMITTED 直接审批）。 */
    String CONFIG_SETTLEMENT_REQUIRE_APPROVAL = "erp-prj.settlement-require-approval";

    /** 预算控制模式取值。 */
    String BUDGET_MODE_WARNING = "WARNING";
    String BUDGET_MODE_STRICT = "STRICT";

    // ---- project-status ----
    String PROJECT_STATUS_DRAFT = "DRAFT";
    String PROJECT_STATUS_OPEN = "OPEN";
    String PROJECT_STATUS_ON_HOLD = "ON_HOLD";
    String PROJECT_STATUS_COMPLETED = "COMPLETED";
    String PROJECT_STATUS_CANCELLED = "CANCELLED";

    // ---- task-status ----
    String TASK_STATUS_TODO = "TODO";
    String TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
    String TASK_STATUS_DONE = "DONE";
    String TASK_STATUS_BLOCKED = "BLOCKED";

    // ---- timesheet-status 已合并到 approve-status（wf/approve-status 四态标准）----

    // ---- approve-status（标准审批轴，归集头/预算头/账单头 approveStatus 字段使用）----
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";

    // ---- erp-fin/doc-status（归集头/预算头沿用 finance 单据状态语义） ----
    String DOC_STATUS_DRAFT = "DRAFT";
    String DOC_STATUS_APPROVED = "APPROVED";
    String DOC_STATUS_CANCELLED = "CANCELLED";

    // ---- 成本分类（cost-collection.md §1.3，ErpPrjCostCollectionLine.costCategory 文本） ----
    String COST_CATEGORY_LABOR = "LABOR";
    String COST_CATEGORY_EXPENSE = "EXPENSE";

    // ---- 归集行来源单据类型（sourceBillType 文本，幂等去重键之一） ----
    String SOURCE_BILL_TYPE_TIMESHEET = "TIMESHEET";
    String SOURCE_BILL_TYPE_EXPENSE = "EXPENSE";

    // ---- pnl-calc-status（ErpPrjProjectPnl.calcStatus） ----
    String PNL_CALC_STATUS_PENDING = "PENDING";
    String PNL_CALC_STATUS_CALCULATED = "CALCULATED";

    // ---- settlement-type（ErpPrjProjectSettlement.settlementType） ----
    String SETTLEMENT_TYPE_FINAL = "FINAL";
    String SETTLEMENT_TYPE_INTERIM = "INTERIM";
    String SETTLEMENT_TYPE_CLOSE = "CLOSE";

    // ---- 结算明细行类型（ErpPrjProjectSettlementLine.lineType 文本） ----
    String SETTLEMENT_LINE_TYPE_INCOME = "INCOME";
    String SETTLEMENT_LINE_TYPE_COST = "COST";

    // ---- 结算明细来源单据类型（sourceBillType 文本） ----
    String SOURCE_BILL_TYPE_BILLING = "BILLING";
    String SOURCE_BILL_TYPE_COST_COLLECTION = "COST_COLLECTION";

    // ---- 成本分类（四类，对齐 PnL costLabor/Material/Expense/Subcontract） ----
    String COST_CATEGORY_MATERIAL = "MATERIAL";
    String COST_CATEGORY_SUBCONTRACT = "SUBCONTRACT";

    // ---- PostingEvent.billData 键（projects 过账派发器填入，Provider 读取） ----
    String BILL_DATA_PROJECT_ID = "PROJECT_ID";
    String BILL_DATA_TASK_ID = "TASK_ID";
    String BILL_DATA_ACTIVITY_TYPE_ID = "ACTIVITY_TYPE_ID";
    String BILL_DATA_COST_AMOUNT = "COST_AMOUNT";
    String BILL_DATA_HOURS = "HOURS";
    String BILL_DATA_COST_RATE = "COST_RATE";
    String BILL_DATA_DEBIT_SUBJECT_CODE = "DEBIT_SUBJECT_CODE";
    String BILL_DATA_CREDIT_SUBJECT_CODE = "CREDIT_SUBJECT_CODE";
    String BILL_DATA_SOURCE_BILL_TYPE = "SOURCE_BILL_TYPE";

    // ---- PostingEvent.billData 键（项目结算过账派发器填入，Provider 读取） ----
    String BILL_DATA_FINAL_REVENUE = "FINAL_REVENUE";
    String BILL_DATA_FINAL_COST = "FINAL_COST";
    String BILL_DATA_FINAL_PROFIT = "FINAL_PROFIT";
    String BILL_DATA_SETTLEMENT_TYPE = "SETTLEMENT_TYPE";
    String BILL_DATA_TRANSFER_TO_ASSET = "TRANSFER_TO_ASSET";
    String BILL_DATA_ASSET_CARD_CODE = "ASSET_CARD_CODE";
    String BILL_DATA_DEBIT_SUBJECT_CODE_SETTLEMENT = "DEBIT_SUBJECT_CODE_SETTLEMENT";
    String BILL_DATA_CREDIT_SUBJECT_CODE_SETTLEMENT = "CREDIT_SUBJECT_CODE_SETTLEMENT";
}
