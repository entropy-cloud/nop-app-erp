package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import app.erp.qa.service.statemachine.ErpQaRecallApprovalStateMachine;
import app.erp.qa.service.statemachine.ErpQaRecallStateMachine;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * 召回事件审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus/setStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 */
public class ErpQaRecallProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpQaRecallApprovalStateMachine approvalStateMachine;

    @Inject
    ErpQaRecallStateMachine statusStateMachine;

    @Inject
    ErpQaRecallSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpQaRecallApproveProcessor approveProcessor;

    @Inject
    ErpQaRecallRejectProcessor rejectProcessor;

    @Inject
    ErpQaRecallReverseApproveProcessor reverseApproveProcessor;

    @Inject
    ErpQaRecallWithdrawApprovalProcessor withdrawApprovalProcessor;

    public ErpQaRecall submitForApproval(String id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpQaRecall withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpQaRecall approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    public ErpQaRecall reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    public ErpQaRecall reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------
    // approveStatus 轴固定来源态守卫委托 ErpQaRecallApprovalStateMachine（非法边→领域码 ERR_INVALID_RECALL_STATUS_TRANSITION）

    protected void validateTransitionForSubmit(ErpQaRecall recall, IServiceContext context) {
        String aStatus = recall.getApproveStatus();
        try {
            approvalStateMachine.assertCanSubmit(aStatus);
        } catch (NopException e) {
            throw illegalTransition(recall, aStatus, ErpQaConstants.APPROVE_STATUS_UNSUBMITTED + " 或 " + ErpQaConstants.APPROVE_STATUS_REJECTED);
        }
    }

    protected void validateTransitionForWithdraw(ErpQaRecall recall, IServiceContext context) {
        String aStatus = recall.getApproveStatus();
        try {
            approvalStateMachine.assertCanWithdraw(aStatus);
        } catch (NopException e) {
            throw illegalTransition(recall, aStatus, ErpQaConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForApprove(ErpQaRecall recall, IServiceContext context) {
        String aStatus = recall.getApproveStatus();
        try {
            approvalStateMachine.assertCanApprove(aStatus);
        } catch (NopException e) {
            throw illegalTransition(recall, aStatus, ErpQaConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReject(ErpQaRecall recall, IServiceContext context) {
        String aStatus = recall.getApproveStatus();
        try {
            approvalStateMachine.assertCanReject(aStatus);
        } catch (NopException e) {
            throw illegalTransition(recall, aStatus, ErpQaConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReverseApprove(ErpQaRecall recall, IServiceContext context) {
        String aStatus = recall.getApproveStatus();
        try {
            approvalStateMachine.assertCanReverseApprove(aStatus);
        } catch (NopException e) {
            throw illegalTransition(recall, aStatus, ErpQaConstants.APPROVE_STATUS_APPROVED);
        }
    }

    // ---------- step：业务规则校验 ----------
    // approve 联动的 status 来源态守卫（须 OPEN）委托 ErpQaRecallStateMachine；submit 的 status=OPEN 前置条件保留原位

    protected void validateBusinessRulesForSubmit(ErpQaRecall recall, IServiceContext context) {
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_OPEN, "OPEN");
    }

    protected void validateBusinessRulesForApprove(ErpQaRecall recall, IServiceContext context) {
        String current = recall.getStatus();
        try {
            statusStateMachine.assertCanApprove(current);
        } catch (NopException e) {
            throw illegalTransition(recall, current, ErpQaConstants.RECALL_STATUS_OPEN);
        }
    }

    // ---------- step：执行（状态推进 + 持久化） ----------
    // approveStatus 目标态委托 approvalStateMachine；status 联动目标态委托 statusStateMachine

    protected void doSubmit(ErpQaRecall recall, IServiceContext context) {
        recall.setApproveStatus(approvalStateMachine.submitTargetStatus());
        recallDao().updateEntity(recall);
    }

    protected void doWithdrawSubmit(ErpQaRecall recall, IServiceContext context) {
        recall.setApproveStatus(approvalStateMachine.withdrawTargetStatus());
        recallDao().updateEntity(recall);
    }

    protected void doApprove(ErpQaRecall recall, IServiceContext context) {
        recall.setApproveStatus(approvalStateMachine.approveTargetStatus());
        recall.setStatus(statusStateMachine.approveTargetStatus());
        recall.setApprovedBy(currentUserId(context));
        recall.setApprovedAt(CoreMetrics.currentTimestamp());
        recallDao().updateEntity(recall);
    }

    protected void doReject(ErpQaRecall recall, IServiceContext context) {
        recall.setApproveStatus(approvalStateMachine.rejectTargetStatus());
        recall.setStatus(statusStateMachine.rejectTargetStatus());
        recall.setApprovedBy(currentUserId(context));
        recall.setApprovedAt(CoreMetrics.currentTimestamp());
        recallDao().updateEntity(recall);
    }

    protected void doReverseApprove(ErpQaRecall recall, IServiceContext context) {
        recall.setApproveStatus(approvalStateMachine.reverseApproveTargetStatus());
        recall.setApprovedBy(null);
        recall.setApprovedAt(null);
        recallDao().updateEntity(recall);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpQaRecall requireRecall(String id, IServiceContext context) {
        ErpQaRecall recall = recallDao().getEntityById(id);
        if (recall == null) {
            throw new NopException(ErpQaErrors.ERR_RECALL_NOT_FOUND)
                    .param(ErpQaErrors.ARG_RECALL_ID, id);
        }
        return recall;
    }

    protected void requireRecallStatus(ErpQaRecall recall, String expected, String expectedLabel) {
        String current = recall.getStatus();
        if (current == null || !expected.equals(current)) {
            throw illegalTransition(recall, current, expectedLabel);
        }
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpQaRecall> recallDao() {
        return daoProvider.daoFor(ErpQaRecall.class);
    }

    protected String currentUserId(IServiceContext context) {
        String userId = context.getUserId();
        if (userId != null) return userId;
        try {
            IUserContext ctx = IUserContext.get();
            return ctx != null ? ctx.getUserId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    protected NopException illegalTransition(ErpQaRecall recall, String current, String expected) {
        return new NopException(ErpQaErrors.ERR_INVALID_RECALL_STATUS_TRANSITION)
                .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode())
                .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                .param(ErpQaErrors.ARG_EXPECTED_STATUS, expected);
    }
}
