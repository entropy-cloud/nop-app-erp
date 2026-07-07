
package app.erp.crm.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.crm.dao.entity.ErpCrmLeadSequenceProgress;

import java.util.List;
import java.util.Map;

/**
 * 销售序列进度业务接口。除标准 CRUD 外，定义序列分配 / 步骤推进 / 切换 / 逾期扫描 / 性能查询。
 *
 * <p>对齐 {@code docs/design/crm/sales-sequence.md}（序列自动分配 / 步骤推进 / 序列性能分析）。
 */
public interface IErpCrmLeadSequenceProgressBiz extends ICrudBiz<ErpCrmLeadSequenceProgress> {

    /**
     * 按 {@code SequenceAssignment} 规则匹配 Lead，分配 Sequence + 建 LeadSequenceProgress(IN_PROGRESS, stepIndex=0)。
     * config-gated {@code erp-crm.sequence.auto-assign-on-qualify}（默认 true）；
     * Lead 已有活跃 progress 抛 {@code ERR_SEQUENCE_ALREADY_ASSIGNED}。
     */
    @BizMutation
    ErpCrmLeadSequenceProgress assignSequence(@Name("leadId") Long leadId, IServiceContext context);

    /**
     * 推进步骤：校验 Event.status=COMPLETED + 匹配 step.activityType + completionCondition 满足 → currentStepIndex+1；
     * 末步完成则 status=COMPLETED + completedAt；推进时 autoCreateEvent 步骤建下一步 ErpCrmEvent。
     */
    @BizMutation
    ErpCrmLeadSequenceProgress advanceStep(@Name("progressId") Long progressId,
                                           @Name("eventId") Long eventId,
                                           IServiceContext context);

    /**
     * 切换序列：旧活跃序列 SKIPPED 快照 + 新序列 stepIndex=0 进度。一 Lead 一活跃序列（多序列并发归 successor）。
     */
    @BizMutation
    ErpCrmLeadSequenceProgress switchSequence(@Name("leadId") Long leadId,
                                              @Name("newSequenceId") Long newSequenceId,
                                              IServiceContext context);

    /**
     * 扫描连续逾期步骤（连续逾期 ≥ max-overdue-steps），返回 leadId → 连续逾期步数列表（用于提醒派发）。
     */
    @BizQuery
    List<Map<String, Object>> scanOverdueSteps(IServiceContext context);

    /**
     * 序列性能分析：完成率 / 平均完成天数 / 步骤流失率，对齐 {@code sales-sequence.md §序列性能分析}。
     *
     * @param templateType 序列模板类型（NEW_LEAD/QUALIFICATION/NEGOTIATION/RE_ENGAGEMENT）
     */
    @BizQuery
    Map<String, Object> getSequencePerformance(@Name("templateType") String templateType,
                                                IServiceContext context);
}
