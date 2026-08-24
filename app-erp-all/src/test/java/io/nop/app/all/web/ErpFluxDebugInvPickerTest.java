package io.nop.app.all.web;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.core.lang.json.JsonTool;
import io.nop.core.resource.component.ResourceComponentManager;
import io.nop.web.WebConfigs;
import io.nop.web.page.PageProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE)
public class ErpFluxDebugInvPickerTest extends JunitBaseTestCase {

    @Inject
    PageProvider pageProvider;

    @Test
    public void dumpAllTestPages() {
        AppConfig.getConfigProvider().updateConfigValue(WebConfigs.CFG_WEB_RENDER_MODE, "flux");
        ResourceComponentManager.instance().clearCache("xlib");
        ResourceComponentManager.instance().clearCache("xpage");

        String[] paths = {
                "/erp/md/pages/ErpMdMaterial/main.page.yaml",
                "/erp/pur/pages/ErpPurOrder/main.page.yaml",
                "/erp/sal/pages/ErpSalOrder/main.page.yaml",
                "/erp/fin/pages/ErpFinVoucher/main.page.yaml",
                "/erp/inv/pages/ErpInvStockMove/main.page.yaml"
        };

        String dir = System.getProperty("java.io.tmpdir");
        for (String path : paths) {
            try {
                Map<String, Object> flux = pageProvider.getPage(path, AppConfig.defaultLocale());
                String json = JsonTool.serialize(flux, true);
                String slug = path.replace("/", "_").replace(".page.yaml", "");
                java.nio.file.Files.writeString(java.nio.file.Path.of(dir, slug + ".flux.json"), json);
                System.out.println("DUMPED: " + path);
            } catch (Exception e) {
                System.out.println("DUMP_FAIL: " + path + " - " + e.getMessage());
            }
        }

        AppConfig.getConfigProvider().updateConfigValue(WebConfigs.CFG_WEB_RENDER_MODE, "amis");
    }
}