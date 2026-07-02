
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinExpenseClaimBiz;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.dao.entity.ErpFinExpenseClaimLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.AdvanceOffsetOrchestrator;
import app.erp.fin.service.posting.ExpenseClaimPostingDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

/**
 * 费用报销单 BizModel（{@code expense-claim.md}）。CRUD 之上实现三轴审批状态机
 * （UNSUBMITTED↔SUBMITTED→APPROVED/REJECTED；reverseApprove APPROVED→REJECTED；cancel 非终态→CANCELLED，
 * APPROVED 须先红冲），对齐 finance 域审批形状。
 *
 * <p>审核前置校验：报销人启用 + {@code claimant.partnerId} 非空（员工须有内部往来单位记录，否则辅助账
 * mandatory FK 违约——见 plan Task Route Decision）、行非空、价税合计=Σ行、{@code reason}/{@code expenseType}
 * 必填（按配置）、paymentMode 合法；预算钩子点（{@code erp-fin.expense-budget-check-enabled}，默认 false，
 * 预算模块未落地，仅留注入位不实现校验）。
 *
 * <p>审核通过触发 EXPENSE_CLAIM 业财过账（凭证 + 员工应付辅助账），posted 标志在过账成功后置位；
 * 过账成功后按 {@code erp-fin.advance-auto-offset-on-expense} 抵扣同员工未还借款（{@link AdvanceOffsetOrchestrator}）。
 * 反审核/作废先反向抵扣再红冲 EXPENSE_CLAIM 凭证（对齐 0300-2 合约）。
 *
 * <p>{@code @BizMutation} 自动包装事务（不叠加 {@code @Transactional}），每迁移校验前置态，违例抛
 * {@link NopException}+{@link ErpFinErrors} 作用域码。
 */
@BizModel("ErpFinExpenseClaim")
public class ErpFinExpenseClaimBizModel extends CrudBizModel<ErpFinExpenseClaim> implements IErpFinExpenseClaimBiz {

    @Inject
    ExpenseClaimPostingDispatcher postingDispatcher;
    @Inject
    AdvanceOffsetOrchestrator offsetOrchestrator;

