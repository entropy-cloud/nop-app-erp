package app.erp.inv.biz;

import java.time.LocalDate;
import java.util.List;

/**
 * 库存预留单创建请求（{@link IErpInvReservationBiz#createReservation} 入参）。
 *
 * <p>跨域调用方（mfg 工单审核 Processor）经本请求创建预留头 + 行。行预留量 = min(requested, 可用量)
 * 由库存侧计算（调用方无需预读余额）；库存余额 reservedQuantity 同步增加。
 */
public class ReservationCreateRequest {
    private Long orgId;
    private LocalDate businessDate;
    private String sourceBillType;
    private String sourceBillCode;
    private Long reservedForPartnerId;
    private String remark;
    private List<ReservationLineRequest> lines;

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public String getSourceBillType() {
        return sourceBillType;
    }

    public void setSourceBillType(String sourceBillType) {
        this.sourceBillType = sourceBillType;
    }

    public String getSourceBillCode() {
        return sourceBillCode;
    }

    public void setSourceBillCode(String sourceBillCode) {
        this.sourceBillCode = sourceBillCode;
    }

    public Long getReservedForPartnerId() {
        return reservedForPartnerId;
    }

    public void setReservedForPartnerId(Long reservedForPartnerId) {
        this.reservedForPartnerId = reservedForPartnerId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<ReservationLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<ReservationLineRequest> lines) {
        this.lines = lines;
    }
}
