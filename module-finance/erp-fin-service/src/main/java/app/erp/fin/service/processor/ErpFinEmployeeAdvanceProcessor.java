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

import java.math.BigDecimal;

/**
 * 员工借款单审批状态机编排 Processor（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 * Facade {@code ErpFinEmployeeAdvanceBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>配置余地：每个 {@code public} 动作方法只编排步骤顺序（加载→校验迁移→校验业务规则→执行），
 * 各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。客户/行业覆盖单步实现时，
 * 写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation}+{@code @SingleSession} 事务，本类不带 {@code @Transactional}。
 */
public class ErpFinEmployeeAdvanceProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    EmployeeAdvancePostingDispatcher postingDispatcher;

    public ErpFinEmployeeAdvance submit(Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        validateNotCancelled(advance, context);
        validateTransitionForSubmit(advance, context);
        validateBusinessRulesForApproval(advance, context);
        deriveAmounts(advance, context);
        doSubmit(advance, context);
        return advance;
    }

    public ErpFinEmployeeAdvance withdrawSubmit(Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        validateNotCancelled(advance, context);
        validateTransitionForWithdraw(advance, context);
        doWithdrawSubmit(advance, context);
        return advance;
    }

    public ErpFinEmployeeAdvance approve(Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        // 幂等：已审核再次审核为空操作。
        if (isAlreadyApproved(advance)) {
            return advance;
        }
        validateNotCancelled(advance, context);
        validateTransitionForApprove(advance, context);
        validateBusinessRulesForApproval(advance, context);
        deriveAmounts(advance, context);
        return doApprove(advanceId, advance, context);
    }

    public ErpFinEmployeeAdvance reject(Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        validateNotCancelled(advance, context);
        validateTransitionForReject(advance, context);
        doReject(advance, context);
        return advance;
    }

    public ErpFinEmployeeAdvance reverseApprove(Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        // 幂等：已 REJECTED 直接返回。
        if (isAlreadyRejected(advance)) {
            return advance;
        }
        validateTransitionForReverseApprove(advance, context);
        return doReverseApprove(advanceId, advance, context);
    }

    public ErpFinEmployeeAdvance cancel(Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        validateTransitionForCancel(advance, context);
        return doCancel(advanceId, advance, context);
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpFinEmployeeAdvance advance, IServiceContext context) {
        Integer status = currentApproveStatus(advance);
        if (status != ErpFinConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpFinConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(advance, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpFinEmployeeAdvance advance, IServiceContext context) {
        Integer status = currentApproveStatus(advance);
        if (status != ErpFinConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(advance, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpFinEmployeeAdvance advance, IServiceContext context) {
        Integer status = currentApproveStatus(advance);
        if (status != ErpFinConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(advance, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpFinEmployeeAdvance advance, IServiceContext context) {
        Integer status = currentApproveStatus(advance);
        if (status != ErpFinConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(advance, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpFinEmployeeAdvance advance, IServiceContext context) {
        Integer status = currentApproveStatus(advance);
        if (status != ErpFinConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(advance, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpFinEmployeeAdvance advance, IServiceContext context) {
        Integer docStatus = advance.getDocStatus();
        if (docStatus != null && docStatus == ErpFinConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(advance, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForApproval(ErpFinEmployeeAdvance advance, IServiceContext context) {
        requireEmployeeReady(advance, context);
        requireAmountPositive(advance, context);
    }

    protected void requireEmployeeReady(ErpFinEmployeeAdvance advance, IServiceContext context) {
        // 经 daoProvider 直接加载员工（避免跨会话关系懒加载）。
        ErpMdEmployee employee = advance.getEmployeeId() == null ? null
                : daoProvider.daoFor(ErpMdEmployee.class).getEntityById(advance.getEmployeeId());
        if (employee == null || employee.getStatus() == null
                || employee.getStatus() != ErpFinConstants.EMPLOYEE_STATUS_ACTIVE) {
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

    /** 派生 settledAmount/outstandingAmount：未还 = 本位币金额 - 已清算。 */
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

    protected ErpFinEmployeeAdvance doApprove(Long advanceId, ErpFinEmployeeAdvance advance, IServiceContext context) {
        boolean posted = postingDispatcher.tryPost(advance);
        // 跨域 post 扰动会话脏跟踪，重新加载后置标志并显式持久化。
        advance = reload(advanceId);
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_APPROVED);
        advance.setApprovedBy(currentUserId());
        advance.setApprovedAt(CoreMetrics.currentDateTime());
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

    protected ErpFinEmployeeAdvance doReverseApprove(Long advanceId, ErpFinEmployeeAdvance advance, IServiceContext context) {
        if (Boolean.TRUE.equals(advance.getPosted())) {
            postingDispatcher.reverse(advance);
            advance = reload(advanceId);
            clearPosted(advance);
        }
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_REJECTED);
        advanceDao().updateEntity(advance);
        return advance;
    }

    protected ErpFinEmployeeAdvance doCancel(Long advanceId, ErpFinEmployeeAdvance advance, IServiceContext context) {
        Integer approveStatus = currentApproveStatus(advance);
        if (approveStatus == ErpFinConstants.APPROVE_STATUS_APPROVED
                && Boolean.TRUE.equals(advance.getPosted())) {
            postingDispatcher.reverse(advance);
            advance = reload(advanceId);
            clearPosted(advance);
        }
        advance.setDocStatus(ErpFinConstants.DOC_STATUS_CANCELLED);
        advanceDao().updateEntity(advance);
        return advance;
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpFinEmployeeAdvance requireAdvance(Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = advanceDao().getEntityById(advanceId);
        if (advance == null) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_NOT_FOUND)
                    .param(ErpFinErrors.ARG_ADVANCE_CODE, String.valueOf(advanceId));
        }
        return advance;
    }

    protected void validateNotCancelled(ErpFinEmployeeAdvance advance, IServiceContext context) {
        validateTransitionForCancel(advance, context);
    }

    private void markPosted(ErpFinEmployeeAdvance advance) {
        advance.setPosted(true);
        advance.setPostedAt(CoreMetrics.currentDateTime());
        advance.setPostedBy(currentUserId());
    }

    private void clearPosted(ErpFinEmployeeAdvance advance) {
        advance.setPosted(false);
        advance.setPostedAt(null);
        advance.setPostedBy(null);
    }

    protected ErpFinEmployeeAdvance reload(Long advanceId) {
        return advanceDao().getEntityById(advanceId);
    }

    protected boolean isAlreadyApproved(ErpFinEmployeeAdvance advance) {
        Integer status = advance.getApproveStatus();
        return status != null && status == ErpFinConstants.APPROVE_STATUS_APPROVED;
    }

    protected boolean isAlreadyRejected(ErpFinEmployeeAdvance advance) {
        Integer status = advance.getApproveStatus();
        return status != null && status == ErpFinConstants.APPROVE_STATUS_REJECTED;
    }

    protected Integer currentApproveStatus(ErpFinEmployeeAdvance advance) {
        Integer status = advance.getApproveStatus();
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

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    protected NopException illegalTransition(ErpFinEmployeeAdvance advance, Integer current, String expected) {
        return new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpFinEmployeeAdvance advance, Integer current, String expected) {
        return new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode())
                .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
