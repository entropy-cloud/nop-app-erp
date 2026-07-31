package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgJobCard resumeJob per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 ON_HOLD→WORK_IN_PROGRESS 翻转编排；共享 protected helper 单一真相源在 {@link ErpMfgJobCardProcessor}。
 */
public class ErpMfgJobCardResumeJobProcessor {

    @Inject
    ErpMfgJobCardProcessor facade;

    public ErpMfgJobCard resumeJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = facade.requireJobCard(jobCardId, context);
        facade.requireStatus(jc, ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD, "ON_HOLD");
        doResumeJob(jc);
        return jc;
    }

    protected void doResumeJob(ErpMfgJobCard jc) {
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS);
        facade.jobCardDao().updateEntity(jc);
    }
}
