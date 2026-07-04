package app.erp.hr.service.payroll;

import io.nop.core.lang.json.JsonTool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 个税税率表 JSON 解析器。{@code ErpHrTaxConfig.taxBrackets} 为 JSON 数组字符串，
 * 每元素形如 {@code {"rangeUpperLimit":36000, "rate":0.03, "quickDeduction":0}}。
 *
 * <p>税率表必须按 rangeUpperLimit 升序排列（payroll.md §4.2）。最后一档 rangeUpperLimit
 * 用 {@code null} 或空表示「无上限」（>960000 档）。
 */
public class TaxBracketParser {

    public List<TaxBracket> parse(String taxBracketsJson) {
        List<TaxBracket> result = new ArrayList<>();
        if (taxBracketsJson == null || taxBracketsJson.trim().isEmpty()) {
            return result;
        }
        Object parsed = JsonTool.parseNonStrict(taxBracketsJson);
        if (!(parsed instanceof List)) {
            return result;
        }
        for (Object item : (List<?>) parsed) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> m = (Map<?, ?>) item;
            BigDecimal upper = toBigDecimal(m.get("rangeUpperLimit"));
            BigDecimal rate = toBigDecimal(m.get("rate"));
            BigDecimal quick = toBigDecimal(m.get("quickDeduction"));
            result.add(new TaxBracket(upper, rate, quick));
        }
        result.sort((a, b) -> {
            if (a.getRangeUpperLimit() == null) return 1;
            if (b.getRangeUpperLimit() == null) return -1;
            return a.getRangeUpperLimit().compareTo(b.getRangeUpperLimit());
        });
        return result;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        try {
            return new BigDecimal(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
