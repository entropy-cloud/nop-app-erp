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

    // ---- sourceBillType（ErpFinArApItem.sourceBillType 字符串值） ----
    String SOURCE_BILL_AP_INVOICE = "AP_INVOICE";
    String SOURCE_BILL_AR_INVOICE = "AR_INVOICE";
    String SOURCE_BILL_PAYMENT = "PAYMENT";
    String SOURCE_BILL_RECEIPT = "RECEIPT";
    String SOURCE_BILL_PUR_RETURN = "PUR_RETURN";
}
