package io.nop.app.all.web;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.commons.util.FileHelper;
import io.nop.web.page.PageExportOptions;
import io.nop.web.page.PageExportResult;
import io.nop.web.page.PageProvider;
import io.nop.web.page.WebPageExporter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE)
public class ErpAllFluxPagesTest extends JunitBaseTestCase {

    @Inject
    PageProvider pageProvider;

    @Test
    public void testAllPagesRenderInFluxMode() {
        File targetDir = getTargetFile("flux-pages");
        FileHelper.deleteAll(targetDir);

        PageExportOptions options = new PageExportOptions();
        options.setRenderMode("flux");

        PageExportResult result = new WebPageExporter(pageProvider).exportPages(options, targetDir);

        long erpCount = result.getPages().stream().filter(p -> p.startsWith("erp/")).count();
        System.out.println("=== FLUX_PAGE_ERROR_COUNT: " + result.getFailedPages().size()
                + " pageCount=" + result.getPages().size() + " erpPages=" + erpCount + " ===");

        assertTrue(result.getFailedPages().isEmpty(),
                "Flux page errors (" + result.getFailedPages().size() + "):\n"
                        + result.getFailedPages().stream()
                                .map(e -> e.getPath() + "\t[" + e.getErrorCode() + "] " + e.getMessage())
                                .reduce((a, b) -> a + "\n" + b).orElse(""));
        assertTrue(result.getPages().size() > 800,
                "Exported page count suspiciously low: " + result.getPages().size());
    }
}
