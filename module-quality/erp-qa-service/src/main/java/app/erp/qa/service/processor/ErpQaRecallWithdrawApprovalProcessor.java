package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.ErpQaErrors;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpQaRecall）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpQaRecall withdrawApproval per-mutation Processor (plan 2026-07-30-2046-1 R5.7, Pattern B)。
 * 原 xbiz withdrawApproval 为 inline-script（requireEntity + status !== 'SUBMITTED' 守卫抛 NopScriptError +
 * set UNSUBMITTED），提取为 custom public override，1:1 复刻 facade {@code withdrawApproval} 编排流，经 facade
 * protected helper（requireRecall → validateTransitionForWithdraw → doWithdrawSubmit）保持单一真相源。
 * NopScriptError → NopException 语义等价：{@code nop.err.wf.approve.invalid-status}（status !== SUBMITTED）
 * → facade illegalTransition 抛 {@link ErpQaErrors#ERR_INVALID_RECALL_STATUS_TRANSITION}
 * （param: recallCode/currentStatus/expectedStatus）。
 * Pattern B 额外正当性：custom override 不引入 AbstractWithdrawApprovalProcessor 骨架的 validateNotCancelled
 * （既有 inline-script 仅检查 status !== SUBMITTED），保真既有行为。
 */
public class ErpQaRecallWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpQaRecall> {

    @Inject
    ErpQaRecallProcessor processor;

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_INVALID_RECALL_STATUS_TRANSITION（参数形态与 facade illegalTransition helper 一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpQaRecall entity, String current, String... expected) {
        return new NopException(ErpQaErrors.ERR_INVALID_RECALL_STATUS_TRANSITION)
                .param(ErpQaErrors.ARG_RECALL_CODE, entity.getCode())
                .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                .param(ErpQaErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    public ErpQaRecall withdrawApproval(String id, IServiceContext context) {
        ErpQaRecall recall = processor.requireRecall(id, context);
        processor.validateTransitionForWithdraw(recall, context);
        processor.doWithdrawSubmit(recall, context);
        return recall;
    }

    @Override
    protected IEntityDao<ErpQaRecall> dao() {
        return daoProvider.daoFor(ErpQaRecall.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpQaRecall entity) {
        // not reached: Pattern B custom public override
        return null;
    }

    @Override
    protected void setApproveStatus(ErpQaRecall entity, String status) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected boolean isCancelled(ErpQaRecall entity) {
        // not reached: Pattern B custom public override
        return false;
    }

    @Override
    protected String unsubmittedStatus() {
        // not reached: Pattern B custom public override
        return null;
    }

    @Override
    protected String submittedStatus() {
        // not reached: Pattern B custom public override
        return null;
    }
}
