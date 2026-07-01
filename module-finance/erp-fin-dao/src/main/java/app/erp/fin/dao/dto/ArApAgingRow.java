package app.erp.fin.dao.dto;

import java.math.BigDecimal;

/**
 * 账龄分析结果行（按往来单位聚合，{@code ar-ap-reconciliation.md §账龄分析}）。
 * 各区间值为该 partner 未核销辅助账本位币 openAmount 落入该账龄区间的合计。
 */
public class ArApAgingRow {
    private Long partnerId;
    private BigDecimal bucket030 = BigDecimal.ZERO;
    private BigDecimal bucket3160 = BigDecimal.ZERO;
    private BigDecimal bucket6190 = BigDecimal.ZERO;
    private BigDecimal bucket91180 = BigDecimal.ZERO;
    private BigDecimal bucket180Plus = BigDecimal.ZERO;
    private BigDecimal totalOpen = BigDecimal.ZERO;

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public BigDecimal getBucket030() {
        return bucket030;
    }

    public void setBucket030(BigDecimal bucket030) {
        this.bucket030 = bucket030;
    }

    public BigDecimal getBucket3160() {
        return bucket3160;
    }

    public void setBucket3160(BigDecimal bucket3160) {
        this.bucket3160 = bucket3160;
    }

    public BigDecimal getBucket6190() {
        return bucket6190;
    }

    public void setBucket6190(BigDecimal bucket6190) {
        this.bucket6190 = bucket6190;
    }

    public BigDecimal getBucket91180() {
        return bucket91180;
    }

    public void setBucket91180(BigDecimal bucket91180) {
        this.bucket91180 = bucket91180;
    }

    public BigDecimal getBucket180Plus() {
        return bucket180Plus;
    }

    public void setBucket180Plus(BigDecimal bucket180Plus) {
        this.bucket180Plus = bucket180Plus;
    }

    public BigDecimal getTotalOpen() {
        return totalOpen;
    }

    public void setTotalOpen(BigDecimal totalOpen) {
        this.totalOpen = totalOpen;
    }
}
