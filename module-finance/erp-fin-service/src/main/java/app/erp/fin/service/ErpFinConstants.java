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
}
