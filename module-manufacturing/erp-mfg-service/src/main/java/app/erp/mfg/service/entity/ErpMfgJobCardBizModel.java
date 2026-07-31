
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgJobCardBiz;
import app.erp.mfg.biz.JobCardWorkRecord;
import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.service.processor.ErpMfgJobCardCancelJobProcessor;
import app.erp.mfg.service.processor.ErpMfgJobCardCompleteJobProcessor;
import app.erp.mfg.service.processor.ErpMfgJobCardHoldJobProcessor;
import app.erp.mfg.service.processor.ErpMfgJobCardRecordWorkProcessor;
import app.erp.mfg.service.processor.ErpMfgJobCardResumeJobProcessor;
import app.erp.mfg.service.processor.ErpMfgJobCardStartJobProcessor;
import app.erp.mfg.service.processor.ErpMfgJobCardSubmitJobProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.RequestBean;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 作业卡 BizModel（Facade，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 作业卡 8 态状态机 + 报工成本归集编排委托 7 个 {@code ErpMfgJobCard<Method>Processor}
 *（R6.2 per-mutation 拆分；共享 protected helper 单一真相源在 {@code ErpMfgJobCardProcessor}）。
 *
 * <p>语义见 {@code docs/design/manufacturing/state-machine.md §适用对象二}。
 */
@BizModel("ErpMfgJobCard")
public class ErpMfgJobCardBizModel extends CrudBizModel<ErpMfgJobCard> implements IErpMfgJobCardBiz {

    @Inject
    ErpMfgJobCardStartJobProcessor startJobProcessor;
    @Inject
    ErpMfgJobCardRecordWorkProcessor recordWorkProcessor;
    @Inject
    ErpMfgJobCardSubmitJobProcessor submitJobProcessor;
    @Inject
    ErpMfgJobCardCompleteJobProcessor completeJobProcessor;
    @Inject
    ErpMfgJobCardHoldJobProcessor holdJobProcessor;
    @Inject
    ErpMfgJobCardResumeJobProcessor resumeJobProcessor;
    @Inject
    ErpMfgJobCardCancelJobProcessor cancelJobProcessor;

    public ErpMfgJobCardBizModel() {
        setEntityName(ErpMfgJobCard.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgJobCard startJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return startJobProcessor.startJob(jobCardId, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard recordWork(@RequestBean JobCardWorkRecord record, IServiceContext context) {
        return recordWorkProcessor.recordWork(record, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard submitJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return submitJobProcessor.submitJob(jobCardId, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard completeJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return completeJobProcessor.completeJob(jobCardId, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard holdJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return holdJobProcessor.holdJob(jobCardId, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard resumeJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return resumeJobProcessor.resumeJob(jobCardId, context);
    }

    @Override
    @BizMutation
    public ErpMfgJobCard cancelJob(@Name("jobCardId") Long jobCardId, IServiceContext context) {
        return cancelJobProcessor.cancelJob(jobCardId, context);
    }

}
