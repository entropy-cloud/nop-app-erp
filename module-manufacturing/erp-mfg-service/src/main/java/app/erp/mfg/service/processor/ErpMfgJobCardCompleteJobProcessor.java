package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.service.statemachine.ErpMfgJobCardStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgJobCard completeJob per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 SUBMITTED→COMPLETED 翻转编排；共享 protected helper 单一真相源在 {@link ErpMfgJobCardProcessor}。
 * 固定来源态/目标态判断经 {@link ErpMfgJobCardStateMachine} Bean（契约 §4/§7）。
 */
public class ErpMfgJobCardCompleteJobProcessor {

    @Inject
    ErpMfgJobCardProcessor facade;
    @Inject
    ErpMfgJobCardStateMachine stateMachine;

    public ErpMfgJobCard completeJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = facade.requireJobCard(jobCardId, context);
        String from = jc.getStatus();
        try {
            stateMachine.assertCanCompleteJob(from);
        } catch (NopException e) {
            throw facade.illegalTransition(jc, from, "SUBMITTED", e);
        }
        doCompleteJob(jc);
        return jc;
    }

    protected void doCompleteJob(ErpMfgJobCard jc) {
        jc.setStatus(stateMachine.completeJobTargetStatus());
        facade.jobCardDao().updateEntity(jc);
    }
}
