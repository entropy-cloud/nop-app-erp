package app.erp.hr.service.competency;

import app.erp.hr.dao.entity.ErpHrGapAnalysis;
import app.erp.hr.dao.entity.ErpHrRoleCompetency;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GapAnalysisCalculator 单元测试（competency-management.md §差距严重程度规则）。覆盖：
 * <ul>
 *   <li>gapValue 各档映射 NONE/MINOR/MODERATE/CRITICAL。</li>
 *   <li>无岗位要求返回空列表。</li>
 *   <li>critical 阈值默认 3 触发。</li>
 *   <li>多胜任力同时计算。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestGapAnalysisCalculator extends JunitAutoTestCase {

    @Inject
    GapAnalysisCalculator calculator;

    @Test
    public void testSeverityMappingAllLevels() {
        assertEquals(ErpHrConstants.GAP_SEVERITY_NONE, calculator.severityOf(0, 3));
        assertEquals(ErpHrConstants.GAP_SEVERITY_NONE, calculator.severityOf(-1, 3));
        assertEquals(ErpHrConstants.GAP_SEVERITY_MINOR, calculator.severityOf(1, 3));
        assertEquals(ErpHrConstants.GAP_SEVERITY_MODERATE, calculator.severityOf(2, 3));
        assertEquals(ErpHrConstants.GAP_SEVERITY_CRITICAL, calculator.severityOf(3, 3));
        assertEquals(ErpHrConstants.GAP_SEVERITY_CRITICAL, calculator.severityOf(5, 3));
    }

    @Test
    public void testCalculateProducesAllGapFields() {
        // required=5, actual=2 → gap=3 (CRITICAL, 默认阈值 3)
        ErpHrRoleCompetency rc = roleCompetency(2001L, 5);
        Map<Long, Integer> actuals = new HashMap<>();
        actuals.put(2001L, 2);

        List<ErpHrGapAnalysis> gaps =
                calculator.calculate(Arrays.asList(rc), actuals, LocalDate.of(2026, 7, 1));

        assertEquals(1, gaps.size());
        ErpHrGapAnalysis g = gaps.get(0);
        assertEquals(2001L, g.getCompetencyId());
        assertEquals(5, g.getRequiredLevel().intValue());
        assertEquals(2, g.getActualLevel().intValue());
        assertEquals(3, g.getGapValue().intValue());
        assertEquals(ErpHrConstants.GAP_SEVERITY_CRITICAL, g.getGapSeverity());
    }

    @Test
    public void testCalculateMissingActualTreatedAsZero() {
        // competency 未出现在 actuals → actual=0, required=4 → gap=4 (CRITICAL)
        ErpHrRoleCompetency rc = roleCompetency(2002L, 4);
        List<ErpHrGapAnalysis> gaps =
                calculator.calculate(Arrays.asList(rc), new HashMap<>(), LocalDate.of(2026, 7, 1));
        assertEquals(1, gaps.size());
        assertEquals(0, gaps.get(0).getActualLevel().intValue());
        assertEquals(4, gaps.get(0).getGapValue().intValue());
        assertEquals(ErpHrConstants.GAP_SEVERITY_CRITICAL, gaps.get(0).getGapSeverity());
    }

    @Test
    public void testCalculateEmptyRoleCompetencies() {
        List<ErpHrGapAnalysis> gaps =
                calculator.calculate(null, new HashMap<>(), LocalDate.of(2026, 7, 1));
        assertEquals(0, gaps.size());
    }

    @Test
    public void testCalculateMultipleCompetencies() {
        ErpHrRoleCompetency rc1 = roleCompetency(2010L, 4);
        ErpHrRoleCompetency rc2 = roleCompetency(2011L, 3);
        ErpHrRoleCompetency rc3 = roleCompetency(2012L, 2);
        Map<Long, Integer> actuals = new HashMap<>();
        actuals.put(2010L, 4); // gap=0 → NONE
        actuals.put(2011L, 2); // gap=1 → MINOR
        actuals.put(2012L, 0); // gap=2 → MODERATE

        List<ErpHrGapAnalysis> gaps =
                calculator.calculate(Arrays.asList(rc1, rc2, rc3), actuals, LocalDate.of(2026, 7, 1));
        assertEquals(3, gaps.size());
        Map<Long, String> severityByCompetency = new HashMap<>();
        for (ErpHrGapAnalysis g : gaps) {
            severityByCompetency.put(g.getCompetencyId(), g.getGapSeverity());
        }
        assertEquals(ErpHrConstants.GAP_SEVERITY_NONE, severityByCompetency.get(2010L));
        assertEquals(ErpHrConstants.GAP_SEVERITY_MINOR, severityByCompetency.get(2011L));
        assertEquals(ErpHrConstants.GAP_SEVERITY_MODERATE, severityByCompetency.get(2012L));
    }

    private ErpHrRoleCompetency roleCompetency(Long competencyId, int requiredLevel) {
        ErpHrRoleCompetency rc = new ErpHrRoleCompetency();
        rc.setCompetencyId(competencyId);
        rc.setRequiredLevel(requiredLevel);
        return rc;
    }
}
