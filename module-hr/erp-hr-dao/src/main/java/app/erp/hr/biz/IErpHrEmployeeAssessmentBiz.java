package app.erp.hr.biz;

import app.erp.hr.dao.entity.ErpHrEmployeeAssessment;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * 员工评估聚合根 Biz（competency-management.md §评估流程）。CRUD 之上承载评估状态机：
 * <ul>
 *   <li>{@link #submitAssessment} DRAFT→SUBMITTED（校验至少一条 AssessmentDetail）。</li>
 *   <li>{@link #completeAssessment} SUBMITTED→COMPLETED（触发 360 聚合 + 差距分析刷新）。</li>
 * </ul>
 *
 * <p>非法迁移抛 {@link app.erp.hr.service.ErpHrErrors#ERR_ASSESSMENT_ILLEGAL_STATUS_TRANSITION}。
 * 聚合委托 {@code AssessmentAggregator}，差距刷新委托 {@link IErpHrGapAnalysisBiz}。
 */
public interface IErpHrEmployeeAssessmentBiz extends ICrudBiz<ErpHrEmployeeAssessment> {

    /** DRAFT→SUBMITTED。前提：至少一条 AssessmentDetail，否则 ERR_ASSESSMENT_NO_DETAILS。 */
    @BizMutation
    ErpHrEmployeeAssessment submitAssessment(@Name("assessmentId") Long assessmentId,
                                             IServiceContext context);

    /** SUBMITTED→COMPLETED。触发 360 聚合（写回 detail.actualLevel）+ 差距分析刷新。 */
    @BizMutation
    ErpHrEmployeeAssessment completeAssessment(@Name("assessmentId") Long assessmentId,
                                               IServiceContext context);
}
