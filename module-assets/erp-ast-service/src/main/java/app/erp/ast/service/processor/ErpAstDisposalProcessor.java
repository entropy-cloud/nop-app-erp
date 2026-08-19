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
import app.erp.ast.service.statemachine.ErpAstDisposalApprovalStateMachine;
import app.erp.ast.service.statemachine.ErpAstDisposalDocumentStateMachine;
import app.erp.mnt.biz.IErpMntEquipmentBiz;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.util.List;
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

    @Inject
    ErpAstDepreciationScheduleProcessor depreciationScheduleFacade;

    @Inject
    ErpAstDepreciationScheduleCatchUpDepreciationProcessor catchUpDepreciationProcessor;

    @Inject
    ErpAstDisposalApprovalStateMachine approvalStateMachine;

    @Inject
    ErpAstDisposalDocumentStateMachine documentStateMachine;

    /**
     * RC-R1.77 / UC-MAIN-08：资产处置→设备 DECOMMISSIONED 联动 Facade（assets→maintenance Java 层新边）。
     * @Nullable：mnt 模块未部署（单域测试容器/裁剪部署）时跳过联动，不阻断处置主流程。
     */
    @Nullable
    @Inject
    IErpMntEquipmentBiz mntEquipmentBiz;

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

        // RC-R1.52 出售补提接线（reuse P1-RC-029 投影，L1 UC-AST-05 ⑤「先补提当期折旧至出售日」）：
        // 损益计算前补提自最近已执行期至出售期的漏提折旧，避免月中处置累计折旧低估→净值高估→gainLoss 误算
        catchUpDepreciationToDisposalPeriod(disposal, asset, context);

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

        // RC-R1.77 / UC-MAIN-08：处置 approve 后置联动设备 DECOMMISSIONED（同 JVM 同事务，失败异常传播
        // 回滚处置——设备停用是 L1 硬断言，处置成功但设备未停用 = 契约破坏；mnt 模块缺失或
        // erp-mnt.disposal-link-enabled 关闭时由 Facade 侧 no-op 跳过）。
        decommissionLinkedEquipment(disposal, asset, context);

        disposal.setGainLoss(gainLoss);
        disposal.setApproveStatus(approvalStateMachine.approveTargetStatus());
        disposal.setDocStatus(documentStateMachine.approveTargetStatus());
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
            // RC-R1.77：冲销对称恢复与资产恢复同分支（仅 posted==TRUE），防「设备 RUNNING / 资产 SCRAPPED」分叉；
            // 设备非 DECOMMISSIONED 时 Facade 侧幂等跳过。
            restoreLinkedEquipment(disposal, context);
            disposal = reload(id);
            disposal.setPosted(false);
            disposal.setPostedAt(null);
            disposal.setPostedBy(null);
            disposal.setGainLoss(null);
        }
        disposal.setApproveStatus(approvalStateMachine.reverseApproveTargetStatus());
        disposalDao().updateEntity(disposal);
        return disposal;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpAstDisposal disposal, IServiceContext context) {
        String status = currentApproveStatus(disposal);
        try {
            approvalStateMachine.assertCanSubmitForApproval(status);
        } catch (NopException e) {
            throw illegalTransition(disposal, status, "UNSUBMITTED 或 REJECTED", e);
        }
    }

    protected void validateTransitionForWithdraw(ErpAstDisposal disposal, IServiceContext context) {
        String status = currentApproveStatus(disposal);
        try {
            approvalStateMachine.assertCanWithdrawApproval(status);
        } catch (NopException e) {
            throw illegalTransition(disposal, status, ErpAstConstants.APPROVE_STATUS_SUBMITTED, e);
        }
    }

    protected void validateTransitionForApprove(ErpAstDisposal disposal, IServiceContext context) {
        String status = currentApproveStatus(disposal);
        try {
            approvalStateMachine.assertCanApprove(status);
        } catch (NopException e) {
            throw illegalTransition(disposal, status, ErpAstConstants.APPROVE_STATUS_SUBMITTED, e);
        }
    }

    protected void validateTransitionForReject(ErpAstDisposal disposal, IServiceContext context) {
        String status = currentApproveStatus(disposal);
        try {
            approvalStateMachine.assertCanReject(status);
        } catch (NopException e) {
            throw illegalTransition(disposal, status, ErpAstConstants.APPROVE_STATUS_SUBMITTED, e);
        }
    }

    protected void validateTransitionForReverseApprove(ErpAstDisposal disposal, IServiceContext context) {
        String status = currentApproveStatus(disposal);
        try {
            approvalStateMachine.assertCanReverseApprove(status);
        } catch (NopException e) {
            throw illegalTransition(disposal, status, ErpAstConstants.APPROVE_STATUS_APPROVED, e);
        }
    }

    protected void validateTransitionForCancel(ErpAstDisposal disposal, IServiceContext context) {
        if (documentStateMachine.isCancelled(disposal.getDocStatus())) {
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

    // ---------- RC-R1.77：设备停用联动（protected step，下游可覆盖） ----------

    protected void decommissionLinkedEquipment(ErpAstDisposal disposal, ErpAstAsset asset, IServiceContext context) {
        if (mntEquipmentBiz == null || asset == null || asset.getId() == null) {
            return;
        }
        mntEquipmentBiz.changeStatusForAssetDisposal(asset.getId(), disposal.getCode(), context);
    }

    protected void restoreLinkedEquipment(ErpAstDisposal disposal, IServiceContext context) {
        if (mntEquipmentBiz == null || disposal.getAssetId() == null) {
            return;
        }
        mntEquipmentBiz.restoreFromAssetDisposal(disposal.getAssetId(), disposal.getCode(), context);
    }

    // ---------- 折旧计划状态联动 ----------

    /**
     * RC-R1.52 出售补提接线（protected step，下游可覆盖）：在清理损益计算前将资产累计折旧补提至出售期。
     * 补提期间 = (最近 EXECUTED 折旧期间, 出售期间] 逐月（含出售当期——L1 UC-AST-05 ⑤「补提当期折旧至出售日」）。
     * 无已执行折旧（无时间基线）或出售期间早于等于最近已执行期时跳过（无漏提）；IDLE 资产跳过
     * （Phase 1 Decision：IDLE 不允许补提——闲置期无折旧义务，恢复至 IN_SERVICE 后方可补提，出售时 IDLE 以卡片账面计提为准）。
     * 补提经 {@code catchUpDepreciation} 以出售期间为当前期间落行 + 汇总凭证（billHeadCode 后缀 #CATCHUP）。
     */
    protected void catchUpDepreciationToDisposalPeriod(ErpAstDisposal disposal, ErpAstAsset asset, IServiceContext context) {
        if (asset.getStatus() == null
                || !Objects.equals(asset.getStatus(), ErpAstConstants.ASSET_STATUS_IN_SERVICE)) {
            return;
        }
        if (disposal.getBusinessDate() == null) {
            return;
        }
        String disposalPeriod;
        try {
            disposalPeriod = java.time.YearMonth.from(disposal.getBusinessDate()).toString();
        } catch (Exception e) {
            return;
        }
        String lastExecuted = depreciationScheduleFacade.findLastExecutedPeriod(asset.getId());
        if (lastExecuted == null || lastExecuted.compareTo(disposalPeriod) >= 0) {
            return;
        }
        List<String> missed = new java.util.ArrayList<>();
        java.time.YearMonth cursor = java.time.YearMonth.parse(lastExecuted).plusMonths(1);
        java.time.YearMonth end = java.time.YearMonth.parse(disposalPeriod);
        while (!cursor.isAfter(end)) {
            missed.add(cursor.toString());
            cursor = cursor.plusMonths(1);
        }
        catchUpDepreciationProcessor.catchUpDepreciation(asset.getId(), disposalPeriod, missed, context);
    }

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
        return illegalTransition(disposal, current, expected, null);
    }

    /**
     * Bean common 码 → 领域码映射（common 作 cause 保留，契约 §7）。参数由本层组装，对外不变。
     */
    protected NopException illegalTransition(ErpAstDisposal disposal, String current, String expected, NopException cause) {
        return new NopException(ErpAstErrors.ERR_DISPOSAL_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpAstErrors.ARG_DISPOSAL_CODE, disposal.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpAstDisposal disposal, String current, String expected) {
        return illegalDocTransition(disposal, current, expected, null);
    }

    /**
     * Bean common 码 → 领域码映射（common 作 cause 保留，契约 §7）。参数由本层组装，对外不变。
     */
    protected NopException illegalDocTransition(ErpAstDisposal disposal, String current, String expected, NopException cause) {
        return new NopException(ErpAstErrors.ERR_DISPOSAL_ILLEGAL_DOC_TRANSITION, cause)
                .param(ErpAstErrors.ARG_DISPOSAL_CODE, disposal.getCode())
                .param(ErpAstErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
