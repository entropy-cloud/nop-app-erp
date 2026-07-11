package app.erp.cs.service;

import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CannedResponseRenderer} 纯函数式工具单元测试（plan 2026-07-11-1234-2 §Phase 4）。
 *
 * <p>不依赖 IoC/H2，纯 JUnit 5 — 验证变量替换 / 自定义覆盖 / 必填校验 / 无变量模板 / JSON variableDefs 解析。
 */
public class TestCannedResponseRenderer {

    private static final String VAR_DEFS = "{\"variables\":[" +
            "{\"key\":\"{customer_name}\",\"label\":\"客户名称\",\"required\":true}," +
            "{\"key\":\"{ticket_id}\",\"label\":\"工单编号\",\"required\":true}," +
            "{\"key\":\"{agent_name}\",\"label\":\"客服姓名\",\"required\":false}," +
            "{\"key\":\"{product_name}\",\"label\":\"产品名称\",\"required\":false}" +
            "]}";

    @Test
    public void testSystemVariableReplacement() {
        Map<String, String> systemVars = new HashMap<>();
        systemVars.put("{customer_name}", "ACME公司");
        systemVars.put("{ticket_id}", "TK-001");
        systemVars.put("{agent_name}", "客服小王");
        systemVars.put("{today}", "2026-07-11");

        String content = "您好 {customer_name}，工单 {ticket_id} 由 {agent_name} 处理，日期 {today}";
        String result = CannedResponseRenderer.render(content, VAR_DEFS, systemVars, null);

        assertEquals("您好 ACME公司，工单 TK-001 由 客服小王 处理，日期 2026-07-11", result);
    }

    @Test
    public void testCustomVariableOverridesSystem() {
        Map<String, String> systemVars = new HashMap<>();
        systemVars.put("{customer_name}", "系统名");
        systemVars.put("{ticket_id}", "TK-002");
        Map<String, String> customVars = new HashMap<>();
        customVars.put("{customer_name}", "自定义名");

        String content = "{customer_name} {ticket_id}";
        String result = CannedResponseRenderer.render(content, VAR_DEFS, systemVars, customVars);

        assertEquals("自定义名 TK-002", result);
    }

    @Test
    public void testRequiredVarMissingThrows() {
        Map<String, String> systemVars = new HashMap<>();
        systemVars.put("{ticket_id}", "TK-003");
        // customer_name 是 required=true，缺失

        String content = "您好 {customer_name}";
        NopException ex = assertThrows(NopException.class, () ->
                CannedResponseRenderer.render(content, VAR_DEFS, systemVars, null));
        assertEquals(ErpCsErrors.ERR_CANNED_RESPONSE_REQUIRED_VAR_MISSING.getErrorCode(), ex.getErrorCode());
        assertEquals("{customer_name}", ex.getParam("variableKey"));
    }

    @Test
    public void testNoVariableTemplateReturnsAsIs() {
        String content = "这是一段没有变量的固定文本";
        String result = CannedResponseRenderer.render(content, null, new HashMap<>(), null);
        assertEquals(content, result);
    }

    @Test
    public void testNullVariableDefsSkipsRequiredValidation() {
        Map<String, String> systemVars = new HashMap<>();
        systemVars.put("{today}", "2026-07-11");

        String content = "日期 {today}，缺失变量 {missing_var}";
        String result = CannedResponseRenderer.render(content, null, systemVars, null);

        assertEquals("日期 2026-07-11，缺失变量 {missing_var}", result);
    }

    @Test
    public void testMalformedJsonVariableDefsDegradesGracefully() {
        Map<String, String> systemVars = new HashMap<>();
        systemVars.put("{today}", "2026-07-11");

        String content = "日期 {today}";
        String result = CannedResponseRenderer.render(content, "{invalid json", systemVars, null);
        assertEquals("日期 2026-07-11", result);
    }

    @Test
    public void testOptionalVarNotPresentNoError() {
        Map<String, String> systemVars = new HashMap<>();
        systemVars.put("{customer_name}", "ACME");
        systemVars.put("{ticket_id}", "TK-004");
        // product_name 是 required=false，缺失不报错

        String content = "{customer_name} {ticket_id} {product_name}";
        String result = CannedResponseRenderer.render(content, VAR_DEFS, systemVars, null);

        assertEquals("ACME TK-004 {product_name}", result);
    }

    @Test
    public void testParseVarDefsHandlesEmptyList() {
        assertTrue(CannedResponseRenderer.parseVarDefs(null).isEmpty());
        assertTrue(CannedResponseRenderer.parseVarDefs("").isEmpty());
        assertTrue(CannedResponseRenderer.parseVarDefs("{}").isEmpty());
    }

    @Test
    public void testParseVarDefsExtractsRequired() {
        var defs = CannedResponseRenderer.parseVarDefs(VAR_DEFS);
        assertFalse(defs.isEmpty());
        assertTrue(defs.stream().anyMatch(d -> d.key.equals("{customer_name}") && d.required));
        assertTrue(defs.stream().anyMatch(d -> d.key.equals("{product_name}") && !d.required));
    }
}
