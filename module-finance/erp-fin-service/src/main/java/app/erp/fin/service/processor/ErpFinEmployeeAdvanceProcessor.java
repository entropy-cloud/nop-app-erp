package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.EmployeeAdvancePostingDispatcher;
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

public class ErpFinEmployeeAdvanceProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    EmployeeAdvancePostingDispatcher postingDispatcher;

    public ErpFinEmployeeAdvance submitForApproval(String id, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(id, context);
        validateNotCancelled(advance, context);
        validateTransitionForSubmit(advance, context);
        validateBusinessRulesForApproval(advance, context);
        deriveAmounts(advance, context);
        doSubmit(advance, context);
        return advance;
    }

    public ErpFinEmployeeAdvance withdrawApproval(String id, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(id, context);
        validateNotCancelled(advance, context);
        validateTransitionForWithdraw(advance, context);
        doWithdrawSubmit(advance, context);
        return advance;
    }

    public ErpFinEmployeeAdvance approve(String id, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(id, context);
        if (isAlreadyApproved(advance)) {
            return advance;
        }
        validateNotCancelled(advance, context);
        validateTransitionForApprove(advance, context);
        validateBusinessRulesForApproval(advance, context);
        deriveAmounts(advance, context);
        return doApprove(id, advance, context);
    }

    public ErpFinEmployeeAdvance reject(String id, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(id, context);
        validateNotCancelled(advance, context);
        validateTransitionForReject(advance, context);
        doReject(advance, context);
        return advance;
    }

    public ErpFinEmployeeAdvance reverseApprove(String id, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(id, context);
        if (isAlreadyRejected(advance)) {
            return advance;
        }
        validateTransitionForReverseApprove(advance, context);
        return doReverseApprove(id, advance, context);
    }

    public ErpFinEmployeeAdvance cancel(Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        validateTransitionForCancel(advance, context);
        return doCancel(advanceId, advance, context);
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpFinEmployeeAdvance advance, IServiceContext context) {
        String status = currentApproveStatus(advance);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(advance, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpFinEmployeeAdvance advance, IServiceContext context) {
        String status = currentApproveStatus(advance);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(advance, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpFinEmployeeAdvance advance, IServiceContext context) {
        String status = currentApproveStatus(advance);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(advance, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpFinEmployeeAdvance advance, IServiceContext context) {
        String status = currentApproveStatus(advance);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(advance, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpFinEmployeeAdvance advance, IServiceContext context) {
        String status = currentApproveStatus(advance);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(advance, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpFinEmployeeAdvance advance, IServiceContext context) {
        String docStatus = advance.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpFinConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(advance, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForApproval(ErpFinEmployeeAdvance advance, IServiceContext context) {
        requireEmployeeReady(advance, context);
        requireAmountPositive(advance, context);
    }

    protected void requireEmployeeReady(ErpFinEmployeeAdvance advance, IServiceContext context) {
        ErpMdEmployee employee = advance.getEmployeeId() == null ? null
                : daoProvider.daoFor(ErpMdEmployee.class).getEntityById(advance.getEmployeeId());
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
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        advanceDao().updateEntity(advance);
    }

    protected void doWithdrawSubmit(ErpFinEmployeeAdvance advance, IServiceContext context) {
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        advanceDao().updateEntity(advance);
    }

    protected ErpFinEmployeeAdvance doApprove(String id, ErpFinEmployeeAdvance advance, IServiceContext context) {
        boolean posted = postingDispatcher.tryPost(advance);
        advance = reload(id);
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_APPROVED);
        advance.setApprovedBy(currentUserId());
        advance.setApprovedAt(CoreMetrics.currentTimestamp());
        if (posted) {
            markPosted(advance);
        }
        advanceDao().updateEntity(advance);
        return advance;
    }

    protected void doReject(ErpFinEmployeeAdvance advance, IServiceContext context) {
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_REJECTED);
        advanceDao().updateEntity(advance);
    }

    protected ErpFinEmployeeAdvance doReverseApprove(String id, ErpFinEmployeeAdvance advance, IServiceContext context) {
        if (Boolean.TRUE.equals(advance.getPosted())) {
            postingDispatcher.reverse(advance);
            advance = reload(id);
            clearPosted(advance);
        }
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_REJECTED);
        advance.setApprovedBy(null);
        advance.setApprovedAt(null);
        advanceDao().updateEntity(advance);
        return advance;
    }

    protected ErpFinEmployeeAdvance doCancel(Long advanceId, ErpFinEmployeeAdvance advance, IServiceContext context) {
        String approveStatus = currentApproveStatus(advance);
        if (Objects.equals(approveStatus, ErpFinConstants.APPROVE_STATUS_APPROVED)
                && Boolean.TRUE.equals(advance.getPosted())) {
            postingDispatcher.reverse(advance);
            advance = reload(String.valueOf(advanceId));
            clearPosted(advance);
        }
        advance.setDocStatus(ErpFinConstants.DOC_STATUS_CANCELLED);
        advanceDao().updateEntity(advance);
        return advance;
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpFinEmployeeAdvance requireAdvance(Long advanceId, IServiceContext context) {
        return requireAdvance(String.valueOf(advanceId), context);
    }

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

    protected boolean isAlreadyApproved(ErpFinEmployeeAdvance advance) {
        String status = advance.getApproveStatus();
        return status != null && Objects.equals(status, ErpFinConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isAlreadyRejected(ErpFinEmployeeAdvance advance) {
        String status = advance.getApproveStatus();
        return status != null && Objects.equals(status, ErpFinConstants.APPROVE_STATUS_REJECTED);
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

    protected NopException illegalTransition(ErpFinEmployeeAdvance advance, String current, String expected) {
        return new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpFinEmployeeAdvance advance, String current, String expected) {
        return new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode())
                .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
