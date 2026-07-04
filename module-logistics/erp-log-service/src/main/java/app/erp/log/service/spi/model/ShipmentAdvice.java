package app.erp.log.service.spi.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 预约取件/发运通知 DTO（adviseShipment）。
 */
public class ShipmentAdvice {
    private String shipmentCode;
    private LocalDate pickupDate;
    private String pickupTimeFrom;
    private String pickupTimeTo;
    private Address pickupAddress;
    private String contactPerson;
    private String contactPhone;
    private List<ParcelInfo> parcels;

    public String getShipmentCode() {
        return shipmentCode;
    }

    public void setShipmentCode(String shipmentCode) {
        this.shipmentCode = shipmentCode;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
    }

    public String getPickupTimeFrom() {
        return pickupTimeFrom;
    }

    public void setPickupTimeFrom(String pickupTimeFrom) {
        this.pickupTimeFrom = pickupTimeFrom;
    }

    public String getPickupTimeTo() {
        return pickupTimeTo;
    }

    public void setPickupTimeTo(String pickupTimeTo) {
        this.pickupTimeTo = pickupTimeTo;
    }

    public Address getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(Address pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public List<ParcelInfo> getParcels() {
        return parcels;
    }

    public void setParcels(List<ParcelInfo> parcels) {
        this.parcels = parcels;
    }
}
