
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgJobCardBiz;
import app.erp.mfg.biz.JobCardWorkRecord;
import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.service.processor.ErpMfgJobCardProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.biz.RequestBean;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 作业卡 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 作业卡 8 态状态机 + 报工成本归集编排委托
 * {@link ErpMfgJobCardProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>语义见 {@code docs/design/manufacturing/state-machine.md §适用对象二}。
 */
@BizModel("ErpMfgJobCard")
public class ErpMfgJobCardBizModel extends CrudBizModel<ErpMfgJobCard> implements IErpMfgJobCardBiz {

    @Inject
    ErpMfgJobCardProcessor jobCardProcessor;

    public ErpMfgJobCardBizModel() {
        setEntityName(ErpMfgJobCard.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgJobCard startJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return jobCardProcessor.startJob(jobCardId, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard recordWork(@RequestBean JobCardWorkRecord record, IServiceContext context) {
        return jobCardProcessor.recordWork(record, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard submitJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return jobCardProcessor.submitJob(jobCardId, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard completeJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return jobCardProcessor.completeJob(jobCardId, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard holdJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return jobCardProcessor.holdJob(jobCardId, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard resumeJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return jobCardProcessor.resumeJob(jobCardId, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard cancelJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return jobCardProcessor.cancelJob(jobCardId, context);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + BizLoader 批量加载防 N+1）----------

    @BizLoader(forType = ErpMfgJobCard.class)
    public List<String> workOrderNo(@ContextSource List<ErpMfgJobCard> cards) {
        orm().batchLoadProps(cards, Collections.singleton("workOrder"));
        List<String> result = new ArrayList<>(cards.size());
        for (ErpMfgJobCard card : cards) {
            result.add(card.getWorkOrder() != null ? card.getWorkOrder().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgJobCard.class)
    public List<String> workcenterName(@ContextSource List<ErpMfgJobCard> cards) {
        orm().batchLoadProps(cards, Collections.singleton("workcenter"));
        List<String> result = new ArrayList<>(cards.size());
        for (ErpMfgJobCard card : cards) {
            result.add(card.getWorkcenter() != null ? card.getWorkcenter().getName() : null);
        }
        return result;
    }
}
