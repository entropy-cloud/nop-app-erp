package app.erp.pur.service.processor;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.dao.entity.ErpPurReturnLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.entity.ReturnQtyValidator;
import app.erp.pur.service.entity.ReturnStockMoveBuilder;
import app.erp.pur.service.posting.PurReturnPostingDispatcher;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import java.util.Objects;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购退货单审批状态机 + 退货审核触发库存反向出库 + PURCHASE_RETURN 过账编排 Processor
 * （{@code processor-extension-pattern.md} Facade + Processor）。Facade {@code ErpPurReturnBizModel}
 * 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>配置余地：每个动作只编排步骤顺序，各步骤为 {@code protected} 方法、以 {@link IServiceContext} 为末参。
 */
public class ErpPurReturnProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Inject
    ReturnStockMoveBuilder stockMoveBuilder;

    @Inject
    ReturnQtyValidator returnQtyValidator;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    PurReturnPostingDispatcher postingDispatcher;

    public ErpPurReturn submit(Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        validateTransitionForSubmit(returnOrder, context);
        validateBusinessRulesForSubmit(returnOrder, context);
        doSubmit(returnOrder, context);
        return returnOrder;
    }

    public ErpPurReturn withdrawSubmit(Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        validateNotCancelled(returnOrder, context);
        validateTransitionForWithdraw(returnOrder, context);
        doWithdrawSubmit(returnOrder, context);
        return returnOrder;
    }

    public ErpPurReturn approve(Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        if (isAlreadyApproved(returnOrder)) {
            return returnOrder;
        }
        validateNotCancelled(returnOrder, context);
        validateTransitionForApprove(returnOrder, context);
        validateBusinessRulesForApprove(returnOrder, context);

        // 库存反向出库移动（物理正确性硬约束，失败抛异常回滚审核）。
        ErpInvStockMove move = triggerOutgoingMove(returnOrder, context);
        // 跨域 generateMove 将移动单推进至 DONE 并更新库存余额；先刷盘使 DONE 状态与余额变动落地到当前事务的 DB 连接，
        // 避免后续 REQUIRES_NEW 过账（独立会话）挂起当前会话时丢失会话内暂存的 DONE 暂态。
        ormTemplate.flushSession();

        boolean posted = postingDispatcher.tryPost(returnOrder);

        // 跨域调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化。
        returnOrder = returnDao().getEntityById(returnId);
        doApprove(returnOrder, posted, context);
        return returnOrder;
    }

    public ErpPurReturn reject(Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        validateNotCancelled(returnOrder, context);
        validateTransitionForReject(returnOrder, context);
        doReject(returnOrder, context);
        return returnOrder;
    }

    public ErpPurReturn reverseApprove(Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        if (isAlreadyRejected(returnOrder)) {
            return returnOrder;
        }
        validateTransitionForReverseApprove(returnOrder, context);
        ensureReversed(returnOrder, context);
        returnOrder = returnDao().getEntityById(returnId);
        doReverseApprove(returnOrder, context);
        return returnOrder;
    }

    public ErpPurReturn cancel(Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        validateTransitionForCancel(returnOrder, context);
        if (isApproved(returnOrder)) {
            ensureReversed(returnOrder, context);
            returnOrder = returnDao().getEntityById(returnId);
        }
        doCancel(returnOrder, context);
        return returnOrder;
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForSubmit(ErpPurReturn returnOrder, IServiceContext context) {
        validateNotCancelled(returnOrder, context);
        String status = returnOrder.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpPurConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(returnOrder, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpPurReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpPurReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpPurReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpPurReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(returnOrder, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpPurReturn returnOrder, IServiceContext context) {
        String docStatus = returnOrder.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpPurConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(returnOrder, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpPurReturn returnOrder, IServiceContext context) {
        requireLinesNonEmpty(returnOrder, context);
        requireSupplierActive(returnOrder, context);
    }

    protected void validateBusinessRulesForApprove(ErpPurReturn returnOrder, IServiceContext context) {
        requireSupplierActive(returnOrder, context);
        requireSourceReceiveApproved(returnOrder, context);
        List<ErpPurReturnLine> lines = loadLines(returnOrder.getId());
        requireReasonIfConfigured(returnOrder, lines, context);
        returnQtyValidator.validate(returnOrder, lines);
    }

    // ---------- step：执行 ----------

    protected void doSubmit(ErpPurReturn returnOrder, IServiceContext context) {
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        returnDao().updateEntity(returnOrder);
    }

    protected void doWithdrawSubmit(ErpPurReturn returnOrder, IServiceContext context) {
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        returnDao().updateEntity(returnOrder);
    }

    protected void doApprove(ErpPurReturn returnOrder, boolean posted, IServiceContext context) {
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        returnOrder.setApprovedBy(currentUserId());
        returnOrder.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            returnOrder.setPosted(true);
            returnOrder.setPostedAt(CoreMetrics.currentDateTime());
            returnOrder.setPostedBy(currentUserId());
        }
        returnDao().updateEntity(returnOrder);
    }

    protected void doReject(ErpPurReturn returnOrder, IServiceContext context) {
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        returnDao().updateEntity(returnOrder);
    }

    protected void doReverseApprove(ErpPurReturn returnOrder, IServiceContext context) {
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        returnDao().updateEntity(returnOrder);
    }

    protected void doCancel(ErpPurReturn returnOrder, IServiceContext context) {
        returnOrder.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        returnDao().updateEntity(returnOrder);
    }

    // ---------- 库存触发 + 冲销 ----------

    protected ErpInvStockMove triggerOutgoingMove(ErpPurReturn returnOrder, IServiceContext context) {
        List<ErpPurReturnLine> lines = loadLines(returnOrder.getId());
        StockMoveRequest request = stockMoveBuilder.build(returnOrder, lines, context);
        request.setOriginReturnedMoveId(resolveSourceReceiveMoveId(returnOrder, context));
        return stockMoveBiz.generateMove(request, context);
    }

    protected Long resolveSourceReceiveMoveId(ErpPurReturn returnOrder, IServiceContext context) {
        ErpPurReceive receive = returnOrder.getReceive();
        if (receive == null) {
            return null;
        }
        ErpInvStockMove sourceMove = stockMoveBiz.findByRelatedBill(
                ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receive.getCode(), context);
        return sourceMove == null ? null : sourceMove.getId();
    }

    protected void ensureReversed(ErpPurReturn returnOrder, IServiceContext context) {
        if (Boolean.TRUE.equals(returnOrder.getPosted())) {
            postingDispatcher.reverse(returnOrder);
            returnOrder = returnDao().getEntityById(returnOrder.getId());
            returnOrder.setPosted(false);
            returnOrder.setPostedAt(null);
            returnOrder.setPostedBy(null);
        }
        ErpInvStockMove original = stockMoveBiz.findByRelatedBill(
                ErpPurConstants.RELATED_BILL_TYPE_PUR_RETURN, returnOrder.getCode(), context);
        if (original == null) {
            throw new NopException(ErpPurErrors.ERR_MOVE_NOT_FOUND)
                    .param(ErpPurErrors.ARG_RETURN_CODE, returnOrder.getCode());
        }
        ErpInvStockMove existingReversal = stockMoveBiz.findByRelatedBill(
                ErpPurConstants.RELATED_BILL_TYPE_REVERSAL, original.getCode(), context);
        if (existingReversal != null) {
            return;
        }
        stockMoveBiz.reverse(original.getId(), context);
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpPurReturn requireReturn(Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = returnDao().getEntityById(returnId);
        if (returnOrder == null) {
            throw new NopException(ErpPurErrors.ERR_RETURN_NOT_FOUND)
                    .param(ErpPurErrors.ARG_RETURN_ID, returnId);
        }
        return returnOrder;
    }

    protected void validateNotCancelled(ErpPurReturn returnOrder, IServiceContext context) {
        String docStatus = returnOrder.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpPurConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(returnOrder, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpPurReturn returnOrder) {
        String status = returnOrder.getApproveStatus();
        return status != null && Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isApproved(ErpPurReturn returnOrder) {
        return isAlreadyApproved(returnOrder);
    }

    protected boolean isAlreadyRejected(ErpPurReturn returnOrder) {
        String status = returnOrder.getApproveStatus();
        return status != null && Objects.equals(status, ErpPurConstants.APPROVE_STATUS_REJECTED);
    }

    protected void requireLinesNonEmpty(ErpPurReturn returnOrder, IServiceContext context) {
        if (loadLines(returnOrder.getId()).isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_RETURN_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_RETURN_CODE, returnOrder.getCode());
        }
    }

    protected void requireSupplierActive(ErpPurReturn returnOrder, IServiceContext context) {
        if (returnOrder.getSupplierId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(returnOrder.getSupplierId(), context);
        if (partner == null || partner.getStatus() == null
                || !Objects.equals(partner.getStatus(), ErpPurConstants.PARTNER_STATUS_ACTIVE)) {
            throw new NopException(ErpPurErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpPurErrors.ARG_SUPPLIER_ID, returnOrder.getSupplierId());
        }
    }

    protected void requireSourceReceiveApproved(ErpPurReturn returnOrder, IServiceContext context) {
        ErpPurReceive receive = returnOrder.getReceive();
        if (receive == null) {
            throw new NopException(ErpPurErrors.ERR_RETURN_RECEIVE_NOT_APPROVED)
                    .param(ErpPurErrors.ARG_CURRENT_STATUS, null);
        }
        String receiveStatus = receive.getApproveStatus();
        if (receiveStatus == null || !Objects.equals(receiveStatus, ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpPurErrors.ERR_RETURN_RECEIVE_NOT_APPROVED)
                    .param(ErpPurErrors.ARG_CURRENT_STATUS, receiveStatus);
        }
    }

    protected void requireReasonIfConfigured(ErpPurReturn returnOrder, List<ErpPurReturnLine> lines,
                                             IServiceContext context) {
        if (!isReasonRequired()) {
            return;
        }
        for (ErpPurReturnLine line : lines) {
            if (line.getReason() == null || line.getReason().trim().isEmpty()) {
                throw new NopException(ErpPurErrors.ERR_RETURN_REASON_REQUIRED)
                        .param(ErpPurErrors.ARG_RETURN_CODE, returnOrder.getCode())
                        .param(ErpPurErrors.ARG_LINE_NO, line.getLineNo());
            }
        }
    }

    protected boolean isReasonRequired() {
        return readBoolConfig(ErpPurConstants.CONFIG_RETURN_REASON_REQUIRED, true);
    }

    protected boolean readBoolConfig(String key, boolean defaultValue) {
        try {
            String value = AppConfig.var(key, String.valueOf(defaultValue));
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return Boolean.parseBoolean(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    protected List<ErpPurReturnLine> loadLines(Long returnId) {
        IEntityDao<ErpPurReturnLine> dao = daoProvider.daoFor(ErpPurReturnLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("returnId", returnId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpPurReturn> returnDao() {
        return daoProvider.daoFor(ErpPurReturn.class);
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

    protected NopException illegalTransition(ErpPurReturn returnOrder, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_RETURN_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RETURN_CODE, returnOrder.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpPurReturn returnOrder, String current, String expected) {
        return new NopException(ErpPurErrors.ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RETURN_CODE, returnOrder.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
