
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstDisposalBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.DisposalPostingDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 资产处置 BizModel（{@code depreciation-and-posting.md} §3）。CRUD 之上实现三轴审批状态机
 * （UNSUBMITTED↔SUBMITTED→APPROVED/REJECTED；reverseApprove APPROVED→REJECTED）。APPROVED 时计算清理损益
 * （gainLoss = 处置收入 − 账面净值，账面净值 = 原值 − 累计折旧，§3.3）+ 资产 status→SCRAPPED/SOLD
 * + 后续未执行折旧计划标记 CANCELLED(40)（§5.1 当月减少当月停）+ 触发 DISPOSAL(90) 业财过账。
 *
 * <p>终态不可恢复（§关键规则3）：已处置资产不可重新激活，需经 reverseApprove 红字冲销处置凭证 + 恢复资产状态。
 * reverseApprove：若已过账，红冲 DISPOSAL 凭证 + 资产 status 恢复 IN_SERVICE + CANCELLED 折旧计划恢复 PENDING。
 *
 * <p>{@code @BizMutation} 自动包装事务（不叠加 {@code @Transactional}）。
 */
@BizModel("ErpAstDisposal")
public class ErpAstDisposalBizModel extends CrudBizModel<ErpAstDisposal> implements IErpAstDisposalBiz {

    @Inject
    DisposalPostingDispatcher postingDispatcher;

