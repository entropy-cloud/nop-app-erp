package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.statemachine.ErpMfgSubcontractOrderApprovalStateMachine;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpMfgSubcontractOrder）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpMfgSubcontractOrder reverseApprove per-mutation Processor (plan 2026-07-30-1909-2 R5.5)。
 * Pattern B（custom public override）：1:1 复刻 facade 公共 reverseApprove 编排流，经 facade protected helper
 * （requireOrder → validateTransitionForReverseApprove → doReverseApprove）保持单一真相源。
 * 域特有保真：doReverseApprove 目标态=REJECTED（非基类 SUBMITTED）+ 清空 approvedBy/approvedAt（对齐 facade，
 * 纠正抽象骨架误设 SUBMITTED）。
 */
public class ErpMfgSubcontractOrderReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpMfgSubcontractOrder> {

    @Inject
    ErpMfgSubcontractOrderProcessor processor;

    @Inject
    ErpMfgSubcontractOrderApprovalStateMachine stateMachine;

    @Override
    public ErpMfgSubcontractOrder reverseApprove(String id, IServiceContext context) {
        ErpMfgSubcontractOrder order = processor.requireOrder(id, context);
        processor.validateTransitionForReverseApprove(order, context);
        processor.doReverseApprove(order, context);
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
    protected void setApprovedBy(ErpMfgSubcontractOrder entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpMfgSubcontractOrder entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpMfgSubcontractOrder entity) {
        return Objects.equals(entity.getApproveStatus(), ErpMfgConstants.APPROVE_STATUS_REJECTED);
    }

    @Override
    protected String approvedStatus() {
        return stateMachine.approveTargetStatus();
    }

    @Override
    protected String submittedStatus() {
        return stateMachine.submitTargetStatus();
    }
}
