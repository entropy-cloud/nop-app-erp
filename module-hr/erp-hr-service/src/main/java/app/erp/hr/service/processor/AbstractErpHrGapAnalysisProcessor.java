package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrAssessmentDetailBiz;
import app.erp.hr.biz.IErpHrEmployeeAssessmentBiz;
import app.erp.hr.biz.IErpHrRoleCompetencyBiz;
import app.erp.hr.dao.entity.ErpHrAssessmentDetail;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmployeeAssessment;
import app.erp.hr.dao.entity.ErpHrGapAnalysis;
import app.erp.hr.dao.entity.ErpHrRoleCompetency;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.competency.AssessmentAggregator;
import app.erp.hr.service.competency.GapAnalysisCalculator;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 差距分析 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 refreshGapAnalysis/refreshGapAnalysisWithLevels 共用的规范化、岗位定位、胜任力要求加载、最新评估聚合、
 * 清旧重建与差距计算辅助（单一真相源）。子类只编排单 mutation 步骤顺序。
 *
 * <p>跨实体读 RoleCompetency 经 {@link IErpHrRoleCompetencyBiz}、读 Employee 取 positionId
 * 经 {@code daoProvider}（Employee 主数据本域直接 dao 即可，无需 IBiz 抽象——仅取外键字段）。
 */
public abstract class AbstractErpHrGapAnalysisProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpHrRoleCompetencyBiz roleCompetencyBiz;
    @Inject
    IErpHrEmployeeAssessmentBiz employeeAssessmentBiz;
    @Inject
    IErpHrAssessmentDetailBiz assessmentDetailBiz;
    @Inject
    GapAnalysisCalculator gapAnalysisCalculator;
    @Inject
    AssessmentAggregator assessmentAggregator;

    protected IEntityDao<ErpHrGapAnalysis> gapDao() {
        return daoProvider.daoFor(ErpHrGapAnalysis.class);
    }

    /**
     * 规范化 aggregatedLevels 入参的键类型。GraphQL generic {@code Map} scalar 经 JSON 反序列化后键为 String，
     * 而内部 Java 调用方（{@code completeAssessment}）传入的是 {@code Map<Long,Integer>}；GapAnalysisCalculator
     * 按 {@code competencyId（Long）} 取值，键类型不匹配会一律回退到默认值 0。这里统一转 Long 键使两条入口行为一致。
     */
    static Map<Long, Integer> normalizeLevelMap(Map<Long, Integer> raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        Map<Long, Integer> normalized = new HashMap<>(raw.size());
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            normalized.put(toLongKey(e.getKey()), toIntValue(e.getValue()));
        }
        return normalized;
    }

    static Long toLongKey(Object key) {
        if (key == null) return null;
        if (key instanceof Long) return (Long) key;
        if (key instanceof Number) return ((Number) key).longValue();
        if (key instanceof String) return Long.parseLong((String) key);
        throw new NopException(ErpHrErrors.ERR_GAP_INVALID_LEVEL_MAP)
                .param(ErpHrErrors.ARG_LEVEL_MAP_KEY, key);
    }

    static Integer toIntValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) return Integer.parseInt((String) value);
        throw new NopException(ErpHrErrors.ERR_GAP_INVALID_LEVEL_MAP)
                .param(ErpHrErrors.ARG_LEVEL_MAP_VALUE, value);
    }

    protected Long loadPositionId(Long employeeId) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = dao.getEntityById(employeeId);
        if (emp == null) {
            throw new NopException(ErpHrErrors.ERR_GAP_NO_ROLE_REQUIREMENT)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId);
        }
        return emp.getPositionId();
    }

    protected List<ErpHrRoleCompetency> findRoleCompetencies(Long positionId, IServiceContext context) {
        if (positionId == null) {
            return new ArrayList<>();
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("positionId", positionId));
        return roleCompetencyBiz.findList(q, null, context);
    }

    /**
     * 聚合员工最新一次 COMPLETED 评估中各胜任力的 actualLevel。
     * AssessmentDetail 在评估 COMPLETED 时已被写回聚合后 level，故这里按 competencyId 分组取均值
     * 即可（多源已聚合到 detail）；若同员工同期存在多个 detail 仍以均值表示最终级别。
     */
    protected Map<Long, Integer> aggregateLatestAssessment(Long employeeId, IServiceContext context) {
        ErpHrEmployeeAssessment latest = findLatestCompletedAssessment(employeeId, context);
        Map<Long, Integer> result = new HashMap<>();
        if (latest == null) {
            return result;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("assessmentId", latest.getId()));
        List<ErpHrAssessmentDetail> details = assessmentDetailBiz.findList(q, null, context);
        Map<Long, List<ErpHrAssessmentDetail>> byCompetency = new HashMap<>();
        for (ErpHrAssessmentDetail d : details) {
            if (d.getCompetencyId() == null) continue;
            byCompetency.computeIfAbsent(d.getCompetencyId(), k -> new ArrayList<>()).add(d);
        }
        for (Map.Entry<Long, List<ErpHrAssessmentDetail>> e : byCompetency.entrySet()) {
            int aggregated = assessmentAggregator.aggregate(
                    e.getKey(), latest.getAssessmentType(), e.getValue());
            result.put(e.getKey(), aggregated);
        }
        return result;
    }

    protected ErpHrEmployeeAssessment findLatestCompletedAssessment(Long employeeId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("status", ErpHrConstants.ASSESSMENT_STATUS_COMPLETED)));
        q.addOrderField("assessmentDate", true);
        q.setLimit(1);
        return employeeAssessmentBiz.findFirst(q, null, context);
    }

    protected LocalDate latestAssessmentDate(Long employeeId, IServiceContext context) {
        ErpHrEmployeeAssessment latest = findLatestCompletedAssessment(employeeId, context);
        return latest != null ? latest.getAssessmentDate() : CoreMetrics.currentDate();
    }

    protected void deleteExistingGaps(Long employeeId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", employeeId));
        List<ErpHrGapAnalysis> existing = gapDao().findAllByQuery(q);
        if (existing.isEmpty()) return;
        for (ErpHrGapAnalysis g : existing) {
            gapDao().deleteEntity(g);
        }
    }

    protected List<ErpHrGapAnalysis> doRefreshWithLevels(Long employeeId, Map<Long, Integer> aggregatedLevels,
                                                          IServiceContext context) {
        Long positionId = loadPositionId(employeeId);
        List<ErpHrRoleCompetency> roleCompetencies = findRoleCompetencies(positionId, context);
        if (roleCompetencies.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_GAP_NO_ROLE_REQUIREMENT)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId);
        }

        LocalDate assessmentDate = latestAssessmentDate(employeeId, context);

        deleteExistingGaps(employeeId, context);

        List<ErpHrGapAnalysis> gaps = gapAnalysisCalculator.calculate(roleCompetencies, aggregatedLevels, assessmentDate);
        gaps.forEach(g -> {
            g.setEmployeeId(employeeId);
            gapDao().saveEntity(g);
        });
        return gaps;
    }
}
