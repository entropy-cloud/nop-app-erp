package app.erp.pur.service.processor;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurReceiveApprovalStateMachine;
import app.erp.common.service.AbstractApproveProcessor;
import app.erp.common.service.SoDGuard;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPurReceive approve per-mutation Processor (plan 2026-07-25-1057-2；审批轴 Bean 接线 plan 2026-08-13-1950-1 M4.14)。
 *
 * <p>整体覆写 public approve 方法以编排业财过账副作用（入库 stock move + posting + receiveStatus 回写）；
 * 固定来源态/目标态判断委托 {@link ErpPurReceiveApprovalStateMachine}（approveStatus 审批轴 Bean，契约 §4/§7）；
 * 动态业务守卫/副作用（triggerIncomingMove/enforceInspectionGate/applyPostingResult/SoD）保留原位。
 * Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpPurReceiveApproveProcessor extends AbstractApproveProcessor<ErpPurReceive> {

    @Inject
    ErpPurReceiveProcessor processor;

    @Inject
    ErpPurReceiveApprovalStateMachine stateMachine;

    @Override
    public ErpPurReceive approve(String id, IServiceContext context) {
        ErpPurReceive receive = requireEntity(id);
        if (isApproved(receive)) {
            return receive;
        }
        SoDGuard.assertApproverNotCreator(getCreatedBy(receive), currentUserId(), sodErrorCode());
        processor.validateNotCancelled(receive, context);
        validateTransitionForApprove(receive, context);
        processor.validateBusinessRulesForApprove(receive, context);
        processor.enforceInspectionGate(receive, context);

        ErpInvStockMove move = processor.triggerIncomingMove(receive, context);
        receive = dao().getEntityById(id);
        // 项目物料成本归集（RC-R1.61 / P1-RC-049）：移动单生成后同事务归集；
        // STRICT 预算/非 OPEN 项目异常传播 → 审核回滚拒绝（L1 UC-PRJ-04 采购审核拒绝该笔归集）
        processor.collectProjectMaterialCost(receive, context);
        // 越库收货识别（RC-R1.81 / P1-RC-081，D1 裁决选项 A）：PENDING 越库记录→STAGING + inboundMoveId 回写；
        // drp 模块未部署 @Nullable 跳过 + 失败隔离不阻断主迁移（对齐 RC-R1.85 容错范式）
        processor.markCrossDockReceived(receive, move, context);
        // 提前期记录（RC-R1.82 / P1-RC-082，D4 裁决选项 A）：actualLeadTime=DATEDIFF(receiptDate, orderDate)
        // 落 ErpInvDrpLeadTimeRecord（幂等同单号+物料）；@Nullable 跳过 + 失败隔离同上
        processor.recordLeadTimeFromReceive(receive, context);

        setApproveStatus(receive, approvedStatus());
        setApprovedBy(receive, currentUserId());
        setApprovedAt(receive, now());
        processor.applyPostingResult(receive, move);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_RECEIVED);
        dao().updateEntity(receive);

        processor.postProcessApprove(receive, context);
        return receive;
    }

    @Override
    protected IEntityDao<ErpPurReceive> dao() {
        return daoProvider.daoFor(ErpPurReceive.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_RECEIVE_NOT_FOUND)
                .param(ErpPurErrors.ARG_RECEIVE_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurReceive entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RECEIVE_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateNotCancelled(ErpPurReceive entity, IServiceContext context) {
        processor.validateNotCancelled(entity, context);
    }

    @Override
    protected void validateTransitionForApprove(ErpPurReceive entity, IServiceContext context) {
        try {
            stateMachine.assertCanApprove(getApproveStatus(entity));
        } catch (NopException e) {
            throw illegalStatusException(entity, getApproveStatus(entity), ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    @Override
    protected String getApproveStatus(ErpPurReceive entity) {
        String status = entity.getApproveStatus();
        return status == null ? ErpPurConstants.APPROVE_STATUS_UNSUBMITTED : status;
    }

    @Override
    protected void setApproveStatus(ErpPurReceive entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpPurReceive entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpPurReceive entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isApproved(ErpPurReceive entity) {
        return entity.isApproved();
    }

    @Override
    protected boolean isCancelled(ErpPurReceive entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpPurConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String approvedStatus() {
        return stateMachine.approveTargetStatus();
    }

    @Override
    protected ErrorCode sodErrorCode() {
        return ErpPurErrors.ERR_PUR_APPROVER_IS_CREATOR;
    }
}
