package app.erp.fin.service;

/**
 * 财务域状态码与配置键常量。权威值来自 {@code module-finance/model/app-erp-finance.orm.xml}
 * 关联字典 {@code erp-fin/ar-ap-direction}、{@code erp-fin/ar-ap-status}、{@code erp-fin/reconciliation-status}。
 */
public interface ErpFinConstants {

    // ---- 配置项（ar-ap-reconciliation.md §配置项），经 AppConfig.var 读取 ----
    /** 应收账龄计算基准（invoice_date/due_date），默认 due_date。 */
    String CONFIG_AR_AGING_BASE = "erp-fin.ar-aging-base";
    /** 应付账龄计算基准（invoice_date/due_date），默认 due_date。 */
    String CONFIG_AP_AGING_BASE = "erp-fin.ap-aging-base";
    /** 核销金额精度（容忍分摊误差），默认 0.01。 */
    String CONFIG_RECONCILE_PRECISION = "erp-fin.reconcile-precision";
    /** 是否允许超额核销，默认 false。 */
    String CONFIG_ALLOW_OVER_RECONCILE = "erp-fin.allow-over-reconcile";

    String AGING_BASE_INVOICE_DATE = "invoice_date";
    String AGING_BASE_DUE_DATE = "due_date";

    // ---- 配置项（expense-claim.md §配置项），经 AppConfig.var 读取 ----
    /** 报销时是否自动抵扣同员工未还借款，默认 true。 */
    String CONFIG_ADVANCE_AUTO_OFFSET_ON_EXPENSE = "erp-fin.advance-auto-offset-on-expense";
    /** 报销 APPROVED 前是否强制预算校验，默认 false（预算模块未落地，钩子预留不实现）。 */
    String CONFIG_EXPENSE_BUDGET_CHECK_ENABLED = "erp-fin.expense-budget-check-enabled";
    /** 报销是否需审核，默认 true。 */
    String CONFIG_EXPENSE_APPROVAL_REQUIRED = "erp-fin.expense-approval-required";
    /** 报销事由是否必填，默认 true。 */
    String CONFIG_EXPENSE_REASON_REQUIRED = "erp-fin.expense-reason-required";

    // ---- ar-ap-direction ----
    int DIRECTION_RECEIVABLE = 10;
    int DIRECTION_PAYABLE = 20;

    // ---- ar-ap-status ----
    int AR_AP_STATUS_OPEN = 10;
    int AR_AP_STATUS_PARTIAL = 20;
    int AR_AP_STATUS_SETTLED = 30;
    int AR_AP_STATUS_CANCELLED = 40;

    // ---- reconciliation-status ----
    int RECON_STATUS_DRAFT = 10;
    int RECON_STATUS_POSTED = 20;
    int RECON_STATUS_REVERSED = 30;

    // ---- approve-status（共用 erp-fin/approve-status） ----
    int APPROVE_STATUS_UNSUBMITTED = 10;
    int APPROVE_STATUS_SUBMITTED = 20;
    int APPROVE_STATUS_APPROVED = 30;
    int APPROVE_STATUS_REJECTED = 40;

    // ---- expense-claim-status / advance-status（docStatus 轴） ----
    int DOC_STATUS_DRAFT = 10;
    int DOC_STATUS_CANCELLED = 50;

    // ---- expense-payment-mode ----
    int PAYMENT_MODE_OWN_ACCOUNT = 10;
    int PAYMENT_MODE_COMPANY_ACCOUNT = 20;

    // ---- 主数据启用状态 erp-md/active-status（员工启用校验） ----
    int EMPLOYEE_STATUS_ACTIVE = 10;

