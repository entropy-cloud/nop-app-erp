package app.erp.b2b.service.spi.model;

import java.io.Serializable;

/**
 * EDI 对方确认回调数据（ACKNOWLEDGE 动作语义载体）。
 */
public class Acknowledgement implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status;
    private String message;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
