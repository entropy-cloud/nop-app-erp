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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
            Files.writeString(Path.of(dir, "erp-md-material.amis.json"),
                    JsonTool.serialize(amis, true));
            Files.writeString(Path.of(dir, "erp-md-material.flux.json"),
                    JsonTool.serialize(flux, true));
            System.out.println("=== DUMPED_TO: " + dir + " ===");
        } catch (Exception e) {
            System.out.println("=== DUMP_FAILED: " + e.getMessage() + " ===");
        }
    }

    /**
     * Plan 2026-08-24-1147-1 Phase 1.1: 抽取 flux 模式下的 picker schema 子集,
     * 验证当前输出是 AMIS 风格(含 source/joinValues/extractValue/pickerSchema),
     * 落盘到 /tmp/erp-picker-baseline/ 用于 Phase 2 修复前后对比。
     */
    @Test
    public void dumpPickerSchemasBaseline() {
        AppConfig.getConfigProvider().updateConfigValue(WebConfigs.CFG_WEB_RENDER_MODE, "flux");
        ResourceComponentManager.instance().clearCache("xlib");
        ResourceComponentManager.instance().clearCache("xpage");

        String[] paths = {
                "/erp/md/pages/ErpMdMaterial/main.page.yaml",
                "/erp/pur/pages/ErpPurOrder/main.page.yaml",
                "/erp/fin/pages/ErpFinVoucher/main.page.yaml",
                "/erp/sal/pages/ErpSalOrder/main.page.yaml",
                "/erp/inv/pages/ErpInvStockMove/main.page.yaml"
        };

        Path outDir = Path.of("/tmp/erp-picker-baseline");
        try {
            Files.createDirectories(outDir);
        } catch (Exception e) {
            System.out.println("=== MKDIR_FAILED: " + e.getMessage() + " ===");
            return;
        }

        for (String path : paths) {
            try {
                Map<String, Object> flux = pageProvider.getPage(path, AppConfig.defaultLocale());
                List<Map<String, Object>> pickers = new ArrayList<>();
                collectPickers(flux, "", pickers);
                String slug = path.replace("/", "_").replace(".page.yaml", "");
                Files.writeString(outDir.resolve(slug + "-pickers.json"),
                        JsonTool.serialize(pickers, true));
                System.out.println("=== " + path + ": " + pickers.size() + " pickers dumped ===");
                if (!pickers.isEmpty()) {
                    Map<String, Object> first = pickers.get(0);
                    System.out.println("  first picker keys: " + first.keySet());
                    System.out.println("  has pickerDialog: " + first.containsKey("pickerDialog"));
                    System.out.println("  has loadAction: " + first.containsKey("loadAction"));
                    System.out.println("  has source: " + first.containsKey("source"));
                    System.out.println("  has joinValues: " + first.containsKey("joinValues"));
                    System.out.println("  has extractValue: " + first.containsKey("extractValue"));
                }
            } catch (Exception e) {
                System.out.println("=== " + path + " FAILED: " + e.getMessage() + " ===");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void collectPickers(Object node, String path, List<Map<String, Object>> out) {
        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;
            if ("picker".equals(map.get("type"))) {
                out.add(map);
            }
            for (Map.Entry<String, Object> e : map.entrySet()) {
                collectPickers(e.getValue(), path + "." + e.getKey(), out);
            }
        } else if (node instanceof List) {
            List<Object> list = (List<Object>) node;
            for (int i = 0; i < list.size(); i++) {
                collectPickers(list.get(i), path + "[" + i + "]", out);
            }
        }
    }
}
