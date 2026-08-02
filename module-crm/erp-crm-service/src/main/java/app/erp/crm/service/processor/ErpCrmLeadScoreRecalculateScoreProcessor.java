package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLeadScore;
import app.erp.crm.service.support.LeadScoringEngine;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpCrmLeadScore recalculateScore per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含线索评分重算编排，委托 {@link LeadScoringEngine}（归一化 totalScore + append-only 历史快照 + auto-qualify 阈值触发）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmLeadScoreRecalculateScoreProcessor {

    @Inject
    LeadScoringEngine scoringEngine;

    public ErpCrmLeadScore recalculateScore(Long leadId, String triggerEvent, IServiceContext context) {
        return scoringEngine.recalculateScore(leadId, triggerEvent, context);
    }
}
