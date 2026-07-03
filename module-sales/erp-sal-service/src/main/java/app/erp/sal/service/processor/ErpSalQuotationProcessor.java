package app.erp.sal.service.processor;

import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.dao.entity.ErpSalQuotationLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售报价单审批状态机 + 客户确认 + 报价→订单转化编排 Processor
 * （{@code processor-extension-pattern.md} Facade + Processor）。Facade {@code ErpSalQuotationBizModel}
 * 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>跨实体：报价→订单转化经 {@link IErpSalOrderBiz}（create{@code FromQuotation} + {@code existsActiveByQuotation} 防重）。
 *
 * <p>模型边界：报价单无 {@code approvedBy}/{@code approvedAt} 列——审核仅翻转 {@code approveStatus}，
 * 不记录审核人/时间。「EXPIRED」不在持久化（无列），由 {@code validTo < today} 在确认/转化时派生校验。
 */
public class ErpSalQuotationProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpSalOrderBiz orderBiz;

    public ErpSalQuotation submit(Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        validateTransitionForSubmit(quotation, context);
        validateBusinessRulesForSubmit(quotation, context);
        doSubmit(quotation, context);
        return quotation;
    }

    public ErpSalQuotation withdrawSubmit(Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        validateNotCancelled(quotation, context);
        validateTransitionForWithdraw(quotation, context);
        doWithdrawSubmit(quotation, context);
        return quotation;
    }

    public ErpSalQuotation approve(Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        // 幂等：已审核再次审核为空操作。
        if (isAlreadyApproved(quotation)) {
            return quotation;
        }
        validateNotCancelled(quotation, context);
        validateTransitionForApprove(quotation, context);
        doApprove(quotation, context);
        return quotation;
    }

    public ErpSalQuotation reject(Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        validateNotCancelled(quotation, context);
        validateTransitionForReject(quotation, context);
        doReject(quotation, context);
        return quotation;
    }

    public ErpSalQuotation reverseApprove(Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        // 幂等：已 REJECTED 直接返回。
        if (isAlreadyRejected(quotation)) {
            return quotation;
        }
        validateTransitionForReverseApprove(quotation, context);
        doReverseApprove(quotation, context);
        return quotation;
    }

    public ErpSalQuotation cancel(Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        validateTransitionForCancel(quotation, context);
        doCancel(quotation, context);
        return quotation;
    }

    public ErpSalQuotation confirmCustomerAccepted(Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        validateNotCancelled(quotation, context);
        validateTransitionForConfirm(quotation, context);
        requireNotExpired(quotation, context);
        doConfirmCustomerAccepted(quotation, context);
        return quotation;
    }

    public ErpSalOrder convertToOrder(Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = requireQuotation(quotationId, context);
        validateReadyForConvert(quotation, context);
        requireNotExpired(quotation, context);
        validateNotAlreadyConverted(quotation, context);
        ErpSalOrder order = createOrderFromQuotation(quotation, context);
        markQuotationAccepted(quotationId, context);
        return order;
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForSubmit(ErpSalQuotation quotation, IServiceContext context) {
        validateNotCancelled(quotation, context);
        Integer status = quotation.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpSalConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpSalConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(quotation, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpSalQuotation quotation, IServiceContext context) {
        Integer status = quotation.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(quotation, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpSalQuotation quotation, IServiceContext context) {
        Integer status = quotation.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(quotation, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpSalQuotation quotation, IServiceContext context) {
        Integer status = quotation.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(quotation, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpSalQuotation quotation, IServiceContext context) {
        Integer status = quotation.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(quotation, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpSalQuotation quotation, IServiceContext context) {
        Integer docStatus = quotation.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(quotation, docStatus, "非已作废");
        }
    }

    protected void validateTransitionForConfirm(ErpSalQuotation quotation, IServiceContext context) {
        Integer status = quotation.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(quotation, status, "APPROVED");
        }
    }

    /**
     * 转化前置校验：须未作废 + APPROVED + isAccepted（否则 ERR_QUOTATION_NOT_READY）。
     */
    protected void validateReadyForConvert(ErpSalQuotation quotation, IServiceContext context) {
        validateNotCancelled(quotation, context);
        Integer status = quotation.getApproveStatus();
        boolean accepted = Boolean.TRUE.equals(quotation.getIsAccepted());
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED || !accepted) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_NOT_READY)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpSalErrors.ARG_IS_ACCEPTED, accepted);
        }
    }

    /**
     * 幂等防重复转化：订单侧查既有 docStatus≠CANCELLED 且 quotationId 命中 → 拒绝。
     */
    protected void validateNotAlreadyConverted(ErpSalQuotation quotation, IServiceContext context) {
        if (orderBiz.existsActiveByQuotation(quotation.getId(), context)) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_ALREADY_CONVERTED)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode());
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpSalQuotation quotation, IServiceContext context) {
        requireLinesNonEmpty(quotation, context);
    }

    /**
     * EXPIRED 派生校验：{@code validTo < today} 视为过期（无持久化 EXPIRED 列）。
     */
    protected void requireNotExpired(ErpSalQuotation quotation, IServiceContext context) {
        LocalDate validTo = quotation.getValidTo();
        if (validTo != null && validTo.isBefore(CoreMetrics.currentDate())) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_EXPIRED)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                    .param(ErpSalErrors.ARG_VALID_TO, validTo);
        }
    }

    // ---------- step：执行 ----------

    protected void doSubmit(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        quotationDao().updateEntity(quotation);
    }

    protected void doWithdrawSubmit(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        quotationDao().updateEntity(quotation);
    }

    protected void doApprove(ErpSalQuotation quotation, IServiceContext context) {
        // 模型边界：无 approvedBy/approvedAt 列，审核仅翻转 approveStatus。
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        quotationDao().updateEntity(quotation);
    }

    protected void doReject(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        quotationDao().updateEntity(quotation);
    }

    protected void doReverseApprove(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        quotationDao().updateEntity(quotation);
    }

    protected void doCancel(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        quotationDao().updateEntity(quotation);
    }

    protected void doConfirmCustomerAccepted(ErpSalQuotation quotation, IServiceContext context) {
        quotation.setIsAccepted(true);
        quotationDao().updateEntity(quotation);
    }

    /**
     * 跨聚合转化：经 {@link IErpSalOrderBiz#createFromQuotation} 委托订单聚合（订单/行的组装与持久化归订单侧）。
     */
    protected ErpSalOrder createOrderFromQuotation(ErpSalQuotation quotation, IServiceContext context) {
        List<ErpSalQuotationLine> quotationLines = loadLines(quotation.getId());
        return orderBiz.createFromQuotation(quotation, quotationLines, context);
    }

    /**
     * 回链：重新加载后置 quotation.isAccepted=true（纯标记，无 ORM FK 改动；quotationId 列已存在于订单）。
     */
    protected void markQuotationAccepted(Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = quotationDao().getEntityById(quotationId);
        quotation.setIsAccepted(true);
        quotationDao().updateEntity(quotation);
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpSalQuotation requireQuotation(Long quotationId, IServiceContext context) {
        ErpSalQuotation quotation = quotationDao().getEntityById(quotationId);
        if (quotation == null) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_NOT_FOUND)
                    .param(ErpSalErrors.ARG_QUOTATION_ID, quotationId);
        }
        return quotation;
    }

    protected void validateNotCancelled(ErpSalQuotation quotation, IServiceContext context) {
        Integer docStatus = quotation.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(quotation, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpSalQuotation quotation) {
        Integer status = quotation.getApproveStatus();
        return status != null && status == ErpSalConstants.APPROVE_STATUS_APPROVED;
    }

    protected boolean isAlreadyRejected(ErpSalQuotation quotation) {
        Integer status = quotation.getApproveStatus();
        return status != null && status == ErpSalConstants.APPROVE_STATUS_REJECTED;
    }

    protected void requireLinesNonEmpty(ErpSalQuotation quotation, IServiceContext context) {
        if (loadLines(quotation.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_QUOTATION_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode());
        }
    }

    protected List<ErpSalQuotationLine> loadLines(Long quotationId) {
        // D2 边界场景：同聚合子表加载，父实体已授权，子行无独立权限规则。
        IEntityDao<ErpSalQuotationLine> dao = daoProvider.daoFor(ErpSalQuotationLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("quotationId", quotationId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpSalQuotation> quotationDao() {
        return daoProvider.daoFor(ErpSalQuotation.class);
    }

    protected NopException illegalTransition(ErpSalQuotation quotation, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpSalQuotation quotation, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_QUOTATION_CODE, quotation.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
