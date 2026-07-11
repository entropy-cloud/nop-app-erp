package app.erp.log.service.event;

import java.math.BigDecimal;

/**
 * 发运单签收事件（path-2 采购运费交接记录）。
 *
 * <p>DELIVERED 时若 {@code relatedBillType=PURCHASE_RECEIPT}，logistics 发布本事件作为到岸成本（Landed Cost）
 * 自动编排的交接记录。path-2 现已从事件占位升级为 config-gated Landed Cost 自动创建
 * （{@code erp-log.path2-landed-cost-auto-create}，plan 2026-07-11-2329-1）。
 *
 * <p>事件保持不派发（无事件总线），仅作为结构化交接意图日志记录。实际编排由
 * {@code ErpLogShipmentBizModel.onDelivered} 直接调用 {@code IErpInvLandedCostBiz.generateFreightLandedCost} 完成。
 */
public class ShipmentDeliveredEvent {
    private final Long shipmentId;
    private final String shipmentCode;
    private final String relatedBillType;
    private final String relatedBillCode;
    private final Long carrierId;
    private final BigDecimal freightAmount;
    private final Long freightCurrencyId;

    public ShipmentDeliveredEvent(Long shipmentId, String shipmentCode, String relatedBillType,
                                  String relatedBillCode, Long carrierId) {
        this(shipmentId, shipmentCode, relatedBillType, relatedBillCode, carrierId, null, null);
    }

    public ShipmentDeliveredEvent(Long shipmentId, String shipmentCode, String relatedBillType,
                                  String relatedBillCode, Long carrierId,
                                  BigDecimal freightAmount, Long freightCurrencyId) {
        this.shipmentId = shipmentId;
        this.shipmentCode = shipmentCode;
        this.relatedBillType = relatedBillType;
        this.relatedBillCode = relatedBillCode;
        this.carrierId = carrierId;
        this.freightAmount = freightAmount;
        this.freightCurrencyId = freightCurrencyId;
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

    public BigDecimal getFreightAmount() {
        return freightAmount;
    }

    public Long getFreightCurrencyId() {
        return freightCurrencyId;
    }
}
