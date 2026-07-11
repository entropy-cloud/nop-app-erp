package app.erp.cs.service;

import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.StringHelper;
import io.nop.core.lang.json.JsonTool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预设应答变量渲染纯函数式工具（canned-response.md §1.3/§3.2）。
 *
 * <p>不注入 Dao，由 BizModel 传入解析后的系统变量 + 用户自定义变量后调用 {@link #render}。
 * 纯函数式便于独立单测（{@code TestCannedResponseRenderer}）。
 *
 * <p>渲染规则：
 * <ol>
 *   <li>合并变量：customVars 覆盖 systemVars（同名键以 customVars 为准）。</li>
 *   <li>必填校验：variableDefs 中 required=true 且合并后值缺失/空 → 抛
 *       {@link ErpCsErrors#ERR_CANNED_RESPONSE_REQUIRED_VAR_MISSING}。</li>
 *   <li>占位符替换：遍历所有合并后变量，{@code {key}} → value（逐字替换，支持任意占位符）。</li>
 *   <li>variableDefs 为 null/空/JSON 非法时退化为「不校验必填、直接替换已知占位符」。</li>
 * </ol>
 */
public final class CannedResponseRenderer {

    private CannedResponseRenderer() {
    }

    /**
     * 渲染模板内容。
     *
     * @param content      模板正文（含 {@code {key}} 占位符）
     * @param variableDefs variableDefs JSON 字符串（canned-response.md §1.3 {@code {variables:[{key,label,required}]}}）
     * @param systemVars   系统变量（customer_name/ticket_id/agent_name/today/now 等，由 BizModel 解析）
     * @param customVars   客服手动填入的自定义变量（覆盖同名系统变量）
     * @return 渲染后的正文
     */
    public static String render(String content, String variableDefs,
                                Map<String, String> systemVars, Map<String, String> customVars) {
        Map<String, String> merged = mergeVars(systemVars, customVars);
        List<VarDef> defs = parseVarDefs(variableDefs);
        validateRequired(defs, merged);
        return replacePlaceholders(content, merged);
    }

    static Map<String, String> mergeVars(Map<String, String> systemVars, Map<String, String> customVars) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (systemVars != null) {
            merged.putAll(systemVars);
        }
        if (customVars != null) {
            merged.putAll(customVars);
        }
        return merged;
    }

    static List<VarDef> parseVarDefs(String variableDefs) {
        if (StringHelper.isEmpty(variableDefs)) {
            return Collections.emptyList();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = JsonTool.parseBeanFromText(variableDefs, Map.class);
            if (root == null) {
                return Collections.emptyList();
            }
            Object vars = root.get("variables");
            if (!(vars instanceof List)) {
                return Collections.emptyList();
            }
            List<VarDef> defs = new ArrayList<>();
            for (Object o : (List<?>) vars) {
                if (!(o instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) o;
                String key = m.get("key") == null ? null : String.valueOf(m.get("key"));
                if (StringHelper.isEmpty(key)) {
                    continue;
                }
                Boolean required = m.get("required") instanceof Boolean
                        ? (Boolean) m.get("required")
                        : null;
                defs.add(new VarDef(key, Boolean.TRUE.equals(required)));
            }
            return defs;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    static void validateRequired(List<VarDef> defs, Map<String, String> merged) {
        for (VarDef d : defs) {
            if (!d.required) {
                continue;
            }
            String val = merged.get(d.key);
            if (StringHelper.isEmpty(val)) {
                throw new NopException(ErpCsErrors.ERR_CANNED_RESPONSE_REQUIRED_VAR_MISSING)
                        .param(ErpCsErrors.ARG_VARIABLE_KEY, d.key);
            }
        }
    }

    static String replacePlaceholders(String content, Map<String, String> merged) {
        if (StringHelper.isEmpty(content) || merged.isEmpty()) {
            return content;
        }
        String result = content;
        for (Map.Entry<String, String> e : merged.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            if (val == null) {
                val = "";
            }
            result = result.replace(key, val);
        }
        return result;
    }

    static final class VarDef {
        final String key;
        final boolean required;

        VarDef(String key, boolean required) {
            this.key = key;
            this.required = required;
        }
    }
}
