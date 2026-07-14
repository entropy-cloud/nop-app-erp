package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrAssessmentDetailBiz;
import app.erp.hr.biz.IErpHrEmployeeAssessmentBiz;
import app.erp.hr.biz.IErpHrGapAnalysisBiz;
import app.erp.hr.dao.entity.ErpHrAssessmentDetail;
import app.erp.hr.dao.entity.ErpHrEmployeeAssessment;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.competency.AssessmentAggregator;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.biz.crud.EntityData;

/**
 * 员工评估聚合根 BizModel（competency-management.md §评估流程/§360 评估流程）。CRUD 之上承载
 * 评估状态机（DRAFT→SUBMITTED→COMPLETED）与 360 多源加权聚合。
 *
 * <p>跨实体读 AssessmentDetail（明细）经注入 {@link IErpHrAssessmentDetailBiz}（同域 I*Biz）；
 * COMPLETED 时委托 {@link IErpHrGapAnalysisBiz#refreshGapAnalysis} 触发差距快照刷新。
 */
@BizModel("ErpHrEmployeeAssessment")
public class ErpHrEmployeeAssessmentBizModel extends CrudBizModel<ErpHrEmployeeAssessment>
        implements IErpHrEmployeeAssessmentBiz {

    @Inject
    IErpHrAssessmentDetailBiz assessmentDetailBiz;
    @Inject
    IErpHrGapAnalysisBiz gapAnalysisBiz;
    @Inject
    AssessmentAggregator assessmentAggregator;

    public ErpHrEmployeeAssessmentBizModel() {
        setEntityName(ErpHrEmployeeAssessment.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrEmployeeAssessment> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrEmployeeAssessment entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

    @Override
    @BizMutation
    public ErpHrEmployeeAssessment submitAssessment(@Name("assessmentId") Long assessmentId,
                                                     IServiceContext context) {
        ErpHrEmployeeAssessment assessment = requireAssessment(assessmentId, context);
        String status = assessment.getStatus();
        if (!Objects.equals(status, ErpHrConstants.ASSESSMENT_STATUS_DRAFT)) {
            throw illegalTransition(assessmentId, status, ErpHrConstants.ASSESSMENT_STATUS_SUBMITTED);
        }
        List<ErpHrAssessmentDetail> details = findDetails(assessmentId, context);
        if (details.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_ASSESSMENT_NO_DETAILS)
                    .param(ErpHrErrors.ARG_ASSESSMENT_ID, assessmentId);
        }
        assessment.setStatus(ErpHrConstants.ASSESSMENT_STATUS_SUBMITTED);
        updateEntity(assessment, null, context);
        return assessment;
    }

    @Override
    @BizMutation
    public ErpHrEmployeeAssessment completeAssessment(@Name("assessmentId") Long assessmentId,
                                                       IServiceContext context) {
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

        java.util.Map<Long, Integer> aggregatedLevels = aggregateAndWriteBack(assessment, details, context);

        assessment.setStatus(ErpHrConstants.ASSESSMENT_STATUS_COMPLETED);
        updateEntity(assessment, null, context);

        // 直传聚合后 levels 给差距刷新，避免二次查询跨事务可见性问题
        gapAnalysisBiz.refreshGapAnalysisWithLevels(assessment.getEmployeeId(), aggregatedLevels, context);

        return assessment;
    }

    /**
     * 按胜任力分组聚合各 detail.actualLevel（360 类型加权，其他类型均值），写回 detail.actualLevel
     * 使其反映聚合后级别；同时把综合评分写入 assessment.overallScore（各胜任力聚合 level 的均值）。
     * 返回 competencyId → aggregatedLevel 映射，供差距刷新直接消费。
     */
    java.util.Map<Long, Integer> aggregateAndWriteBack(ErpHrEmployeeAssessment assessment,
                                                       List<ErpHrAssessmentDetail> details,
                                                       IServiceContext context) {
        java.util.Map<Long, List<ErpHrAssessmentDetail>> byCompetency = new java.util.HashMap<>();
        for (ErpHrAssessmentDetail d : details) {
            if (d.getCompetencyId() == null) continue;
            byCompetency.computeIfAbsent(d.getCompetencyId(), k -> new java.util.ArrayList<>()).add(d);
        }

        BigDecimal scoreSum = BigDecimal.ZERO;
        int scoreCnt = 0;
        java.util.Map<Long, Integer> aggregatedLevels = new java.util.HashMap<>();
        for (java.util.Map.Entry<Long, List<ErpHrAssessmentDetail>> e : byCompetency.entrySet()) {
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

    ErpHrEmployeeAssessment requireAssessment(Long assessmentId, IServiceContext context) {
        return requireEntity(String.valueOf(assessmentId), null, context);
    }

    List<ErpHrAssessmentDetail> findDetails(Long assessmentId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("assessmentId", assessmentId));
        return assessmentDetailBiz.findList(q, null, context);
    }

    private NopException illegalTransition(Long assessmentId, String current, String expected) {
        return new NopException(ErpHrErrors.ERR_ASSESSMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpHrErrors.ARG_ASSESSMENT_ID, assessmentId)
                .param(ErpHrErrors.ARG_CURRENT_STATUS, current)
                .param(ErpHrErrors.ARG_EXPECTED_STATUS, expected);
    }

    @BizLoader(forType = ErpHrEmployeeAssessment.class)
    public List<String> employeeDisplayName(@ContextSource List<ErpHrEmployeeAssessment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrEmployeeAssessment row : rows) {
            result.add(row.orm_attached() && row.getEmployee() != null ? row.getEmployee().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrEmployeeAssessment.class)
    public List<String> assessorDisplayName(@ContextSource List<ErpHrEmployeeAssessment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("assessor"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrEmployeeAssessment row : rows) {
            result.add(row.orm_attached() && row.getAssessor() != null ? row.getAssessor().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrEmployeeAssessment.class)
    public List<String> orgName(@ContextSource List<ErpHrEmployeeAssessment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrEmployeeAssessment row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
