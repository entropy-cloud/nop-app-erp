
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinVoucherTemplateBiz;
import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;
import app.erp.fin.service.posting.ErpFinPostingErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.dao.api.IEntityDao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.or;

@BizModel("ErpFinVoucherTemplate")
public class ErpFinVoucherTemplateBizModel extends CrudBizModel<ErpFinVoucherTemplate> implements IErpFinVoucherTemplateBiz {
    public ErpFinVoucherTemplateBizModel(){
        setEntityName(ErpFinVoucherTemplate.class.getName());
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * 凭证模板预览（F16 P1，plan §Phase 2，Phase 0 Explore (b) 候选 (c) 落地）。
     *
     * <p>按 {@code businessType} 读取启用模板 + 行，对每行：
     * <ul>
     *   <li>{@code subjectCode}/{@code memoTemplate} 做 {@code ${placeholder}} 替换（context 变量）；</li>
     *   <li>金额：{@code amountKey} 命中 context 则取其值；否则 {@code amountExpression} 经最小安全算术求值器
     *       （BigDecimal + {@code + - * / ()} + 变量引用，白名单字符、无反射）求值；否则 0。</li>
     * </ul>
     *
     * <p>仅用于前端预览，**不触碰过账引擎**（既有 {@code ErpFinTemplateAcctDocProvider} 保持字面/amountKey 路径）。
     * 算术表达式进真实过账归 successor（plan §Deferred）。
     *
     * @param businessType 业务类型（模板查找键）
     * @param context      占位符/变量上下文（如 {@code {DOC_TOTAL:1000}}）
     * @return 预览分录行列表，每行含 lineNo/subjectCode/dcDirection/debitAmount/creditAmount/memo/accountKey/amountKey
     */
    @BizMutation
    public List<Map<String, Object>> renderTemplate(
            @Name("businessType") String businessType,
            @Name("context") Map<String, Object> context) {
        Map<String, Object> ctx = context != null ? context : Collections.emptyMap();
        ErpFinVoucherTemplate template = findActiveTemplate(businessType);
        if (template == null) {
            throw new NopException(ErpFinPostingErrors.ERR_TEMPLATE_NOT_FOUND)
                    .param(ErpFinPostingErrors.ARG_BUSINESS_TYPE, businessType);
        }
        List<ErpFinVoucherTemplateLine> lines = new ArrayList<>(template.getLines());
        lines.sort(Comparator.comparingInt(l -> l.getLineNo() == null ? Integer.MAX_VALUE : l.getLineNo()));

        List<Map<String, Object>> result = new ArrayList<>(lines.size());
        for (ErpFinVoucherTemplateLine line : lines) {
            BigDecimal amount = resolveAmount(line, ctx);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineNo", line.getLineNo());
            row.put("subjectCode", resolvePlaceholders(line.getSubjectCode(), ctx));
            row.put("dcDirection", line.getDcDirection());
            boolean isDebit = "DEBIT".equalsIgnoreCase(line.getDcDirection());
            row.put("debitAmount", isDebit ? amount : BigDecimal.ZERO);
            row.put("creditAmount", isDebit ? BigDecimal.ZERO : amount);
            row.put("memo", resolvePlaceholders(line.getMemoTemplate(), ctx));
            row.put("accountKey", line.getAccountKey());
            row.put("amountKey", line.getAmountKey());
            result.add(row);
        }
        return result;
    }

    private ErpFinVoucherTemplate findActiveTemplate(String businessType) {
        IEntityDao<ErpFinVoucherTemplate> dao = dao();
        QueryBean q = new QueryBean();
        q.addFilter(eq("businessType", businessType));
        q.addFilter(eq("isActive", Boolean.TRUE));
        q.addFilter(or(eq("acctSchemaId", null), isNull("acctSchemaId")));
        List<ErpFinVoucherTemplate> candidates = dao.findAllByQuery(q);
        LocalDate today = LocalDate.now();
        ErpFinVoucherTemplate generic = null;
        for (ErpFinVoucherTemplate t : candidates) {
            if (!inValidRange(t, today)) {
                continue;
            }
            if (generic == null) {
                generic = t;
            }
        }
        return generic;
    }

    private boolean inValidRange(ErpFinVoucherTemplate t, LocalDate date) {
        if (date == null) {
            return true;
        }
        LocalDate from = t.getValidFrom();
        LocalDate to = t.getValidTo();
        if (from != null && date.isBefore(from)) {
            return false;
        }
        if (to != null && date.isAfter(to)) {
            return false;
        }
        return true;
    }

    private BigDecimal resolveAmount(ErpFinVoucherTemplateLine line, Map<String, Object> ctx) {
        String amountKey = line.getAmountKey();
        if (amountKey != null && !amountKey.isEmpty()) {
            Object raw = ctx.get(amountKey);
            if (raw != null) {
                return toBigDecimal(raw);
            }
        }
        String expr = line.getAmountExpression();
        if (expr != null && !expr.isEmpty()) {
            // 先尝试字面量（兼容既有 provider 行为），失败则走算术求值
            String trimmed = expr.trim();
            try {
                return new BigDecimal(trimmed);
            } catch (NumberFormatException notLiteral) {
                return TemplateExprEvaluator.eval(trimmed, ctx);
            }
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal toBigDecimal(Object raw) {
        try {
            return new BigDecimal(raw.toString().trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String resolvePlaceholders(String template, Map<String, Object> ctx) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object val = ctx.get(key);
            m.appendReplacement(sb, Matcher.quoteReplacement(val == null ? "" : val.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 最小安全算术求值器（F16 P1）。仅支持 BigDecimal 四则运算 {@code + - * /}、括号、一元负号、
     * 与十进制数字字面量及标识符变量引用（从 context Map 解析）。白名单字符集，无反射、无代码执行，
     * 除零 / 未定义变量 / 非法字符均抛 NopException。用于凭证模板预览，不接入过账引擎。
     */
    static final class TemplateExprEvaluator {
        static BigDecimal eval(String expr, Map<String, Object> ctx) {
            Parser p = new Parser(expr, ctx);
            BigDecimal v = p.parseExpr();
            p.expectEnd();
            return v;
        }
    }

    private static final class Parser {
        private final String s;
        private final int n;
        private int pos = 0;
        private final Map<String, Object> ctx;

        Parser(String s, Map<String, Object> ctx) {
            this.s = s;
            this.n = s.length();
            this.ctx = ctx;
        }

        BigDecimal parseExpr() {
            BigDecimal v = parseTerm();
            skipWs();
            while (pos < n) {
                char c = s.charAt(pos);
                if (c == '+') {
                    pos++;
                    v = v.add(parseTerm());
                } else if (c == '-') {
                    pos++;
                    v = v.subtract(parseTerm());
                } else {
                    break;
                }
                skipWs();
            }
            return v;
        }

        BigDecimal parseTerm() {
            BigDecimal v = parseFactor();
            skipWs();
            while (pos < n) {
                char c = s.charAt(pos);
                if (c == '*') {
                    pos++;
                    v = v.multiply(parseFactor());
                } else if (c == '/') {
                    pos++;
                    BigDecimal divisor = parseFactor();
                    if (divisor.signum() == 0) {
                        throw new NopException(ErpFinPostingErrors.ERR_TEMPLATE_EXPR_DIV_ZERO)
                                .param(ErpFinPostingErrors.ARG_EXPRESSION, s);
                    }
                    v = v.divide(divisor, 8, RoundingMode.HALF_UP);
                } else {
                    break;
                }
                skipWs();
            }
            return v;
        }

        BigDecimal parseFactor() {
            skipWs();
            if (pos >= n) {
                throw invalid("表达式为空或意外结束");
            }
            char c = s.charAt(pos);
            if (c == '(') {
                pos++;
                BigDecimal v = parseExpr();
                skipWs();
                if (pos >= n || s.charAt(pos) != ')') {
                    throw invalid("括号不匹配，缺少 ')'");
                }
                pos++;
                return v;
            }
            if (c == '-') {
                pos++;
                return parseFactor().negate();
            }
            if (c == '+') {
                pos++;
                return parseFactor();
            }
            if (isIdentStart(c)) {
                return readVariable();
            }
            if (isDigit(c) || c == '.') {
                return readNumber();
            }
            throw invalid("非白名单字符 '" + c + "'");
        }

        private BigDecimal readVariable() {
            int start = pos;
            pos++;
            while (pos < n && isIdentPart(s.charAt(pos))) {
                pos++;
            }
            String name = s.substring(start, pos);
            Object val = ctx.get(name);
            if (val == null) {
                throw new NopException(ErpFinPostingErrors.ERR_TEMPLATE_EXPR_VAR_UNDEF)
                        .param(ErpFinPostingErrors.ARG_EXPRESSION, s)
                        .param(ErpFinPostingErrors.ARG_VAR, name);
            }
            try {
                return new BigDecimal(val.toString().trim());
            } catch (NumberFormatException e) {
                throw new NopException(ErpFinPostingErrors.ERR_TEMPLATE_EXPR_INVALID)
                        .param(ErpFinPostingErrors.ARG_EXPRESSION, s)
                        .param(ErpFinPostingErrors.ARG_REASON, "变量 " + name + " 值非数字：" + val);
            }
        }

        private BigDecimal readNumber() {
            int start = pos;
            while (pos < n && (isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
                pos++;
            }
            String num = s.substring(start, pos);
            try {
                return new BigDecimal(num);
            } catch (NumberFormatException e) {
                throw invalid("非法数字字面量：" + num);
            }
        }

        void expectEnd() {
            skipWs();
            if (pos < n) {
                throw invalid("表达式尾部存在未解析字符 '" + s.charAt(pos) + "'");
            }
        }

        private void skipWs() {
            while (pos < n && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private static boolean isIdentStart(char c) {
            return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
        }

        private static boolean isIdentPart(char c) {
            return isIdentStart(c) || isDigit(c);
        }

        private NopException invalid(String reason) {
            return new NopException(ErpFinPostingErrors.ERR_TEMPLATE_EXPR_INVALID)
                    .param(ErpFinPostingErrors.ARG_EXPRESSION, s)
                    .param(ErpFinPostingErrors.ARG_REASON, reason);
        }
    }
}
