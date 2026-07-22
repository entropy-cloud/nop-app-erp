package app.erp.fin.service.entity;

import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 凭证模板金额表达式算术求值器单元测试（F16 P1，plan §Phase 2，Phase 0 Explore (b) 候选 (c)）。
 *
 * <p>纯逻辑测试（无 Nop 基座 / 无 DB），覆盖 {@link ErpFinVoucherTemplateBizModel.TemplateExprEvaluator} 的：
 * <ul>
 *   <li>四则运算 + 括号 + 一元负号 + 优先级；</li>
 *   <li>变量引用（context Map）；</li>
 *   <li>字面量（既有 provider 兼容路径，由 BizModel resolveAmount 字面分支覆盖，此处不重复）；</li>
 *   <li>错误处理：除零 / 未定义变量 / 非白名单字符 / 括号不匹配。</li>
 * </ul>
 */
public class TestErpFinVoucherTemplateExpr {

    private static BigDecimal eval(String expr, Map<String, Object> ctx) {
        return ErpFinVoucherTemplateBizModel.TemplateExprEvaluator.eval(expr, ctx);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    public void literal() {
        assertEquals(bd("1000"), eval("1000", ctx()));
    }

    @Test
    public void decimalLiteral() {
        assertEquals(bd("0.13"), eval("0.13", ctx()));
    }

    @Test
    public void variableRef() {
        assertEquals(bd("1000"), eval("DOC_TOTAL", ctx("DOC_TOTAL", 1000)));
    }

    @Test
    public void multiplyVariable() {
        // roadmap 典型用例：DOC_TOTAL * 0.13 = 130（值等，scale 由 BigDecimal 乘法决定）
        assertEquals(0, eval("DOC_TOTAL * 0.13", ctx("DOC_TOTAL", 1000)).compareTo(bd("130")));
    }

    @Test
    public void addSubtract() {
        // 1000 - 5 + 5 - 5 = 995
        assertEquals(bd("995"), eval("1000 - 5 + 5 - 5", ctx()));
    }

    @Test
    public void precedence() {
        // 2 + 3 * 4 = 14
        assertEquals(bd("14"), eval("2 + 3 * 4", ctx()));
    }

    @Test
    public void parentheses() {
        // (2 + 3) * 4 = 20
        assertEquals(bd("20"), eval("(2 + 3) * 4", ctx()));
    }

    @Test
    public void nestedParensAndUnaryNeg() {
        // -(2 + 3) * (4 - 1) = -15
        assertEquals(bd("-15"), eval("-(2 + 3) * (4 - 1)", ctx()));
    }

    @Test
    public void divisionRounding() {
        // 10 / 3 → HALF_UP scale 8
        assertEquals(new BigDecimal("3.33333333"), eval("10 / 3", ctx()));
    }

    @Test
    public void complexExpression() {
        // (DOC_TOTAL - DISCOUNT) * RATE  = (1000 - 100) * 0.1 = 90
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("DOC_TOTAL", 1000);
        ctx.put("DISCOUNT", 100);
        ctx.put("RATE", 0.1);
        assertEquals(0, eval("(DOC_TOTAL - DISCOUNT) * RATE", ctx).compareTo(bd("90")));
    }

    @Test
    public void whitespaceTolerant() {
        assertEquals(bd("6"), eval("  2   *   3  ", ctx()));
    }

    @Test
    public void errorDivZero() {
        NopException ex = assertThrows(NopException.class, () -> eval("10 / 0", ctx()));
        assertEquals("erp.err.fin.posting.template-expr-div-zero", ex.getErrorCode());
    }

    @Test
    public void errorDivByZeroExpression() {
        assertThrows(NopException.class, () -> eval("10 / (5 - 5)", ctx()));
    }

    @Test
    public void errorUndefinedVar() {
        NopException ex = assertThrows(NopException.class, () -> eval("UNDEFINED_VAR * 2", ctx()));
        assertEquals("erp.err.fin.posting.template-expr-var-undef", ex.getErrorCode());
    }

    @Test
    public void errorIllegalChar() {
        NopException ex = assertThrows(NopException.class, () -> eval("2 # 3", ctx()));
        assertEquals("erp.err.fin.posting.template-expr-invalid", ex.getErrorCode());
    }

    @Test
    public void errorUnbalancedParens() {
        NopException ex = assertThrows(NopException.class, () -> eval("(2 + 3", ctx()));
        assertEquals("erp.err.fin.posting.template-expr-invalid", ex.getErrorCode());
    }

    @Test
    public void errorTrailingGarbage() {
        assertThrows(NopException.class, () -> eval("2 + 3 garbage", ctx()));
    }

    private static Map<String, Object> ctx() {
        return new HashMap<>();
    }

    private static Map<String, Object> ctx(String key, Object val) {
        Map<String, Object> m = new HashMap<>();
        m.put(key, val);
        return m;
    }
}
