package app.erp.ct.service.spi.model;

/**
 * OCR 识别结果（{@code IErpCtOcrEngine#recognize} 输出）。
 *
 * <p>{@code success=true} 携带识别文本；{@code success=false} 携带失败原因
 * （由 BizModel 落 FAILED 状态 + remark 记录，人工可重新提交或补录）。
 */
public class OcrRecognizeResponse {

    private final boolean success;
    private final String text;
    private final String errorMsg;

    private OcrRecognizeResponse(boolean success, String text, String errorMsg) {
        this.success = success;
        this.text = text;
        this.errorMsg = errorMsg;
    }

    public static OcrRecognizeResponse success(String text) {
        return new OcrRecognizeResponse(true, text, null);
    }

    public static OcrRecognizeResponse failure(String errorMsg) {
        return new OcrRecognizeResponse(false, null, errorMsg);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getText() {
        return text;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