    public ErpAstDisposalBizModel() {
        setEntityName(ErpAstDisposal.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDisposal submit(@Name("id") Long id, IServiceContext context) {
        ErpAstDisposal disposal = requireDisposal(id, context);
        requireNotCancelled(disposal);
        Integer status = currentApproveStatus(disposal);
        if (status != ErpAstConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpAstConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(disposal, status, "UNSUBMITTED 或 REJECTED");
        }
        validateForApproval(disposal);
        disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(disposal);
        return disposal;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDisposal approve(@Name("id") Long id, IServiceContext context) {
        ErpAstDisposal disposal = requireDisposal(id, context);
        Integer status = currentApproveStatus(disposal);
        if (status == ErpAstConstants.APPROVE_STATUS_APPROVED) {
            return disposal;
        }
        requireNotCancelled(disposal);
        if (status != ErpAstConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(disposal, status, "SUBMITTED");
        }
        validateForApproval(disposal);

        ErpAstAsset asset = daoProvider().daoFor(ErpAstAsset.class).getEntityById(disposal.getAssetId());
        Integer assetStatus = asset.getStatus();
        if (assetStatus != null
                && (assetStatus == ErpAstConstants.ASSET_STATUS_SCRAPPED
                        || assetStatus == ErpAstConstants.ASSET_STATUS_SOLD)) {
            throw new NopException(ErpAstErrors.ERR_DISPOSAL_ASSET_ALREADY_DISPOSED)
                    .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }
        if (assetStatus == null
                || (assetStatus != ErpAstConstants.ASSET_STATUS_IN_SERVICE
                        && assetStatus != ErpAstConstants.ASSET_STATUS_IDLE)) {
            throw new NopException(ErpAstErrors.ERR_DISPOSAL_ASSET_NOT_DISPOSABLE)
                    .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }

        // 清理损益 = 处置收入 − 账面净值（账面净值 = 原值 − 累计折旧）
        BigDecimal original = nz(asset.getOriginalValue());
        BigDecimal accumDep = nz(asset.getAccumulatedDepreciation());
        BigDecimal nbv = original.subtract(accumDep);
        BigDecimal disposalAmount = nz(disposal.getDisposalAmount());
        BigDecimal gainLoss = disposalAmount.subtract(nbv);

        // 资产终态：SCRAPPED/SOLD
        int terminalStatus = disposal.getDisposalType() != null
                && disposal.getDisposalType() == ErpAstConstants.DISPOSAL_TYPE_SOLD
                        ? ErpAstConstants.ASSET_STATUS_SOLD
                        : ErpAstConstants.ASSET_STATUS_SCRAPPED;
        asset.setStatus(terminalStatus);
        daoProvider().daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);

        // 后续未执行折旧计划标记 CANCELLED（§5.1 当月减少当月停）
        cancelPendingSchedules(asset.getId());

        disposal.setGainLoss(gainLoss);
        disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_APPROVED);
        disposal.setDocStatus(ErpAstConstants.DOC_STATUS_ACTIVE);
        disposal.setApprovedBy(currentUserId());
        disposal.setApprovedAt(CoreMetrics.currentDateTime());
        dao().updateEntity(disposal);
        orm().flushSession();

        // DISPOSAL(90) 业财过账
        ErpAstAssetCategory category = asset.getCategoryId() == null ? null
                : daoProvider().daoFor(ErpAstAssetCategory.class).getEntityById(asset.getCategoryId());
        Long voucherId = postingDispatcher.tryPost(disposal, asset, category);

        disposal = requireEntity(String.valueOf(id), null, context);
        LocalDateTime now = CoreMetrics.currentDateTime();
        if (voucherId != null) {
            disposal.setPosted(true);
            disposal.setPostedAt(now);
            disposal.setPostedBy(currentUserId());
        }
        dao().updateEntity(disposal);
        return disposal;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDisposal reject(@Name("id") Long id, IServiceContext context) {
        ErpAstDisposal disposal = requireDisposal(id, context);
        requireNotCancelled(disposal);
        Integer status = currentApproveStatus(disposal);
        if (status != ErpAstConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(disposal, status, "SUBMITTED");
        }
        disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(disposal);
        return disposal;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDisposal reverseApprove(@Name("id") Long id, IServiceContext context) {
        ErpAstDisposal disposal = requireDisposal(id, context);
        Integer status = currentApproveStatus(disposal);
        if (status == ErpAstConstants.APPROVE_STATUS_REJECTED) {
            return disposal;
        }
        if (status != ErpAstConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(disposal, status, "APPROVED");
        }
        if (Boolean.TRUE.equals(disposal.getPosted())) {
            // 红字冲销 DISPOSAL 凭证（硬前置）
            postingDispatcher.reverse(disposal);
            ErpAstAsset asset = daoProvider().daoFor(ErpAstAsset.class).getEntityById(disposal.getAssetId());
            if (asset != null) {
                // 恢复资产状态（终态可经冲销恢复，§关键规则3 例外）
                asset.setStatus(ErpAstConstants.ASSET_STATUS_IN_SERVICE);
                daoProvider().daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);
            }
            // CANCELLED 折旧计划恢复 PENDING
            restoreCancelledSchedules(disposal.getAssetId());
            disposal = requireEntity(String.valueOf(id), null, context);
            disposal.setPosted(false);
            disposal.setPostedAt(null);
            disposal.setPostedBy(null);
            disposal.setGainLoss(null);
        }
        disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(disposal);
        return disposal;
    }

    // ---------- validation / helpers ----------

    private void validateForApproval(ErpAstDisposal disposal) {
        if (disposal.getAssetId() == null || disposal.getDisposalType() == null) {
            throw new NopException(ErpAstErrors.ERR_DISPOSAL_ASSET_NOT_DISPOSABLE)
                    .param(ErpAstErrors.ARG_DISPOSAL_CODE, disposal.getCode());
        }
    }

    private void cancelPendingSchedules(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider().daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", assetId));
        q.addFilter(eq("status", ErpAstConstants.SCHEDULE_STATUS_PENDING));
        for (ErpAstDepreciationSchedule s : dao.findAllByQuery(q)) {
            s.setStatus(ErpAstConstants.SCHEDULE_STATUS_CANCELLED);
            dao.saveOrUpdateEntity(s);
        }
    }

    private void restoreCancelledSchedules(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider().daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", assetId));
        q.addFilter(eq("status", ErpAstConstants.SCHEDULE_STATUS_CANCELLED));
        for (ErpAstDepreciationSchedule s : dao.findAllByQuery(q)) {
            s.setStatus(ErpAstConstants.SCHEDULE_STATUS_PENDING);
            dao.saveOrUpdateEntity(s);
        }
    }

    private ErpAstDisposal requireDisposal(Long id, IServiceContext context) {
        return requireEntity(String.valueOf(id), null, context);
    }

    private void requireNotCancelled(ErpAstDisposal disposal) {
        Integer docStatus = disposal.getDocStatus();
        if (docStatus != null && docStatus == ErpAstConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(disposal, docStatus, "非已作废");
        }
    }

    private Integer currentApproveStatus(ErpAstDisposal disposal) {
        Integer status = disposal.getApproveStatus();
        return status != null ? status : ErpAstConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private NopException illegalTransition(ErpAstDisposal disposal, Integer current, String expected) {
        return new NopException(ErpAstErrors.ERR_DISPOSAL_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_DISPOSAL_CODE, disposal.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpAstDisposal disposal, Integer current, String expected) {
        return new NopException(ErpAstErrors.ERR_DISPOSAL_ILLEGAL_DOC_TRANSITION)
                .param(ErpAstErrors.ARG_DISPOSAL_CODE, disposal.getCode())
                .param(ErpAstErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
