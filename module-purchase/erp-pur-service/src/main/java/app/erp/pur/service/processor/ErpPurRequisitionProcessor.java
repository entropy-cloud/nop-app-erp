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
 * 采购请购单审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 */
public class ErpPurRequisitionProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpPurOrderBiz orderBiz;

    public ErpPurRequisition submitForApproval(String id, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(id, context);
        validateTransitionForSubmit(req, context);
        validateBusinessRulesForSubmit(req, context);
        doSubmit(req, context);
        return req;
    }

    public ErpPurRequisition withdrawApproval(String id, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(id, context);
        validateNotCancelled(req, context);
        validateTransitionForWithdraw(req, context);
        doWithdrawSubmit(req, context);
        return req;
    }

    public ErpPurRequisition approve(String id, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(id, context);
        if (req.isApproved()) {
            return req;
        }
        validateNotCancelled(req, context);
        validateTransitionForApprove(req, context);
        doApprove(req, context);
        return req;
    }

    public ErpPurRequisition reject(String id, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(id, context);
        validateNotCancelled(req, context);
        validateTransitionForReject(req, context);
        doReject(req, context);
        return req;
    }

    public ErpPurRequisition reverseApprove(String id, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(id, context);
        if (req.isRejected()) {
            return req;
        }
        validateTransitionForReverseApprove(req, context);
        doReverseApprove(req, context);
        return req;
    }

    public ErpPurRequisition cancel(String id, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(id, context);
        validateTransitionForCancel(req, context);
        doCancel(req, context);
        return req;
    }

    public ErpPurOrder convertToOrder(String id, ConvertToOrderRequest request, IServiceContext context) {
        ErpPurRequisition req = requireRequisition(id, context);
        validateApprovedForConversion(req, context);
        List<ErpPurRequisitionLine> lines = loadLines(req);
        validateLinesNonEmptyForConversion(req, lines, context);
        Long supplierId = validateConsistentSupplier(req, lines, context);
        validateNotAlreadyConverted(req.getId(), context);
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
        req.setApprovedAt(CoreMetrics.currentTimestamp());
        requisitionDao().updateEntity(req);
    }

    protected void doReject(ErpPurRequisition req, IServiceContext context) {
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        requisitionDao().updateEntity(req);
    }

    protected void doReverseApprove(ErpPurRequisition req, IServiceContext context) {
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        req.setApprovedBy(null);
        req.setApprovedAt(null);
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

    protected ErpPurRequisition requireRequisition(String id, IServiceContext context) {
        ErpPurRequisition req = requisitionDao().getEntityById(id);
        if (req == null) {
            throw new NopException(ErpPurErrors.ERR_REQ_NOT_FOUND)
                    .param(ErpPurErrors.ARG_REQUISITION_ID, id);
        }
        return req;
    }

    protected void validateNotCancelled(ErpPurRequisition req, IServiceContext context) {
        if (req.isCancelled()) {
            throw illegalDocTransition(req, req.getDocStatus(), "非已作废");
        }
    }

    protected void requireLinesNonEmpty(ErpPurRequisition req, IServiceContext context) {
        if (req.getLines().isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_REQ_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_REQUISITION_CODE, req.getCode());
        }
    }

    /**
     * 通过 ORM to-many 关系 {@code ErpPurRequisition.lines} 加载行（懒加载，复用主实体 session）。
     * 关系已在 {@code app-erp-purchase.orm.xml} 声明。
     */
    protected List<ErpPurRequisitionLine> loadLines(ErpPurRequisition req) {
        return new ArrayList<>(req.getLines());
    }

    // ---------- misc helpers ----------

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
