package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstValueAdjustment;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.ValueAdjustmentPostingDispatcher;
import app.erp.ast.service.statemachine.ErpAstValueAdjustmentApprovalStateMachine;
import app.erp.ast.service.statemachine.ErpAstValueAdjustmentDocumentStateMachine;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.config.AppConfig;
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

public class ErpAstValueAdjustmentProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ValueAdjustmentPostingDispatcher postingDispatcher;

    @Inject
    ErpAstValueAdjustmentSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpAstValueAdjustmentApproveProcessor approveProcessor;

    @Inject
    ErpAstValueAdjustmentRejectProcessor rejectProcessor;

    @Inject
    ErpAstValueAdjustmentReverseApproveProcessor reverseApproveProcessor;

    @Inject
    ErpAstValueAdjustmentWithdrawApprovalProcessor withdrawApprovalProcessor;

    @Inject
    ErpAstValueAdjustmentCancelProcessor cancelProcessor;

    @Inject
    ErpAstValueAdjustmentApprovalStateMachine approvalStateMachine;

    @Inject
    ErpAstValueAdjustmentDocumentStateMachine documentStateMachine;

    public ErpAstValueAdjustment submitForApproval(String id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpAstValueAdjustment withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpAstValueAdjustment approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    protected ErpAstValueAdjustment executeApprove(String id, ErpAstValueAdjustment adjustment,
                                                     IServiceContext context) {
        ErpAstAsset asset = adjustment.getAsset();
        validateAssetAdjustable(asset, context);

        adjustment.setApproveStatus(approvalStateMachine.approveTargetStatus());
        adjustment.setDocStatus(documentStateMachine.approveTargetStatus());
        adjustment.setApprovedBy(currentUserId());
        adjustment.setApprovedAt(CoreMetrics.currentTimestamp());
        adjustmentDao().updateEntity(adjustment);
        orm().flushSession();

        ErpAstAssetCategory category = asset.getCategory();
        String voucherId = postingDispatcher.tryPost(adjustment, asset, category);

        if (voucherId != null) {
            applyAssetValueChange(adjustment, asset);
        }

        adjustment = reload(id);
        Timestamp now = CoreMetrics.currentTimestamp();
        if (voucherId != null) {
            adjustment.setPosted(true);
            adjustment.setPostedAt(now);
            adjustment.setPostedBy(currentUserId());
        }
        adjustmentDao().updateEntity(adjustment);
        return adjustment;
    }

    public ErpAstValueAdjustment reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    public ErpAstValueAdjustment reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    protected ErpAstValueAdjustment executeReverseApprove(String id, ErpAstValueAdjustment adjustment,
                                                            IServiceContext context) {
        if (Boolean.TRUE.equals(adjustment.getPosted())) {
            postingDispatcher.reverse(adjustment);
            rollbackAssetValue(adjustment);
            adjustment = reload(id);
            adjustment.setPosted(false);
            adjustment.setPostedAt(null);
            adjustment.setPostedBy(null);
        }
        adjustment.setApproveStatus(approvalStateMachine.reverseApproveTargetStatus());
        adjustmentDao().updateEntity(adjustment);
        return adjustment;
    }

    public ErpAstValueAdjustment cancel(String id, IServiceContext context) {
        return cancelProcessor.cancel(id, context);
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String status = currentApproveStatus(adjustment);
        try {
            approvalStateMachine.assertCanSubmitForApproval(status);
        } catch (NopException e) {
            throw illegalTransition(adjustment, status, "UNSUBMITTED 或 REJECTED", e);
        }
    }

    protected void validateTransitionForWithdraw(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String status = currentApproveStatus(adjustment);
        try {
            approvalStateMachine.assertCanWithdrawApproval(status);
        } catch (NopException e) {
            throw illegalTransition(adjustment, status, ErpAstConstants.APPROVE_STATUS_SUBMITTED, e);
        }
    }

    protected void validateTransitionForApprove(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String status = currentApproveStatus(adjustment);
        try {
            approvalStateMachine.assertCanApprove(status);
        } catch (NopException e) {
            throw illegalTransition(adjustment, status, ErpAstConstants.APPROVE_STATUS_SUBMITTED, e);
        }
    }

    protected void validateTransitionForReject(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String status = currentApproveStatus(adjustment);
        try {
            approvalStateMachine.assertCanReject(status);
        } catch (NopException e) {
            throw illegalTransition(adjustment, status, ErpAstConstants.APPROVE_STATUS_SUBMITTED, e);
        }
    }

    protected void validateTransitionForReverseApprove(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String status = currentApproveStatus(adjustment);
        try {
            approvalStateMachine.assertCanReverseApprove(status);
        } catch (NopException e) {
            throw illegalTransition(adjustment, status, ErpAstConstants.APPROVE_STATUS_APPROVED, e);
        }
    }

    protected void validateTransitionForCancel(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String docStatus = adjustment.getDocStatus();
        try {
            documentStateMachine.assertCanCancel(docStatus);
        } catch (NopException e) {
            boolean active = ErpAstConstants.DOC_STATUS_ACTIVE.equals(docStatus);
            throw illegalDocTransition(adjustment, docStatus, active ? "非已生效" : "非已作废", e);
        }
        // posted 动态守卫保留原位（posted 不入轴，契约 §3）
        if (Boolean.TRUE.equals(adjustment.getPosted())) {
            throw illegalDocTransition(adjustment, docStatus, "非已过账");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateForApproval(ErpAstValueAdjustment adjustment, IServiceContext context) {
        if (adjustment.getAssetId() == null || adjustment.getAdjustmentType() == null) {
            throw new NopException(ErpAstErrors.ERR_ADJUSTMENT_TYPE_INVALID)
                    .param(ErpAstErrors.ARG_ADJUSTMENT_CODE, adjustment.getCode())
                    .param(ErpAstErrors.ARG_ADJUSTMENT_TYPE, adjustment.getAdjustmentType());
        }
        if (adjustment.getAdjustmentAmount() == null
                || adjustment.getAdjustmentAmount().signum() <= 0) {
            throw new NopException(ErpAstErrors.ERR_ADJUSTMENT_AMOUNT_INVALID)
                    .param(ErpAstErrors.ARG_ADJUSTMENT_CODE, adjustment.getCode())
                    .param(ErpAstErrors.ARG_AMOUNT, adjustment.getAdjustmentAmount());
        }
    }

    protected void validateAssetAdjustable(ErpAstAsset asset, IServiceContext context) {
        String assetStatus = asset.getStatus();
        if (assetStatus != null
                && (Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_SCRAPPED)
                        || Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_SOLD))) {
            throw new NopException(ErpAstErrors.ERR_ADJUSTMENT_ASSET_ALREADY_DISPOSED)
                    .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }
        if (assetStatus == null
                || (!Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_IN_SERVICE)
                        && !Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_IDLE))) {
            throw new NopException(ErpAstErrors.ERR_ADJUSTMENT_ASSET_NOT_ADJUSTABLE)
                    .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }
    }

    // ---------- 核心编排：审核 → 过账 → 资产净值联动 ----------

    protected void applyAssetValueChange(ErpAstValueAdjustment adjustment, ErpAstAsset asset) {
        BigDecimal amount = nz(adjustment.getAdjustmentAmount());
        BigDecimal currentNbv = nz(asset.getNetBookValue());
        String type = adjustment.getAdjustmentType();

        BigDecimal newNbv;
        if (Objects.equals(type, ErpAstConstants.ADJUSTMENT_TYPE_REVALUATION_UP)) {
            newNbv = currentNbv.add(amount);
        } else {
            newNbv = currentNbv.subtract(amount);
        }
        asset.setNetBookValue(newNbv);
        asset.setCurrentValue(newNbv);

        if (shouldAdjustDepreciationBase(type)) {
            BigDecimal residual = nz(asset.getResidualValue());
            BigDecimal newDepreciableBase = newNbv.subtract(residual);
            if (newDepreciableBase.signum() < 0) {
                newDepreciableBase = BigDecimal.ZERO;
            }
        }

        daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);
    }

    protected void rollbackAssetValue(ErpAstValueAdjustment adjustment) {
        ErpAstAsset asset = adjustment.getAsset();
        if (asset == null) {
            return;
        }
        BigDecimal amount = nz(adjustment.getAdjustmentAmount());
        BigDecimal currentNbv = nz(asset.getNetBookValue());
        String type = adjustment.getAdjustmentType();

        BigDecimal restoredNbv;
        if (Objects.equals(type, ErpAstConstants.ADJUSTMENT_TYPE_REVALUATION_UP)) {
            restoredNbv = currentNbv.subtract(amount);
        } else {
            restoredNbv = currentNbv.add(amount);
        }
        asset.setNetBookValue(restoredNbv);
        asset.setCurrentValue(restoredNbv);
        daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);
    }

    protected boolean shouldAdjustDepreciationBase(String adjustmentType) {
        if (Objects.equals(adjustmentType, ErpAstConstants.ADJUSTMENT_TYPE_IMPAIRMENT)) {
            return true;
        }
        if (Objects.equals(adjustmentType, ErpAstConstants.ADJUSTMENT_TYPE_REVALUATION_UP)) {
            return AppConfig.var(ErpAstConstants.CONFIG_REVALUATION_ADJUST_DEPRECIATION_BASE, true);
        }
        return false;
    }

    public boolean isApprovalRequired() {
        return AppConfig.var(ErpAstConstants.CONFIG_VALUE_ADJUSTMENT_REQUIRE_APPROVAL, true);
    }

    // ---------- 自动审批（无需审批流程时由 submitForApproval 调用） ----------

    protected ErpAstValueAdjustment doAutoApprove(String id, ErpAstValueAdjustment adjustment, IServiceContext context) {
        ErpAstAsset asset = adjustment.getAsset();
        validateAssetAdjustable(asset, context);

        adjustment.setApproveStatus(approvalStateMachine.approveTargetStatus());
        adjustment.setDocStatus(documentStateMachine.approveTargetStatus());
        adjustment.setApprovedBy(currentUserId());
        adjustment.setApprovedAt(CoreMetrics.currentTimestamp());
        adjustmentDao().updateEntity(adjustment);
        orm().flushSession();

        ErpAstAssetCategory category = asset.getCategory();
        String voucherId = postingDispatcher.tryPost(adjustment, asset, category);

        if (voucherId != null) {
            applyAssetValueChange(adjustment, asset);
        }

        adjustment = reload(id);
        Timestamp now = CoreMetrics.currentTimestamp();
        if (voucherId != null) {
            adjustment.setPosted(true);
            adjustment.setPostedAt(now);
            adjustment.setPostedBy(currentUserId());
        }
        adjustmentDao().updateEntity(adjustment);
        return adjustment;
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpAstValueAdjustment requireAdjustment(String id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = adjustmentDao().getEntityById(id);
        if (adjustment == null) {
            throw new NopException(ErpAstErrors.ERR_ADJUSTMENT_NOT_FOUND)
                    .param(ErpAstErrors.ARG_ADJUSTMENT_ID, id);
        }
        return adjustment;
    }

    protected void validateNotCancelled(ErpAstValueAdjustment adjustment, IServiceContext context) {
        if (documentStateMachine.isCancelled(adjustment.getDocStatus())) {
            throw illegalDocTransition(adjustment, adjustment.getDocStatus(), "非已作废");
        }
    }

    protected String currentApproveStatus(ErpAstValueAdjustment adjustment) {
        String status = adjustment.getApproveStatus();
        return status != null ? status : ErpAstConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    protected ErpAstValueAdjustment reload(String id) {
        return adjustmentDao().getEntityById(id);
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpAstValueAdjustment> adjustmentDao() {
        return daoProvider.daoFor(ErpAstValueAdjustment.class);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) adjustmentDao()).getOrmTemplate();
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

    protected NopException illegalTransition(ErpAstValueAdjustment adjustment, String current, String expected) {
        return illegalTransition(adjustment, current, expected, null);
    }

    /**
     * Bean common 码 → 领域码映射（common 作 cause 保留，契约 §7）。参数由本层组装，对外不变。
     */
    protected NopException illegalTransition(ErpAstValueAdjustment adjustment, String current, String expected, NopException cause) {
        return new NopException(ErpAstErrors.ERR_ADJUSTMENT_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpAstErrors.ARG_ADJUSTMENT_CODE, adjustment.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpAstValueAdjustment adjustment, String current, String expected) {
        return illegalDocTransition(adjustment, current, expected, null);
    }

    /**
     * Bean common 码 → 领域码映射（common 作 cause 保留，契约 §7）。参数由本层组装，对外不变。
     */
    protected NopException illegalDocTransition(ErpAstValueAdjustment adjustment, String current, String expected, NopException cause) {
        return new NopException(ErpAstErrors.ERR_ADJUSTMENT_ILLEGAL_DOC_TRANSITION, cause)
                .param(ErpAstErrors.ARG_ADJUSTMENT_CODE, adjustment.getCode())
                .param(ErpAstErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