    // ---- sourceBillType（ErpFinArApItem.sourceBillType 字符串值） ----
    String SOURCE_BILL_AP_INVOICE = "AP_INVOICE";
    String SOURCE_BILL_AR_INVOICE = "AR_INVOICE";
    String SOURCE_BILL_PAYMENT = "PAYMENT";
    String SOURCE_BILL_RECEIPT = "RECEIPT";
    String SOURCE_BILL_PUR_RETURN = "PUR_RETURN";
    String SOURCE_BILL_SAL_RETURN = "SAL_RETURN";
    String SOURCE_BILL_EXPENSE_CLAIM = "EXPENSE_CLAIM";
    String SOURCE_BILL_EMPLOYEE_ADVANCE = "EMPLOYEE_ADVANCE";
    String SOURCE_BILL_NOTES_RECEIVABLE = "NOTES_RECEIVABLE";
    String SOURCE_BILL_NOTES_ENDORSED = "NOTES_ENDORSED";

    // ---- PostingEvent.billData 键（员工→partnerId 解析，派发器填入已解析的 employee.partnerId） ----
    /** billData 键：携带已解析的 employee.partnerId（非 employee.id），供 ArApItemGenerator.resolvePartnerId 直接采用。 */
    String BILL_DATA_EMPLOYEE_ID = "EMPLOYEE_ID";
    /** billData 键：报销价税合计（本位币）。 */
    String BILL_DATA_TOTAL_AMOUNT_WITH_TAX = "TOTAL_AMOUNT_WITH_TAX";
    /** billData 键：报销不含税金额（本位币）。 */
    String BILL_DATA_TOTAL_AMOUNT = "TOTAL_AMOUNT";
    /** billData 键：报销进项税额（本位币）。 */
    String BILL_DATA_TOTAL_TAX_AMOUNT = "TOTAL_TAX_AMOUNT";
    /** billData 键：付款方式（expense-payment-mode 数值）。 */
    String BILL_DATA_PAYMENT_MODE = "PAYMENT_MODE";
    /** billData 键：部门 ID。 */
    String BILL_DATA_DEPARTMENT_ID = "DEPARTMENT_ID";

    // ---- 期末结账配置项（period-close.md §配置项），经 AppConfig.var 读取 ----
    /** 结账时 posted=false 单据阻断(false)/提示(true)，默认 false（阻断）。 */
    String CONFIG_AUTO_POST_ON_CLOSE = "erp-fin.auto-post-on-close";
    /** 结账时是否自动计提折旧（引用 assets 域），默认 true。 */
    String CONFIG_AUTO_DEPRECIATION_ON_CLOSE = "erp-fin.auto-depreciation-on-close";
    /** 结账提醒提前天数，默认 3。 */
    String CONFIG_CLOSING_REMINDER_DAYS = "erp-fin.closing-reminder-days";
    /** 反结账是否需审批门控，默认 true。 */
    String CONFIG_REVERSE_CLOSE_APPROVAL_REQUIRED = "erp-fin.reverse-close-approval-required";
    /** 本年利润科目编码（损益结转必配）。 */
    String CONFIG_CURRENT_YEAR_PROFIT_SUBJECT_CODE = "erp-fin.current-year-profit-subject-code";
    /** 未分配利润科目编码（年度结转预留）。 */
    String CONFIG_RETAINED_EARNINGS_SUBJECT_CODE = "erp-fin.retained-earnings-subject-code";
    /** 是否启用期末汇兑重估，默认 true。 */
    String CONFIG_EXCHANGE_REVALUATION_ENABLED = "erp-fin.exchange-revaluation-enabled";
    /** 结账时是否触发存货成本兜底重算（引用 inventory 域 IErpInvCostingBiz，period-close.md §步骤2），默认 true。 */
    String CONFIG_INV_COSTING_RECLOSE_ON_CLOSE = "erp-fin.inv-costing-reclose-on-close";
    /** 期末汇率（启用汇兑重估且有外币未核销项时必配）。 */
    String CONFIG_PERIOD_END_EXCHANGE_RATE = "erp-fin.period-end-exchange-rate";
    /** 应收科目编码（汇兑重估用）。 */
    String CONFIG_AR_SUBJECT_CODE = "erp-fin.ar-subject-code";
    /** 应付科目编码（汇兑重估用）。 */
    String CONFIG_AP_SUBJECT_CODE = "erp-fin.ap-subject-code";
    /** 汇兑损益科目编码（汇兑重估用）。 */
    String CONFIG_FX_GAIN_LOSS_SUBJECT_CODE = "erp-fin.exchange-gain-loss-subject-code";

