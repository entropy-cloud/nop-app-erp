package app.erp.log.service.spi.model;

import java.time.LocalDateTime;

/**
 * 追踪事件 DTO（时间线元素：时间、位置、状态描述）。
 */
public class TrackingEvent {
    private LocalDateTime eventTime;
    private String location;
    private String statusCode;
    private String description;

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
