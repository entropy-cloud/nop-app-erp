
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import app.erp.pur.biz.IErpPurRfqBiz;
import app.erp.pur.dao.constants.ErpPurDocStatus;
import app.erp.pur.dao.entity.ErpPurRfq;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurRfqApprovalStateMachine;
import app.erp.pur.service.statemachine.ErpPurRfqDocumentStateMachine;

/**
 * 询价单 BizModel。
 *
 * <p><b>docStatus cancel 守卫（plan 2026-08-12-0918-1 Phase 3 Fix）</b>：{@link #cancel} 经
 * {@link ErpPurRfqDocumentStateMachine} 断言来源态合法（owner doc §2「非已作废」守卫）。
 *
 * <p><b>approveStatus 审批轴 Bean 接线（plan 2026-08-13-0945-1 Phase 3）</b>：5 动作经 {@link #prepareSubmit} 等
 * {@code @BizQuery} helper 委托 {@link ErpPurRfqApprovalStateMachine}（INLINE 路径，契约 §4/§7）。
 * {@code ErpPurRfq.xbiz} 内联守卫改调本 helper。错误码 Decision 分支 (b)：平台码 → 领域码
 * （isCancelled→{@link ErpPurErrors#ERR_RFQ_ILLEGAL_DOC_STATUS_TRANSITION}，来源态→{@link ErpPurErrors#ERR_RFQ_ILLEGAL_STATUS_TRANSITION}）。
 */
@BizModel("ErpPurRfq")
public class ErpPurRfqBizModel extends CrudBizModel<ErpPurRfq> implements IErpPurRfqBiz {

    @Inject
    ErpPurRfqDocumentStateMachine stateMachine;

    @Inject
    ErpPurRfqApprovalStateMachine approvalStateMachine;

    public ErpPurRfqBizModel(){
        setEntityName(ErpPurRfq.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurRfq cancel(@Name("rfqId") Long rfqId, IServiceContext context) {
        ErpPurRfq rfq = requireEntity(String.valueOf(rfqId), null, context);
        try {
            stateMachine.assertCanCancel(rfq.getDocStatus());
        } catch (NopException e) {
            throw new NopException(ErpPurErrors.ERR_RFQ_ILLEGAL_DOC_STATUS_TRANSITION)
                    .param(ErpPurErrors.ARG_RFQ_CODE, rfq.getCode())
                    .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, rfq.getDocStatus())
                    .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, "非已作废");
        }
        rfq.setDocStatus(stateMachine.cancelTargetStatus());
        updateEntity(rfq, null, context);
        return rfq;
    }

    // ---------- approveStatus 审批轴 helper（plan 2026-08-13-0945-1 Phase 3，INLINE 路径委托 Bean） ----------

    @Override
    @BizQuery
    public String prepareSubmit(@Name("code") String code, @Name("approveStatus") String approveStatus,
                                @Name("docStatus") String docStatus, IServiceContext context) {
        requireNotCancelled(code, docStatus);
        try {
            approvalStateMachine.assertCanSubmit(approveStatus);
        } catch (NopException e) {
            throw illegalStatus(code, approveStatus, "UNSUBMITTED 或 REJECTED");
        }
        return approvalStateMachine.submitTargetStatus();
    }

    @Override
    @BizQuery
    public String prepareApprove(@Name("code") String code, @Name("approveStatus") String approveStatus,
                                 @Name("docStatus") String docStatus, IServiceContext context) {
        requireNotCancelled(code, docStatus);
        try {
            approvalStateMachine.assertCanApprove(approveStatus);
        } catch (NopException e) {
            throw illegalStatus(code, approveStatus, ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        }
        return approvalStateMachine.approveTargetStatus();
    }

    @Override
    @BizQuery
    public String prepareReject(@Name("code") String code, @Name("approveStatus") String approveStatus,
                                @Name("docStatus") String docStatus, IServiceContext context) {
        requireNotCancelled(code, docStatus);
        try {
            approvalStateMachine.assertCanReject(approveStatus);
        } catch (NopException e) {
            throw illegalStatus(code, approveStatus, ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        }
        return approvalStateMachine.rejectTargetStatus();
    }

    @Override
    @BizQuery
    public String prepareReverseApprove(@Name("code") String code, @Name("approveStatus") String approveStatus,
                                        @Name("docStatus") String docStatus, IServiceContext context) {
        requireNotCancelled(code, docStatus);
        try {
            approvalStateMachine.assertCanReverseApprove(approveStatus);
        } catch (NopException e) {
            throw illegalStatus(code, approveStatus, ErpPurDocStatus.APPROVE_STATUS_APPROVED);
        }
        return approvalStateMachine.reverseApproveTargetStatus();
    }

    @Override
    @BizQuery
    public String prepareWithdraw(@Name("code") String code, @Name("approveStatus") String approveStatus,
                                  @Name("docStatus") String docStatus, IServiceContext context) {
        requireNotCancelled(code, docStatus);
        try {
            approvalStateMachine.assertCanWithdraw(approveStatus);
        } catch (NopException e) {
            throw illegalStatus(code, approveStatus, ErpPurDocStatus.APPROVE_STATUS_SUBMITTED);
        }
        return approvalStateMachine.withdrawTargetStatus();
    }

    private void requireNotCancelled(String code, String docStatus) {
        if (ErpPurDocStatus.DOC_STATUS_CANCELLED.equals(docStatus)) {
            throw new NopException(ErpPurErrors.ERR_RFQ_ILLEGAL_DOC_STATUS_TRANSITION)
                    .param(ErpPurErrors.ARG_RFQ_CODE, code)
                    .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, docStatus)
                    .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, "非已作废");
        }
    }

    private NopException illegalStatus(String code, String currentStatus, String expectedStatus) {
        return new NopException(ErpPurErrors.ERR_RFQ_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RFQ_CODE, code)
                .param(ErpPurErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expectedStatus);
    }
}
