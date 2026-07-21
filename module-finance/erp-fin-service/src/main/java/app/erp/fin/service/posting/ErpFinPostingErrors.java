package app.erp.fin.service.posting;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 业财过账引擎错误码。所有过账流程中的业务异常使用 {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 */
public interface ErpFinPostingErrors {

    String ARG_BUSINESS_TYPE = "businessType";
    String ARG_BILL_HEAD_CODE = "billHeadCode";
    String ARG_SUBJECT_CODE = "subjectCode";
    String ARG_AMOUNT_KEY = "amountKey";
    String ARG_PROVIDER = "provider";
    String ARG_EXISTING_PROVIDER = "existingProvider";
    String ARG_CONFLICT_PROVIDER = "conflictProvider";
    String ARG_VOUCHER_DATE = "voucherDate";
    String ARG_PERIOD_STATUS = "periodStatus";
    String ARG_TOTAL_DEBIT = "totalDebit";
    String ARG_TOTAL_CREDIT = "totalCredit";

    ErrorCode ERR_DUPLICATE_PROVIDER = ErrorCode.define("erp.err.fin.posting.duplicate-provider",
            "业务类型 {businessType} 被多个非默认 Provider 声明：{existingProvider} 与 {conflictProvider}",
            ARG_BUSINESS_TYPE, ARG_EXISTING_PROVIDER, ARG_CONFLICT_PROVIDER);

    ErrorCode ERR_NO_PROVIDER = ErrorCode.define("erp.err.fin.posting.no-provider",
            "未找到业务类型 {businessType} 的过账 Provider", ARG_BUSINESS_TYPE);

    ErrorCode ERR_TEMPLATE_NOT_FOUND = ErrorCode.define("erp.err.fin.posting.template-not-found",
            "未找到业务类型 {businessType} 的启用凭证模板", ARG_BUSINESS_TYPE);

    ErrorCode ERR_SUBJECT_NOT_FOUND = ErrorCode.define("erp.err.fin.posting.subject-not-found",
            "科目编码 {subjectCode} 不存在", ARG_SUBJECT_CODE);

    ErrorCode ERR_AMOUNT_KEY_NOT_RESOLVED = ErrorCode.define("erp.err.fin.posting.amount-key-not-resolved",
            "模板金额占位键 {amountKey} 在单据数据中无对应值", ARG_AMOUNT_KEY);

    ErrorCode ERR_PERIOD_NOT_FOUND = ErrorCode.define("erp.err.fin.posting.period-not-found",
            "凭证日期 {voucherDate} 未匹配到会计期间", ARG_VOUCHER_DATE);

    ErrorCode ERR_PERIOD_CLOSED = ErrorCode.define("erp.err.fin.posting.period-closed",
            "会计期间未开启，不允许过账（期间状态={periodStatus}）", ARG_PERIOD_STATUS);

    ErrorCode ERR_UNBALANCED = ErrorCode.define("erp.err.fin.posting.unbalanced",
            "借贷不平衡：借方合计={totalDebit}，贷方合计={totalCredit}", ARG_TOTAL_DEBIT, ARG_TOTAL_CREDIT);

    ErrorCode ERR_REVERSE_SOURCE_NOT_FOUND = ErrorCode.define("erp.err.fin.posting.reverse-source-not-found",
            "未找到业务单据 {billHeadCode}（业务类型 {businessType}）的已过账凭证，无法红冲",
            ARG_BILL_HEAD_CODE, ARG_BUSINESS_TYPE);

    // ===================== 过账异常工作台（异常处置状态机守门） =====================

    String ARG_EXCEPTION_ID = "exceptionId";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_RESOLUTION = "resolution";
    String ARG_LISTENER = "listener";

    ErrorCode ERR_POSTING_EXCEPTION_NOT_PENDING = ErrorCode.define("erp.err.fin.posting-exception.not-pending",
            "过账异常 {exceptionId} 当前状态为 {currentStatus}，仅 PENDING 可处置", ARG_EXCEPTION_ID, ARG_CURRENT_STATUS);

    ErrorCode ERR_POSTING_EXCEPTION_IGNORE_REASON_REQUIRED = ErrorCode.define(
            "erp.err.fin.posting-exception.ignore-reason-required",
            "忽略过账异常 {exceptionId} 须填写处置说明（resolutionNote）", ARG_EXCEPTION_ID);

    ErrorCode ERR_POSTING_EXCEPTION_MANUAL_VOUCHER_REQUIRED = ErrorCode.define(
            "erp.err.fin.posting-exception.manual-voucher-required",
            "手工补录过账异常 {exceptionId} 须关联已过账凭证（voucherId）", ARG_EXCEPTION_ID);

    ErrorCode ERR_POSTING_EXCEPTION_NOT_FOUND = ErrorCode.define("erp.err.fin.posting-exception.not-found",
            "未找到过账异常记录 {exceptionId}", ARG_EXCEPTION_ID);

    // ===================== 冲销反写闭环（凭证红冲→业务单据回退） =====================

    ErrorCode ERR_REVERSAL_LISTENER_FAILED = ErrorCode.define("erp.err.fin.posting.reversal-listener-failed",
            "凭证红冲监听者 {listener} 回退业务单据 {billHeadCode} 失败（业务类型 {businessType}）",
            ARG_LISTENER, ARG_BILL_HEAD_CODE, ARG_BUSINESS_TYPE);

    // ===================== 兜底扫描与未知异常（O-2/O-6/O-16） =====================

    /** 非 NopException 的未知异常统一记录到过账异常工作台（O-6：recordPostFailure 不再丢弃非 NopException）。 */
    ErrorCode ERR_POSTING_UNEXPECTED_FAILURE = ErrorCode.define("erp.err.fin.posting.unexpected-failure",
            "过账过程抛出非业务异常（未预期失败），stage={failedStage}，message={errorMessage}",
            "failedStage", "errorMessage");

    /** 红冲监听者注册中心启动期校验失败：未注册任何监听者（O-19）。 */
    ErrorCode ERR_POSTING_NO_LISTENERS_REGISTERED = ErrorCode.define("erp.err.fin.posting.no-listeners-registered",
            "凭证红冲监听者注册中心启动时未发现任何监听者实现，红冲反写闭环不可用");

    // ===================== GL 映射规则（A1：科目映射规则表 + 解析引擎） =====================

    String ARG_ACCOUNT_KEY = "accountKey";
    String ARG_DIMENSIONS = "dimensions";

    /**
     * GL 映射规则缺失：strict-mode（{@code erp-fin.gl-mapping.strict-mode=true}）下，{@code VoucherFact.accountKey}
     * 已设置但规则表无对应 {@code businessType+accountKey} 任何配置时抛出。
     * 默认 strict-mode=false（试点期保留 Provider 既有 fallback 行为）。
     * 权威：{@code docs/design/finance/gl-mapping-rules.md §5.1}。
     */
    ErrorCode ERR_GL_MAPPING_NOT_FOUND = ErrorCode.define("erp.err.fin.posting.gl-mapping-not-found",
            "GL 映射规则缺失: businessType={businessType} accountKey={accountKey} dimensions={dimensions}",
            ARG_BUSINESS_TYPE, ARG_ACCOUNT_KEY, ARG_DIMENSIONS);
}
