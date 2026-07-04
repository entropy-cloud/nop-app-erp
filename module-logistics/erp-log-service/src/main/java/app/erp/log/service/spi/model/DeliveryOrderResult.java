package app.erp.log.service.spi.model;

import java.time.LocalDate;

/**
 * 发运下单结果 DTO（completeDeliveryOrder 返回：承运商单号 + 面单 URL + 预计送达）。
 */
public class DeliveryOrderResult {
    private String trackingNo;
    private String labelUrl;
    private LocalDate estimatedDelivery;

    public String getTrackingNo() {
        return trackingNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }

    public String getLabelUrl() {
        return labelUrl;
    }

    public void setLabelUrl(String labelUrl) {
        this.labelUrl = labelUrl;
    }

    public LocalDate getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(LocalDate estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }
}
