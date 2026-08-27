package app.erp.fin.service.ocr;

import io.nop.commons.util.StringHelper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.nio.charset.StandardCharsets;

/**
 * 默认 OCR 引擎 = 纯 Java 本地文本抽取（E3.5 前置调研裁决：PDFBox 已随 nop-report-pdf 在依赖树，
 * 零新增第三方依赖；对数字 PDF/txt 抽取文本；扫描件/图片无文本 → 空结果挂人工门）。
 *
 * <p>真 OCR（tess4j）为 SPI 可插拔项，本类不引入 JNI/系统级二进制依赖。
 */
public class ErpFinTextExtractOcrEngine implements IErpFinOcrEngine {

    @Override
    public String extractText(String fileName, String mimeType, byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        if (isPdf(mimeType, fileName)) {
            try (PDDocument doc = Loader.loadPDF(content)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc);
                return StringHelper.isBlank(text) ? null : text;
            } catch (Exception e) {
                // 损坏/加密 PDF：无文本可抽取，交由低置信人工门处理（不抛出——扫描件路径是合法业务分支）
                return null;
            }
        }
        if (isPlainText(mimeType, fileName)) {
            String text = new String(content, StandardCharsets.UTF_8);
            return StringHelper.isBlank(text) ? null : text;
        }
        // 图片/Office 等无文本层格式：默认引擎不支持 → 低置信人工门
        return null;
    }

    private boolean isPdf(String mimeType, String fileName) {
        if (mimeType != null && mimeType.contains("pdf")) {
            return true;
        }
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    private boolean isPlainText(String mimeType, String fileName) {
        if (mimeType != null && (mimeType.startsWith("text/") || mimeType.contains("json") || mimeType.contains("xml"))) {
            return true;
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".csv") || lower.endsWith(".json") || lower.endsWith(".xml");
    }
}
