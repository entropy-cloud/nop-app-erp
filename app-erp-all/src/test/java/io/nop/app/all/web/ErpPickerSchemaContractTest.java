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
 * Plan 2026-08-24-1147-1 Phase 1.2: picker schema 契约测试 (v3.2 更新, plan 2026-09-02-2028-1).
 *
 * 在 flux 模式下,form JSON 中所有 type=='picker' 节点必须满足 flux PickerSchema v3 契约:
 * - 必须含 pickerPopup 键 (字符串/对象/boolean 均可)
 * - 必须含 valueField 与 labelField (替代 v1 的 valueKey/labelKey)
 * - 必须含 pickerSchema 子树 (含 type + loadAction 或 source)
 * - 必须不含 AMIS 关键字: joinValues / extractValue / x:extends
 * - 必须不含 v1 已废弃字段: pickerDialog / valueKey / labelKey
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
        // F0 (v3): 必须含 pickerPopup
        if (!picker.containsKey("pickerPopup")) return false;
        // F1 (v3): 必须用 valueField/labelField (不能含 v1 的 valueKey/labelKey)
        if (picker.containsKey("valueKey") || picker.containsKey("labelKey")) return false;
        if (!picker.containsKey("valueField") || !picker.containsKey("labelField")) return false;
        // F2 (v3): 必须含 pickerSchema 子树, 且 pickerSchema 必须有 type + loadAction 或 source
        Object pickerSchema = picker.get("pickerSchema");
        if (!(pickerSchema instanceof Map)) return false;
        Map<String, Object> schema = (Map<String, Object>) pickerSchema;
        if (!schema.containsKey("type")) return false;
        boolean schemaHasLoadAction = schema.containsKey("loadAction");
        boolean schemaHasSource = schema.containsKey("source");
        if (!schemaHasLoadAction && !schemaHasSource) return false;
        // F3 (legacy): loadAction.args.url 不能含 literal "null"
        Object loadAction = schema.get("loadAction");
        if (loadAction instanceof Map) {
            Object args = ((Map<?, ?>) loadAction).get("args");
            if (args instanceof Map) {
                Object url = ((Map<?, ?>) args).get("url");
                if (url instanceof String && ((String) url).contains("null")) return false;
            }
        }
        // AMIS 关键字: 必须不含
        for (String keyword : AMIS_KEYWORDS) {
            if (picker.containsKey(keyword)) return false;
        }
        // v1 已废弃字段: 必须不含 pickerDialog
        if (picker.containsKey("pickerDialog")) return false;
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