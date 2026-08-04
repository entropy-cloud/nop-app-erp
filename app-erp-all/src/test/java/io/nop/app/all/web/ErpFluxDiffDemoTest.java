package io.nop.app.all.web;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.core.resource.component.ResourceComponentManager;
import io.nop.web.WebConfigs;
import io.nop.web.page.PageProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE)
public class ErpFluxDiffDemoTest extends JunitBaseTestCase {

    @Inject
    PageProvider pageProvider;

    private Map<String, Object> render(String mode, String path) {
        AppConfig.getConfigProvider().updateConfigValue(WebConfigs.CFG_WEB_RENDER_MODE, mode);
        ResourceComponentManager.instance().clearCache("xlib");
        ResourceComponentManager.instance().clearCache("xpage");
        return pageProvider.getPage(path, AppConfig.defaultLocale());
    }

    @Test
    public void dumpErpMdMaterialAmisVsFlux() {
        String path = "/erp/md/pages/ErpMdMaterial/main.page.yaml";
        Map<String, Object> amis = render("amis", path);
        Map<String, Object> flux = render("flux", path);
        AppConfig.getConfigProvider().updateConfigValue(WebConfigs.CFG_WEB_RENDER_MODE, "amis");

        System.out.println("=== AMIS_TOP_KEYS: " + (amis == null ? "null" : amis.keySet()) + " ===");
        System.out.println("=== FLUX_TOP_KEYS: " + (flux == null ? "null" : flux.keySet()) + " ===");
        System.out.println("=== AMIS_TYPE: " + (amis == null ? "null" : amis.get("type")) + " ===");
        System.out.println("=== FLUX_TYPE: " + (flux == null ? "null" : flux.get("type")) + " ===");
        System.out.println("=== AMIS_BODY_TYPE: "
                + (amis == null || amis.get("body") == null ? "n/a" : ((Map<?, ?>) amis.get("body")).get("type")) + " ===");
        System.out.println("=== FLUX_BODY_TYPE: "
                + (flux == null || flux.get("body") == null ? "n/a" : ((Map<?, ?>) flux.get("body")).get("type")) + " ===");
        org.junit.jupiter.api.Assertions.assertNotNull(amis, "amis output");
        org.junit.jupiter.api.Assertions.assertNotNull(flux, "flux output");
        try {
            String dir = System.getProperty("java.io.tmpdir");
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir, "erp-md-material.amis.json"),
                    io.nop.core.lang.json.JsonTool.serialize(amis, true));
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir, "erp-md-material.flux.json"),
                    io.nop.core.lang.json.JsonTool.serialize(flux, true));
            System.out.println("=== DUMPED_TO: " + dir + " ===");
        } catch (Exception e) {
            System.out.println("=== DUMP_FAILED: " + e.getMessage() + " ===");
        }
    }
}
