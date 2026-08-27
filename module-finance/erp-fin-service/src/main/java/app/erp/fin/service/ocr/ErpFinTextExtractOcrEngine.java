package app.erp.fin.service.ocr;

import app.erp.fin.service.ErpFinConfigs;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * 默认 OCR 引擎 = 纯 Java 本地文本抽取（E3.5 前置调研裁决：PDFBox 已随 nop-report-pdf 在依赖树，
 * 零新增第三方依赖；对数字 PDF/txt 抽取文本；扫描件/图片无文本 → 空结果挂人工门）。
 *
 * <p>真 OCR（tess4j）为 SPI 可插拔项，本类不引入 JNI/系统级二进制依赖。
 *
 * <p>P2-7 可观测与上界（plan 2026-08-28-0219-1）：损坏/加密 PDF 的 catch 分支补 WARN 日志
 * （fileName/mimeType/异常摘要——与无文本扫描件可区分）；PDF 页数上限
 * {@code erp-fin.ap-doc-pdf-max-pages}（默认 50）config-gate，超限 WARN + 返回 null 落人工门
 * （防大 PDF 全内存 CPU/内存放大，返回 null 语义不变）。
 */
public class ErpFinTextExtractOcrEngine implements IErpFinOcrEngine {

    static final Logger LOG = LoggerFactory.getLogger(ErpFinTextExtractOcrEngine.class);

    @Override
    public String extractText(String fileName, String mimeType, byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        if (isPdf(mimeType, fileName)) {
            try (PDDocument doc = Loader.loadPDF(content)) {
                int maxPages = AppConfig.var(ErpFinConfigs.CONFIG_AP_DOC_PDF_MAX_PAGES,
                        ErpFinConfigs.DEFAULT_AP_DOC_PDF_MAX_PAGES);
                if (maxPages > 0 && doc.getNumberOfPages() > maxPages) {
                    // 超页数上界：拒绝全量抽取（CPU/内存放大防护），返回 null 落人工门
                    LOG.warn("erp-fin-ap-doc-pdf-pages-exceeded: fileName={}, mimeType={}, pages={}, maxPages={}",
                            fileName, mimeType, doc.getNumberOfPages(), maxPages);
                    return null;
                }
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc);
                return StringHelper.isBlank(text) ? null : text;
            } catch (Exception e) {
                // 损坏/加密 PDF：无文本可抽取，交由低置信人工门处理（不抛出——扫描件路径是合法业务分支）；
                // P2-7：WARN 留痕使损坏文件与无文本扫描件可区分
                LOG.warn("erp-fin-ap-doc-pdf-extract-failed: fileName={}, mimeType={}, reason={}",
                        fileName, mimeType, e.getMessage());
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
