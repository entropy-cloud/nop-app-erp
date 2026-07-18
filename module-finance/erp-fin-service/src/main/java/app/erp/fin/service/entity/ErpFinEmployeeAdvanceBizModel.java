
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinEmployeeAdvanceBiz;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.EmployeeAdvancePostingDispatcher;
import app.erp.fin.service.processor.ErpFinEmployeeAdvanceProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 员工借款单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpFinEmployeeAdvanceProcessor} 全权处理。
 *
 * <p>{@link #cashRepay} 为金额更新 + 凭证生成动作（非状态机迁移），BizModel Facade 直落（plan 2026-07-18-0718-2）。
 */
@BizModel("ErpFinEmployeeAdvance")
public class ErpFinEmployeeAdvanceBizModel extends CrudBizModel<ErpFinEmployeeAdvance> implements IErpFinEmployeeAdvanceBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinEmployeeAdvanceBizModel.class);

    @Inject
    ErpFinEmployeeAdvanceProcessor advanceProcessor;

    @Inject
    EmployeeAdvancePostingDispatcher advancePostingDispatcher;

    public ErpFinEmployeeAdvanceBizModel() {
        setEntityName(ErpFinEmployeeAdvance.class.getName());
    }

    @Override
    @BizMutation
    public ErpFinEmployeeAdvance cancel(@Name("advanceId") Long advanceId, IServiceContext context) {
        return advanceProcessor.cancel(advanceId, context);
    }

    @Override
    @BizMutation
    public ErpFinEmployeeAdvance cashRepay(@Name("advanceId") Long advanceId,
                                           @Name("amount") BigDecimal amount,
                                           IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);

        // 守卫 1：须已过账且 APPROVED
        if (!Boolean.TRUE.equals(advance.getPosted())
                || !Objects.equals(advance.getApproveStatus(), ErpFinConstants.APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_NOT_REPAYABLE)
                    .param(ErpFinErrors.ARG_ADVANCE_ID, advanceId);
        }

        // 守卫 2：amount > 0
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID)
                    .param(ErpFinErrors.ARG_ADVANCE_ID, advanceId)
                    .param(ErpFinErrors.ARG_SETTLE_AMOUNT, amount);
        }

        // 守卫 3：amount <= outstandingAmount
        BigDecimal outstanding = nz(advance.getOutstandingAmount());
        if (amount.compareTo(outstanding) > 0) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_CASH_REPAY_EXCEEDS_OUTSTANDING)
                    .param(ErpFinErrors.ARG_ADVANCE_ID, advanceId)
                    .param(ErpFinErrors.ARG_SETTLE_AMOUNT, amount)
                    .param(ErpFinErrors.ARG_OPEN_AMOUNT, outstanding);
        }

        // 字段翻转（先持久化，对齐 postSettle 失败不阻断范式——残留风险：字段已更新但凭证缺失，归异常工作台补录）
        advance.setSettledAmount(nz(advance.getSettledAmount()).add(amount));
        advance.setOutstandingAmount(outstanding.subtract(amount));
        // docStatus 保持 APPROVED 不变（owner doc §还款状态派生：outstandingAmount=0 由派生投影表达「已结清」）
        updateEntity(advance, null, context);

        // 委派过账派发器：失败不阻断字段更新（仅 log warn）
        boolean posted = advancePostingDispatcher.postCashRepay(advance, amount, context);
        if (!posted) {
            LOG.warn("员工借款现金还款凭证生成失败但字段已更新：advanceId={}, amount={}", advanceId, amount);
        }

        return advance;
    }

    private ErpFinEmployeeAdvance requireAdvance(Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = get(String.valueOf(advanceId), true, context);
        if (advance == null) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_NOT_FOUND)
                    .param(ErpFinErrors.ARG_ADVANCE_ID, advanceId);
        }
        return advance;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

}
