package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstValueAdjustment;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.ValueAdjustmentPostingDispatcher;
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

    public ErpAstValueAdjustment submitForApproval(String id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = requireAdjustment(id, context);
        validateNotCancelled(adjustment, context);
        validateTransitionForSubmit(adjustment, context);
        validateForApproval(adjustment, context);
        if (!isApprovalRequired()) {
            return doAutoApprove(id, adjustment, context);
        }
        adjustment.setApproveStatus(ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        adjustmentDao().updateEntity(adjustment);
        return adjustment;
    }

    public ErpAstValueAdjustment withdrawApproval(String id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = requireAdjustment(id, context);
        validateNotCancelled(adjustment, context);
        validateTransitionForWithdraw(adjustment, context);
        adjustment.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        adjustmentDao().updateEntity(adjustment);
        return adjustment;
    }

    public ErpAstValueAdjustment approve(String id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = requireAdjustment(id, context);
        if (adjustment.isApproved()) {
            return adjustment;
        }
        validateNotCancelled(adjustment, context);
        validateTransitionForApprove(adjustment, context);
        validateForApproval(adjustment, context);

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(adjustment.getAssetId());
        validateAssetAdjustable(asset, context);

        adjustment.setApproveStatus(ErpAstConstants.APPROVE_STATUS_APPROVED);
        adjustment.setDocStatus(ErpAstConstants.DOC_STATUS_ACTIVE);
        adjustment.setApprovedBy(currentUserId());
        adjustment.setApprovedAt(CoreMetrics.currentTimestamp());
        adjustmentDao().updateEntity(adjustment);
        orm().flushSession();

        ErpAstAssetCategory category = asset.getCategoryId() == null ? null
                : daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(asset.getCategoryId());
        Long voucherId = postingDispatcher.tryPost(adjustment, asset, category);

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
        ErpAstValueAdjustment adjustment = requireAdjustment(id, context);
        validateNotCancelled(adjustment, context);
        validateTransitionForReject(adjustment, context);
        adjustment.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        adjustmentDao().updateEntity(adjustment);
        return adjustment;
    }

    public ErpAstValueAdjustment reverseApprove(String id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = requireAdjustment(id, context);
        if (adjustment.isRejected()) {
            return adjustment;
        }
        validateTransitionForReverseApprove(adjustment, context);
        if (Boolean.TRUE.equals(adjustment.getPosted())) {
            postingDispatcher.reverse(adjustment);
            rollbackAssetValue(adjustment);
            adjustment = reload(id);
            adjustment.setPosted(false);
            adjustment.setPostedAt(null);
            adjustment.setPostedBy(null);
        }
        adjustment.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        adjustmentDao().updateEntity(adjustment);
        return adjustment;
    }

    public ErpAstValueAdjustment cancel(Long id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = requireAdjustment(id, context);
        validateTransitionForCancel(adjustment, context);
        adjustment.setDocStatus(ErpAstConstants.DOC_STATUS_CANCELLED);
        adjustmentDao().updateEntity(adjustment);
        return adjustment;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String status = currentApproveStatus(adjustment);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpAstConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(adjustment, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String status = currentApproveStatus(adjustment);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(adjustment, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String status = currentApproveStatus(adjustment);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(adjustment, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String status = currentApproveStatus(adjustment);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(adjustment, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String status = currentApproveStatus(adjustment);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(adjustment, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpAstValueAdjustment adjustment, IServiceContext context) {
        String docStatus = adjustment.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpAstConstants.DOC_STATUS_ACTIVE)) {
            throw illegalDocTransition(adjustment, docStatus, "非已生效");
        }
        if (docStatus != null && Objects.equals(docStatus, ErpAstConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(adjustment, docStatus, "非已作废");
        }
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
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(adjustment.getAssetId());
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
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(adjustment.getAssetId());
        validateAssetAdjustable(asset, context);

        adjustment.setApproveStatus(ErpAstConstants.APPROVE_STATUS_APPROVED);
        adjustment.setDocStatus(ErpAstConstants.DOC_STATUS_ACTIVE);
        adjustment.setApprovedBy(currentUserId());
        adjustment.setApprovedAt(CoreMetrics.currentTimestamp());
        adjustmentDao().updateEntity(adjustment);
        orm().flushSession();

        ErpAstAssetCategory category = asset.getCategoryId() == null ? null
                : daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(asset.getCategoryId());
        Long voucherId = postingDispatcher.tryPost(adjustment, asset, category);

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

    protected ErpAstValueAdjustment requireAdjustment(Long id, IServiceContext context) {
        return requireAdjustment(String.valueOf(id), context);
    }

    protected ErpAstValueAdjustment requireAdjustment(String id, IServiceContext context) {
        ErpAstValueAdjustment adjustment = adjustmentDao().getEntityById(id);
        if (adjustment == null) {
            throw new NopException(ErpAstErrors.ERR_ADJUSTMENT_NOT_FOUND)
                    .param(ErpAstErrors.ARG_ADJUSTMENT_ID, id);
        }
        return adjustment;
    }

    protected void validateNotCancelled(ErpAstValueAdjustment adjustment, IServiceContext context) {
        if (adjustment.isCancelled()) {
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
        return new NopException(ErpAstErrors.ERR_ADJUSTMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_ADJUSTMENT_CODE, adjustment.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpAstValueAdjustment adjustment, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_ADJUSTMENT_ILLEGAL_DOC_TRANSITION)
                .param(ErpAstErrors.ARG_ADJUSTMENT_CODE, adjustment.getCode())
                .param(ErpAstErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
