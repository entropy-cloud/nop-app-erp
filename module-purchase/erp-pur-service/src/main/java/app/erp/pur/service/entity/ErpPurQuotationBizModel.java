
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurQuotationBiz;
import app.erp.pur.dao.constants.ErpPurDocStatus;
import app.erp.pur.dao.entity.ErpPurQuotation;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.SupplierEligibilityChecker;
import app.erp.pur.service.statemachine.ErpPurQuotationApprovalStateMachine;
import app.erp.pur.service.statemachine.ErpPurQuotationDocumentStateMachine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 供应商报价单 BizModel。报价单是供应商参与询价（RFQ）的入口——供应商经报价单成为 RFQ 收件人，
 * 故 AVL 准入与评分 standing 联动校验落在报价单保存前置钩子
 * （{@code docs/design/purchase/supplier-evaluation.md §业务规则3/5}）。
 *
 * <p>设计偏离补注：RFQ 头 {@code ErpPurRfq} 无 supplierId（一份询价发往多个供应商），
 * 「RFQ 创建校验」的供应商落点为报价单（supplier 参与点）。设计意图（SUSPENDED/RED 供应商不可参与询价）不变。
 *
 * <p>{@link #defaultPrepareSave} 委托 {@link SupplierEligibilityChecker}：
 * PREVENT → 抛 {@link ErpPurErrors#ERR_SUPPLIER_NOT_APPROVED}；WARN（YELLOW）→ 仅记录日志提示，不阻止保存。
 *
 * <p><b>docStatus cancel 守卫（plan 2026-08-12-0918-1 Phase 3 Fix）</b>：{@link #cancel} 经
 * {@link ErpPurQuotationDocumentStateMachine} 断言来源态合法（owner doc §2「非已作废」守卫）。
 *
 * <p><b>approveStatus 审批轴 Bean 接线（plan 2026-08-13-0945-1 Phase 3）</b>：5 动作经 {@link #prepareSubmit} 等
 * {@code @BizQuery} helper 委托 {@link ErpPurQuotationApprovalStateMachine}（INLINE 路径，契约 §4/§7）。
 * {@code ErpPurQuotation.xbiz} 内联 {@code isCancelled} + 来源态守卫改调本 helper，目标态写入改用 helper 返回值。
 * 错误码 Decision 分支 (b)：迁移前 xbiz 抛平台码 {@code nop.err.wf.approve.*}；迁移后 isCancelled 守卫→领域码
 * {@link ErpPurErrors#ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION}（同 sales precedent），来源态守卫→领域码
 * {@link ErpPurErrors#ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION}（同 Order/Req 模式）。行为变化已显式记录。
 */
@BizModel("ErpPurQuotation")
public class ErpPurQuotationBizModel extends CrudBizModel<ErpPurQuotation> implements IErpPurQuotationBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpPurQuotationBizModel.class);

    @Inject
    SupplierEligibilityChecker eligibilityChecker;

    @Inject
    ErpPurQuotationDocumentStateMachine stateMachine;

    @Inject
    ErpPurQuotationApprovalStateMachine approvalStateMachine;

    public ErpPurQuotationBizModel() {
        setEntityName(ErpPurQuotation.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpPurQuotation> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpPurQuotation quotation = entityData.getEntity();
        if (quotation == null || quotation.getSupplierId() == null) {
            return;
        }
        SupplierEligibilityChecker.Decision decision = eligibilityChecker.check(quotation.getSupplierId(), context);
        if (decision == SupplierEligibilityChecker.Decision.PREVENT) {
            throw new NopException(ErpPurErrors.ERR_SUPPLIER_NOT_APPROVED)
                    .param(ErpPurErrors.ARG_PARTNER_ID, quotation.getSupplierId())
                    .param(ErpPurErrors.ARG_STANDING, "SUSPENDED/REJECTED/RED");
        }
        if (decision == SupplierEligibilityChecker.Decision.WARN) {
            LOG.warn("供应商 {} 近期评分偏低（YELLOW），请关注其交付/质量表现", quotation.getSupplierId());
        }
    }

    @Override
    @BizMutation
    public ErpPurQuotation cancel(@Name("quotationId") Long quotationId, IServiceContext context) {
        ErpPurQuotation quotation = requireEntity(String.valueOf(quotationId), null, context);
        try {
            stateMachine.assertCanCancel(quotation.getDocStatus());
        } catch (NopException e) {
            throw new NopException(ErpPurErrors.ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION)
                    .param(ErpPurErrors.ARG_QUOTATION_CODE, quotation.getCode())
                    .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, quotation.getDocStatus())
                    .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, "非已作废");
        }
        quotation.setDocStatus(stateMachine.cancelTargetStatus());
        updateEntity(quotation, null, context);
        return quotation;
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
            throw new NopException(ErpPurErrors.ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION)
                    .param(ErpPurErrors.ARG_QUOTATION_CODE, code)
                    .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, docStatus)
                    .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, "非已作废");
        }
    }

    private NopException illegalStatus(String code, String currentStatus, String expectedStatus) {
        return new NopException(ErpPurErrors.ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_QUOTATION_CODE, code)
                .param(ErpPurErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expectedStatus);
    }
}

