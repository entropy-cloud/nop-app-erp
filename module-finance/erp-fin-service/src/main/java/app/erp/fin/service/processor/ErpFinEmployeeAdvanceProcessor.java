package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.EmployeeAdvancePostingDispatcher;
import app.erp.fin.service.statemachine.ErpFinEmployeeAdvanceApprovalStateMachine;
import app.erp.fin.service.statemachine.ErpFinEmployeeAdvanceDocumentStateMachine;
import app.erp.common.service.SoDGuard;
import app.erp.md.dao.entity.ErpMdEmployee;
import io.nop.api.core.auth.IUserContext;
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

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpFinEmployeeAdvance）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。

public class ErpFinEmployeeAdvanceProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    EmployeeAdvancePostingDispatcher postingDispatcher;

    @Inject
    ErpFinEmployeeAdvanceSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpFinEmployeeAdvanceApproveProcessor approveProcessor;

    @Inject
    ErpFinEmployeeAdvanceRejectProcessor rejectProcessor;

    @Inject
    ErpFinEmployeeAdvanceReverseApproveProcessor reverseApproveProcessor;

    @Inject
    ErpFinEmployeeAdvanceWithdrawApprovalProcessor withdrawApprovalProcessor;

    @Inject
    ErpFinEmployeeAdvanceCancelProcessor cancelProcessor;

    @Inject
    ErpFinEmployeeAdvanceApprovalStateMachine approvalStateMachine;

    @Inject
    ErpFinEmployeeAdvanceDocumentStateMachine documentStateMachine;

    public ErpFinEmployeeAdvance submitForApproval(String id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpFinEmployeeAdvance withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpFinEmployeeAdvance approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    public ErpFinEmployeeAdvance reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    public ErpFinEmployeeAdvance reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    public ErpFinEmployeeAdvance cancel(String advanceId, IServiceContext context) {
        return cancelProcessor.cancel(advanceId, context);
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    /**
     * 固定来源态/目标态矩阵守卫委托 {@link ErpFinEmployeeAdvanceApprovalStateMachine}（approveStatus 轴 Bean，
     * plan 2026-08-13-1146-3 M4.7；契约 entity-state-machine-bean.md §4/§7）。Bean 自 plan 2026-09-07-2200-1 起
     * 直抛领域码 {@code ERR_EMPLOYEE_ADVANCE_ILLEGAL_STATUS_TRANSITION}，本处同码补参 advanceCode（参数对外不变）。
     */
    protected void validateTransitionForSubmit(ErpFinEmployeeAdvance advance, IServiceContext context) {
        String status = currentApproveStatus(advance);
        try {
            approvalStateMachine.assertCanSubmit(status);
        } catch (NopException e) {
            throw e.param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode());
        }
    }

    protected void validateTransitionForWithdraw(ErpFinEmployeeAdvance advance, IServiceContext context) {
        String status = currentApproveStatus(advance);
        try {
            approvalStateMachine.assertCanWithdraw(status);
        } catch (NopException e) {
            throw e.param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode());
        }
    }

    protected void validateTransitionForApprove(ErpFinEmployeeAdvance advance, IServiceContext context) {
        String status = currentApproveStatus(advance);
        try {
            approvalStateMachine.assertCanApprove(status);
        } catch (NopException e) {
            throw e.param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode());
        }
    }

    protected void validateTransitionForReject(ErpFinEmployeeAdvance advance, IServiceContext context) {
        String status = currentApproveStatus(advance);
        try {
            approvalStateMachine.assertCanReject(status);
        } catch (NopException e) {
            throw e.param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode());
        }
    }

    protected void validateTransitionForReverseApprove(ErpFinEmployeeAdvance advance, IServiceContext context) {
        String status = currentApproveStatus(advance);
        try {
            approvalStateMachine.assertCanReverseApprove(status);
        } catch (NopException e) {
            throw e.param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode());
        }
    }

    /**
     * docStatus 轴矩阵守卫委托 {@link ErpFinEmployeeAdvanceDocumentStateMachine}（plan 2026-08-13-1146-3 M4.6；
     * 契约 §7）。Bean 直抛领域码 {@code ERR_EMPLOYEE_ADVANCE_ILLEGAL_DOC_STATUS_TRANSITION}，本处同码补参 advanceCode
     * （plan 2026-09-07-2200-1）。已 CANCELLED 拒绝；
     * dict 残余值 SUBMITTED/APPROVED/REJECTED 不在 Bean 矩阵（intentional reserved，生命周期推进由 approveStatus 承载）。
     */
    protected void validateTransitionForCancel(ErpFinEmployeeAdvance advance, IServiceContext context) {
        try {
            documentStateMachine.assertCanCancel(advance.getDocStatus());
        } catch (NopException e) {
            throw e.param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode());
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForApproval(ErpFinEmployeeAdvance advance, IServiceContext context) {
        requireEmployeeReady(advance, context);
        requireAmountPositive(advance, context);
    }

    protected void requireEmployeeReady(ErpFinEmployeeAdvance advance, IServiceContext context) {
        ErpMdEmployee employee = advance.getEmployee();
        if (employee == null || employee.getStatus() == null
                || !Objects.equals(employee.getStatus(), ErpFinConstants.EMPLOYEE_STATUS_ACTIVE)) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_EMPLOYEE_INACTIVE)
                    .param(ErpFinErrors.ARG_EMPLOYEE_ID, advance.getEmployeeId());
        }
        if (employee.getPartnerId() == null) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_EMPLOYEE_PARTNER_MISSING)
                    .param(ErpFinErrors.ARG_EMPLOYEE_ID, advance.getEmployeeId());
        }
    }

    protected void requireAmountPositive(ErpFinEmployeeAdvance advance, IServiceContext context) {
        BigDecimal amountFunctional = advance.getAmountFunctional();
        if (amountFunctional == null || amountFunctional.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_AMOUNT_INVALID)
                    .param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode());
        }
    }

    protected void deriveAmounts(ErpFinEmployeeAdvance advance, IServiceContext context) {
        BigDecimal amount = nz(advance.getAmountFunctional());
        BigDecimal settled = nz(advance.getSettledAmount());
        advance.setSettledAmount(settled);
        advance.setOutstandingAmount(amount.subtract(settled));
    }

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected void doSubmit(ErpFinEmployeeAdvance advance, IServiceContext context) {
        advance.setApproveStatus(approvalStateMachine.submitTargetStatus());
        advanceDao().updateEntity(advance);
    }

    protected void doWithdrawSubmit(ErpFinEmployeeAdvance advance, IServiceContext context) {
        advance.setApproveStatus(approvalStateMachine.withdrawTargetStatus());
        advanceDao().updateEntity(advance);
    }

    protected ErpFinEmployeeAdvance doApprove(String id, ErpFinEmployeeAdvance advance, IServiceContext context) {
        SoDGuard.assertApproverNotCreator(advance.getCreatedBy(), currentUserId(), ErpFinErrors.ERR_FIN_APPROVER_IS_CREATOR);
        boolean posted = postingDispatcher.tryPost(advance);
        advance = reload(id);
        advance.setApproveStatus(approvalStateMachine.approveTargetStatus());
        advance.setApprovedBy(currentUserId());
        advance.setApprovedAt(CoreMetrics.currentTimestamp());
        if (posted) {
            markPosted(advance);
        }
        advanceDao().updateEntity(advance);
        return advance;
    }

    protected void doReject(ErpFinEmployeeAdvance advance, IServiceContext context) {
        advance.setApproveStatus(approvalStateMachine.rejectTargetStatus());
        advanceDao().updateEntity(advance);
    }

    protected ErpFinEmployeeAdvance doReverseApprove(String id, ErpFinEmployeeAdvance advance, IServiceContext context) {
        if (Boolean.TRUE.equals(advance.getPosted())) {
            postingDispatcher.reverse(advance);
            advance = reload(id);
            clearPosted(advance);
        }
        advance.setApproveStatus(approvalStateMachine.reverseApproveTargetStatus());
        advance.setApprovedBy(null);
        advance.setApprovedAt(null);
        advanceDao().updateEntity(advance);
        return advance;
    }

    protected ErpFinEmployeeAdvance doCancel(String advanceId, ErpFinEmployeeAdvance advance, IServiceContext context) {
        String approveStatus = currentApproveStatus(advance);
        if (Objects.equals(approveStatus, ErpFinConstants.APPROVE_STATUS_APPROVED)
                && Boolean.TRUE.equals(advance.getPosted())) {
            postingDispatcher.reverse(advance);
            advance = reload(advanceId);
            clearPosted(advance);
        }
        advance.setDocStatus(documentStateMachine.cancelTargetStatus());
        advanceDao().updateEntity(advance);
        return advance;
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpFinEmployeeAdvance requireAdvance(String id, IServiceContext context) {
        ErpFinEmployeeAdvance advance = advanceDao().getEntityById(id);
        if (advance == null) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_NOT_FOUND)
                    .param(ErpFinErrors.ARG_ADVANCE_CODE, id);
        }
        return advance;
    }

    protected void validateNotCancelled(ErpFinEmployeeAdvance advance, IServiceContext context) {
        validateTransitionForCancel(advance, context);
    }

    protected void markPosted(ErpFinEmployeeAdvance advance) {
        advance.setPosted(true);
        advance.setPostedAt(CoreMetrics.currentTimestamp());
        advance.setPostedBy(currentUserId());
    }

    protected void clearPosted(ErpFinEmployeeAdvance advance) {
        advance.setPosted(false);
        advance.setPostedAt(null);
        advance.setPostedBy(null);
    }

    protected ErpFinEmployeeAdvance reload(String id) {
        return advanceDao().getEntityById(id);
    }

    protected String currentApproveStatus(ErpFinEmployeeAdvance advance) {
        String status = advance.getApproveStatus();
        return status != null ? status : ErpFinConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpFinEmployeeAdvance> advanceDao() {
        return daoProvider.daoFor(ErpFinEmployeeAdvance.class);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) advanceDao()).getOrmTemplate();
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
}
