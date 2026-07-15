package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmLeadScoreBiz;
import app.erp.crm.dao.entity.ErpCrmLeadScore;
import app.erp.crm.service.support.LeadScoringEngine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import java.util.List;

/**
 * 线索评分 BizModel。config 驱动评分计算委托 {@link LeadScoringEngine}（归一化 totalScore + append-only 历史快照 +
 * auto-qualify 阈值触发复用 3.1 qualify）。
 *
 * <p>对齐 {@code docs/design/crm/lead-scoring.md}。Lead 当前分数 = 按 leadId + calculatedAt DESC 取最新 ErpCrmLeadScore
 * （Option B 不扩 ORM，详见 Phase 2 Decision）。
 */
@BizModel("ErpCrmLeadScore")
public class ErpCrmLeadScoreBizModel extends CrudBizModel<ErpCrmLeadScore> implements IErpCrmLeadScoreBiz {

    @Inject
    LeadScoringEngine scoringEngine;

    public ErpCrmLeadScoreBizModel() {
        setEntityName(ErpCrmLeadScore.class.getName());
    }

    @Override
    @BizMutation
    public ErpCrmLeadScore recalculateScore(@Name("leadId") Long leadId,
                                            @Optional @Name("triggerEvent") String triggerEvent,
                                            IServiceContext context) {
        return scoringEngine.recalculateScore(leadId, triggerEvent, context);
    }

    

}
