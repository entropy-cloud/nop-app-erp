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
    /** 是否启用自动核销（总开关），默认 false（ar-ap-reconciliation.md §配置项）。 */
    String CONFIG_AUTO_RECONCILE = "erp-fin.auto-reconcile";
    /** 自动核销分摊策略（FIFO/BY_AMOUNT/BY_RATIO），默认 FIFO（plan 2026-07-05-0115-1 §配置项）。 */
    String CONFIG_AUTO_RECON_STRATEGY = "erp-fin.auto-recon-strategy";
    /** 定时自动核销 cron（空=不调度；plan 2026-07-05-0115-1 §配置项）。 */
    String CONFIG_AR_AP_AUTO_RECON_CRON = "erp-fin.ar-ap-auto-recon-cron";
    /** 定时现金预测刷新 cron（空=不调度；plan 2026-07-05-0306-1 §配置点）。 */
    String CONFIG_CASH_FORECAST_CRON = "erp-fin.cash-forecast-cron";
    /** 现金预测默认向前预测窗口（天），默认 30。 */
    String CONFIG_CASH_FORECAST_WINDOW_DAYS = "erp-fin.cash-forecast-window-days";
    /** CONFIG_CASH_FORECAST_WINDOW_DAYS 缺省值。 */
    int DEFAULT_CASH_FORECAST_WINDOW_DAYS = 30;

    String AGING_BASE_INVOICE_DATE = "invoice_date";
    String AGING_BASE_DUE_DATE = "due_date";

    // ---- 自动核销分摊策略（CONFIG_AUTO_RECON_STRATEGY 取值）----
    String AUTO_RECON_STRATEGY_FIFO = "FIFO";
    String AUTO_RECON_STRATEGY_BY_AMOUNT = "BY_AMOUNT";
    String AUTO_RECON_STRATEGY_BY_RATIO = "BY_RATIO";

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

    /** 过账异常告警通知派发开关（默认 true；plan 2026-07-06-0642-1 §Phase 1）。关闭时跳过 notify 调用。 */
    String CONFIG_POSTING_EXCEPTION_NOTIFY_ENABLED = "erp-fin.posting-exception-notify-enabled";

    /** 通知事件类型：过账异常告警（对应 erp_sys_notification_template.notification_type）。 */
    String NOTIFY_EVENT_POSTING_EXCEPTION = "fin.posting-exception";

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

    // ---- 运行监控配置项（posting-log.md §裁决3，经 AppConfig.var 读取）----
    /** 自动化记账率阈值，默认 0.95（达标值）。 */
    String CONFIG_METRIC_AUTO_POSTING_RATE_THRESHOLD = "erp-fin.metric.auto-posting-rate-threshold";
    /** 凭证生成时延 P99 阈值（毫秒），默认 30000。 */
    String CONFIG_METRIC_LATENCY_P99_THRESHOLD_MILLIS = "erp-fin.metric.latency-p99-threshold-millis";
    /** 过账异常率阈值，默认 0.01。 */
    String CONFIG_METRIC_EXCEPTION_RATE_THRESHOLD = "erp-fin.metric.exception-rate-threshold";
    /** 业财闭环成功率阈值，默认 0.995。 */
    String CONFIG_METRIC_LOOPBACK_RATE_THRESHOLD = "erp-fin.metric.loopback-rate-threshold";
    /** 指标聚合窗口（小时），默认 24。 */
    String CONFIG_METRIC_WINDOW_HOURS = "erp-fin.metric.window-hours";
    /** 时延内存采样窗口大小（样本数），默认 1000。 */
    String CONFIG_METRIC_LATENCY_SAMPLE_WINDOW = "erp-fin.metric.latency-sample-window";

    double DEFAULT_METRIC_AUTO_POSTING_RATE_THRESHOLD = 0.95;
    long DEFAULT_METRIC_LATENCY_P99_THRESHOLD_MILLIS = 30_000L;
    double DEFAULT_METRIC_EXCEPTION_RATE_THRESHOLD = 0.01;
    double DEFAULT_METRIC_LOOPBACK_RATE_THRESHOLD = 0.995;
    int DEFAULT_METRIC_WINDOW_HOURS = 24;
    int DEFAULT_METRIC_LATENCY_SAMPLE_WINDOW = 1000;

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

    // ---- 银行对账配置项（bank-reconciliation.md §配置项），经 AppConfig.var 读取 ----
    /** 自动勾对日期容差窗口（天），默认 3。 */
    String CONFIG_BANK_MATCH_TOLERANCE_DAYS = "erp-fin.bank-match-tolerance-days";
    /** 缺 refNo 时是否拒绝导入，默认 false。 */
    String CONFIG_BANK_IMPORT_STRICT_REFNO = "erp-fin.bank-import-strict-refno";
    /** 未达调整凭证是否下月初自动红冲，默认 true（实际红冲由定时任务触发，本计划交付 reverse 入口）。 */
    String CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH = "erp-fin.bank-recon-auto-reverse-next-month";
    /** 银行对账勾对默认天数窗口（CONFIG_BANK_MATCH_TOLERANCE_DAYS 缺省值）。 */
    int DEFAULT_BANK_MATCH_TOLERANCE_DAYS = 3;

    // ---- bank-match-status（erp-fin/bank-match-status） ----
    String BANK_MATCH_UNMATCHED = "UNMATCHED";
    String BANK_MATCH_MATCHED = "MATCHED";
    String BANK_MATCH_MANUAL_MATCHED = "MANUAL_MATCHED";
    String BANK_MATCH_SUSPENSE = "SUSPENSE";

    // ---- fund-account-type ----
    String FUND_ACCOUNT_TYPE_BANK = "BANK";

    // ---- voucher-status CANCELLED（docStatus 终态）----
    String VOUCHER_STATUS_CANCELLED = "CANCELLED";

    // ---- 银行对账调节表 side（在途/未达方） ----
    String BANK_RECON_SIDE_IN_TRANSIT = "IN_TRANSIT";
    String BANK_RECON_SIDE_UNRECORDED = "UNRECORDED";

    /** PostingEvent.billData 键：未达账项行金额合计（本位币）。 */
    String BILL_DATA_BANK_RECON_ADJ_AMOUNT = "BANK_RECON_ADJ_AMOUNT";
    /** PostingEvent.billData 键：资金账户对应科目编码（${SUBJECT_CODE} 占位符填充）。 */
    String BILL_DATA_BANK_SUBJECT_CODE = "SUBJECT_CODE";

    // ---- 坏账准备配置项（bad-debt.md §配置点），经 AppConfig.var 读取 ----
    /** 坏账计提方法（AGING_BUCKET/HISTORICAL_PERCENT/RISK_CLASS/PARETO），默认 AGING_BUCKET。 */
    String CONFIG_BAD_DEBT_METHOD = "erp-fin.bad-debt-method";
    /** 0-30 天历史损失率，默认 0.005。 */
    String CONFIG_BAD_DEBT_LOSS_RATE_0_30 = "erp-fin.bad-debt-loss-rate-0-30";
    /** 31-60 天历史损失率，默认 0.02。 */
    String CONFIG_BAD_DEBT_LOSS_RATE_31_60 = "erp-fin.bad-debt-loss-rate-31-60";
    /** 61-90 天历史损失率，默认 0.05。 */
    String CONFIG_BAD_DEBT_LOSS_RATE_61_90 = "erp-fin.bad-debt-loss-rate-61-90";
    /** 91-180 天历史损失率，默认 0.15。 */
    String CONFIG_BAD_DEBT_LOSS_RATE_91_180 = "erp-fin.bad-debt-loss-rate-91-180";
    /** 180 天以上历史损失率，默认 0.40。 */
    String CONFIG_BAD_DEBT_LOSS_RATE_180_PLUS = "erp-fin.bad-debt-loss-rate-180-plus";
    /** 坏账核销/恢复是否强制财务主管审批，默认 true。 */
    String CONFIG_BAD_DEBT_WRITE_OFF_REQUIRE_APPROVAL = "erp-fin.bad-debt-write-off-require-approval";
    /** 计提基础是否排除争议发票，默认 true。 */
    String CONFIG_BAD_DEBT_EXCLUDE_DISPUTED = "erp-fin.bad-debt-exclude-disputed";
    /** 坏账准备（Allowance）科目编码（期末计提/核销/释放必配）。 */
    String CONFIG_BAD_DEBT_ALLOWANCE_SUBJECT_CODE = "erp-fin.bad-debt-allowance-subject-code";
    /** 信用减值损失（Bad Debt Expense）科目编码（期末计提/释放必配）。 */
    String CONFIG_BAD_DEBT_EXPENSE_SUBJECT_CODE = "erp-fin.bad-debt-expense-subject-code";
    /** 期末 allowance 充足性门控开关，默认 true（不足阻止结账）。 */
    String CONFIG_BAD_DEBT_ALLOWANCE_GATE_ENABLED = "erp-fin.bad-debt-allowance-gate-enabled";

    // ---- 年度结转配置项（period-close.md §年度结转规则，plan 2026-07-05-0540-2），经 AppConfig.var 读取 ----
    /** 年度结转总开关（12 月结账后是否执行本年利润→未分配利润 + 次年期间创建 + 年初余额 populate），默认 true。 */
    String CONFIG_ANNUAL_CLOSE_ENABLED = "erp-fin.annual-close-enabled";
    /** 次年期间生成幂等策略：已存在同年期间时是否仅补缺失月份（true=补缺，false=抛错），默认 false。 */
    String CONFIG_PERIOD_GENERATE_SKIP_EXISTING = "erp-fin.period-generate-skip-existing";
    /** 银行存款外币汇兑重估开关，默认 true。 */
    String CONFIG_BANK_FX_REVALUATION_ENABLED = "erp-fin.bank-fx-revaluation-enabled";
    /** 年度结转时是否自动触发次年期间创建，默认 true。 */
    String CONFIG_AUTO_GENERATE_NEXT_YEAR_PERIODS = "erp-fin.auto-generate-next-year-periods";
    /** 辅助账跨年对账门控开关（不一致阻止年度结账），默认 true。 */
    String CONFIG_AUXILIARY_RECON_GATE_ENABLED = "erp-fin.auxiliary-recon-gate-enabled";

    /** CONFIG_BAD_DEBT_METHOD 取值：账龄分桶法（默认）。 */
    String BAD_DEBT_METHOD_AGING_BUCKET = "AGING_BUCKET";

    /** 默认损失率（各账龄区间）。 */
    java.math.BigDecimal DEFAULT_LOSS_RATE_0_30 = new java.math.BigDecimal("0.005");
    java.math.BigDecimal DEFAULT_LOSS_RATE_31_60 = new java.math.BigDecimal("0.02");
    java.math.BigDecimal DEFAULT_LOSS_RATE_61_90 = new java.math.BigDecimal("0.05");
    java.math.BigDecimal DEFAULT_LOSS_RATE_91_180 = new java.math.BigDecimal("0.15");
    java.math.BigDecimal DEFAULT_LOSS_RATE_180_PLUS = new java.math.BigDecimal("0.40");

    /** 坏账计提凭证业财回链 billHeadCode 前缀。 */
    String BAD_DEBT_RESERVE_BILL_CODE_PREFIX = "BAD-DEBT-RESERVE-";
    /** 坏账准备释放凭证业财回链 billHeadCode 前缀。 */
    String BAD_DEBT_RELEASE_BILL_CODE_PREFIX = "BAD-DEBT-RELEASE-";

    // ---- bad-debt-type（erp-fin/bad-debt-type） ----
    String BAD_DEBT_TYPE_WRITE_OFF = "WRITE_OFF";
    String BAD_DEBT_TYPE_RECOVERY = "RECOVERY";

    // ---- ar-ap-status 坏账维度扩展 ----
    String AR_AP_STATUS_WRITTEN_OFF = "WRITTEN_OFF";
}
