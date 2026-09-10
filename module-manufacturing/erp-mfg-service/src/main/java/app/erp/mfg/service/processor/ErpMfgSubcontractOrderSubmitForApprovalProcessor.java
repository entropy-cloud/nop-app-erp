package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.statemachine.ErpMfgSubcontractOrderApprovalStateMachine;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpMfgSubcontractOrder）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpMfgSubcontractOrder submitForApproval per-mutation Processor (plan 2026-07-30-1909-2 R5.5)。
 * Pattern B（custom public override）：1:1 复刻 facade 公共 submitForApproval 编排流，经 facade protected helper
 * （requireOrder → validateTransitionForSubmit → doSubmit）保持单一真相源。doReject/doReverseApprove 偏离驱动
 * Pattern B（custom override 完全绕过基类模板，零偏离风险）。
 */
public class ErpMfgSubcontractOrderSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpMfgSubcontractOrder> {

    @Inject
    ErpMfgSubcontractOrderProcessor processor;

    @Inject
    ErpMfgSubcontractOrderApprovalStateMachine stateMachine;

    public ErpMfgSubcontractOrderSubmitForApprovalProcessor() {
        super("ErpMfgSubcontractOrder");
    }

    @Override
    public ErpMfgSubcontractOrder submitForApproval(String id, IServiceContext context) {
        ErpMfgSubcontractOrder order = processor.requireOrder(id, context);
        processor.validateTransitionForSubmit(order, context);
        processor.doSubmit(order, context);
        return order;
    }

    @Override
    protected IEntityDao<ErpMfgSubcontractOrder> dao() {
        return daoProvider.daoFor(ErpMfgSubcontractOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpMfgErrors.ERR_SUBCONTRACT_ORDER_NOT_FOUND)
                .param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_ID, id);
    }

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3，有意契约修正）：非法迁移错误由 common 码
     * 改为直抛领域码 {@link ErpMfgErrors#ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION}
     * （参数形态与 facade 同码补参后的端到端消息一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpMfgSubcontractOrder entity, String current, String... expected) {
        return new NopException(ErpMfgErrors.ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, entity.getCode())
                .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMfgErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getApproveStatus(ErpMfgSubcontractOrder entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpMfgSubcontractOrder entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpMfgSubcontractOrder entity) {
        return Objects.equals(entity.getDocStatus(), ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED);
    }

    @Override
    protected String unsubmittedStatus() {
        return stateMachine.withdrawTargetStatus();
    }

    @Override
    protected String submittedStatus() {
        return stateMachine.submitTargetStatus();
    }

    @Override
    protected String rejectedStatus() {
        return stateMachine.rejectTargetStatus();
    }
}
