package app.erp.ct.service.spi.testfix;

import app.erp.ct.service.spi.IErpCtOcrEngine;
import app.erp.ct.service.spi.model.OcrRecognizeRequest;
import app.erp.ct.service.spi.model.OcrRecognizeResponse;

/**
 * 测试专用固定文本 OCR 引擎（engineCode="test-fixed"，仅 src/test 源集）。
 * 识别恒成功返回固定文本，用于断言 PENDING→PROCESSING→COMPLETED 迁移与
 * ocrText/fullTextSearch 联动（经 test-mock.beans.xml 注册，config erp-ct.ocr-engine 切换选型）。
 */
public class FixedTextOcrEngine implements IErpCtOcrEngine {

    public static final String ENGINE_CODE = "test-fixed";
    public static final String FIXED_TEXT = "FIXED-OCR-TEXT 合同金额一万元 整";

    @Override
    public String getEngineCode() {
        return ENGINE_CODE;
    }

    @Override
    public OcrRecognizeResponse recognize(OcrRecognizeRequest request) {
        return OcrRecognizeResponse.success(FIXED_TEXT);
    }
}
