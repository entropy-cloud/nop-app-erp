package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.dao.entity.ErpFinExpenseClaimLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.AdvanceOffsetOrchestrator;
import app.erp.fin.service.posting.ExpenseClaimPostingDispatcher;
import app.erp.md.dao.entity.ErpMdEmployee;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

public class ErpFinExpenseClaimProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ExpenseClaimPostingDispatcher postingDispatcher;

    @Inject
    AdvanceOffsetOrchestrator offsetOrchestrator;

    public ErpFinExpenseClaim submitForApproval(String id, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(id, context);
        validateNotCancelled(claim, context);
        validateTransitionForSubmit(claim, context);
        validateForApproval(claim, context);
        doSubmit(claim, context);
        return claim;
    }

    public ErpFinExpenseClaim withdrawApproval(String id, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(id, context);
        validateNotCancelled(claim, context);
        validateTransitionForWithdraw(claim, context);
        doWithdrawSubmit(claim, context);
        return claim;
    }

    public ErpFinExpenseClaim approve(String id, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(id, context);
        if (isAlreadyApproved(claim)) {
            return claim;
        }
        validateNotCancelled(claim, context);
        validateTransitionForApprove(claim, context);
        validateForApproval(claim, context);
        runBudgetCheckHook(claim, context);
        return doApprove(id, claim, context);
    }

    public ErpFinExpenseClaim reject(String id, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(id, context);
        validateNotCancelled(claim, context);
        validateTransitionForReject(claim, context);
        doReject(claim, context);
        return claim;
    }

    public ErpFinExpenseClaim reverseApprove(String id, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(id, context);
        if (isAlreadyRejected(claim)) {
            return claim;
        }
        validateTransitionForReverseApprove(claim, context);
        return doReverseApprove(id, claim, context);
    }

    public ErpFinExpenseClaim cancel(Long claimId, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(claimId, context);
        validateTransitionForCancel(claim, context);
        return doCancel(claimId, claim, context);
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpFinExpenseClaim claim, IServiceContext context) {
        validateNotCancelled(claim, context);
        String status = currentApproveStatus(claim);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpFinConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(claim, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpFinExpenseClaim claim, IServiceContext context) {
        String status = currentApproveStatus(claim);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(claim, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpFinExpenseClaim claim, IServiceContext context) {
        String status = currentApproveStatus(claim);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(claim, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpFinExpenseClaim claim, IServiceContext context) {
        String status = currentApproveStatus(claim);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(claim, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpFinExpenseClaim claim, IServiceContext context) {
        String status = currentApproveStatus(claim);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(claim, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpFinExpenseClaim claim, IServiceContext context) {
        String docStatus = claim.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpFinConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(claim, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateForApproval(ErpFinExpenseClaim claim, IServiceContext context) {
        requireClaimantReady(claim, context);
        requireLinesValid(claim, context);
        requireAmountConsistency(claim, context);
        if (isReasonRequired() && isBlank(claim.getReason())) {
            throw new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_REASON_REQUIRED)
                    .param(ErpFinErrors.ARG_CLAIM_CODE, claim.getCode());
        }
    }

    protected void requireClaimantReady(ErpFinExpenseClaim claim, IServiceContext context) {
        ErpMdEmployee claimant = claim.getClaimantId() == null ? null
                : daoProvider.daoFor(ErpMdEmployee.class).getEntityById(claim.getClaimantId());
        if (claimant == null || claimant.getStatus() == null
                || !Objects.equals(claimant.getStatus(), ErpFinConstants.EMPLOYEE_STATUS_ACTIVE)) {
            throw new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_CLAIMANT_INACTIVE)
                    .param(ErpFinErrors.ARG_CLAIMANT_ID, claim.getClaimantId());
        }
        if (claimant.getPartnerId() == null) {
            throw new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_CLAIMANT_PARTNER_MISSING)
                    .param(ErpFinErrors.ARG_CLAIMANT_ID, claim.getClaimantId());
        }
    }

    protected void requireLinesValid(ErpFinExpenseClaim claim, IServiceContext context) {
        List<ErpFinExpenseClaimLine> lines = loadLines(claim.getId());
        if (lines.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_LINES_EMPTY)
                    .param(ErpFinErrors.ARG_CLAIM_CODE, claim.getCode());
        }
        boolean expenseTypeRequired = isExpenseTypeRequired();
        int lineNo = 0;
        for (ErpFinExpenseClaimLine line : lines) {
            lineNo++;
            if (expenseTypeRequired && line.getExpenseType() == null) {
                throw new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_EXPENSE_TYPE_REQUIRED)
                        .param(ErpFinErrors.ARG_CLAIM_CODE, claim.getCode())
                        .param(ErpFinErrors.ARG_LINE_NO, lineNo);
            }
        }
    }

    protected void requireAmountConsistency(ErpFinExpenseClaim claim, IServiceContext context) {
        BigDecimal lineTotal = BigDecimal.ZERO;
        for (ErpFinExpenseClaimLine line : loadLines(claim.getId())) {
            lineTotal = lineTotal.add(nz(line.getAmountWithTax()));
        }
        BigDecimal headTotal = nz(claim.getAmountWithTax());
        if (headTotal.compareTo(lineTotal) != 0) {
            throw new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_AMOUNT_MISMATCH)
                    .param(ErpFinErrors.ARG_CLAIM_CODE, claim.getCode())
                    .param(ErpFinErrors.ARG_AMOUNT_WITH_TAX, headTotal)
                    .param(ErpFinErrors.ARG_LINE_TOTAL, lineTotal);
        }
    }

    protected void runBudgetCheckHook(ErpFinExpenseClaim claim, IServiceContext context) {
        AppConfig.var(ErpFinConstants.CONFIG_EXPENSE_BUDGET_CHECK_ENABLED, Boolean.FALSE);
    }

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected void doSubmit(ErpFinExpenseClaim claim, IServiceContext context) {
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        claimDao().updateEntity(claim);
    }

    protected void doWithdrawSubmit(ErpFinExpenseClaim claim, IServiceContext context) {
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        claimDao().updateEntity(claim);
    }

    protected ErpFinExpenseClaim doApprove(String id, ErpFinExpenseClaim claim, IServiceContext context) {
        boolean posted = postingDispatcher.tryPost(claim);
        claim = reload(id);
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_APPROVED);
        claim.setApprovedBy(currentUserId());
        claim.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            claim.setPosted(true);
            claim.setPostedAt(CoreMetrics.currentDateTime());
            claim.setPostedBy(currentUserId());
            offsetOrchestrator.offset(claim);
            claim = reload(id);
        }
        claimDao().updateEntity(claim);
        return claim;
    }

    protected void doReject(ErpFinExpenseClaim claim, IServiceContext context) {
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_REJECTED);
        claimDao().updateEntity(claim);
    }

    protected ErpFinExpenseClaim doReverseApprove(String id, ErpFinExpenseClaim claim, IServiceContext context) {
        if (Boolean.TRUE.equals(claim.getPosted())) {
            offsetOrchestrator.reverseOffset(claim);
            postingDispatcher.reverse(claim);
            claim = reload(id);
            claim.setPosted(false);
            claim.setPostedAt(null);
            claim.setPostedBy(null);
            claim.setSettleAdvanceId(null);
        }
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_REJECTED);
        claim.setApprovedBy(null);
        claim.setApprovedAt(null);
        claimDao().updateEntity(claim);
        return claim;
    }

    protected ErpFinExpenseClaim doCancel(Long claimId, ErpFinExpenseClaim claim, IServiceContext context) {
        String approveStatus = currentApproveStatus(claim);
        if (Objects.equals(approveStatus, ErpFinConstants.APPROVE_STATUS_APPROVED)
                && Boolean.TRUE.equals(claim.getPosted())) {
            offsetOrchestrator.reverseOffset(claim);
            postingDispatcher.reverse(claim);
            claim = reload(String.valueOf(claimId));
            claim.setPosted(false);
            claim.setPostedAt(null);
            claim.setPostedBy(null);
            claim.setSettleAdvanceId(null);
        }
        claim.setDocStatus(ErpFinConstants.DOC_STATUS_CANCELLED);
        claimDao().updateEntity(claim);
        return claim;
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpFinExpenseClaim requireClaim(Long claimId, IServiceContext context) {
        return requireClaim(String.valueOf(claimId), context);
    }

    protected ErpFinExpenseClaim requireClaim(String id, IServiceContext context) {
        ErpFinExpenseClaim claim = claimDao().getEntityById(id);
        if (claim == null) {
            throw new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_NOT_FOUND)
                    .param(ErpFinErrors.ARG_CLAIM_CODE, id);
        }
        return claim;
    }

    protected void validateNotCancelled(ErpFinExpenseClaim claim, IServiceContext context) {
        validateTransitionForCancel(claim, context);
    }

    protected List<ErpFinExpenseClaimLine> loadLines(Long claimId) {
        IEntityDao<ErpFinExpenseClaimLine> dao = daoProvider.daoFor(ErpFinExpenseClaimLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("claimId", claimId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    protected ErpFinExpenseClaim reload(String id) {
        return claimDao().getEntityById(id);
    }

    protected boolean isAlreadyApproved(ErpFinExpenseClaim claim) {
        String status = claim.getApproveStatus();
        return status != null && Objects.equals(status, ErpFinConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isAlreadyRejected(ErpFinExpenseClaim claim) {
        String status = claim.getApproveStatus();
        return status != null && Objects.equals(status, ErpFinConstants.APPROVE_STATUS_REJECTED);
    }

    protected String currentApproveStatus(ErpFinExpenseClaim claim) {
        String status = claim.getApproveStatus();
        return status != null ? status : ErpFinConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    protected boolean isReasonRequired() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_EXPENSE_REASON_REQUIRED, Boolean.TRUE);
        return flag == null || flag;
    }

    protected boolean isExpenseTypeRequired() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_EXPENSE_APPROVAL_REQUIRED, Boolean.TRUE);
        return flag == null || flag;
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpFinExpenseClaim> claimDao() {
        return daoProvider.daoFor(ErpFinExpenseClaim.class);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) claimDao()).getOrmTemplate();
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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    protected NopException illegalTransition(ErpFinExpenseClaim claim, String current, String expected) {
        return new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_CLAIM_CODE, claim.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpFinExpenseClaim claim, String current, String expected) {
        return new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_CLAIM_CODE, claim.getCode())
                .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
