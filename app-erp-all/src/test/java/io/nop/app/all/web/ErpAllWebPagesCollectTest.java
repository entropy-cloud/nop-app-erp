package io.nop.app.all.web;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.api.core.config.AppConfig;
import io.nop.core.module.ModuleManager;
import io.nop.core.resource.IResource;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.web.page.PageProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE)
public class ErpAllWebPagesCollectTest extends JunitBaseTestCase {

    @Inject
    PageProvider pageProvider;

    @Test
    public void testCollectAllPageErrors() {
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
                        NopException ne = (NopException) root;
                        code = " [" + ne.getErrorCode() + "]";
                    }
                    errors.add(resource.getPath() + "\t" + root.getMessage() + code);
                }
            }
        });
        System.out.println("=== PAGE_ERROR_COUNT: " + errors.size() + " ===");
        for (String err : errors) System.out.println("PAGE_ERR\t" + err);
        org.junit.jupiter.api.Assertions.assertTrue(errors.isEmpty(),
                "Page build errors (" + errors.size() + "):\n" + String.join("\n", errors));
    }
}
