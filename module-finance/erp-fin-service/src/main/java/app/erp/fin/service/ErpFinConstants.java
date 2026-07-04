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
    String DIRECTION_RECEIVABLE = "RECEIVABLE";
    String DIRECTION_PAYABLE = "PAYABLE";

    // ---- ar-ap-status ----
    String AR_AP_STATUS_OPEN = "OPEN";
    String AR_AP_STATUS_PARTIAL = "PARTIAL";
    String AR_AP_STATUS_SETTLED = "SETTLED";
    String AR_AP_STATUS_CANCELLED = "CANCELLED";

    // ---- reconciliation-status ----
    String RECON_STATUS_DRAFT = "DRAFT";
    String RECON_STATUS_POSTED = "POSTED";
    String RECON_STATUS_REVERSED = "REVERSED";

    // ---- approve-status（共用 erp-fin/approve-status） ----
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";

    // ---- expense-claim-status / advance-status（docStatus 轴） ----
    String DOC_STATUS_DRAFT = "DRAFT";
    String DOC_STATUS_CANCELLED = "CANCELLED";

    // ---- expense-payment-mode ----
    String PAYMENT_MODE_OWN_ACCOUNT = "OWN_ACCOUNT";
    String PAYMENT_MODE_COMPANY_ACCOUNT = "COMPANY_ACCOUNT";

    // ---- 主数据启用状态 erp-md/active-status（员工启用校验） ----
    String EMPLOYEE_STATUS_ACTIVE = "ACTIVE";

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
    // 所有权转移生成的应付（VMI 消耗等，consignment.md；待供应商采购发票核销）
    String SOURCE_BILL_OWNERSHIP_TRANSFER = "OWNERSHIP_TRANSFER";

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
    String PERIOD_STATUS_OPEN = "OPEN";
    String PERIOD_STATUS_CLOSING = "CLOSING";
    String PERIOD_STATUS_CLOSED = "CLOSED";
    String PERIOD_STATUS_NEVER_OPENED = "NEVER_OPENED";
    String PERIOD_STATUS_CLOSED_FINAL = "CLOSED_FINAL";

    // ---- module-close-status（erp-fin/module-close-status） ----
    String MODULE_CLOSE_OPEN = "OPEN";
    String MODULE_CLOSE_CLOSING = "CLOSING";
    String MODULE_CLOSE_CLOSED = "CLOSED";

    // ---- subject-class（erp-md/subject-class）损益结转识别三类 ----
    String SUBJECT_CLASS_INCOME = "INCOME";
    String SUBJECT_CLASS_EXPENSE = "EXPENSE";
    String SUBJECT_CLASS_COST = "COST";

    // ---- 借贷方向 ----
    String DC_DEBIT = "DEBIT";
    String DC_CREDIT = "CREDIT";

    // ---- 凭证状态 ----
    String VOUCHER_STATUS_DRAFT = "DRAFT";
    String VOUCHER_STATUS_POSTED = "POSTED";

    // ---- 过账类型（与 erp-fin/posting-type 字典对齐） ----
    String POSTING_TYPE_NORMAL = "NORMAL";
    String POSTING_TYPE_REVERSAL = "REVERSAL";

    // ---- 配置项（posting.md §冲销机制方向二 §实现策略 裁决3），经 AppConfig.var 读取 ----
    /** 凭证红冲事件派发模式：SYNC（默认，同事务同步通知）/ ASYNC（post-commit afterCommit）。 */
    String CONFIG_REVERSAL_DISPATCH_MODE = "erp-fin.reversal-dispatch-mode";
    /** 派发模式取值：SYNC（默认）。 */
    String REVERSAL_DISPATCH_MODE_SYNC = "SYNC";
    /** 派发模式取值：ASYNC（post-commit afterCommit）。 */
    String REVERSAL_DISPATCH_MODE_ASYNC = "ASYNC";

    /** 监听者派发失败记录的 failedStage 标识（落入 ErpFinPostingException 异常工作台）。 */
    String FAILED_STAGE_NOTIFY_REVERSAL_LISTENER = "notify-reversal-listener";

    // ---- 过账异常处置状态（与 erp-fin/posting-exception-status 字典对齐，见 posting-log.md §过账异常处置） ----
    String POSTING_EXCEPTION_STATUS_PENDING = "PENDING";
    String POSTING_EXCEPTION_STATUS_RETRYING = "RETRYING";
    String POSTING_EXCEPTION_STATUS_RETRIED = "RETRIED";
    String POSTING_EXCEPTION_STATUS_IGNORED = "IGNORED";
    String POSTING_EXCEPTION_STATUS_MANUAL = "MANUAL";

    // ---- 过账异常处置动作（与 erp-fin/posting-exception-resolution 字典对齐） ----
    String POSTING_EXCEPTION_RESOLUTION_RETRY = "RETRY";
    String POSTING_EXCEPTION_RESOLUTION_IGNORE = "IGNORE";
    String POSTING_EXCEPTION_RESOLUTION_MANUAL = "MANUAL";

    // ---- 资金/票据配置项（treasury.md §配置点），经 AppConfig.var 读取 ----
    /** 开银承前是否强制校验授信可用额度，默认 true。 */
    String CONFIG_CREDIT_CHECK_ON_ISSUE = "erp-fin.credit-check-on-issue";
    /** 票据注销是否需审批门控，默认 true。 */
    String CONFIG_NOTES_WRITEOFF_APPROVAL_REQUIRED = "erp-fin.notes-writeoff-approval-required";

    // ---- notes-type ----
    String NOTES_TYPE_BANK_ACCEPTANCE = "BANK_ACCEPTANCE";
    String NOTES_TYPE_COMMERCIAL_ACCEPTANCE = "COMMERCIAL_ACCEPTANCE";

    // ---- notes-receivable-status（7 态状态机，treasury.md） ----
    String NOTES_RECV_RECEIVED = "RECEIVED";
    String NOTES_RECV_DISCOUNTED = "DISCOUNTED";
    String NOTES_RECV_ENDORSED = "ENDORSED";
    String NOTES_RECV_COLLECTION_PENDING = "COLLECTION_PENDING";
    String NOTES_RECV_HONORED = "HONORED";
    String NOTES_RECV_DISHONORED = "DISHONORED";
    String NOTES_RECV_WRITE_OFF = "WRITE_OFF";

    // ---- notes-payable-status ----
    String NOTES_PAY_ISSUED = "ISSUED";
    String NOTES_PAY_HONORED = "HONORED";
    String NOTES_PAY_DISHONORED = "DISHONORED";
    String NOTES_PAY_WRITE_OFF = "WRITE_OFF";

    // ---- cash-flow-direction ----
    String CASH_FLOW_INFLOW = "INFLOW";
    String CASH_FLOW_OUTFLOW = "OUTFLOW";

    // ---- PostingEvent.billData 键（票据过账派发器填入） ----
    String BILL_DATA_PARTNER_ID = "partnerId";
    String BILL_DATA_FACE_AMOUNT = "FACE_AMOUNT";
    String BILL_DATA_DISCOUNT_INTEREST = "DISCOUNT_INTEREST";
    String BILL_DATA_NET_AMOUNT = "NET_AMOUNT";
    String BILL_DATA_EXCHANGE_GAIN_LOSS = "EXCHANGE_GAIN_LOSS";
    String BILL_DATA_BUSINESS_DATE = "businessDate";
    String BILL_DATA_DUE_DATE = "dueDate";
}
