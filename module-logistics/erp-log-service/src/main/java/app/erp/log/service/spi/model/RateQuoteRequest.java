package app.erp.log.service.spi.model;

import java.util.List;

/**
 * 比价请求 DTO（getRateQuote / getRateQuotes）。
 */
public class RateQuoteRequest {
    private Address senderAddress;
    private Address receiverAddress;
    private List<ParcelInfo> parcels;
    private List<String> serviceTypes;

    public Address getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(Address senderAddress) {
        this.senderAddress = senderAddress;
    }

    public Address getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(Address receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public List<ParcelInfo> getParcels() {
        return parcels;
    }

    public void setParcels(List<ParcelInfo> parcels) {
        this.parcels = parcels;
    }

    public List<String> getServiceTypes() {
        return serviceTypes;
    }

    public void setServiceTypes(List<String> serviceTypes) {
        this.serviceTypes = serviceTypes;
    }
}
