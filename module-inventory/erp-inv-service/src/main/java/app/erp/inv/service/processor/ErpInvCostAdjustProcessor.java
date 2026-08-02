package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvCostAdjustLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 成本调整单编排 facade Processor（plan 2026-07-05-2352-3；R6.4 slim-to-S-delegation）。
 *
 * <p>承接 use-approval DIRECT 审批范式（plan 2050-1）：标准 5 action（submitForApproval/approve/
 * reject/reverseApprove/withdrawApproval）单行委托对应 per-mutation Processor。
 *
 * <p>域动作 {@code applyCostAdjust}/{@code reverseCostAdjust} 已拆为独立 per-mutation Processor
 * （{@link ErpInvCostAdjustApplyCostAdjustProcessor}/{@link ErpInvCostAdjustReverseCostAdjustProcessor}）。
 * 成本变更/过账编排服务由各 per-mutation Processor 自包含持有，本 facade 仅保留审批门控判断、状态守卫、
 * 查询加载等被多 mutation 共享的 protected helper（单一真相源）。
 *
 * <p>审批门控（config {@code erp-fin.cost-adjust-approval}，默认 true）：
 * <ul>
 *   <li>开启：apply 前置 approveStatus=APPROVED（DIRECT 审批状态机）</li>
 *   <li>关闭：允许 UNSUBMITTED 直接 apply（DRAFT 免审）</li>
 * </ul>
 *
 * <p>step 方法标记 protected，下游可逐个覆盖（产品化拓扑可变）。
 */
public class ErpInvCostAdjustProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpInvCostAdjustSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpInvCostAdjustApproveProcessor approveProcessor;

    @Inject
    ErpInvCostAdjustRejectProcessor rejectProcessor;

    @Inject
    ErpInvCostAdjustReverseApproveProcessor reverseApproveProcessor;

    @Inject
    ErpInvCostAdjustWithdrawApprovalProcessor withdrawApprovalProcessor;

    // ---------- 审批状态机（DIRECT 模式标准 5 action） ----------

    public ErpInvCostAdjust submitForApproval(String id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpInvCostAdjust withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpInvCostAdjust approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    public ErpInvCostAdjust reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    public ErpInvCostAdjust reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpInvCostAdjust adjust) {
        String status = currentApproveStatus(adjust);
        if (!Objects.equals(status, ErpInvConstants.APPROVE_STATUS_UNSUBMITTED) && !Objects.equals(status, ErpInvConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(adjust, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpInvCostAdjust adjust) {
        String status = currentApproveStatus(adjust);
        if (!Objects.equals(status, ErpInvConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(adjust, status, ErpInvConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForApprove(ErpInvCostAdjust adjust) {
        String status = currentApproveStatus(adjust);
        if (!Objects.equals(status, ErpInvConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(adjust, status, ErpInvConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReject(ErpInvCostAdjust adjust) {
        String status = currentApproveStatus(adjust);
        if (!Objects.equals(status, ErpInvConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(adjust, status, ErpInvConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReverseApprove(ErpInvCostAdjust adjust) {
        String status = currentApproveStatus(adjust);
        if (!Objects.equals(status, ErpInvConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(adjust, status, ErpInvConstants.APPROVE_STATUS_APPROVED);
        }
    }

    protected void validateNotCancelled(ErpInvCostAdjust adjust, IServiceContext context) {
        if (adjust.isCancelled()) {
            throw illegalTransition(adjust, adjust.getDocStatus(), "非已取消");
        }
    }

    public boolean isApprovalRequired() {
        return AppConfig.var(ErpInvConstants.CONFIG_COST_ADJUST_APPROVAL, true);
    }

    protected String currentApproveStatus(ErpInvCostAdjust adjust) {
        String status = adjust.getApproveStatus();
        return status != null ? status : ErpInvConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    // ---------- 查询/加载辅助 ----------

    protected ErpInvCostAdjust requireAdjustment(Long id, IServiceContext context) {
        return requireAdjustment(String.valueOf(id), context);
    }

    protected ErpInvCostAdjust requireAdjustment(String id, IServiceContext context) {
        ErpInvCostAdjust adjust = adjustDao().getEntityById(id);
        if (adjust == null) {
            throw new NopException(ErpInvErrors.ERR_COST_ADJUST_NOT_FOUND)
                    .param(ErpInvErrors.ARG_ADJUST_ID, id);
        }
        return adjust;
    }

    protected ErpInvCostAdjust reload(Long id) {
        return adjustDao().getEntityById(id);
    }

    protected List<ErpInvCostAdjustLine> loadLines(Long adjustId) {
        IEntityDao<ErpInvCostAdjustLine> dao = daoProvider.daoFor(ErpInvCostAdjustLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("adjustId", adjustId));
        q.addOrderField("lineNo", false);
        return dao.findAllByQuery(q);
    }

    protected IEntityDao<ErpInvCostAdjust> adjustDao() {
        return daoProvider.daoFor(ErpInvCostAdjust.class);
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected NopException illegalTransition(ErpInvCostAdjust adjust, String current, String expected) {
        return new NopException(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpInvErrors.ARG_MOVE_CODE, adjust.getCode())
                .param(ErpInvErrors.ARG_CURRENT_STATUS, current)
                .param(ErpInvErrors.ARG_EXPECTED_STATUS, expected);
    }
}
