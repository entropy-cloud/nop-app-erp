package app.erp.b2b.service.spi.transport.model;

import java.io.Serializable;

/**
 * 传输结果 DTO（出站 send 返回）。
 */
public class TransportResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String messageId;
    private String fileHash;
    private String mdnStatus;
    private String errorCode;
    private String errorMessage;

    public static TransportResult success(String messageId, String fileHash) {
        TransportResult r = new TransportResult();
        r.success = true;
        r.messageId = messageId;
        r.fileHash = fileHash;
        r.mdnStatus = "processed";
        return r;
    }

    public static TransportResult failure(String errorCode, String errorMessage) {
        TransportResult r = new TransportResult();
        r.success = false;
        r.errorCode = errorCode;
        r.errorMessage = errorMessage;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getMdnStatus() {
        return mdnStatus;
    }

    public void setMdnStatus(String mdnStatus) {
        this.mdnStatus = mdnStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
