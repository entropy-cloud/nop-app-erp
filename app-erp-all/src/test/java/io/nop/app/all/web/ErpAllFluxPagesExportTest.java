package io.nop.app.all.web;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.commons.util.FileHelper;
import io.nop.core.lang.json.JsonTool;
import io.nop.core.resource.component.ResourceComponentManager;
import io.nop.web.WebConfigs;
import io.nop.web.page.PageExportOptions;
import io.nop.web.page.PageExportResult;
import io.nop.web.page.PageProvider;
import io.nop.web.page.WebPageExporter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 把全部 enabled modules 的页面以 flux 渲染模式批量导出到 target/flux-pages/，
 * 供 JS 侧编译验证（scripts/validate-flux-pages.sh → nop-chaos-flux validate-pages.mjs）。
 * 架构契约见 docs/architecture/flux-page-export-and-validation.md。
 *
 * <p>与 {@link ErpAllFluxPagesTest} 的职责边界：后者验证 flux 模式下页面「加载正确性」
 * （getPage 零异常，Java 生成链）；本测试产出「导出产物」（flux 页面 JSON + manifest），
 * 把页面交给 flux 编译器验证 schema/表达式合法性——JS 侧发现的问题在浏览器 E2E 之前暴露。
 */
@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE)
public class ErpAllFluxPagesExportTest extends JunitBaseTestCase {

    @Inject
    PageProvider pageProvider;

    @AfterEach
    public void tearDownFlux() {
        setRenderMode("amis");
    }

    private void setRenderMode(String mode) {
        AppConfig.getConfigProvider().updateConfigValue(WebConfigs.CFG_WEB_RENDER_MODE, mode);
        ResourceComponentManager.instance().clearCache("xlib");
        ResourceComponentManager.instance().clearCache("xpage");
    }

    @Test
    public void testExportAllFluxPages() {
        File targetDir = getTargetFile("flux-pages");
        FileHelper.deleteAll(targetDir);

        PageExportOptions options = new PageExportOptions();
        options.setRenderMode("flux");
        options.setLocale("zh-CN");

        PageExportResult result = new WebPageExporter(pageProvider).exportPages(options, targetDir);

        long erpCount = result.getPages().stream().filter(p -> p.startsWith("erp/")).count();
        System.out.println("=== FLUX_EXPORT pageCount=" + result.getPages().size()
                + " erpPages=" + erpCount + " failed=" + result.getFailedPages().size() + " ===");

        assertTrue(result.getFailedPages().isEmpty(),
                "Flux page export failures (" + result.getFailedPages().size() + "):\n"
                        + result.getFailedPages().stream()
                                .map(e -> e.getPath() + "\t[" + e.getErrorCode() + "] " + e.getMessage())
                                .reduce((a, b) -> a + "\n" + b).orElse(""));
        assertTrue(result.getPages().size() > 800,
                "Exported page count suspiciously low: " + result.getPages().size()
                        + " (expected ERP 855 + platform module pages)");
        assertTrue(result.getManifestFile().exists(), "manifest.json should be written");

        assertExportMatchesGetPage(targetDir, "/erp/aps/pages/dashboard/schedule-gantt.page.yaml");
        assertExportMatchesGetPage(targetDir, "/erp/crm/pages/ErpCrmActivity/calendar.page.yaml");
        assertExportMatchesGetPage(targetDir, "/erp/fin/pages/period-close-wizard/main.page.yaml");
    }

    /**
     * D1 等价性实证：手写 flux.yaml 孪生页面的导出内容必须与生产 getPage 返回完全一致
     * （导出即浏览器 PageProvider__getPage 将收到的内容）。三个样例覆盖 dashboard/活动页/向导三类。
     */
    private void assertExportMatchesGetPage(File targetDir, String pagePath) {
        File exported = new File(targetDir, pagePath.substring(1).replace(".page.yaml", ".page.json"));
        assertTrue(exported.exists(), "exported file should exist for twin page: " + pagePath);
        Map<String, Object> loaded = pageProvider.getPage(pagePath, "zh-CN");
        assertNotNull(loaded, "getPage should load twin page: " + pagePath);
        assertEquals(JsonTool.serialize(loaded, true), FileHelper.readText(exported, null),
                "exported content must equal production getPage for twin page: " + pagePath);
    }
}
