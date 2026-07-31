package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpMfgJobCard completeJob per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 SUBMITTED→COMPLETED 翻转编排；共享 protected helper 单一真相源在 {@link ErpMfgJobCardProcessor}。
 */
public class ErpMfgJobCardCompleteJobProcessor {

    @Inject
    ErpMfgJobCardProcessor facade;

    public ErpMfgJobCard completeJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = facade.requireJobCard(jobCardId, context);
        facade.requireStatus(jc, ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED, "SUBMITTED");
        doCompleteJob(jc);
        return jc;
    }

    protected void doCompleteJob(ErpMfgJobCard jc) {
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_COMPLETED);
        facade.jobCardDao().updateEntity(jc);
    }
}
