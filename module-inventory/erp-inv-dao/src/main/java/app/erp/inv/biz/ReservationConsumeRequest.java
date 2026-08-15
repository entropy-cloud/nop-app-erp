package app.erp.inv.biz;

import java.util.List;

/**
 * 预留消耗请求（{@link IErpInvReservationBiz#consumeReservation} 入参）。
 *
 * <p>按 {@code sourceBillType}+{@code sourceBillCode} 定位预留头（查无 → 静默 no-op 返回 null，
 * 既有无预留工单零回归）。行维度消耗：consumedQuantity += 实耗、reservedQuantity −= 实耗、
 * 库存余额 reservedQuantity −= 实耗；超出未消耗量的部分按 min 语义封顶（不产生负预留）。
 */
public class ReservationConsumeRequest {
    private String sourceBillType;
    private String sourceBillCode;
    private List<ReservationConsumeLine> lines;

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

    public List<ReservationConsumeLine> getLines() {
        return lines;
    }

    public void setLines(List<ReservationConsumeLine> lines) {
        this.lines = lines;
    }
}
