package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrAssessmentDetailBiz;
import app.erp.hr.biz.IErpHrGapAnalysisBiz;
import app.erp.hr.dao.entity.ErpHrAssessmentDetail;
import app.erp.hr.dao.entity.ErpHrEmployeeAssessment;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.competency.AssessmentAggregator;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.exceptions.UnknownEntityException;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpHrEmployeeAssessment completeAssessment per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含评估完成编排（SUBMITTED 守卫 + 明细存在校验 + 360 多源加权聚合写回 + 综合评分写回 + COMPLETED 状态翻转 +
 * 委托差距刷新直传聚合后 levels）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>假设：ErpHrErrors 未定义评估 not-found 专用错误码，故 {@link #requireAssessment} 复刻
 * {@code CrudBizModel.requireEntity} 的语义，不存在时抛平台 {@link UnknownEntityException}（与原 BizModel 行为一致）。
 */
public class ErpHrEmployeeAssessmentCompleteAssessmentProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpHrAssessmentDetailBiz assessmentDetailBiz;
    @Inject
    IErpHrGapAnalysisBiz gapAnalysisBiz;
    @Inject
    AssessmentAggregator assessmentAggregator;

    public ErpHrEmployeeAssessment completeAssessment(Long assessmentId, IServiceContext context) {
        ErpHrEmployeeAssessment assessment = requireAssessment(assessmentId, context);
        String status = assessment.getStatus();
        if (!Objects.equals(status, ErpHrConstants.ASSESSMENT_STATUS_SUBMITTED)) {
            throw illegalTransition(assessmentId, status, ErpHrConstants.ASSESSMENT_STATUS_COMPLETED);
        }

        List<ErpHrAssessmentDetail> details = findDetails(assessmentId, context);
        if (details.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_ASSESSMENT_NO_DETAILS)
                    .param(ErpHrErrors.ARG_ASSESSMENT_ID, assessmentId);
        }

        Map<Long, Integer> aggregatedLevels = aggregateAndWriteBack(assessment, details, context);

        assessment.setStatus(ErpHrConstants.ASSESSMENT_STATUS_COMPLETED);
        assessmentDao().updateEntity(assessment);

        // 直传聚合后 levels 给差距刷新，避免二次查询跨事务可见性问题
        gapAnalysisBiz.refreshGapAnalysisWithLevels(assessment.getEmployeeId(), aggregatedLevels, context);

        return assessment;
    }

    /**
     * 按胜任力分组聚合各 detail.actualLevel（360 类型加权，其他类型均值），写回 detail.actualLevel
     * 使其反映聚合后级别；同时把综合评分写入 assessment.overallScore（各胜任力聚合 level 的均值）。
     * 返回 competencyId → aggregatedLevel 映射，供差距刷新直接消费。
     */
    protected Map<Long, Integer> aggregateAndWriteBack(ErpHrEmployeeAssessment assessment,
                                                       List<ErpHrAssessmentDetail> details,
                                                       IServiceContext context) {
        Map<Long, List<ErpHrAssessmentDetail>> byCompetency = new HashMap<>();
        for (ErpHrAssessmentDetail d : details) {
            if (d.getCompetencyId() == null) continue;
            byCompetency.computeIfAbsent(d.getCompetencyId(), k -> new java.util.ArrayList<>()).add(d);
        }

        BigDecimal scoreSum = BigDecimal.ZERO;
        int scoreCnt = 0;
        Map<Long, Integer> aggregatedLevels = new HashMap<>();
        for (Map.Entry<Long, List<ErpHrAssessmentDetail>> e : byCompetency.entrySet()) {
            int aggregated = assessmentAggregator.aggregate(
                    e.getKey(), assessment.getAssessmentType(), e.getValue());
            aggregatedLevels.put(e.getKey(), aggregated);
            for (ErpHrAssessmentDetail d : e.getValue()) {
                d.setActualLevel(aggregated);
                assessmentDetailBiz.updateEntity(d, null, context);
            }
            scoreSum = scoreSum.add(BigDecimal.valueOf(aggregated));
            scoreCnt++;
        }
        if (scoreCnt > 0) {
            BigDecimal overall = scoreSum.divide(BigDecimal.valueOf(scoreCnt), 2, RoundingMode.HALF_UP);
            assessment.setOverallScore(overall);
        }
        return aggregatedLevels;
    }

    protected ErpHrEmployeeAssessment requireAssessment(Long assessmentId, IServiceContext context) {
        ErpHrEmployeeAssessment assessment = assessmentDao().getEntityById(assessmentId);
        if (assessment == null) {
            throw new UnknownEntityException(assessmentDao().getEntityName(), assessmentId);
        }
        return assessment;
    }

    protected List<ErpHrAssessmentDetail> findDetails(Long assessmentId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("assessmentId", assessmentId));
        return assessmentDetailBiz.findList(q, null, context);
    }

    protected NopException illegalTransition(Long assessmentId, String current, String expected) {
        return new NopException(ErpHrErrors.ERR_ASSESSMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpHrErrors.ARG_ASSESSMENT_ID, assessmentId)
                .param(ErpHrErrors.ARG_CURRENT_STATUS, current)
                .param(ErpHrErrors.ARG_EXPECTED_STATUS, expected);
    }

    private IEntityDao<ErpHrEmployeeAssessment> assessmentDao() {
        return daoProvider.daoFor(ErpHrEmployeeAssessment.class);
    }
}