    // ---- period-status（erp-fin/period-status）四态关账机 ----
    int PERIOD_STATUS_OPEN = 10;
    int PERIOD_STATUS_CLOSING = 20;
    int PERIOD_STATUS_CLOSED = 30;
    int PERIOD_STATUS_NEVER_OPENED = 40;
    int PERIOD_STATUS_CLOSED_FINAL = 50;

    // ---- module-close-status（erp-fin/module-close-status） ----
    int MODULE_CLOSE_OPEN = 10;
    int MODULE_CLOSE_CLOSING = 20;
    int MODULE_CLOSE_CLOSED = 30;

    // ---- subject-class（erp-md/subject-class）损益结转识别三类 ----
    int SUBJECT_CLASS_INCOME = 40;
    int SUBJECT_CLASS_EXPENSE = 50;
    int SUBJECT_CLASS_COST = 60;

    // ---- 借贷方向 ----
    int DC_DEBIT = 10;
    int DC_CREDIT = 20;

    // ---- 凭证状态 ----
    int VOUCHER_STATUS_DRAFT = 10;
    int VOUCHER_STATUS_POSTED = 20;

    // ---- 资金/票据配置项（treasury.md §配置点），经 AppConfig.var 读取 ----
    /** 开银承前是否强制校验授信可用额度，默认 true。 */
    String CONFIG_CREDIT_CHECK_ON_ISSUE = "erp-fin.credit-check-on-issue";
    /** 票据注销是否需审批门控，默认 true。 */
    String CONFIG_NOTES_WRITEOFF_APPROVAL_REQUIRED = "erp-fin.notes-writeoff-approval-required";

    // ---- notes-type ----
    int NOTES_TYPE_BANK_ACCEPTANCE = 10;
    int NOTES_TYPE_COMMERCIAL_ACCEPTANCE = 20;

    // ---- notes-receivable-status（7 态状态机，treasury.md） ----
    int NOTES_RECV_RECEIVED = 10;
    int NOTES_RECV_DISCOUNTED = 20;
    int NOTES_RECV_ENDORSED = 30;
    int NOTES_RECV_COLLECTION_PENDING = 40;
    int NOTES_RECV_HONORED = 50;
    int NOTES_RECV_DISHONORED = 60;
    int NOTES_RECV_WRITE_OFF = 70;

    // ---- notes-payable-status ----
    int NOTES_PAY_ISSUED = 10;
    int NOTES_PAY_HONORED = 20;
    int NOTES_PAY_DISHONORED = 30;
    int NOTES_PAY_WRITE_OFF = 40;

    // ---- cash-flow-direction ----
    int CASH_FLOW_INFLOW = 10;
    int CASH_FLOW_OUTFLOW = 20;

    // ---- PostingEvent.billData 键（票据过账派发器填入） ----
    String BILL_DATA_PARTNER_ID = "partnerId";
    String BILL_DATA_FACE_AMOUNT = "FACE_AMOUNT";
    String BILL_DATA_DISCOUNT_INTEREST = "DISCOUNT_INTEREST";
    String BILL_DATA_NET_AMOUNT = "NET_AMOUNT";
    String BILL_DATA_EXCHANGE_GAIN_LOSS = "EXCHANGE_GAIN_LOSS";
    String BILL_DATA_BUSINESS_DATE = "businessDate";
    String BILL_DATA_DUE_DATE = "dueDate";
}
