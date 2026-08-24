package io.nop.app.all.web;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.core.resource.component.ResourceComponentManager;
import io.nop.web.WebConfigs;
import io.nop.web.page.PageProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan 2026-08-24-1147-1 Phase 1.2: picker schema 契约测试.
 *
 * 在 flux 模式下,form JSON 中所有 type=='picker' 节点必须满足 flux PickerSchema 契约:
 * - 必须含 pickerDialog 键 (字符串/对象均可)
 * - 必须含 loadAction 或 options 至少一个
 * - 必须不含 AMIS 关键字: joinValues / extractValue / x:extends
 *
 * 当前代码 (nop-entropy flux-control.xlib 输出 AMIS 风格 schema) 应当全红;
 * Phase 2 引入 _vfs/_delta/default/nop/web/xlib/flux-control.xlib 覆盖后应当转绿.
 */
@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE)
public class ErpPickerSchemaContractTest extends JunitBaseTestCase {

    @Inject
    PageProvider pageProvider;

    private static final String[] TEST_PAGES = {
            "/erp/md/pages/ErpMdMaterial/main.page.yaml",
            "/erp/pur/pages/ErpPurOrder/main.page.yaml",
            "/erp/sal/pages/ErpSalOrder/main.page.yaml",
            "/erp/fin/pages/ErpFinVoucher/main.page.yaml",
            "/erp/inv/pages/ErpInvStockMove/main.page.yaml"
    };

    private static final String[] AMIS_KEYWORDS = {"joinValues", "extractValue", "x:extends"};

    @BeforeEach
    public void fluxMode() {
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
    public void pickerFieldsMatchFluxSchema() {
        int totalPickers = 0;
        int compliant = 0;
        List<String> violations = new ArrayList<>();

        for (String path : TEST_PAGES) {
            Map<String, Object> page = pageProvider.getPage(path, AppConfig.defaultLocale());
            List<Map<String, Object>> pickers = new ArrayList<>();
            collectPickers(page, "", pickers);
            totalPickers += pickers.size();
            for (Map<String, Object> picker : pickers) {
                if (isCompliant(picker)) {
                    compliant++;
                } else {
                    violations.add(path + " picker keys=" + picker.keySet());
                }
            }
        }

        System.out.println("=== PICKER_TOTAL: " + totalPickers + ", COMPLIANT: " + compliant + ", VIOLATIONS: " + violations.size() + " ===");
        for (String v : violations) {
            System.out.println("VIOLATION\t" + v);
        }

        assertEquals(totalPickers, compliant,
                "Expected all " + totalPickers + " picker schemas to match flux contract, but " + violations.size() + " violated.");
    }

    private static boolean isCompliant(Map<String, Object> picker) {
        if (!picker.containsKey("pickerDialog")) return false;
        boolean hasLoadAction = picker.containsKey("loadAction");
        boolean hasOptions = picker.containsKey("options");
        if (!hasLoadAction && !hasOptions) return false;
        for (String keyword : AMIS_KEYWORDS) {
            if (picker.containsKey(keyword)) return false;
        }
        // F4: 必须用 valueKey/labelKey (flux PickerSchema 契约), 不能用 valueField/labelField
        if (picker.containsKey("valueField") || picker.containsKey("labelField")) return false;
        // F3: loadAction.args.url 不能含 literal "null" (bizObjName 派生失败的退化痕迹)
        Object loadAction = picker.get("loadAction");
        if (loadAction instanceof Map) {
            Object args = ((Map<?, ?>) loadAction).get("args");
            if (args instanceof Map) {
                Object url = ((Map<?, ?>) args).get("url");
                if (url instanceof String && ((String) url).contains("null")) return false;
            }
        }
        return true;
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