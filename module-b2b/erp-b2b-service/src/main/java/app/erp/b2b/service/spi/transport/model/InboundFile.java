package app.erp.b2b.service.spi.transport.model;

import java.io.Serializable;

/**
 * 入站文件 DTO（SFTP 轮询 pullInbound 返回）。
 */
public class InboundFile implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileName;
    private String content;
    private String fileHash;
    private long fileSize;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
