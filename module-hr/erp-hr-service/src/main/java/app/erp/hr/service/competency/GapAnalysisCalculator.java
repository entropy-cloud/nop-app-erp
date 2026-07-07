package app.erp.hr.service.competency;

import app.erp.hr.dao.entity.ErpHrGapAnalysis;
import app.erp.hr.dao.entity.ErpHrRoleCompetency;
import app.erp.hr.service.ErpHrConfigs;
import app.erp.hr.service.ErpHrConstants;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 差距分析引擎（competency-management.md §差距分析/§差距严重程度规则）。纯函数式：
 * 对每个岗位-胜任力组合计算 {@code gapValue = requiredLevel - actualLevel}，按规则映射 gapSeverity
 * （≤0 NONE / 1 MINOR / 2 MODERATE / ≥criticalThreshold CRITICAL，阈值 config-gated 默认 3）。
 *
 * <p>不直接持久化——返回内存 ErpHrGapAnalysis 列表，由 {@link app.erp.hr.biz.IErpHrGapAnalysisBiz}
 * 的清旧重建范式持久化（对齐 0700-1 forecast 快照范式）。
 *
 * <p>注入 {@link IDaoProvider} 仅为 {@link #newGapEntity()} 实体工厂；核心 {@link #calculate} 不依赖 IoC，
 * 便于单测直接构造入参。
 */
@Singleton
public class GapAnalysisCalculator {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 对员工的所有岗位-胜任力组合计算差距。
     *
     * @param roleCompetencies 该员工岗位的全部 RoleCompetency 要求
     * @param aggregatedLevels key=competencyId，value=该员工该胜任力聚合后 actualLevel（来自评估）
     * @param assessmentDate   评估日期（写入快照）
     * @return 待持久化的 ErpHrGapAnalysis 内存对象列表（competencyId 缺实际等级时按 0 计算）
     */
    public List<ErpHrGapAnalysis> calculate(List<ErpHrRoleCompetency> roleCompetencies,
                                            Map<Long, Integer> aggregatedLevels,
                                            LocalDate assessmentDate) {
        java.util.List<ErpHrGapAnalysis> result = new java.util.ArrayList<>();
        if (roleCompetencies == null) return result;

        int criticalThreshold = ErpHrConfigs.gapCriticalThreshold();
        LocalDateTime analysisDate = LocalDateTime.now();

        for (ErpHrRoleCompetency rc : roleCompetencies) {
            if (rc.getCompetencyId() == null || rc.getRequiredLevel() == null) continue;
            Integer actual = aggregatedLevels.getOrDefault(rc.getCompetencyId(), 0);
            int required = rc.getRequiredLevel();
            int gapValue = required - actual;
            String severity = severityOf(gapValue, criticalThreshold);

            ErpHrGapAnalysis gap = newGapEntity();
            gap.setCompetencyId(rc.getCompetencyId());
            gap.setRequiredLevel(required);
            gap.setActualLevel(actual);
            gap.setGapValue(gapValue);
            gap.setGapSeverity(severity);
            gap.setAssessmentDate(assessmentDate);
            gap.setAnalysisDate(analysisDate);
            result.add(gap);
        }
        return result;
    }

    /**
     * gapValue → gapSeverity 映射（competency-management.md §差距严重程度规则）。
     * 公开便于单测直接验证。
     */
    public String severityOf(int gapValue, int criticalThreshold) {
        if (gapValue <= 0) return ErpHrConstants.GAP_SEVERITY_NONE;
        if (gapValue == 1) return ErpHrConstants.GAP_SEVERITY_MINOR;
        if (gapValue == 2) return ErpHrConstants.GAP_SEVERITY_MODERATE;
        if (gapValue >= criticalThreshold) return ErpHrConstants.GAP_SEVERITY_CRITICAL;
        // 介于 3 和 criticalThreshold-1 之间（仅当 criticalThreshold > 3 时可达）按 MODERATE 处理
        return ErpHrConstants.GAP_SEVERITY_MODERATE;
    }

    ErpHrGapAnalysis newGapEntity() {
        if (daoProvider != null) {
            IEntityDao<ErpHrGapAnalysis> dao = daoProvider.daoFor(ErpHrGapAnalysis.class);
            return dao.newEntity();
        }
        return new ErpHrGapAnalysis();
    }
}
