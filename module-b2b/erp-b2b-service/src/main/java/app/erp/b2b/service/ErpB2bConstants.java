package app.erp.b2b.service;

/**
 * B2B/EDI 域常量。EDI 文档状态、ASN 状态、方向、阻断级别、关联单据类型、EDI 动作语义编码等。
 */
public interface ErpB2bConstants {

    // ---- EDI 文档状态（erp-b2b/edi-doc-state）----
    String EDI_DOC_STATE_TO_SEND = "TO_SEND";
    String EDI_DOC_STATE_SENT = "SENT";
    String EDI_DOC_STATE_TO_CANCEL = "TO_CANCEL";
    String EDI_DOC_STATE_CANCELLED = "CANCELLED";
    String EDI_DOC_STATE_ERROR = "ERROR";
    String EDI_DOC_STATE_RECEIVED = "RECEIVED";
    String EDI_DOC_STATE_ACKNOWLEDGED = "ACKNOWLEDGED";
    String EDI_DOC_STATE_ARCHIVED = "ARCHIVED";

    // ---- ASN 状态（erp-b2b/asn-status）----
    String ASN_STATUS_RECEIVED = "RECEIVED";
    String ASN_STATUS_MATCHED = "MATCHED";
    String ASN_STATUS_RECEIVED_TO_STOCK = "RECEIVED_TO_STOCK";
    String ASN_STATUS_CANCELLED = "CANCELLED";

    // ---- EDI 方向（erp-b2b/edi-direction）----
    String DIRECTION_OUTBOUND = "OUTBOUND";
    String DIRECTION_INBOUND = "INBOUND";

    // ---- 阻断级别（erp-b2b/blocking-level）----
    String BLOCKING_LEVEL_INFO = "INFO";
    String BLOCKING_LEVEL_WARN = "WARN";
    String BLOCKING_LEVEL_ERROR = "ERROR";

    // ---- 关联单据类型 ----
    String RELATED_BILL_TYPE_AR_INVOICE = "AR_INVOICE";
    String RELATED_BILL_TYPE_SALES_ORDER = "SALES_ORDER";
    String RELATED_BILL_TYPE_PO_ORDER = "PO_ORDER";
    String RELATED_BILL_TYPE_ASN_INBOUND = "ASN_INBOUND";
    String RELATED_BILL_TYPE_PURCHASE_RECEIPT = "PURCHASE_RECEIPT";
    String RELATED_BILL_TYPE_B2B_ASN = "B2B_ASN";

    // ---- 代码映射类型（erp-b2b/mapping-type）----
    String MAPPING_TYPE_MATERIAL = "MATERIAL";
    String MAPPING_TYPE_PARTNER = "PARTNER";
    String MAPPING_TYPE_UOM = "UOM";

    // ---- EDI 结果码（ErpB2bEdiLog.resultCode）----
    String EDI_RESULT_SUCCESS = "SUCCESS";
    String EDI_RESULT_ERROR = "ERROR";

    // ---- MFT 状态（erp-b2b/mft-status）----
    String MFT_STATUS_PENDING = "PENDING";
    String MFT_STATUS_SENT = "SENT";
    String MFT_STATUS_RECEIVED = "RECEIVED";
    String MFT_STATUS_FAILED = "FAILED";
    String MFT_STATUS_RETRYING = "RETRYING";
    String MFT_STATUS_DEAD_LETTER = "DEAD_LETTER";

    // ---- EDI Provider 格式码 ----
    String EDI_FORMAT_UBL_DESPATCH_ADVICE = "UBL_DESPATCH_ADVICE";
    String EDI_FORMAT_UBL_INVOICE = "UBL_INVOICE";

    // ---- 传输协议 ----
    String MFT_PROTOCOL_HTTPS = "HTTPS";

    // ---- UBL XML 常量 ----
    String UBL_NS_DESPATCH_ADVICE = "urn:oasis:names:specification:ubl:schema:xsd:DespatchAdvice-2";
    String UBL_NS_INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    String UBL_NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    String UBL_NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
}
