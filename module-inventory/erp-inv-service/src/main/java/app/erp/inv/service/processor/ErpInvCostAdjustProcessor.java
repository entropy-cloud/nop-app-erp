package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvCostAdjustLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.costing.CostAdjustmentService;
import app.erp.inv.service.posting.CostAdjustmentPostingDispatcher;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 成本调整单编排 Processor（plan 2026-07-05-2352-3）。
 *
 * <p>承接 use-approval DIRECT 审批范式（plan 2050-1）：标准 5 action（submitForApproval/approve/
 * reject/reverseApprove/withdrawApproval）+ 域动作 {@code applyCostAdjust}/{@code reverseCostAdjust}。
 * 成本变更委托 {@link CostAdjustmentService}，过账委托 {@link CostAdjustmentPostingDispatcher}。
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

    static final String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    static final String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    static final String APPROVE_STATUS_APPROVED = "APPROVED";
    static final String APPROVE_STATUS_REJECTED = "REJECTED";

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    CostAdjustmentService costAdjustmentService;

    @Inject
    CostAdjustmentPostingDispatcher postingDispatcher;

    // ---------- 审批状态机（DIRECT 模式标准 5 action） ----------

    public ErpInvCostAdjust submitForApproval(String id, IServiceContext context) {
        ErpInvCostAdjust adjust = requireAdjustment(id, context);
        validateNotCancelled(adjust);
        validateTransitionForSubmit(adjust);
        adjust.setApproveStatus(APPROVE_STATUS_SUBMITTED);
        adjustDao().updateEntity(adjust);
        return adjust;
    }

    public ErpInvCostAdjust withdrawApproval(String id, IServiceContext context) {
        ErpInvCostAdjust adjust = requireAdjustment(id, context);
        validateNotCancelled(adjust);
        validateTransitionForWithdraw(adjust);
        adjust.setApproveStatus(APPROVE_STATUS_UNSUBMITTED);
        adjustDao().updateEntity(adjust);
        return adjust;
    }

    public ErpInvCostAdjust approve(String id, IServiceContext context) {
        ErpInvCostAdjust adjust = requireAdjustment(id, context);
        if (isAlreadyApproved(adjust)) {
            return adjust;
        }
        validateNotCancelled(adjust);
        validateTransitionForApprove(adjust);
        adjust.setApproveStatus(APPROVE_STATUS_APPROVED);
        adjust.setApprovedBy(currentUserId());
        adjust.setApprovedAt(CoreMetrics.currentTimestamp());
        adjustDao().updateEntity(adjust);
        return adjust;
    }

    public ErpInvCostAdjust reject(String id, IServiceContext context) {
        ErpInvCostAdjust adjust = requireAdjustment(id, context);
        validateNotCancelled(adjust);
        validateTransitionForReject(adjust);
        adjust.setApproveStatus(APPROVE_STATUS_REJECTED);
        adjustDao().updateEntity(adjust);
        return adjust;
    }

    public ErpInvCostAdjust reverseApprove(String id, IServiceContext context) {
        ErpInvCostAdjust adjust = requireAdjustment(id, context);
        if (isAlreadyRejected(adjust)) {
            return adjust;
        }
        validateTransitionForReverseApprove(adjust);
        if (Boolean.TRUE.equals(adjust.getPosted())) {
            throw illegalTransition(adjust, currentApproveStatus(adjust), "未过账（先冲销再反审）");
        }
        adjust.setApproveStatus(APPROVE_STATUS_REJECTED);
        adjustDao().updateEntity(adjust);
        return adjust;
    }

    // ---------- 域动作：apply / reverse ----------

    public ErpInvCostAdjust applyCostAdjust(Long id, IServiceContext context) {
        ErpInvCostAdjust adjust = requireAdjustment(id, context);
        validateNotCancelled(adjust);
        if (Boolean.TRUE.equals(adjust.getPosted())) {
            throw new NopException(ErpInvErrors.ERR_COST_ADJUST_ALREADY_APPLIED)
                    .param(ErpInvErrors.ARG_ADJUST_CODE, adjust.getCode());
        }
        if (isApprovalRequired() && !Objects.equals(currentApproveStatus(adjust), APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpInvErrors.ERR_COST_ADJUST_NOT_APPROVED)
                    .param(ErpInvErrors.ARG_ADJUST_CODE, adjust.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, currentApproveStatus(adjust));
        }

        List<ErpInvCostAdjustLine> lines = loadLines(adjust.getId());
        BigDecimal totalAdjustAmount = costAdjustmentService.applyCostAdjust(adjust, lines);
        ormTemplate.flushSession();

        Long voucherId = postingDispatcher.tryPost(adjust, lines, totalAdjustAmount);

        adjust = reload(id);
        Timestamp now = CoreMetrics.currentTimestamp();
        adjust.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
        if (voucherId != null) {
            adjust.setPosted(true);
            adjust.setPostedAt(now);
            adjust.setPostedBy(currentUserId());
        }
        adjustDao().updateEntity(adjust);
        return adjust;
    }

    public ErpInvCostAdjust reverseCostAdjust(Long id, IServiceContext context) {
        ErpInvCostAdjust adjust = requireAdjustment(id, context);
        if (!Boolean.TRUE.equals(adjust.getPosted())) {
            throw new NopException(ErpInvErrors.ERR_COST_ADJUST_NOT_APPLIED)
                    .param(ErpInvErrors.ARG_ADJUST_CODE, adjust.getCode());
        }

        List<ErpInvCostAdjustLine> lines = loadLines(adjust.getId());
        costAdjustmentService.reverseCostAdjust(adjust, lines);
        ormTemplate.flushSession();

        postingDispatcher.reverse(adjust);

        adjust = reload(id);
        adjust.setPosted(false);
        adjust.setPostedAt(null);
        adjust.setPostedBy(null);
        adjust.setDocStatus(ErpInvConstants.DOC_STATUS_CONFIRMED);
        adjustDao().updateEntity(adjust);
        return adjust;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpInvCostAdjust adjust) {
        String status = currentApproveStatus(adjust);
        if (!Objects.equals(status, APPROVE_STATUS_UNSUBMITTED) && !Objects.equals(status, APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(adjust, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpInvCostAdjust adjust) {
        String status = currentApproveStatus(adjust);
        if (!Objects.equals(status, APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(adjust, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpInvCostAdjust adjust) {
        String status = currentApproveStatus(adjust);
        if (!Objects.equals(status, APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(adjust, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpInvCostAdjust adjust) {
        String status = currentApproveStatus(adjust);
        if (!Objects.equals(status, APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(adjust, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpInvCostAdjust adjust) {
        String status = currentApproveStatus(adjust);
        if (!Objects.equals(status, APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(adjust, status, "APPROVED");
        }
    }

    protected void validateNotCancelled(ErpInvCostAdjust adjust) {
        String docStatus = adjust.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpInvConstants.DOC_STATUS_CANCELLED)) {
            throw illegalTransition(adjust, docStatus, "非已取消");
        }
    }

    public boolean isApprovalRequired() {
        return AppConfig.var(ErpInvConstants.CONFIG_COST_ADJUST_APPROVAL, true);
    }

    protected boolean isAlreadyApproved(ErpInvCostAdjust adjust) {
        return Objects.equals(adjust.getApproveStatus(), APPROVE_STATUS_APPROVED);
    }

    protected boolean isAlreadyRejected(ErpInvCostAdjust adjust) {
        return Objects.equals(adjust.getApproveStatus(), APPROVE_STATUS_REJECTED);
    }

    protected String currentApproveStatus(ErpInvCostAdjust adjust) {
        String status = adjust.getApproveStatus();
        return status != null ? status : APPROVE_STATUS_UNSUBMITTED;
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
