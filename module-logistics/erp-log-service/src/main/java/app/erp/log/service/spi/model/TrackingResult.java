package app.erp.log.service.spi.model;

import java.util.List;

/**
 * 追踪结果 DTO（trackShipment 返回：状态码、位置、时间线）。
 */
public class TrackingResult {
    private String trackingNo;
    /** 当前承运商侧状态：PICKED_UP / IN_TRANSIT / OUT_FOR_DELIVERY / DELIVERED / EXCEPTION。 */
    private String currentStatus;
    private String currentLocation;
    private List<TrackingEvent> events;

    public String getTrackingNo() {
        return trackingNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public List<TrackingEvent> getEvents() {
        return events;
    }

    public void setEvents(List<TrackingEvent> events) {
        this.events = events;
    }
}
