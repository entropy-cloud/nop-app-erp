package app.erp.qa.service.spc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPC 采样数值统计辅助单测（plan 2026-07-07-0305-2 Phase 2 Exit Criteria）。
 *
 * <p>{@link Statistics} 为纯函数，独立单测无 DB 依赖。
 */
public class TestSpcStatistics {

    @Test
    public void meanOfFiveValues() {
        BigDecimal result = Statistics.mean(toBdList("10", "20", "30", "40", "50"));
        assertEquals(new BigDecimal("30.0000000000"), result);
    }

    @Test
    public void rangeComputesMaxMinusMin() {
        BigDecimal result = Statistics.range(toBdList("10", "20", "30", "40", "55"));
        assertEquals(new BigDecimal("45"), result);
    }

    @Test
    public void stdDevSampleDenominatorNMinusOne() {
        // values: 2,4,4,4,5,5,7,9 -> sample stddev = 2.138...
        BigDecimal result = Statistics.stdDev(toBdList("2", "4", "4", "4", "5", "5", "7", "9"));
        assertNotNull(result);
        assertTrue(result.doubleValue() > 2.13 && result.doubleValue() < 2.14,
                "expected ~2.138, got " + result);
    }

    @Test
    public void populationStdDevDenominatorN() {
        BigDecimal result = Statistics.populationStdDev(toBdList("2", "4", "4", "4", "5", "5", "7", "9"));
        assertNotNull(result);
        assertTrue(result.doubleValue() > 1.99 && result.doubleValue() < 2.01,
                "expected ~2.0, got " + result);
    }

    @Test
    public void emptyOrNullReturnsNull() {
        assertNull(Statistics.mean(null));
        assertNull(Statistics.mean(Collections.emptyList()));
        assertNull(Statistics.range(Collections.emptyList()));
        assertNull(Statistics.stdDev(Collections.singletonList(new BigDecimal("1"))));
    }

    private List<BigDecimal> toBdList(String... values) {
        return Arrays.stream(values).map(BigDecimal::new).collect(Collectors.toList());
    }
}
