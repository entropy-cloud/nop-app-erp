package app.erp.log.service.event;

/**
 * 发运单签收事件（path-2 采购运费交接占位）。
 *
 * <p>DELIVERED 时若 {@code relatedBillType=PURCHASE_RECEIPT}，logistics 发布本事件作为到岸成本（Landed Cost）
 * 分摊的交接占位。实际的分摊/成本调整/差异凭证依赖 finance Landed Cost 能力
 * （{@code docs/design/finance/costing-methods.md:40} Deferred Non-Goal），本期仅事件交接。
 *
 * <p>事件总线订阅（finance Landed Cost 落地后）归 follow-up；当前发布即记录交接意图 + 日志。
 */
public class ShipmentDeliveredEvent {
    private final Long shipmentId;
    private final String shipmentCode;
    private final String relatedBillType;
    private final String relatedBillCode;
    private final Long carrierId;

    public ShipmentDeliveredEvent(Long shipmentId, String shipmentCode, String relatedBillType,
                                  String relatedBillCode, Long carrierId) {
        this.shipmentId = shipmentId;
        this.shipmentCode = shipmentCode;
        this.relatedBillType = relatedBillType;
        this.relatedBillCode = relatedBillCode;
        this.carrierId = carrierId;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public String getShipmentCode() {
        return shipmentCode;
    }

    public String getRelatedBillType() {
        return relatedBillType;
    }

    public String getRelatedBillCode() {
        return relatedBillCode;
    }

    public Long getCarrierId() {
        return carrierId;
    }
}
