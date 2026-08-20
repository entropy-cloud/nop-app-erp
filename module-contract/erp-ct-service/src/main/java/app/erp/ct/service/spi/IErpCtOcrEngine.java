package app.erp.ct.service.spi;

import app.erp.ct.service.spi.model.OcrRecognizeRequest;
import app.erp.ct.service.spi.model.OcrRecognizeResponse;

/**
 * OCR 识别引擎 SPI（对应 {@code docs/design/contract/contract-repository.md §OCR 识别}）。
 *
 * <p>每个具体引擎（Tesseract / 云 OCR / 电子 PDF 文字层提取 / ...）实现一个 Bean，
 * 由 {@link ErpCtOcrEngineRegistry} 按 {@link #getEngineCode()} 建图派发
 * （config {@code erp-ct.ocr-engine} 选型，默认 MANUAL）。
 *
 * <p>实现约束：
 * <ul>
 *   <li>{@link #recognize} 应为非阻塞；外部 HTTP 调用在实现内做超时/重试控制。</li>
 *   <li>无法识别（无文字层/无识别能力）返回 {@code success=false} + errorMsg，
 *       由 BizModel 落 FAILED 状态 + 失败原因，人工可重新提交或补录。</li>
 * </ul>
 *
 * <p>本期实现：{@code app.erp.ct.service.spi.manual.ManualOcrEngine}（engineCode="manual"，
 * 零依赖无操作识别器——识别结果恒为空，文档落 FAILED 引导人工补录 ocrText，
 * 补录等同 COMPLETED 语义）。真实引擎（Tesseract/云 OCR/OFD 解析）为部署 successor
 * （对齐 UC-CT-09 MockSignatureProvider 先例——SPI 生命周期是契约，真实 provider 是部署决策）。
 */
public interface IErpCtOcrEngine {

    /** 引擎编码（config {@code erp-ct.ocr-engine} 匹配值，如 "manual"/"tesseract"/"cloud-ocr"）。 */
    String getEngineCode();

    /**
     * 识别文档文本。输入文档引用（附件 ID/名称/MIME），输出识别文本或失败原因。
     */
    OcrRecognizeResponse recognize(OcrRecognizeRequest request);
}
