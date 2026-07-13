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
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
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

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmLeadScore.class)
    public List<String> leadCode(@ContextSource List<ErpCrmLeadScore> rows) {
        orm().batchLoadProps(rows, Collections.singleton("lead"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmLeadScore row : rows) {
            result.add(row.orm_attached() && row.getLead() != null ? row.getLead().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmLeadScore.class)
    public List<String> orgName(@ContextSource List<ErpCrmLeadScore> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmLeadScore row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmLeadScore.class)
    public List<String> configName(@ContextSource List<ErpCrmLeadScore> rows) {
        orm().batchLoadProps(rows, Collections.singleton("config"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmLeadScore row : rows) {
            result.add(row.orm_attached() && row.getConfig() != null ? row.getConfig().getConfigName() : null);
        }
        return result;
    }

}
