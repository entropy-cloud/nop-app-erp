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

    // --- 报销单 / 借款单作用域参数键 ---
    String ARG_CLAIM_CODE = "claimCode";
    String ARG_CLAIM_ID = "claimId";
    String ARG_ADVANCE_CODE = "advanceCode";
    String ARG_ADVANCE_ID = "advanceId";
    String ARG_CLAIMANT_ID = "claimantId";
    String ARG_EMPLOYEE_ID = "employeeId";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";
    String ARG_CURRENT_DOC_STATUS = "currentDocStatus";
    String ARG_EXPECTED_DOC_STATUS = "expectedDocStatus";
    String ARG_LINE_NO = "lineNo";
    String ARG_EXPENSE_TYPE = "expenseType";
    String ARG_AMOUNT_WITH_TAX = "amountWithTax";
    String ARG_LINE_TOTAL = "lineTotal";
    String ARG_ADVANCE_TYPE = "advanceType";

    // --- 票据/资金作用域参数键 ---
    String ARG_NOTES_CODE = "notesCode";
    String ARG_NOTES_ID = "notesId";
    String ARG_CREDIT_FACILITY_ID = "creditFacilityId";
    String ARG_AVAILABLE_AMOUNT = "availableAmount";
    String ARG_FACE_AMOUNT = "faceAmount";
    String ARG_DISCOUNT_RATE = "discountRate";
    String ARG_CONFIG_KEY = "configKey";

    // --- 期末结账作用域参数键 ---
    String ARG_PERIOD_ID = "periodId";
    String ARG_PERIOD_CODE = "periodCode";
    String ARG_MODULE = "module";
    String ARG_PREV_MODULE = "prevModule";
    String ARG_CURRENT_PERIOD_STATUS = "currentPeriodStatus";
    String ARG_EXPECTED_PERIOD_STATUS = "expectedPeriodStatus";
    String ARG_ISSUE_COUNT = "issueCount";

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

    // --- 报销单作用域 ---

    ErrorCode ERR_EXPENSE_CLAIM_NOT_FOUND = ErrorCode.define("erp.err.fin.expense-claim.not-found",
            "费用报销单 {claimId} 不存在", ARG_CLAIM_ID);

    ErrorCode ERR_EXPENSE_CLAIM_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.fin.expense-claim.illegal-status-transition",
            "费用报销单 {claimCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_CLAIM_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_EXPENSE_CLAIM_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.fin.expense-claim.illegal-doc-status-transition",
            "费用报销单 {claimCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_CLAIM_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_EXPENSE_CLAIM_LINES_EMPTY = ErrorCode.define("erp.err.fin.expense-claim.lines-empty",
            "费用报销单 {claimCode} 无行明细，不可提交审核", ARG_CLAIM_CODE);

    ErrorCode ERR_EXPENSE_CLAIM_CLAIMANT_INACTIVE = ErrorCode.define("erp.err.fin.expense-claim.claimant-inactive",
            "报销人 {claimantId} 已停用，不可提交或审核", ARG_CLAIMANT_ID);

    ErrorCode ERR_EXPENSE_CLAIM_CLAIMANT_PARTNER_MISSING = ErrorCode.define("erp.err.fin.expense-claim.claimant-partner-missing",
            "报销人 {claimantId} 未配置内部往来单位（partnerId 为空），无法生成员工应付辅助账",
            ARG_CLAIMANT_ID);

    ErrorCode ERR_EXPENSE_CLAIM_AMOUNT_MISMATCH = ErrorCode.define("erp.err.fin.expense-claim.amount-mismatch",
            "费用报销单 {claimCode} 价税合计 {amountWithTax} 不等于行明细合计 {lineTotal}",
            ARG_CLAIM_CODE, ARG_AMOUNT_WITH_TAX, ARG_LINE_TOTAL);

    ErrorCode ERR_EXPENSE_CLAIM_REASON_REQUIRED = ErrorCode.define("erp.err.fin.expense-claim.reason-required",
            "费用报销单 {claimCode} 缺少报销事由（按配置必填）", ARG_CLAIM_CODE);

    ErrorCode ERR_EXPENSE_CLAIM_EXPENSE_TYPE_REQUIRED = ErrorCode.define("erp.err.fin.expense-claim.expense-type-required",
            "费用报销单 {claimCode} 第 {lineNo} 行缺少费用类型（按配置必填）",
            ARG_CLAIM_CODE, ARG_LINE_NO);

    ErrorCode ERR_EXPENSE_CLAIM_NOT_REVERSED_BEFORE_CANCEL = ErrorCode.define("erp.err.fin.expense-claim.not-reversed-before-cancel",
            "费用报销单 {claimCode} 已过账，须先反审核（红字冲销）再作废", ARG_CLAIM_CODE);

    // --- 借款单作用域 ---

    ErrorCode ERR_EMPLOYEE_ADVANCE_NOT_FOUND = ErrorCode.define("erp.err.fin.employee-advance.not-found",
            "员工借款单 {advanceId} 不存在", ARG_ADVANCE_ID);

    ErrorCode ERR_EMPLOYEE_ADVANCE_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.fin.employee-advance.illegal-status-transition",
            "员工借款单 {advanceCode} 当前审核状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_ADVANCE_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_EMPLOYEE_ADVANCE_ILLEGAL_DOC_STATUS_TRANSITION = ErrorCode.define("erp.err.fin.employee-advance.illegal-doc-status-transition",
            "员工借款单 {advanceCode} 当前单据状态={currentDocStatus}，不允许执行该操作（期望状态={expectedDocStatus}）",
            ARG_ADVANCE_CODE, ARG_CURRENT_DOC_STATUS, ARG_EXPECTED_DOC_STATUS);

    ErrorCode ERR_EMPLOYEE_ADVANCE_EMPLOYEE_INACTIVE = ErrorCode.define("erp.err.fin.employee-advance.employee-inactive",
            "借款人 {employeeId} 已停用，不可提交或审核", ARG_EMPLOYEE_ID);

    ErrorCode ERR_EMPLOYEE_ADVANCE_EMPLOYEE_PARTNER_MISSING = ErrorCode.define("erp.err.fin.employee-advance.employee-partner-missing",
            "借款人 {employeeId} 未配置内部往来单位（partnerId 为空），无法生成员工预支应收辅助账",
            ARG_EMPLOYEE_ID);

    ErrorCode ERR_EMPLOYEE_ADVANCE_AMOUNT_INVALID = ErrorCode.define("erp.err.fin.employee-advance.amount-invalid",
            "员工借款单 {advanceCode} 借款金额必须大于 0", ARG_ADVANCE_CODE);

    ErrorCode ERR_EMPLOYEE_ADVANCE_NOT_REVERSED_BEFORE_CANCEL = ErrorCode.define("erp.err.fin.employee-advance.not-reversed-before-cancel",
            "员工借款单 {advanceCode} 已过账，须先反审核（红字冲销）再作废", ARG_ADVANCE_CODE);

    // --- 票据（应收/应付）作用域 ---

    ErrorCode ERR_NOTES_RECEIVABLE_NOT_FOUND = ErrorCode.define("erp.err.fin.notes-receivable.not-found",
            "应收票据 {notesId} 不存在", ARG_NOTES_ID);

    ErrorCode ERR_NOTES_RECEIVABLE_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.fin.notes-receivable.illegal-status-transition",
            "应收票据 {notesCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_NOTES_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_NOTES_PAYABLE_NOT_FOUND = ErrorCode.define("erp.err.fin.notes-payable.not-found",
            "应付票据 {notesId} 不存在", ARG_NOTES_ID);

    ErrorCode ERR_NOTES_PAYABLE_ILLEGAL_STATUS_TRANSITION = ErrorCode.define("erp.err.fin.notes-payable.illegal-status-transition",
            "应付票据 {notesCode} 当前状态={currentStatus}，不允许执行该操作（期望状态={expectedStatus}）",
            ARG_NOTES_CODE, ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS);

    ErrorCode ERR_NOTES_AMOUNT_INVALID = ErrorCode.define("erp.err.fin.notes.amount-invalid",
            "票据 {notesCode} 票面金额必须大于 0", ARG_NOTES_CODE);

    // --- 授信额度作用域 ---

    ErrorCode ERR_CREDIT_FACILITY_NOT_FOUND = ErrorCode.define("erp.err.fin.credit-facility.not-found",
            "授信额度 {creditFacilityId} 不存在", ARG_CREDIT_FACILITY_ID);

    ErrorCode ERR_CREDIT_FACILITY_INSUFFICIENT = ErrorCode.define("erp.err.fin.credit-facility.insufficient",
            "授信额度 {creditFacilityId} 可用额度 {availableAmount} 不足，无法开出银承票面 {faceAmount}",
            ARG_CREDIT_FACILITY_ID, ARG_AVAILABLE_AMOUNT, ARG_FACE_AMOUNT);

    // --- 期末结账作用域 ---

    ErrorCode ERR_PERIOD_NOT_FOUND = ErrorCode.define("erp.err.fin.period-close.period-not-found",
            "会计期间 {periodId} 不存在", ARG_PERIOD_ID);

    ErrorCode ERR_PERIOD_ILLEGAL_TRANSITION = ErrorCode.define("erp.err.fin.period-close.illegal-transition",
            "会计期间 {periodCode} 当前状态={currentPeriodStatus}，不允许执行该操作（期望状态={expectedPeriodStatus}）",
            ARG_PERIOD_CODE, ARG_CURRENT_PERIOD_STATUS, ARG_EXPECTED_PERIOD_STATUS);

    ErrorCode ERR_MODULE_OUT_OF_ORDER = ErrorCode.define("erp.err.fin.period-close.module-out-of-order",
            "模块 {module} 关账前置未满足：上一模块 {prevModule} 尚未关账",
            ARG_MODULE, ARG_PREV_MODULE);

    ErrorCode ERR_PRE_CHECK_BLOCKED = ErrorCode.define("erp.err.fin.period-close.pre-check-blocked",
            "会计期间 {periodCode} 前置检查未通过（共 {issueCount} 项问题），阻止结账",
            ARG_PERIOD_CODE, ARG_ISSUE_COUNT);

    ErrorCode ERR_REVERSE_CLOSE_APPROVAL_REQUIRED = ErrorCode.define("erp.err.fin.period-close.reverse-approval-required",
            "会计期间 {periodCode} 反结账需审批（配置 erp-fin.reverse-close-approval-required=true）",
            ARG_PERIOD_CODE);

    ErrorCode ERR_CLOSE_SUBJECT_NOT_CONFIGURED = ErrorCode.define("erp.err.fin.period-close.subject-not-configured",
            "期末结账所需科目/汇率未配置：配置键 {configKey}",
            ARG_CONFIG_KEY);
}
