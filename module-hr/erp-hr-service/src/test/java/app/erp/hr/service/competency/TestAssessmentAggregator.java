package app.erp.hr.service.competency;

import app.erp.hr.dao.entity.ErpHrAssessmentDetail;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AssessmentAggregator 单元测试（competency-management.md §聚合规则）。覆盖：
 * <ul>
 *   <li>四类型齐全加权平均（默认权重 15%/50%/25%/10%）。</li>
 *   <li>缺类型重归一化（仅 MANAGER+PEER 时按两者权重归一）。</li>
 *   <li>全缺抛 ERR_AGGREGATE_NO_DETAILS。</li>
 *   <li>非 360 类型（SELF 单源）跳过加权取均值。</li>
 * </ul>
 *
 * <p>使用 {@link JunitAutoTestCase} 仅为获取 IoC 容器（{@code AppConfig.var} 默认值回退）；
 * 无 DB 读写，直接断言纯函数返回值。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestAssessmentAggregator extends JunitAutoTestCase {

    @Inject
    AssessmentAggregator aggregator;

    @Test
    public void testFull360WeightedAverage() {
        // self=3 (w15%), manager=4 (w50%), peer avg=(3+5)/2=4 (w25%), sub avg=5 (w10%)
        // 期望：0.15*3 + 0.50*4 + 0.25*4 + 0.10*5 = 0.45+2.0+1.0+0.5 = 3.95 → 4
        List<ErpHrAssessmentDetail> details = Arrays.asList(
                detail(1001L, ErpHrConstants.ASSESSMENT_TYPE_SELF, 3),
                detail(1001L, ErpHrConstants.ASSESSMENT_TYPE_MANAGER, 4),
                detail(1001L, ErpHrConstants.ASSESSMENT_TYPE_PEER, 3),
                detail(1001L, ErpHrConstants.ASSESSMENT_TYPE_PEER, 5),
                detail(1001L, ErpHrConstants.ASSESSMENT_TYPE_SUBORDINATE, 5));
        int result = aggregator.aggregate(1001L, ErpHrConstants.ASSESSMENT_TYPE_360, details);
        assertEquals(4, result);
    }

    @Test
    public void testMissingTypeReNormalization() {
        // 仅 MANAGER=5 (w50%) 和 PEER avg=4 (w25%)：剩余权重 0.75 归一
        // 期望：(0.50*5 + 0.25*4) / 0.75 = (2.5+1.0)/0.75 = 4.666... → 5
        List<ErpHrAssessmentDetail> details = Arrays.asList(
                detail(1002L, ErpHrConstants.ASSESSMENT_TYPE_MANAGER, 5),
                detail(1002L, ErpHrConstants.ASSESSMENT_TYPE_PEER, 4));
        int result = aggregator.aggregate(1002L, ErpHrConstants.ASSESSMENT_TYPE_360, details);
        assertEquals(5, result);
    }

    @Test
    public void testOnlyManagerWeightedToSelf() {
        // 仅 MANAGER=3 → 归一后等于 3
        List<ErpHrAssessmentDetail> details = Arrays.asList(
                detail(1003L, ErpHrConstants.ASSESSMENT_TYPE_MANAGER, 3));
        int result = aggregator.aggregate(1003L, ErpHrConstants.ASSESSMENT_TYPE_360, details);
        assertEquals(3, result);
    }

    @Test
    public void testAllMissingThrows() {
        NopException ex = assertThrows(NopException.class, () ->
                aggregator.aggregate(1004L, ErpHrConstants.ASSESSMENT_TYPE_360, new ArrayList<>()));
        assertEquals("erp.err.hr.assessment-aggregate-no-details", ex.getErrorCode());
    }

    @Test
    public void testNon360SingleSourceAverages() {
        // assessmentType=SELF，多 detail 取均值
        List<ErpHrAssessmentDetail> details = Arrays.asList(
                detail(1005L, ErpHrConstants.ASSESSMENT_TYPE_SELF, 2),
                detail(1005L, ErpHrConstants.ASSESSMENT_TYPE_SELF, 4));
        int result = aggregator.aggregate(1005L, ErpHrConstants.ASSESSMENT_TYPE_SELF, details);
        assertEquals(3, result);
    }

    @Test
    public void testResultClampedToLevelRange() {
        // 极高加权值应被钳制到 5
        List<ErpHrAssessmentDetail> details = Arrays.asList(
                detail(1006L, ErpHrConstants.ASSESSMENT_TYPE_MANAGER, 5),
                detail(1006L, ErpHrConstants.ASSESSMENT_TYPE_MANAGER, 5),
                detail(1006L, ErpHrConstants.ASSESSMENT_TYPE_PEER, 5),
                detail(1006L, ErpHrConstants.ASSESSMENT_TYPE_SUBORDINATE, 5));
        int result = aggregator.aggregate(1006L, ErpHrConstants.ASSESSMENT_TYPE_360, details);
        assertTrue(result >= ErpHrConstants.COMPETENCY_LEVEL_MIN
                && result <= ErpHrConstants.COMPETENCY_LEVEL_MAX);
        assertEquals(5, result);
    }

    @Test
    public void testAggregateWithLoaderInjectable() {
        // 验证注入式加载函数路径可用（competency-management.md §纯函数式 + 注入加载函数便于单测）
        List<ErpHrAssessmentDetail> details = Arrays.asList(
                detail(1007L, ErpHrConstants.ASSESSMENT_TYPE_MANAGER, 4));
        int result = aggregator.aggregateWithLoader(1007L, ErpHrConstants.ASSESSMENT_TYPE_360,
                cid -> details);
        assertEquals(4, result);
    }

    private ErpHrAssessmentDetail detail(Long competencyId, String sourceType, int level) {
        ErpHrAssessmentDetail d = new ErpHrAssessmentDetail();
        d.setCompetencyId(competencyId);
        d.setSourceType(sourceType);
        d.setActualLevel(level);
        return d;
    }
}
