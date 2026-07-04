package app.erp.b2b.service.spi.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * EDI 报文解析后的中立 DTO（入站方向）。
 *
 * <p>对应 {@code edi-formats.md §1.3}。Provider.parsePayload() 返回本对象，
 * 由 ASN 入站处理 / 信封状态机消费，与具体 EDI 标准解耦。
 */
public class ParsedPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ediFormatCode;
    private String relatedBillType;
    private String relatedBillCode;
    private String partnerCode;
    private java.util.List<ParsedLine> lines = new java.util.ArrayList<>();
    private Map<String, Object> headers = new HashMap<>();
    private String rawPayload;

    public String getEdiFormatCode() {
        return ediFormatCode;
    }

    public void setEdiFormatCode(String ediFormatCode) {
        this.ediFormatCode = ediFormatCode;
    }

    public String getRelatedBillType() {
        return relatedBillType;
    }

    public void setRelatedBillType(String relatedBillType) {
        this.relatedBillType = relatedBillType;
    }

    public String getRelatedBillCode() {
        return relatedBillCode;
    }

    public void setRelatedBillCode(String relatedBillCode) {
        this.relatedBillCode = relatedBillCode;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public java.util.List<ParsedLine> getLines() {
        return lines;
    }

    public void setLines(java.util.List<ParsedLine> lines) {
        this.lines = lines != null ? lines : new java.util.ArrayList<>();
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers != null ? headers : new HashMap<>();
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public static class ParsedLine implements Serializable {

        private static final long serialVersionUID = 1L;

        private String lineNo;
        private String supplierPartNo;
        private String materialCode;
        private BigDecimal quantity;
        private BigDecimal shippedQty;
        private BigDecimal price;
        private String unit;
        private Map<String, Object> extensions = new HashMap<>();

        public String getLineNo() {
            return lineNo;
        }

        public void setLineNo(String lineNo) {
            this.lineNo = lineNo;
        }

        public String getSupplierPartNo() {
            return supplierPartNo;
        }

        public void setSupplierPartNo(String supplierPartNo) {
            this.supplierPartNo = supplierPartNo;
        }

        public String getMaterialCode() {
            return materialCode;
        }

        public void setMaterialCode(String materialCode) {
            this.materialCode = materialCode;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getShippedQty() {
            return shippedQty;
        }

        public void setShippedQty(BigDecimal shippedQty) {
            this.shippedQty = shippedQty;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Map<String, Object> getExtensions() {
            return extensions;
        }

        public void setExtensions(Map<String, Object> extensions) {
            this.extensions = extensions != null ? extensions : new HashMap<>();
        }
    }
}
