package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

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

    public ErpQaRecall submitForApproval(String id, IServiceContext context) {
        ErpQaRecall recall = requireRecall(id, context);
        validateTransitionForSubmit(recall, context);
        validateBusinessRulesForSubmit(recall, context);
        doSubmit(recall, context);
        return recall;
    }

    public ErpQaRecall withdrawApproval(String id, IServiceContext context) {
        ErpQaRecall recall = requireRecall(id, context);
        validateTransitionForWithdraw(recall, context);
        doWithdrawSubmit(recall, context);
        return recall;
    }

    public ErpQaRecall approve(String id, IServiceContext context) {
        ErpQaRecall recall = requireRecall(id, context);
        validateTransitionForApprove(recall, context);
        validateBusinessRulesForApprove(recall, context);
        doApprove(recall, context);
        return recall;
    }

    public ErpQaRecall reject(String id, IServiceContext context) {
        ErpQaRecall recall = requireRecall(id, context);
        validateTransitionForReject(recall, context);
        doReject(recall, context);
        return recall;
    }

    public ErpQaRecall reverseApprove(String id, IServiceContext context) {
        ErpQaRecall recall = requireRecall(id, context);
        validateTransitionForReverseApprove(recall, context);
        doReverseApprove(recall, context);
        return recall;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpQaRecall recall, IServiceContext context) {
        String aStatus = recall.getApproveStatus();
        if (aStatus == null) {
            aStatus = ErpQaConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(aStatus, ErpQaConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(aStatus, ErpQaConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(recall, aStatus, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpQaRecall recall, IServiceContext context) {
        String aStatus = recall.getApproveStatus();
        if (aStatus == null || !Objects.equals(aStatus, ErpQaConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(recall, aStatus, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpQaRecall recall, IServiceContext context) {
        String aStatus = recall.getApproveStatus();
        if (aStatus == null || !Objects.equals(aStatus, ErpQaConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(recall, aStatus, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpQaRecall recall, IServiceContext context) {
        String aStatus = recall.getApproveStatus();
        if (aStatus == null || !Objects.equals(aStatus, ErpQaConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(recall, aStatus, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpQaRecall recall, IServiceContext context) {
        String aStatus = recall.getApproveStatus();
        if (aStatus == null || !Objects.equals(aStatus, ErpQaConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(recall, aStatus, "APPROVED");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpQaRecall recall, IServiceContext context) {
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_OPEN, "OPEN");
    }

    protected void validateBusinessRulesForApprove(ErpQaRecall recall, IServiceContext context) {
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_OPEN, "OPEN");
    }

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected void doSubmit(ErpQaRecall recall, IServiceContext context) {
        recall.setApproveStatus(ErpQaConstants.APPROVE_STATUS_SUBMITTED);
        recallDao().updateEntity(recall);
    }

    protected void doWithdrawSubmit(ErpQaRecall recall, IServiceContext context) {
        recall.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
        recallDao().updateEntity(recall);
    }

    protected void doApprove(ErpQaRecall recall, IServiceContext context) {
        recall.setApproveStatus(ErpQaConstants.APPROVE_STATUS_APPROVED);
        recall.setStatus(ErpQaConstants.RECALL_STATUS_APPROVED);
        recall.setApprovedBy(currentUserId(context));
        recall.setApprovedAt(CoreMetrics.currentDateTime());
        recallDao().updateEntity(recall);
    }

    protected void doReject(ErpQaRecall recall, IServiceContext context) {
        recall.setApproveStatus(ErpQaConstants.APPROVE_STATUS_REJECTED);
        recall.setStatus(ErpQaConstants.RECALL_STATUS_CANCELLED);
        recall.setApprovedBy(currentUserId(context));
        recall.setApprovedAt(CoreMetrics.currentDateTime());
        recallDao().updateEntity(recall);
    }

    protected void doReverseApprove(ErpQaRecall recall, IServiceContext context) {
        recall.setApproveStatus(ErpQaConstants.APPROVE_STATUS_REJECTED);
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
        if (current == null || !Objects.equals(current, expected)) {
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
