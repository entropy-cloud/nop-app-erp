
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinEmployeeAdvanceBiz;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;

import app.erp.fin.service.posting.EmployeeAdvancePostingDispatcher;

/**
 * 员工借款单 BizModel（{@code expense-claim.md}）。CRUD 之上实现三轴审批状态机
 * （UNSUBMITTED↔SUBMITTED→APPROVED/REJECTED；reverseApprove APPROVED→REJECTED；cancel 非终态→CANCELLED，
 * APPROVED 须先红冲），对齐 finance 域审批形状。
 *
 * <p>审核前置校验：员工启用 + {@code employee.partnerId} 非空（员工须有内部往来单位记录，否则辅助账
 * mandatory FK 违约——见 plan Task Route Decision）、金额>0、advanceType 合法。{@code settledAmount}/
 * {@code outstandingAmount} 派生（=amount/outstanding）。
 *
 * <p>审核通过触发 EMPLOYEE_ADVANCE 业财过账（借其他应收款-员工预支 / 贷银行存款），posted 标志在过账
 * 成功后置位（失败吞异常保持 APPROVED+posted=false，对齐 0300-2 合约）。反审核/作废对已过账单据红字冲销。
 *
 * <p>{@code @BizMutation} 自动包装事务（不叠加 {@code @Transactional}），每迁移校验前置态，违例抛
 * {@link NopException}+{@link ErpFinErrors} 作用域码。
 */
@BizModel("ErpFinEmployeeAdvance")
public class ErpFinEmployeeAdvanceBizModel extends CrudBizModel<ErpFinEmployeeAdvance> implements IErpFinEmployeeAdvanceBiz {

    @Inject
    EmployeeAdvancePostingDispatcher postingDispatcher;

    public ErpFinEmployeeAdvanceBizModel() {
        setEntityName(ErpFinEmployeeAdvance.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance submit(@Name("advanceId") Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        requireNotCancelled(advance);
        Integer status = currentApproveStatus(advance);
        if (status != ErpFinConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpFinConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(advance, status, "UNSUBMITTED 或 REJECTED");
        }
        requireEmployeeReady(advance);
        requireAmountPositive(advance);
        deriveAmounts(advance);
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(advance);
        return advance;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance withdrawSubmit(@Name("advanceId") Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        requireNotCancelled(advance);
        Integer status = currentApproveStatus(advance);
        if (status != ErpFinConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(advance, status, "SUBMITTED");
        }
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(advance);
        return advance;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance approve(@Name("advanceId") Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        Integer status = currentApproveStatus(advance);
        if (status == ErpFinConstants.APPROVE_STATUS_APPROVED) {
            return advance;
        }
        requireNotCancelled(advance);
        if (status != ErpFinConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(advance, status, "SUBMITTED");
        }
        requireEmployeeReady(advance);
        requireAmountPositive(advance);
        deriveAmounts(advance);

        boolean posted = postingDispatcher.tryPost(advance);

        advance = requireEntity(String.valueOf(advanceId), null, context);
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_APPROVED);
        advance.setApprovedBy(currentUserId());
        advance.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            advance.setPosted(true);
            advance.setPostedAt(CoreMetrics.currentDateTime());
            advance.setPostedBy(currentUserId());
        }
        dao().updateEntity(advance);
        return advance;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance reject(@Name("advanceId") Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        requireNotCancelled(advance);
        Integer status = currentApproveStatus(advance);
        if (status != ErpFinConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(advance, status, "SUBMITTED");
        }
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(advance);
        return advance;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance reverseApprove(@Name("advanceId") Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        Integer status = currentApproveStatus(advance);
        if (status == ErpFinConstants.APPROVE_STATUS_REJECTED) {
            return advance;
        }
        if (status != ErpFinConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(advance, status, "APPROVED");
        }
        if (Boolean.TRUE.equals(advance.getPosted())) {
            postingDispatcher.reverse(advance);
            advance = requireEntity(String.valueOf(advanceId), null, context);
            advance.setPosted(false);
            advance.setPostedAt(null);
            advance.setPostedBy(null);
        }
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(advance);
        return advance;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance cancel(@Name("advanceId") Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);
        Integer docStatus = advance.getDocStatus();
        if (docStatus != null && docStatus == ErpFinConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(advance, docStatus, "非已作废");
        }
        Integer approveStatus = currentApproveStatus(advance);
        if (approveStatus == ErpFinConstants.APPROVE_STATUS_APPROVED
                && Boolean.TRUE.equals(advance.getPosted())) {
            postingDispatcher.reverse(advance);
            advance = requireEntity(String.valueOf(advanceId), null, context);
            advance.setPosted(false);
            advance.setPostedAt(null);
            advance.setPostedBy(null);
        }
        advance.setDocStatus(ErpFinConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(advance);
        return advance;
    }

    // ---------- validation helpers ----------

    private ErpFinEmployeeAdvance requireAdvance(Long advanceId, IServiceContext context) {
        return requireEntity(String.valueOf(advanceId), null, context);
    }

    private void requireNotCancelled(ErpFinEmployeeAdvance advance) {
        Integer docStatus = advance.getDocStatus();
        if (docStatus != null && docStatus == ErpFinConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(advance, docStatus, "非已作废");
        }
    }

    private Integer currentApproveStatus(ErpFinEmployeeAdvance advance) {
        Integer status = advance.getApproveStatus();
        return status != null ? status : ErpFinConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    private void requireEmployeeReady(ErpFinEmployeeAdvance advance) {
        // 经 daoProvider 直接加载员工（避免跨会话关系懒加载，对齐 ErpFinReconciliationBizModel.loadItem 范式）。
        app.erp.md.dao.entity.ErpMdEmployee employee = advance.getEmployeeId() == null ? null
                : daoProvider().daoFor(app.erp.md.dao.entity.ErpMdEmployee.class)
                        .getEntityById(advance.getEmployeeId());
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

    private void requireAmountPositive(ErpFinEmployeeAdvance advance) {
        BigDecimal amountFunctional = advance.getAmountFunctional();
        if (amountFunctional == null || amountFunctional.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_AMOUNT_INVALID)
                    .param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode());
        }
    }

    /** 派生 settledAmount/outstandingAmount：未还 = 本位币金额 - 已清算。初始 outstanding = amountFunctional。 */
    private void deriveAmounts(ErpFinEmployeeAdvance advance) {
        BigDecimal amount = nz(advance.getAmountFunctional());
        BigDecimal settled = nz(advance.getSettledAmount());
        advance.setSettledAmount(settled);
        advance.setOutstandingAmount(amount.subtract(settled));
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

    private NopException illegalTransition(ErpFinEmployeeAdvance advance, Integer current, String expected) {
        return new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpFinEmployeeAdvance advance, Integer current, String expected) {
        return new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode())
                .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
