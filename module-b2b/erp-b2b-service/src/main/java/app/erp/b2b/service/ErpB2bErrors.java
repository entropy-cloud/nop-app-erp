package app.erp.b2b.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * B2B/EDI 域业务异常错误码。所有 B2B 流程中的业务异常使用 {@link io.nop.api.core.exceptions.NopException} + 本接口的 {@link ErrorCode}。
 */
public interface ErpB2bErrors {

    String ARG_EDI_FORMAT_CODE = "ediFormatCode";
    String ARG_RELATED_BILL_TYPE = "relatedBillType";
    String ARG_RELATED_BILL_CODE = "relatedBillCode";
    String ARG_EDI_DOC_ID = "ediDocId";
    String ARG_EDI_DOC_CODE = "ediDocCode";
    String ARG_CURRENT_STATE = "currentState";
    String ARG_EXPECTED_STATE = "expectedState";
    String ARG_ASN_ID = "asnId";
    String ARG_ASN_CODE = "asnCode";
    String ARG_PARTNER_CODE = "partnerCode";
    String ARG_PROTOCOL = "protocol";
    String ARG_PARTNER_ID = "partnerId";
    String ARG_CONFIG_ID = "configId";
    String ARG_EVENT_ID = "eventId";

    ErrorCode ERR_B2B_EDI_FORMAT_NOT_REGISTERED = ErrorCode.define("erp.err.b2b.edi-format-not-registered",
            "EDI 格式 {ediFormatCode} 未注册任何 Provider", ARG_EDI_FORMAT_CODE);

    ErrorCode ERR_B2B_EDI_FORMAT_UNIDENTIFIED = ErrorCode.define("erp.err.b2b.edi-format-unidentified",
            "无法识别入站报文对应的 EDI 格式（所有 Provider 解析均失败）");

    ErrorCode ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION = ErrorCode.define("erp.err.b2b.edi-doc-illegal-transition",
            "EDI 事务 {ediDocCode} 当前状态={currentState}，不允许执行该操作（期望状态={expectedState}）",
            ARG_EDI_DOC_CODE, ARG_CURRENT_STATE, ARG_EXPECTED_STATE);

    ErrorCode ERR_B2B_EDI_DOC_ALREADY_PROCESSED = ErrorCode.define("erp.err.b2b.edi-doc-already-processed",
            "EDI 事务已存在：formatId+relatedBillType={relatedBillType}+relatedBillCode={relatedBillCode}（UNIQUE 防重）",
            ARG_RELATED_BILL_TYPE, ARG_RELATED_BILL_CODE);

    ErrorCode ERR_B2B_EDI_PARSE_FAILED = ErrorCode.define("erp.err.b2b.edi-parse-failed",
            "EDI 报文解析失败：{relatedBillType}/{relatedBillCode}", ARG_RELATED_BILL_TYPE, ARG_RELATED_BILL_CODE);

    ErrorCode ERR_B2B_ASN_ILLEGAL_TRANSITION = ErrorCode.define("erp.err.b2b.asn-illegal-transition",
            "ASN {asnCode} 当前状态={currentState}，不允许执行该操作（期望状态={expectedState}）",
            ARG_ASN_CODE, ARG_CURRENT_STATE, ARG_EXPECTED_STATE);

    ErrorCode ERR_B2B_WEBHOOK_SIGNATURE_INVALID = ErrorCode.define("erp.err.b2b.webhook-signature-invalid",
            "伙伴 {partnerCode} 回调签名校验失败", ARG_PARTNER_CODE);

    ErrorCode ERR_B2B_WEBHOOK_DUPLICATE_EVENT = ErrorCode.define("erp.err.b2b.webhook-duplicate-event",
            "重复的 webhook 事件：eventId={eventId} formatCode={ediFormatCode}", ARG_EVENT_ID, ARG_EDI_FORMAT_CODE);

    ErrorCode ERR_B2B_MFT_CONFIG_MISSING = ErrorCode.define("erp.err.b2b.mft-config-missing",
            "伙伴 {partnerId} 缺少启用的 MFT 配置（ErpB2bMftConfig）", ARG_PARTNER_ID);

    ErrorCode ERR_B2B_MFT_ADAPTER_NOT_REGISTERED = ErrorCode.define("erp.err.b2b.mft-adapter-not-registered",
            "协议 {protocol} 未注册任何传输适配器", ARG_PROTOCOL);
}
