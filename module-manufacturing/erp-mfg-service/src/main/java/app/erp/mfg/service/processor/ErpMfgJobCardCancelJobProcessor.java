package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpMfgJobCard cancelJob per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 OPEN/WORK_IN_PROGRESS/ON_HOLD→CANCELLED 翻转编排；共享 protected helper 单一真相源在 {@link ErpMfgJobCardProcessor}。
 */
public class ErpMfgJobCardCancelJobProcessor {

    @Inject
    ErpMfgJobCardProcessor facade;

    public ErpMfgJobCard cancelJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = facade.requireJobCard(jobCardId, context);
        validateTransitionForCancel(jc);
        doCancelJob(jc);
        return jc;
    }

    protected void validateTransitionForCancel(ErpMfgJobCard jc) {
        String status = jc.getStatus();
        if (status == null || (!Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_OPEN)
                && !Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS)
                && !Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD))) {
            throw facade.illegalTransition(jc, status, "OPEN、WORK_IN_PROGRESS 或 ON_HOLD");
        }
    }

    protected void doCancelJob(ErpMfgJobCard jc) {
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_CANCELLED);
        facade.jobCardDao().updateEntity(jc);
    }
}
