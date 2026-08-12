
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurRfq;

public interface IErpPurRfqBiz extends ICrudBiz<ErpPurRfq>{

    /**
     * 作废/流标询价单：docStatus→CANCELLED。
     */
    @BizMutation
    ErpPurRfq cancel(@Name("rfqId") Long rfqId, IServiceContext context);

    /**
     * 审批轴守卫 + 目标态（plan 2026-08-13-0945-1 Phase 3，INLINE 路径 Bean 接线）。
     * 经 {@code ErpPurRfqApprovalStateMachine} 校验来源态合法性，返回目标态供 xbiz 写回。
     * 非法 → 抛领域码（{@code ERR_RFQ_ILLEGAL_DOC_STATUS_TRANSITION} / {@code ERR_RFQ_ILLEGAL_STATUS_TRANSITION}）。
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
