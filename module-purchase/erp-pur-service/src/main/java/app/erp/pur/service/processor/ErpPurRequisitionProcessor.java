package app.erp.pur.service.processor;

import app.erp.pur.biz.ConvertToOrderRequest;
import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购请购单审批状态机 + 请购→订单转化编排 Processor（{@code processor-extension-pattern.md} Facade + Processor）。
 * Facade {@code ErpPurRequisitionBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>配置余地：每个动作只编排步骤顺序，各步骤为 {@code protected} 方法、以 {@link IServiceContext} 为末参。
 * 请购→订单转化为跨聚合写，委托 {@link IErpPurOrderBiz}（订单/行组装与持久化归订单侧）。
 */
public class ErpPurRequisitionProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpPurOrderBiz orderBiz;

    public ErpPurRequisition submit(Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        validateTransitionForSubmit(req, context);
        validateBusinessRulesForSubmit(req, context);
        doSubmit(req, context);
        return req;
    }

    public ErpPurRequisition withdrawSubmit(Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        validateNotCancelled(req, context);
        validateTransitionForWithdraw(req, context);
        doWithdrawSubmit(req, context);
        return req;
    }

    public ErpPurRequisition approve(Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        if (isAlreadyApproved(req)) {
            return req;
        }
        validateNotCancelled(req, context);
        validateTransitionForApprove(req, context);
        doApprove(req, context);
        return req;
    }

    public ErpPurRequisition reject(Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        validateNotCancelled(req, context);
        validateTransitionForReject(req, context);
        doReject(req, context);
        return req;
    }

    public ErpPurRequisition reverseApprove(Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        if (isAlreadyRejected(req)) {
            return req;
        }
        validateTransitionForReverseApprove(req, context);
        doReverseApprove(req, context);
        return req;
    }

    public ErpPurRequisition cancel(Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        validateTransitionForCancel(req, context);
        doCancel(req, context);
        return req;
    }

    public ErpPurOrder convertToOrder(Long requisitionId, ConvertToOrderRequest request, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(requisitionId, context);
        validateApprovedForConversion(req, context);
        List<ErpPurRequisitionLine> lines = loadLines(requisitionId);
        validateLinesNonEmptyForConversion(req, lines, context);
        Long supplierId = validateConsistentSupplier(req, lines, context);
        validateNotAlreadyConverted(requisitionId, context);
        return doConvertToOrder(req, lines, supplierId, request, context);
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForSubmit(ErpPurRequisition req, IServiceContext context) {
        validateNotCancelled(req, context);
        String status = req.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpPurConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(req, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpPurRequisition req, IServiceContext context) {
        String status = req.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(req, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpPurRequisition req, IServiceContext context) {
        String status = req.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(req, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpPurRequisition req, IServiceContext context) {
        String status = req.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(req, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpPurRequisition req, IServiceContext context) {
        String status = req.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(req, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpPurRequisition req, IServiceContext context) {
        String docStatus = req.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpPurConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(req, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpPurRequisition req, IServiceContext context) {
        requireLinesNonEmpty(req, context);
    }

    protected void validateApprovedForConversion(ErpPurRequisition req, IServiceContext context) {
        String status = req.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpPurErrors.ERR_REQ_NOT_APPROVED)
                    .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode())
                    .param(ErpPurErrors.ARG_CURRENT_STATUS, status);
        }
    }

    protected void validateLinesNonEmptyForConversion(ErpPurRequisition req, List<ErpPurRequisitionLine> lines,
                                                      IServiceContext context) {
        if (lines.isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_REQ_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode());
        }
    }

    protected Long validateConsistentSupplier(ErpPurRequisition req, List<ErpPurRequisitionLine> lines,
                                              IServiceContext context) {
        Set<Long> suppliers = new HashSet<>();
        for (ErpPurRequisitionLine line : lines) {
            if (line.getSuggestedSupplierId() == null) {
                throw new NopException(ErpPurErrors.ERR_REQ_MIXED_OR_MISSING_SUPPLIER)
                        .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode());
            }
            suppliers.add(line.getSuggestedSupplierId());
        }
        if (suppliers.size() != 1) {
            throw new NopException(ErpPurErrors.ERR_REQ_MIXED_OR_MISSING_SUPPLIER)
                    .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode());
        }
        return suppliers.iterator().next();
    }

    protected void validateNotAlreadyConverted(Long requisitionId, IServiceContext context) {
        if (orderBiz.existsActiveByRequisition(requisitionId, context)) {
            throw new NopException(ErpPurErrors.ERR_REQ_ALREADY_CONVERTED)
                    .param(ErpPurErrors.ARG_REQUISITION_ID, requisitionId);
        }
    }

    // ---------- step：执行 ----------

    protected void doSubmit(ErpPurRequisition req, IServiceContext context) {
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        requisitionDao().updateEntity(req);
    }

    protected void doWithdrawSubmit(ErpPurRequisition req, IServiceContext context) {
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        requisitionDao().updateEntity(req);
    }

    protected void doApprove(ErpPurRequisition req, IServiceContext context) {
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        req.setApprovedBy(currentUserId());
        req.setApprovedAt(CoreMetrics.currentDateTime());
        requisitionDao().updateEntity(req);
    }

    protected void doReject(ErpPurRequisition req, IServiceContext context) {
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        requisitionDao().updateEntity(req);
    }

    protected void doReverseApprove(ErpPurRequisition req, IServiceContext context) {
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        requisitionDao().updateEntity(req);
    }

    protected void doCancel(ErpPurRequisition req, IServiceContext context) {
        req.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        requisitionDao().updateEntity(req);
    }

    protected ErpPurOrder doConvertToOrder(ErpPurRequisition req, List<ErpPurRequisitionLine> lines,
                                           Long supplierId, ConvertToOrderRequest request, IServiceContext context) {
        return orderBiz.createFromRequisition(req, lines, supplierId, request, context);
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpPurRequisition requireRequisition(Long requisitionId, IServiceContext context) {
        ErpPurRequisition req = requisitionDao().getEntityById(requisitionId);
        if (req == null) {
            throw new NopException(ErpPurErrors.ERR_REQ_NOT_FOUND)
                    .param(ErpPurErrors.ARG_REQUISITION_ID, requisitionId);
        }
        return req;
    }

    protected void validateNotCancelled(ErpPurRequisition req, IServiceContext context) {
        String docStatus = req.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpPurConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(req, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpPurRequisition req) {
        String status = req.getApproveStatus();
        return status != null && Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isAlreadyRejected(ErpPurRequisition req) {
        String status = req.getApproveStatus();
        return status != null && Objects.equals(status, ErpPurConstants.APPROVE_STATUS_REJECTED);
    }

    protected void requireLinesNonEmpty(ErpPurRequisition req, IServiceContext context) {
        if (loadLines(req.getId()).isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_REQ_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode());
        }
    }

    protected List<ErpPurRequisitionLine> loadLines(Long requisitionId) {
        IEntityDao<ErpPurRequisitionLine> dao = daoProvider.daoFor(ErpPurRequisitionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("requisitionId", requisitionId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    protected IEntityDao<ErpPurRequisition> requisitionDao() {
        return daoProvider.daoFor(ErpPurRequisition.class);
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            if (ctx == null) {
                return null;
            }
            return ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected NopException illegalTransition(ErpPurRequisition req, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_REQ_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpPurRequisition req, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_REQ_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
