package app.erp.fin.service.ocr;

/**
 * E3.5 OCR 引擎 SPI（`document-driven-ap-automation.md` 前置调研结论：本地文本抽取为默认实现，
 * tess4j 适配器为可插拔项，触发条件 = 目标环境具备 tesseract 二进制与语言包）。
 *
 * <p>实现注册为 Nop IoC bean（{@code ioc:collect-beans} 收集到管道 Processor）；无外部云依赖。
 */
public interface IErpFinOcrEngine {

    /**
     * 抽取文档文本。
     *
     * @return 抽取文本；无法抽取（扫描件/图片/不支持格式）返回 null 或空串 —— 调用方按低置信挂人工门
     */
    String extractText(String fileName, String mimeType, byte[] content);
}
