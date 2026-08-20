package app.erp.ct.service.spi.model;

/**
 * OCR 识别请求（{@code IErpCtOcrEngine#recognize} 输入）。携带文档引用（附件 ID/名称/MIME），
 * 引擎自行决定从文件存储读取或按 MIME 分派识别策略。
 */
public class OcrRecognizeRequest {

    private final Long documentId;
    private final String attachmentFileId;
    private final String docName;
    private final String mimeType;

    public OcrRecognizeRequest(Long documentId, String attachmentFileId, String docName, String mimeType) {
        this.documentId = documentId;
        this.attachmentFileId = attachmentFileId;
        this.docName = docName;
        this.mimeType = mimeType;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getAttachmentFileId() {
        return attachmentFileId;
    }

    public String getDocName() {
        return docName;
    }

    public String getMimeType() {
        return mimeType;
    }
}
