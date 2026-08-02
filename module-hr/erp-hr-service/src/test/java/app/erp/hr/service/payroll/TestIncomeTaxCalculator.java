package app.erp.hr.service.payroll;

import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 个税计算器纯函数单元测试（payroll.md §4.2/§4.5）。
 * <ul>
 *   <li>{@link IncomeTaxCalculator#resolveBracket}：七级累进税率表档位解析（P1-MA4-016 末档 null 防御）。</li>
 *   <li>{@link IncomeTaxCalculator#parseCumulativeData}：累计薪酬 JSON 解析（P1-MA4-018 移除静默吞）。</li>
 * </ul>
 */
public class TestIncomeTaxCalculator {

    /** 构建七级超额累进税率表（对齐 seedTaxConfig + seed erp_hr_tax_config，末档 null 表「无上限」）。 */
    private List<TaxBracket> sevenBrackets() {
        List<TaxBracket> b = new ArrayList<>();
        b.add(new TaxBracket(new BigDecimal("36000"), new BigDecimal("0.03"), new BigDecimal("0")));
        b.add(new TaxBracket(new BigDecimal("144000"), new BigDecimal("0.10"), new BigDecimal("2520")));
        b.add(new TaxBracket(new BigDecimal("300000"), new BigDecimal("0.20"), new BigDecimal("16920")));
        b.add(new TaxBracket(new BigDecimal("420000"), new BigDecimal("0.25"), new BigDecimal("31920")));
        b.add(new TaxBracket(new BigDecimal("660000"), new BigDecimal("0.30"), new BigDecimal("52920")));
        b.add(new TaxBracket(new BigDecimal("960000"), new BigDecimal("0.35"), new BigDecimal("85920")));
        b.add(new TaxBracket(null, new BigDecimal("0.45"), new BigDecimal("181920")));
        return b;
    }

    // ===== P1-MA4-016：resolveBracket 末档 null 防御 =====

    @Test
    public void resolveBracket_highIncomeAboveMaxLimit_hitsLastBracketNotNpe() {
        List<TaxBracket> brackets = sevenBrackets();
        // 累计应纳税所得额 > 960000（前 6 档全跳过，触达末档 rangeUpperLimit=null）
        BigDecimal income = new BigDecimal("1500000");
        TaxBracket bracket = assertDoesNotThrow(() -> IncomeTaxCalculator.resolveBracket(brackets, income),
                "高收入累计不再 NPE，命中末档");
        assertEquals(0, new BigDecimal("0.45").compareTo(bracket.getRate()), "命中末档 45%");
        assertEquals(0, new BigDecimal("181920").compareTo(bracket.getQuickDeduction()));
        // 累计税额 = 1500000 × 0.45 − 181920 = 493080（验证算术可正确计算，非 NPE）
        BigDecimal cumTax = income.multiply(bracket.getRate()).subtract(bracket.getQuickDeduction());
        assertEquals(0, new BigDecimal("493080").compareTo(cumTax), "高档税率计算正确");
    }

    @Test
    public void resolveBracket_withinMaxLimit_hitsSixthBracket() {
        List<TaxBracket> brackets = sevenBrackets();
        // 累计应纳税所得额 ≤ 960000 命中前 6 档（行为不变）
        BigDecimal income = new BigDecimal("500000");
        TaxBracket bracket = IncomeTaxCalculator.resolveBracket(brackets, income);
        assertEquals(0, new BigDecimal("0.30").compareTo(bracket.getRate()), "命中第 5 档 30%");
    }

    @Test
    public void resolveBracket_boundaryAtMaxLimit_hitsSixthBracketUpperBound() {
        List<TaxBracket> brackets = sevenBrackets();
        // 边界值 = 960000（命中第 6 档上界，35%）
        BigDecimal income = new BigDecimal("960000");
        TaxBracket bracket = IncomeTaxCalculator.resolveBracket(brackets, income);
        assertEquals(0, new BigDecimal("0.35").compareTo(bracket.getRate()), "边界值 960000 命中第 6 档 35%");
    }

    @Test
    public void resolveBracket_justAboveMaxLimit_hitsLastBracket() {
        List<TaxBracket> brackets = sevenBrackets();
        // 960000.01 刚越过第 6 档上界 → 末档 45%
        BigDecimal income = new BigDecimal("960000.01");
        TaxBracket bracket = IncomeTaxCalculator.resolveBracket(brackets, income);
        assertEquals(0, new BigDecimal("0.45").compareTo(bracket.getRate()), "刚越过上界命中末档 45%");
    }

    // ===== P1-MA4-018：parseCumulativeData 移除静默吞 =====

    @Test
    public void parseCumulativeData_corruptJson_throwsErrorCodeNotSilentReset() {
        // 损坏 JSON（非法格式）→ 抛 ERR_HR_CUMULATIVE_DATA_CORRUPT，非静默返回空 map
        NopException ex = assertThrows(NopException.class,
                () -> IncomeTaxCalculator.parseCumulativeData("{not-valid-json", 100L, 2026));
        assertEquals(ErpHrErrors.ERR_HR_CUMULATIVE_DATA_CORRUPT.getErrorCode(), ex.getErrorCode());
        assertEquals(100L, ex.getParam(ErpHrErrors.ARG_EMPLOYEE_ID));
        assertEquals(2026, ex.getParam(ErpHrErrors.ARG_YEAR));
    }

    @Test
    public void parseCumulativeData_nullOrEmpty_returnsEmptyMap() {
        // null/空 json 返回空 map（1 月无历史合法路径不变）
        assertTrue(IncomeTaxCalculator.parseCumulativeData(null, 100L, 2026).isEmpty(),
                "null json 返回空 map");
        assertTrue(IncomeTaxCalculator.parseCumulativeData("", 100L, 2026).isEmpty(),
                "空 json 返回空 map");
        assertTrue(IncomeTaxCalculator.parseCumulativeData("   ", 100L, 2026).isEmpty(),
                "空白 json 返回空 map");
    }

    @Test
    public void parseCumulativeData_validJson_parsesNormally() {
        // 合法 JSON → 正常解析（行为不变）
        Map<String, BigDecimal> result = IncomeTaxCalculator.parseCumulativeData(
                "{\"cumulativeGross\":60000.00,\"cumulativePrepaidTax\":870.00}", 100L, 2026);
        assertNotNull(result);
        assertEquals(0, new BigDecimal("60000.00").compareTo(result.get("cumulativeGross")));
        assertEquals(0, new BigDecimal("870.00").compareTo(result.get("cumulativePrepaidTax")));
    }
}
