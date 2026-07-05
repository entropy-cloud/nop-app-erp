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
}
