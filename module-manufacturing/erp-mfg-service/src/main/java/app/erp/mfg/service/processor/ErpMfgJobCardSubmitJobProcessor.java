package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.service.statemachine.ErpMfgJobCardStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgJobCard submitJob per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 WORK_IN_PROGRESS/ON_HOLD→SUBMITTED 翻转编排；共享 protected helper 单一真相源在 {@link ErpMfgJobCardProcessor}。
 * 固定来源态/目标态判断经 {@link ErpMfgJobCardStateMachine} Bean（契约 §4/§7）。
 */
public class ErpMfgJobCardSubmitJobProcessor {

    @Inject
    ErpMfgJobCardProcessor facade;
    @Inject
    ErpMfgJobCardStateMachine stateMachine;

    public ErpMfgJobCard submitJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = facade.requireJobCard(jobCardId, context);
        String from = jc.getStatus();
        try {
            stateMachine.assertCanSubmitJob(from);
        } catch (NopException e) {
            throw facade.illegalTransition(jc, from, "WORK_IN_PROGRESS 或 ON_HOLD", e);
        }
        doSubmitJob(jc);
        return jc;
    }

    protected void doSubmitJob(ErpMfgJobCard jc) {
        jc.setStatus(stateMachine.submitJobTargetStatus());
        facade.jobCardDao().updateEntity(jc);
    }
}
