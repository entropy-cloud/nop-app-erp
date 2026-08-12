
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurQuotation;

public interface IErpPurQuotationBiz extends ICrudBiz<ErpPurQuotation>{

    /**
     * 作废报价单：docStatus→CANCELLED。
     */
    @BizMutation
    ErpPurQuotation cancel(@Name("quotationId") Long quotationId, IServiceContext context);

    /**
     * 审批轴守卫 + 目标态（plan 2026-08-13-0945-1 Phase 3，INLINE 路径 Bean 接线）。
     * 经 {@code ErpPurQuotationApprovalStateMachine} 校验来源态合法性（isCancelled + approveStatus 来源态），
     * 返回目标态供 xbiz 写回。非法 → 抛领域码（{@code ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION} /
     * {@code ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION}）。
     */
    @BizQuery
    String prepareSubmit(@Name("code") String code, @Name("approveStatus") String approveStatus,
                         @Name("docStatus") String docStatus, IServiceContext context);

    @BizQuery
    String prepareApprove(@Name("code") String code, @Name("approveStatus") String approveStatus,
                          @Name("docStatus") String docStatus, IServiceContext context);

    @BizQuery
    String prepareReject(@Name("code") String code, @Name("approveStatus") String approveStatus,
                         @Name("docStatus") String docStatus, IServiceContext context);

    @BizQuery
    String prepareReverseApprove(@Name("code") String code, @Name("approveStatus") String approveStatus,
                                 @Name("docStatus") String docStatus, IServiceContext context);

    @BizQuery
    String prepareWithdraw(@Name("code") String code, @Name("approveStatus") String approveStatus,
                           @Name("docStatus") String docStatus, IServiceContext context);
}
