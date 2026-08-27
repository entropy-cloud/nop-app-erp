package app.erp.fin.service.ocr;

import app.erp.fin.service.ErpFinConfigs;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.nop.api.core.config.AppConfig;
import io.nop.core.unittest.BaseTestCase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2-7（plan 2026-08-28-0219-1 Phase 1）：默认 OCR 引擎可观测与上界单测（纯逻辑层）。
 *
 * <ul>
 *   <li>损坏 PDF：返回 null（人工门语义不变）+ WARN 日志留痕（与无文本扫描件可区分）；</li>
 *   <li>PDF 页数上限 config-gate（{@code erp-fin.ap-doc-pdf-max-pages}）：超限 WARN + 返回 null；</li>
 *   <li>数字 PDF / txt 正常抽取回归（上限与日志不改变合法路径行为）。</li>
 * </ul>
 */
public class TestErpFinTextExtractOcrEngineObservability extends BaseTestCase {

    private final ErpFinTextExtractOcrEngine engine = new ErpFinTextExtractOcrEngine();

    private ListAppender<ILoggingEvent> logAppender;
    private Logger engineLogger;

    @BeforeEach
    void attachLogAppender() {
        engineLogger = (Logger) LoggerFactory.getLogger(ErpFinTextExtractOcrEngine.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        engineLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        setConfig(String.valueOf(ErpFinConfigs.DEFAULT_AP_DOC_PDF_MAX_PAGES));
        if (engineLogger != null && logAppender != null) {
            engineLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    @Test
    public void testCorruptedPdfReturnsNullWithWarnLog() {
        byte[] corrupted = "this is not a pdf content".getBytes(StandardCharsets.UTF_8);
        String text = engine.extractText("broken.pdf", "application/pdf", corrupted);
        assertNull(text, "损坏 PDF 返回 null（低置信人工门语义不变）");
        ILoggingEvent warn = findLog("erp-fin-ap-doc-pdf-extract-failed");
        assertNotNull(warn, "损坏 PDF 应记录 WARN 日志（与无文本扫描件可区分）");
        assertTrue(warn.getFormattedMessage().contains("broken.pdf"), "WARN 日志应含 fileName");
        assertTrue(warn.getFormattedMessage().contains("application/pdf"), "WARN 日志应含 mimeType");
    }

    @Test
    public void testPdfPageLimitConfigGate() throws Exception {
        byte[] pdf3Pages = buildPdf(3, false);
        setConfig("2");
        String overLimit = engine.extractText("big.pdf", "application/pdf", pdf3Pages);
        assertNull(overLimit, "超页数上限返回 null 落人工门");
        ILoggingEvent warn = findLog("erp-fin-ap-doc-pdf-pages-exceeded");
        assertNotNull(warn, "超页数上限应记录 WARN 日志（可观测）");
        assertTrue(warn.getFormattedMessage().contains("big.pdf"));
        assertTrue(warn.getFormattedMessage().contains("pages=3"), "WARN 含实际页数: " + warn.getFormattedMessage());
        assertTrue(warn.getFormattedMessage().contains("maxPages=2"));

        // 限额内（3 页 ≤ 3）：正常抽取路径可达（空文本页 → null 无 WARN-pages）
        logAppender.list.clear();
        setConfig("3");
        assertNull(engine.extractText("big.pdf", "application/pdf", pdf3Pages), "空文本 3 页 PDF 无文本返回 null");
        assertNull(findLog("erp-fin-ap-doc-pdf-pages-exceeded"), "限额内不触发超限 WARN");
    }

    @Test
    public void testDigitalPdfAndPlainTextExtractionUnchanged() throws Exception {
        byte[] pdf = buildPdf(1, true);
        String text = engine.extractText("digital.pdf", "application/pdf", pdf);
        assertNotNull(text, "数字 PDF 应抽取文本");
        assertTrue(text.contains("INVOICE-TEST"), "抽取文本含写入内容: " + text);

        assertEquals("发票号码：INV-1", engine.extractText("a.txt", "text/plain",
                "发票号码：INV-1".getBytes(StandardCharsets.UTF_8)).trim(), "txt 抽取回归");
    }

    // ---------- helpers ----------

    private void setConfig(String value) {
        AppConfig.getConfigProvider().assignConfigValue(ErpFinConfigs.CONFIG_AP_DOC_PDF_MAX_PAGES, value);
    }

    private byte[] buildPdf(int pages, boolean withText) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                if (withText) {
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        cs.newLineAtOffset(50, 700);
                        cs.showText("Invoice No. INVOICE-TEST");
                        cs.endText();
                    }
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private ILoggingEvent findLog(String marker) {
        for (ILoggingEvent event : logAppender.list) {
            if (event.getFormattedMessage().contains(marker)) {
                return event;
            }
        }
        return null;
    }
}
