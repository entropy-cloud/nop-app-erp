package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrAssessmentDetailBiz;
import app.erp.hr.biz.IErpHrEmployeeAssessmentBiz;
import app.erp.hr.biz.IErpHrGapAnalysisBiz;
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
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 差距分析 BizModel（competency-management.md §差距分析）。CRUD 之上承载差距快照刷新：
 * 评估 COMPLETED 后由 {@link ErpHrEmployeeAssessmentBizModel#completeAssessment} 调用
 * {@link #refreshGapAnalysis} ——清旧重建（对齐 0700-1 forecast 快照范式）。
 *
 * <p>跨实体读 RoleCompetency 经 {@link IErpHrRoleCompetencyBiz}、读 Employee 取 positionId
 * 经 {@code daoProvider}（Employee 主数据本域直接 dao 即可，无需 IBiz 抽象——仅取外键字段）。
 */
@BizModel("ErpHrGapAnalysis")
public class ErpHrGapAnalysisBizModel extends CrudBizModel<ErpHrGapAnalysis>
        implements IErpHrGapAnalysisBiz {

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

    public ErpHrGapAnalysisBizModel() {
        setEntityName(ErpHrGapAnalysis.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public List<ErpHrGapAnalysis> refreshGapAnalysis(@Name("employeeId") Long employeeId,
                                                      IServiceContext context) {
        Map<Long, Integer> aggregatedLevels = aggregateLatestAssessment(employeeId, context);
        return refreshGapAnalysisWithLevels(employeeId, aggregatedLevels, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public List<ErpHrGapAnalysis> refreshGapAnalysisWithLevels(@Name("employeeId") Long employeeId,
                                                                @Name("aggregatedLevels") Map<Long, Integer> aggregatedLevels,
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
            saveEntity(g, null, context);
        });
        return gaps;
    }

    Long loadPositionId(Long employeeId) {
        IEntityDao<ErpHrEmployee> dao = daoProvider().daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = dao.getEntityById(employeeId);
        if (emp == null) {
            throw new NopException(ErpHrErrors.ERR_GAP_NO_ROLE_REQUIREMENT)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId);
        }
        return emp.getPositionId();
    }

    List<ErpHrRoleCompetency> findRoleCompetencies(Long positionId, IServiceContext context) {
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
    Map<Long, Integer> aggregateLatestAssessment(Long employeeId, IServiceContext context) {
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

    ErpHrEmployeeAssessment findLatestCompletedAssessment(Long employeeId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("status", ErpHrConstants.ASSESSMENT_STATUS_COMPLETED)));
        q.addOrderField("assessmentDate", true);
        q.setLimit(1);
        return employeeAssessmentBiz.findFirst(q, null, context);
    }

    LocalDate latestAssessmentDate(Long employeeId, IServiceContext context) {
        ErpHrEmployeeAssessment latest = findLatestCompletedAssessment(employeeId, context);
        return latest != null ? latest.getAssessmentDate() : CoreMetrics.currentDate();
    }

    void deleteExistingGaps(Long employeeId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", employeeId));
        List<ErpHrGapAnalysis> existing = findList(q, null, context);
        if (existing.isEmpty()) return;
        IEntityDao<ErpHrGapAnalysis> dao = dao();
        for (ErpHrGapAnalysis g : existing) {
            dao.deleteEntity(g);
        }
    }
}