    public ErpFinExpenseClaimBizModel() {
        setEntityName(ErpFinExpenseClaim.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim submit(@Name("claimId") Long claimId, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(claimId, context);
        requireNotCancelled(claim);
        Integer status = currentApproveStatus(claim);
        if (status != ErpFinConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpFinConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(claim, status, "UNSUBMITTED 或 REJECTED");
        }
        validateForApproval(claim);
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(claim);
        return claim;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim withdrawSubmit(@Name("claimId") Long claimId, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(claimId, context);
        requireNotCancelled(claim);
        Integer status = currentApproveStatus(claim);
        if (status != ErpFinConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(claim, status, "SUBMITTED");
        }
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(claim);
        return claim;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim approve(@Name("claimId") Long claimId, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(claimId, context);
        Integer status = currentApproveStatus(claim);
        if (status == ErpFinConstants.APPROVE_STATUS_APPROVED) {
            return claim;
        }
        requireNotCancelled(claim);
        if (status != ErpFinConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(claim, status, "SUBMITTED");
        }
        validateForApproval(claim);
        runBudgetCheckHook(claim);

        boolean posted = postingDispatcher.tryPost(claim);
        // 跨域 post 扰动会话脏跟踪，重新加载后置标志并显式持久化（对齐 ErpSalReceiptBizModel）
        claim = requireEntity(String.valueOf(claimId), null, context);
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_APPROVED);
        claim.setApprovedBy(currentUserId());
        claim.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            claim.setPosted(true);
            claim.setPostedAt(CoreMetrics.currentDateTime());
            claim.setPostedBy(currentUserId());
            // 过账生成辅助账后、同事务抵扣同员工未还借款
            offsetOrchestrator.offset(claim);
            claim = requireEntity(String.valueOf(claimId), null, context);
        }
        dao().updateEntity(claim);
        return claim;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim reject(@Name("claimId") Long claimId, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(claimId, context);
        requireNotCancelled(claim);
        Integer status = currentApproveStatus(claim);
        if (status != ErpFinConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(claim, status, "SUBMITTED");
        }
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(claim);
        return claim;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim reverseApprove(@Name("claimId") Long claimId, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(claimId, context);
        Integer status = currentApproveStatus(claim);
        if (status == ErpFinConstants.APPROVE_STATUS_REJECTED) {
            return claim;
        }
        if (status != ErpFinConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(claim, status, "APPROVED");
        }
        if (Boolean.TRUE.equals(claim.getPosted())) {
            // 先反向抵扣（恢复借款应收 + 红冲 SETTLE 凭证），再红冲 EXPENSE_CLAIM（取消报销应付辅助账）
            offsetOrchestrator.reverseOffset(claim);
            postingDispatcher.reverse(claim);
            claim = requireEntity(String.valueOf(claimId), null, context);
            claim.setPosted(false);
            claim.setPostedAt(null);
            claim.setPostedBy(null);
            claim.setSettleAdvanceId(null);
        }
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(claim);
        return claim;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim cancel(@Name("claimId") Long claimId, IServiceContext context) {
        ErpFinExpenseClaim claim = requireClaim(claimId, context);
        Integer docStatus = claim.getDocStatus();
        if (docStatus != null && docStatus == ErpFinConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(claim, docStatus, "非已作废");
        }
        Integer approveStatus = currentApproveStatus(claim);
        if (approveStatus == ErpFinConstants.APPROVE_STATUS_APPROVED
                && Boolean.TRUE.equals(claim.getPosted())) {
            offsetOrchestrator.reverseOffset(claim);
            postingDispatcher.reverse(claim);
            claim = requireEntity(String.valueOf(claimId), null, context);
            claim.setPosted(false);
            claim.setPostedAt(null);
            claim.setPostedBy(null);
            claim.setSettleAdvanceId(null);
        }
        claim.setDocStatus(ErpFinConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(claim);
        return claim;
    }

    // ---------- validation ----------

    private void validateForApproval(ErpFinExpenseClaim claim) {
        requireClaimantReady(claim);
        requireLinesValid(claim);
        requireAmountConsistency(claim);
        if (isReasonRequired() && isBlank(claim.getReason())) {
            throw new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_REASON_REQUIRED)
                    .param(ErpFinErrors.ARG_CLAIM_CODE, claim.getCode());
        }
    }

    private void requireClaimantReady(ErpFinExpenseClaim claim) {
        // 经 daoProvider 直接加载员工（对齐 ErpFinReconciliationBizModel.loadItem 范式，避免跨会话关系懒加载）。
        app.erp.md.dao.entity.ErpMdEmployee claimant = claim.getClaimantId() == null ? null
                : daoProvider().daoFor(app.erp.md.dao.entity.ErpMdEmployee.class)
                        .getEntityById(claim.getClaimantId());
        if (claimant == null || claimant.getStatus() == null
                || claimant.getStatus() != ErpFinConstants.EMPLOYEE_STATUS_ACTIVE) {
            throw new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_CLAIMANT_INACTIVE)
                    .param(ErpFinErrors.ARG_CLAIMANT_ID, claim.getClaimantId());
        }
        if (claimant.getPartnerId() == null) {
            throw new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_CLAIMANT_PARTNER_MISSING)
                    .param(ErpFinErrors.ARG_CLAIMANT_ID, claim.getClaimantId());
        }
    }

    private void requireLinesValid(ErpFinExpenseClaim claim) {
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

    private void requireAmountConsistency(ErpFinExpenseClaim claim) {
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

    /**
     * 预算控制钩子点。{@code erp-fin.expense-budget-check-enabled} 默认 false，预算模块未落地，仅留注入位不实现校验。
     */
    private void runBudgetCheckHook(ErpFinExpenseClaim claim) {
        Boolean enabled = AppConfig.var(ErpFinConstants.CONFIG_EXPENSE_BUDGET_CHECK_ENABLED, Boolean.FALSE);
        // 预算模块落地前为空操作（配置门控预留，见 expense-claim.md 实现偏离补注 §2）
    }

    // ---------- helpers ----------

    private ErpFinExpenseClaim requireClaim(Long claimId, IServiceContext context) {
        return requireEntity(String.valueOf(claimId), null, context);
    }

    private List<ErpFinExpenseClaimLine> loadLines(Long claimId) {
        // D2 边界场景：同聚合子表加载，父实体已由 requireEntity 经数据权限/Meta 管道授权，子行无独立权限规则。
        IEntityDao<ErpFinExpenseClaimLine> dao = daoProvider().daoFor(ErpFinExpenseClaimLine.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("claimId", claimId));
        return new java.util.ArrayList<>(dao.findAllByQuery(q));
    }

    private void requireNotCancelled(ErpFinExpenseClaim claim) {
        Integer docStatus = claim.getDocStatus();
        if (docStatus != null && docStatus == ErpFinConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(claim, docStatus, "非已作废");
        }
    }

    private Integer currentApproveStatus(ErpFinExpenseClaim claim) {
        Integer status = claim.getApproveStatus();
        return status != null ? status : ErpFinConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    private boolean isReasonRequired() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_EXPENSE_REASON_REQUIRED, Boolean.TRUE);
        return flag == null || flag;
    }

    private boolean isExpenseTypeRequired() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_EXPENSE_APPROVAL_REQUIRED, Boolean.TRUE);
        return flag == null || flag;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private NopException illegalTransition(ErpFinExpenseClaim claim, Integer current, String expected) {
        return new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_CLAIM_CODE, claim.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpFinExpenseClaim claim, Integer current, String expected) {
        return new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_CLAIM_CODE, claim.getCode())
                .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
