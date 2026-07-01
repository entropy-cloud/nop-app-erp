package app.erp.fin.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 财务域业务错误码。应收应付辅助账生成与核销流程中的业务异常使用
 * {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 */
public interface ErpFinErrors {

    String ARG_SOURCE_BILL_CODE = "sourceBillCode";
    String ARG_SOURCE_BILL_TYPE = "sourceBillType";
    String ARG_PARTNER_ID = "partnerId";
    String ARG_DIRECTION = "direction";
    String ARG_RECONCILIATION_ID = "reconciliationId";
    String ARG_PAYMENT_ITEM_ID = "paymentItemId";
    String ARG_INVOICE_ITEM_ID = "invoiceItemId";
    String ARG_SETTLE_AMOUNT = "settleAmount";
    String ARG_OPEN_AMOUNT = "openAmount";
    String ARG_RECON_DATE = "reconDate";
    String ARG_INVOICE_DATE = "invoiceDate";
    String ARG_DOC_STATUS = "docStatus";
    String ARG_ID = "id";

    ErrorCode ERR_AR_AP_ITEM_PARTNER_MISSING = ErrorCode.define("erp.err.fin.ar-ap.partner-missing",
            "来源单据 {sourceBillCode}（类型 {sourceBillType}）的 billData 缺少 partnerId，无法生成辅助账",
            ARG_SOURCE_BILL_CODE, ARG_SOURCE_BILL_TYPE);

    ErrorCode ERR_AR_AP_ITEM_AMOUNT_MISSING = ErrorCode.define("erp.err.fin.ar-ap.amount-missing",
            "来源单据 {sourceBillCode}（类型 {sourceBillType}）的 billData 缺少金额，无法生成辅助账",
            ARG_SOURCE_BILL_CODE, ARG_SOURCE_BILL_TYPE);

    ErrorCode ERR_RECONCILIATION_NOT_FOUND = ErrorCode.define("erp.err.fin.reconciliation.not-found",
            "核销单 {reconciliationId} 不存在", ARG_RECONCILIATION_ID);

    ErrorCode ERR_RECONCILIATION_STATUS_INVALID = ErrorCode.define("erp.err.fin.reconciliation.status-invalid",
            "核销单 {reconciliationId} 当前状态({docStatus})不允许此操作", ARG_RECONCILIATION_ID, ARG_DOC_STATUS);

    ErrorCode ERR_RECONCILIATION_DIRECTION_MISMATCH = ErrorCode.define("erp.err.fin.reconciliation.direction-mismatch",
            "核销行双方应收应付方向不一致（核销单方向={direction}）", ARG_DIRECTION);

    ErrorCode ERR_RECONCILIATION_PARTNER_MISMATCH = ErrorCode.define("erp.err.fin.reconciliation.partner-mismatch",
            "核销项 {paymentItemId} 与发票项 {invoiceItemId} 往来单位不一致",
            ARG_PAYMENT_ITEM_ID, ARG_INVOICE_ITEM_ID);

    ErrorCode ERR_RECONCILIATION_ITEM_NOT_OPEN = ErrorCode.define("erp.err.fin.reconciliation.item-not-open",
            "核销项 {paymentItemId} 已结清/作废，不可再核销", ARG_PAYMENT_ITEM_ID);

    ErrorCode ERR_RECONCILIATION_OVER_AMOUNT = ErrorCode.define("erp.err.fin.reconciliation.over-amount",
            "核销金额 {settleAmount} 超过未核销余额 {openAmount}", ARG_SETTLE_AMOUNT, ARG_OPEN_AMOUNT);

    ErrorCode ERR_RECONCILIATION_DATE_BEFORE_INVOICE = ErrorCode.define("erp.err.fin.reconciliation.date-before-invoice",
            "核销日期 {reconDate} 早于发票业务日期 {invoiceDate}", ARG_RECON_DATE, ARG_INVOICE_DATE);

    ErrorCode ERR_AR_AP_ITEM_NOT_FOUND = ErrorCode.define("erp.err.fin.ar-ap.not-found",
            "辅助账项 {id} 不存在", ARG_ID);
}
