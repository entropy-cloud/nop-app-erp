package app.erp.fin.service.classify;

import io.nop.api.core.annotations.data.DataBean;

import java.math.BigDecimal;

/**
 * E3.5 AP 文档分类结果（`document-driven-ap-automation.md` §2：规则结果附置信度，低置信挂人工队列）。
 */
@DataBean
public class ApDocClassification {

    /** 单据类型（erp-fin/ap-doc-type：VAT_INVOICE/GENERAL_INVOICE/RECEIPT/OTHER）。 */
    private String docType;

    /** 匹配到的对应方（ErpMdPartner id；未匹配为 null）。 */
    private String partnerId;

    /** 置信度 [0,1]：低于阈值挂人工复核。 */
    private BigDecimal confidence;

    /** 分类依据说明（审计用）。 */
    private String reason;

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
