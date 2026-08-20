package app.erp.ct.service.spi.manual;

import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.spi.IErpCtOcrEngine;
import app.erp.ct.service.spi.model.OcrRecognizeRequest;
import app.erp.ct.service.spi.model.OcrRecognizeResponse;

/**
 * 手动/无操作 OCR 识别器（engineCode="manual"，config {@code erp-ct.ocr-engine} 默认值）。
 *
 * <p>零依赖基线实现（RC-R1.80 D1 裁决）：识别恒失败，文档落 FAILED + 失败原因，
 * 引导人工补录 ocrText（{@code submitOcrText} 补录等同 COMPLETED 语义）。
 * 真实引擎（Tesseract/云 OCR/OFD 解析）实现 {@link IErpCtOcrEngine} 即插
 * （对齐 UC-CT-09 MockSignatureProvider 先例——SPI 生命周期是契约，真实 provider 是部署决策）。
 */
public class ManualOcrEngine implements IErpCtOcrEngine {

    @Override
    public String getEngineCode() {
        return ErpCtConstants.OCR_ENGINE_MANUAL;
    }

    @Override
    public OcrRecognizeResponse recognize(OcrRecognizeRequest request) {
        return OcrRecognizeResponse.failure("manual 引擎无自动识别能力，请人工补录 ocrText（submitOcrText）");
    }
}
