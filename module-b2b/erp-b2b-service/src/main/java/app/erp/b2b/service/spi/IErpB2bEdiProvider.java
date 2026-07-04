package app.erp.b2b.service.spi;

import app.erp.b2b.service.spi.model.Acknowledgement;
import app.erp.b2b.service.spi.model.EdiApplicability;
import app.erp.b2b.service.spi.model.ParsedPayload;

/**
 * EDI 格式 Provider SPI。每个 EDI 标准格式（UBL/X12/EDIFACT/CUSTOM）实现本接口，
 * 封装该格式的报文生成（builder）与解析（decoder）双向逻辑。
 *
 * <p>对应 {@code edi-formats.md §一 Format SPI}。注册为 Bean 后由 {@link ErpB2bEdiRegistry} 按 {@link #getCode()} 聚合。
 * 中立 DTO 见 {@link app.erp.b2b.service.spi.model} 包。
 */
public interface IErpB2bEdiProvider {

    /** 格式唯一标识，对应 {@code ErpB2bEdiFormat.code}。 */
    String getCode();

    /**
     * 适用性派发（核心）：判断本格式是否处理某类业务单据。
     *
     * @param relatedBillType 关联单据类型（AR_INVOICE / SALES_ORDER / PO_ORDER / ASN_INBOUND / PURCHASE_RECEIPT）
     * @return 适用性描述（outbound/inbound/batchable）
     */
    EdiApplicability getApplicability(String relatedBillType);

    /**
     * 导出（builder）：业务单 → EDI 报文。
     *
     * @param relatedBillType 关联单据类型
     * @param relatedBillCode 关联单据编号
     * @return EDI 报文字符串（XML/EDIFACT/X12 文本）
     */
    String generatePayload(String relatedBillType, String relatedBillCode);

    /**
     * 导入（decoder）：EDI 报文 → 业务数据。
     *
     * @param formatCode 格式标识（冗余传入，便于 Provider 内部分支）
     * @param payload    EDI 报文字符串
     * @return 解析后的中立 DTO
     */
    ParsedPayload parsePayload(String formatCode, String payload);

    /** 是否需要 web service（true 走异步队列）。 */
    boolean needsWebService();

    /** 对方确认处理的回调（可选，默认空实现）。 */
    default void handleAcknowledgement(String relatedBillType, String relatedBillCode, Acknowledgement ack) {
    }
}
