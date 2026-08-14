package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.DisposalPostingDispatcher;
import app.erp.ast.service.statemachine.ErpAstAssetStateMachine;
import app.erp.ast.service.statemachine.ErpAstDepreciationScheduleStateMachine;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.sql.Timestamp;

import static io.nop.api.core.beans.FilterBeans.eq;

public class ErpAstDisposalProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    DisposalPostingDispatcher postingDispatcher;

    @Inject
    ErpAstDisposalSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpAstDisposalApproveProcessor approveProcessor;

    @Inject
    ErpAstDisposalRejectProcessor rejectProcessor;

    @Inject
    ErpAstDisposalReverseApproveProcessor reverseApproveProcessor;

    @Inject
    ErpAstDisposalWithdrawApprovalProcessor withdrawApprovalProcessor;

    @Inject
    ErpAstAssetStateMachine assetStateMachine;

    @Inject
    ErpAstDepreciationScheduleStateMachine scheduleStateMachine;

    public ErpAstDisposal submitForApproval(String id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpAstDisposal withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpAstDisposal approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    protected ErpAstDisposal executeApprove(String id, ErpAstDisposal disposal, IServiceContext context) {
        ErpAstAsset asset = disposal.getAsset();
        validateAssetDisposable(asset, context);

        BigDecimal original = nz(asset.getOriginalValue());
        BigDecimal accumDep = nz(asset.getAccumulatedDepreciation());
        BigDecimal nbv = original.subtract(accumDep);
        BigDecimal disposalAmount = nz(disposal.getDisposalAmount());
        BigDecimal gainLoss = disposalAmount.subtract(nbv);

        // 固定来源/目标态判断委托 StateMachine Bean（M4.40，契约 §4/§7；按 disposalType 选 scrap/sell 目标态）
        assetStateMachine.assertCanDispose(asset.getStatus());
        String terminalStatus = disposal.getDisposalType() != null
                && Objects.equals(disposal.getDisposalType(), ErpAstConstants.DISPOSAL_TYPE_SOLD)
                        ? assetStateMachine.disposeSellTargetStatus()
                        : assetStateMachine.disposeScrapTargetStatus();
        asset.setStatus(terminalStatus);
        daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);

        cancelPendingSchedules(asset.getId());

        disposal.setGainLoss(gainLoss);
        disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_APPROVED);
        disposal.setDocStatus(ErpAstConstants.DOC_STATUS_ACTIVE);
        disposal.setApprovedBy(currentUserId());
        disposal.setApprovedAt(CoreMetrics.currentTimestamp());
        disposalDao().updateEntity(disposal);
        orm().flushSession();

        ErpAstAssetCategory category = asset.getCategory();
        Long voucherId = postingDispatcher.tryPost(disposal, asset, category);

        disposal = reload(id);
        Timestamp now = CoreMetrics.currentTimestamp();
        if (voucherId != null) {
            disposal.setPosted(true);
            disposal.setPostedAt(now);
            disposal.setPostedBy(currentUserId());
        }
        disposalDao().updateEntity(disposal);
        return disposal;
    }

    public ErpAstDisposal reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    public ErpAstDisposal reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    protected ErpAstDisposal executeReverseApprove(String id, ErpAstDisposal disposal, IServiceContext context) {
        if (Boolean.TRUE.equals(disposal.getPosted())) {
            postingDispatcher.reverse(disposal);
            ErpAstAsset asset = disposal.getAsset();
            if (asset != null) {
                // 固定来源/目标态判断委托 StateMachine Bean（M4.40，契约 §4/§7）
                assetStateMachine.assertCanReverseDispose(asset.getStatus());
                asset.setStatus(assetStateMachine.reverseDisposalTargetStatus());
                daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);
            }
            restoreCancelledSchedules(disposal.getAssetId());
            disposal = reload(id);
            disposal.setPosted(false);
            disposal.setPostedAt(null);
            disposal.setPostedBy(null);
            disposal.setGainLoss(null);
        }
        disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        disposalDao().updateEntity(disposal);
        return disposal;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpAstDisposal disposal, IServiceContext context) {
        String status = currentApproveStatus(disposal);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpAstConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(disposal, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpAstDisposal disposal, IServiceContext context) {
        String status = currentApproveStatus(disposal);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(disposal, status, ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForApprove(ErpAstDisposal disposal, IServiceContext context) {
        String status = currentApproveStatus(disposal);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(disposal, status, ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReject(ErpAstDisposal disposal, IServiceContext context) {
        String status = currentApproveStatus(disposal);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(disposal, status, ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReverseApprove(ErpAstDisposal disposal, IServiceContext context) {
        String status = currentApproveStatus(disposal);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(disposal, status, ErpAstConstants.APPROVE_STATUS_APPROVED);
        }
    }

    protected void validateTransitionForCancel(ErpAstDisposal disposal, IServiceContext context) {
        if (disposal.isCancelled()) {
            throw illegalDocTransition(disposal, disposal.getDocStatus(), "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateForApproval(ErpAstDisposal disposal, IServiceContext context) {
        if (disposal.getAssetId() == null || disposal.getDisposalType() == null) {
            throw new NopException(ErpAstErrors.ERR_DISPOSAL_ASSET_NOT_DISPOSABLE)
                    .param(ErpAstErrors.ARG_DISPOSAL_CODE, disposal.getCode());
        }
    }

    protected void validateAssetDisposable(ErpAstAsset asset, IServiceContext context) {
        String assetStatus = asset.getStatus();
        if (assetStatus != null
                && (Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_SCRAPPED)
                        || Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_SOLD))) {
            throw new NopException(ErpAstErrors.ERR_DISPOSAL_ASSET_ALREADY_DISPOSED)
                    .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }
        if (assetStatus == null
                || (!Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_IN_SERVICE)
                        && !Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_IDLE))) {
            throw new NopException(ErpAstErrors.ERR_DISPOSAL_ASSET_NOT_DISPOSABLE)
                    .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }
    }

    // ---------- 折旧计划状态联动 ----------

    protected void cancelPendingSchedules(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", assetId));
        q.addFilter(eq("status", ErpAstConstants.SCHEDULE_STATUS_PENDING));
        for (ErpAstDepreciationSchedule s : dao.findAllByQuery(q)) {
            s.setStatus(scheduleStateMachine.cancelTargetStatus());
            dao.saveOrUpdateEntity(s);
        }
    }

    protected void restoreCancelledSchedules(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", assetId));
        q.addFilter(eq("status", ErpAstConstants.SCHEDULE_STATUS_CANCELLED));
        for (ErpAstDepreciationSchedule s : dao.findAllByQuery(q)) {
            s.setStatus(scheduleStateMachine.restoreTargetStatus());
            dao.saveOrUpdateEntity(s);
        }
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpAstDisposal requireDisposal(String id, IServiceContext context) {
        ErpAstDisposal disposal = disposalDao().getEntityById(id);
        if (disposal == null) {
            throw new NopException(ErpAstErrors.ERR_DISPOSAL_NOT_FOUND)
                    .param(ErpAstErrors.ARG_DISPOSAL_ID, id);
        }
        return disposal;
    }

    protected void validateNotCancelled(ErpAstDisposal disposal, IServiceContext context) {
        validateTransitionForCancel(disposal, context);
    }

    protected String currentApproveStatus(ErpAstDisposal disposal) {
        String status = disposal.getApproveStatus();
        return status != null ? status : ErpAstConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    protected ErpAstDisposal reload(String id) {
        return disposalDao().getEntityById(id);
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpAstDisposal> disposalDao() {
        return daoProvider.daoFor(ErpAstDisposal.class);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) disposalDao()).getOrmTemplate();
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    protected NopException illegalTransition(ErpAstDisposal disposal, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_DISPOSAL_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_DISPOSAL_CODE, disposal.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpAstDisposal disposal, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_DISPOSAL_ILLEGAL_DOC_TRANSITION)
                .param(ErpAstErrors.ARG_DISPOSAL_CODE, disposal.getCode())
                .param(ErpAstErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
