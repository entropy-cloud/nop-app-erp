package app.erp.log.service.spi.model;

import java.util.List;

/**
 * 发运下单请求 DTO（completeDeliveryOrder）。
 */
public class DeliveryOrderRequest {
    private String shipmentCode;
    private String serviceType;
    private Address sender;
    private Address receiver;
    private List<ParcelInfo> parcels;
    /** 幂等键（发运单号），重试时同一 referenceNo 网关返回已有结果。 */
    private String referenceNo;

    public String getShipmentCode() {
        return shipmentCode;
    }

    public void setShipmentCode(String shipmentCode) {
        this.shipmentCode = shipmentCode;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Address getSender() {
        return sender;
    }

    public void setSender(Address sender) {
        this.sender = sender;
    }

    public Address getReceiver() {
        return receiver;
    }

    public void setReceiver(Address receiver) {
        this.receiver = receiver;
    }

    public List<ParcelInfo> getParcels() {
        return parcels;
    }

    public void setParcels(List<ParcelInfo> parcels) {
        this.parcels = parcels;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }
}
