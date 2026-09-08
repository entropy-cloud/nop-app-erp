package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.statemachine.ErpMfgJobCardStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgJobCard cancelJob per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 OPEN/WORK_IN_PROGRESS/ON_HOLD→CANCELLED 翻转编排；共享 protected helper 单一真相源在 {@link ErpMfgJobCardProcessor}。
 * 固定来源态/目标态判断经 {@link ErpMfgJobCardStateMachine} Bean（Bean 直抛领域码，本处同码补参 jobCardId），cancelJob 保持 3 源。
 */
public class ErpMfgJobCardCancelJobProcessor {

    @Inject
    ErpMfgJobCardProcessor facade;
    @Inject
    ErpMfgJobCardStateMachine stateMachine;

    public ErpMfgJobCard cancelJob(String jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = facade.requireJobCard(jobCardId, context);
        String from = jc.getStatus();
        try {
            stateMachine.assertCanCancelJob(from);
        } catch (NopException e) {
            throw e.param(ErpMfgErrors.ARG_JOB_CARD_ID, jc.getId());
        }
        doCancelJob(jc);
        return jc;
    }

    protected void doCancelJob(ErpMfgJobCard jc) {
        jc.setStatus(stateMachine.cancelJobTargetStatus());
        facade.jobCardDao().updateEntity(jc);
    }
}
