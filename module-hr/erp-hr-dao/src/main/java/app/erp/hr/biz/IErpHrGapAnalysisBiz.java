package app.erp.hr.biz;

import app.erp.hr.dao.entity.ErpHrGapAnalysis;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;
import java.util.Map;

/**
 * 差距分析 Biz（competency-management.md §差距分析）。CRUD 之上承载差距快照刷新：
 * <ul>
 *   <li>{@link #refreshGapAnalysis} 评估 COMPLETED 后调用——读取员工岗位胜任力要求、各胜任力聚合
 *       actualLevel，委托 {@code GapAnalysisCalculator} 计算，清旧重建 ErpHrGapAnalysis 快照。</li>
 * </ul>
 *
 * <p>无岗位要求时抛 {@link app.erp.hr.service.ErpHrErrors#ERR_GAP_NO_ROLE_REQUIREMENT}。
 */
public interface IErpHrGapAnalysisBiz extends ICrudBiz<ErpHrGapAnalysis> {

    /**
     * 清旧重建员工差距快照。返回新建（已持久化）的差距列表；无差距时返回空列表。
     * 内部从最新 COMPLETED 评估聚合 actualLevel。
     */
    @BizMutation
    List<ErpHrGapAnalysis> refreshGapAnalysis(@Name("employeeId") Long employeeId,
                                              IServiceContext context);

    /**
     * 同 {@link #refreshGapAnalysis(Long, IServiceContext)} 但使用调用方预先聚合的 actualLevel 映射
     * （key=competencyId, value=聚合后 level）。供 {@code completeAssessment} 内部直传避免二次查询。
     */
    @BizMutation
    List<ErpHrGapAnalysis> refreshGapAnalysisWithLevels(@Name("employeeId") Long employeeId,
                                                        @Name("aggregatedLevels") Map<Long, Integer> aggregatedLevels,
                                                        IServiceContext context);
}

