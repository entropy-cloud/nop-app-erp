package io.nop.app.all.web;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.core.module.ModuleManager;
import io.nop.core.resource.IResource;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.core.resource.component.ResourceComponentManager;
import io.nop.web.WebConfigs;
import io.nop.web.page.PageProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE)
public class ErpAllFluxPagesTest extends JunitBaseTestCase {

    @Inject
    PageProvider pageProvider;

    private void fluxMode() {
        AppConfig.getConfigProvider().updateConfigValue(WebConfigs.CFG_WEB_RENDER_MODE, "flux");
        ResourceComponentManager.instance().clearCache("xlib");
        ResourceComponentManager.instance().clearCache("xpage");
    }

    @AfterEach
    public void tearDownFlux() {
        AppConfig.getConfigProvider().updateConfigValue(WebConfigs.CFG_WEB_RENDER_MODE, "amis");
        ResourceComponentManager.instance().clearCache("xlib");
        ResourceComponentManager.instance().clearCache("xpage");
    }

    @Test
    public void testAllPagesRenderInFluxMode() {
        fluxMode();
        List<String> errors = new ArrayList<>();
        ModuleManager.instance().getEnabledModules(true).forEach(module -> {
            List<IResource> pageFiles = VirtualFileSystem.instance().findAll(
                    "/" + module.getModuleId(), "pages/*/*.page.yaml");
            for (IResource resource : pageFiles) {
                try {
                    pageProvider.getPage(resource.getPath(), AppConfig.defaultLocale());
                } catch (Exception e) {
                    Throwable root = e;
                    while (root.getCause() != null && root.getCause() != root) root = root.getCause();
                    String code = "";
                    if (root instanceof NopException) {
                        code = " [" + ((NopException) root).getErrorCode() + "]";
                    }
                    errors.add(resource.getPath() + "\t" + root.getMessage() + code);
                }
            }
        });
        System.out.println("=== FLUX_PAGE_ERROR_COUNT: " + errors.size() + " ===");
        for (String err : errors) System.out.println("FLUX_PAGE_ERR\t" + err);
        org.junit.jupiter.api.Assertions.assertTrue(errors.isEmpty(),
                "Flux page build errors (" + errors.size() + "):\n" + String.join("\n", errors));
    }
}
