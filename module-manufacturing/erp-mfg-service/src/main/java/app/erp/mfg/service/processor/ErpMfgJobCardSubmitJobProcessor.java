package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpMfgJobCard submitJob per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 WORK_IN_PROGRESS/ON_HOLD→SUBMITTED 翻转编排；共享 protected helper 单一真相源在 {@link ErpMfgJobCardProcessor}。
 */
public class ErpMfgJobCardSubmitJobProcessor {

    @Inject
    ErpMfgJobCardProcessor facade;

    public ErpMfgJobCard submitJob(Long jobCardId, IServiceContext context) {
        ErpMfgJobCard jc = facade.requireJobCard(jobCardId, context);
        validateTransitionForSubmit(jc);
        doSubmitJob(jc);
        return jc;
    }

    protected void validateTransitionForSubmit(ErpMfgJobCard jc) {
        String status = jc.getStatus();
        if (status == null || (!Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS)
                && !Objects.equals(status, ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD))) {
            throw facade.illegalTransition(jc, status, "WORK_IN_PROGRESS 或 ON_HOLD");
        }
    }

    protected void doSubmitJob(ErpMfgJobCard jc) {
        jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED);
        facade.jobCardDao().updateEntity(jc);
    }
}
