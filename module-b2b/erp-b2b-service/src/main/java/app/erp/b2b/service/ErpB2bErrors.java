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
    // O-11 扩展参数键
    String ARG_FIELD_NAME = "fieldName";
    String ARG_REASON = "reason";
    String ARG_PAYLOAD_SIZE = "payloadSize";
    String ARG_MAX_SIZE = "maxSize";
    String ARG_CREDENTIAL_ID = "credentialId";
    // RC-R1.36 伙伴上线状态机参数键
    String ARG_MISSING_FIELDS = "missingFields";
    String ARG_PASS_RATE = "passRate";
    String ARG_THRESHOLD = "threshold";
    String ARG_UNPASSED_ITEMS = "unpassedItems";
    String ARG_MISSING_KEY_CASES = "missingKeyCases";

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

    // ---------- O-11 扩展：解析/传输/凭证/伙伴等细粒度错误码 ----------

    ErrorCode ERR_B2B_EDI_DOC_NOT_FOUND = ErrorCode.define("erp.err.b2b.edi-doc-not-found",
            "EDI 事务 {ediDocCode} 不存在", ARG_EDI_DOC_CODE);

    ErrorCode ERR_B2B_EDI_FIELD_REQUIRED = ErrorCode.define("erp.err.b2b.edi-field-required",
            "EDI 报文必填字段 {fieldName} 缺失（{relatedBillType}/{relatedBillCode}）",
            ARG_FIELD_NAME, ARG_RELATED_BILL_TYPE, ARG_RELATED_BILL_CODE);

    ErrorCode ERR_B2B_EDI_PAYLOAD_TOO_LARGE = ErrorCode.define("erp.err.b2b.edi-payload-too-large",
            "EDI 报文大小 {payloadSize} 超过最大允许 {maxSize}",
            ARG_PAYLOAD_SIZE, ARG_MAX_SIZE);

    ErrorCode ERR_B2B_EDI_GENERATE_FAILED = ErrorCode.define("erp.err.b2b.edi-generate-failed",
            "EDI 报文生成失败：{relatedBillType}/{relatedBillCode}，原因 {reason}",
            ARG_RELATED_BILL_TYPE, ARG_RELATED_BILL_CODE, ARG_REASON);

    ErrorCode ERR_B2B_ASN_NOT_FOUND = ErrorCode.define("erp.err.b2b.asn-not-found",
            "ASN {asnCode} 不存在", ARG_ASN_CODE);

    ErrorCode ERR_B2B_ASN_LINE_QUANTITY_MISMATCH = ErrorCode.define("erp.err.b2b.asn-line-quantity-mismatch",
            "ASN {asnCode} 行数量与采购单行数量不匹配", ARG_ASN_CODE);

    ErrorCode ERR_B2B_PARTNER_NOT_FOUND = ErrorCode.define("erp.err.b2b.partner-not-found",
            "伙伴 {partnerCode} 不存在", ARG_PARTNER_CODE);

    ErrorCode ERR_B2B_PARTNER_CREDENTIAL_INVALID = ErrorCode.define("erp.err.b2b.partner-credential-invalid",
            "伙伴 {partnerCode} 凭证 {credentialId} 无效或已过期",
            ARG_PARTNER_CODE, ARG_CREDENTIAL_ID);

    ErrorCode ERR_B2B_MFT_TRANSFER_FAILED = ErrorCode.define("erp.err.b2b.mft-transfer-failed",
            "MFT 传输失败：伙伴 {partnerId} / 协议 {protocol} / 原因 {reason}",
            ARG_PARTNER_ID, ARG_PROTOCOL, ARG_REASON);

    ErrorCode ERR_B2B_CODE_MAPPING_NOT_FOUND = ErrorCode.define("erp.err.b2b.code-mapping-not-found",
            "代码映射不存在：格式 {ediFormatCode} / 代码 {relatedBillCode}",
            ARG_EDI_FORMAT_CODE, ARG_RELATED_BILL_CODE);

    String ARG_MATERIAL_ID = "materialId";
    String ARG_LINE_NO = "lineNo";

    ErrorCode ERR_B2B_ASN_LINE_MATERIAL_REQUIRED = ErrorCode.define("erp.err.b2b.asn-line-material-required",
            "ASN {asnCode} 行 {lineNo} 缺失物料或物料主数据 {materialId} 不存在，无法回填 ReceiveLine",
            ARG_ASN_CODE, ARG_LINE_NO, ARG_MATERIAL_ID);

    // ---------- RC-R1.36 伙伴上线状态机（UC-B2B-007）----------

    ErrorCode ERR_B2B_PARTNER_ILLEGAL_TRANSITION = ErrorCode.define("erp.err.b2b.partner-illegal-transition",
            "伙伴 {partnerCode} 当前状态={currentState}，不允许执行该操作（期望状态={expectedState}）",
            ARG_PARTNER_CODE, ARG_CURRENT_STATE, ARG_EXPECTED_STATE);

    ErrorCode ERR_B2B_PARTNER_PROFILE_INCOMPLETE = ErrorCode.define("erp.err.b2b.partner-profile-incomplete",
            "伙伴 {partnerCode} 基本配置不完整，缺少：{missingFields}（推进至 TESTING 前须补齐）",
            ARG_PARTNER_CODE, ARG_MISSING_FIELDS);

    ErrorCode ERR_B2B_PARTNER_PASS_RATE_NOT_MET = ErrorCode.define("erp.err.b2b.partner-pass-rate-not-met",
            "伙伴 {partnerCode} 测试通过率 {passRate} 低于门槛 {threshold}，无法推进至 CERTIFIED",
            ARG_PARTNER_CODE, ARG_PASS_RATE, ARG_THRESHOLD);

    ErrorCode ERR_B2B_PARTNER_CERTIFICATION_NOT_MET = ErrorCode.define("erp.err.b2b.partner-certification-not-met",
            "伙伴 {partnerCode} 认证清单存在未通过的必检项（{unpassedItems}），无法推进至 CERTIFIED",
            ARG_PARTNER_CODE, ARG_UNPASSED_ITEMS);
}
