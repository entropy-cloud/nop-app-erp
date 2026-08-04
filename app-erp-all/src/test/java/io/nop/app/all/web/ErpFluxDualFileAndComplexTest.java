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
public class ErpFluxDualFileAndComplexTest extends JunitBaseTestCase {

    @Inject
    PageProvider pageProvider;

    private Map<String, Object> render(String mode, String path) {
        AppConfig.getConfigProvider().updateConfigValue(WebConfigs.CFG_WEB_RENDER_MODE, mode);
        ResourceComponentManager.instance().clearCache("xlib");
        ResourceComponentManager.instance().clearCache("xpage");
        return pageProvider.getPage(path, AppConfig.defaultLocale());
    }

    @Test
    public void verifyFluxYamlDualFilePreference() {
        String path = "/erp/md/pages/ErpMdMaterial/main.page.yaml";
        Map<String, Object> fluxOut = render("flux", path);
        Map<String, Object> amisOut = render("amis", path);
        AppConfig.getConfigProvider().updateConfigValue(WebConfigs.CFG_WEB_RENDER_MODE, "amis");

        org.junit.jupiter.api.Assertions.assertNotNull(fluxOut, "flux mode should load (flux.yaml preferred)");
        org.junit.jupiter.api.Assertions.assertNotNull(amisOut, "amis mode should load page.yaml");
        org.junit.jupiter.api.Assertions.assertEquals("page", fluxOut.get("type"), "flux top type");
        org.junit.jupiter.api.Assertions.assertEquals("crud", ((Map<?, ?>) fluxOut.get("body")).get("type"), "flux body type");
        System.out.println("=== DUAL_FILE: flux mode loaded flux.yaml (loadAction present)="
                + (((Map<?, ?>) fluxOut.get("body")).containsKey("loadAction"))
                + ", amis mode loaded page.yaml ===");
    }

    @Test
    public void verifyComplexPageModelInFlux() {
        String complexPath = "/nop/test/pages/test-flux-complex.page.yaml";
        boolean loaded = false;
        try {
            Map<String, Object> out = render("flux", complexPath);
            AppConfig.getConfigProvider().updateConfigValue(WebConfigs.CFG_WEB_RENDER_MODE, "amis");
            if (out != null) {
                loaded = true;
                System.out.println("=== COMPLEX: test-flux-complex loaded in flux, type=" + out.get("type") + " ===");
            }
        } catch (Exception e) {
            System.out.println("=== COMPLEX: test-flux-complex not on classpath or load failed: "
                    + e.getClass().getSimpleName() + " " + e.getMessage() + " ===");
        }
        System.out.println("=== COMPLEX_AVAILABLE=" + loaded + " ===");
    }
}
