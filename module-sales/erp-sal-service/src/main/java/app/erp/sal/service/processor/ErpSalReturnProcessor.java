package app.erp.sal.service.processor;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.entity.ReturnQtyValidator;
import app.erp.sal.service.entity.ReturnRefundOrchestrator;
import app.erp.sal.service.entity.ReturnStockMoveBuilder;
import app.erp.sal.service.posting.SalReturnPostingDispatcher;
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
 * 销售退货单审批状态机 + 退货审核触发库存反向入库移动 + SALES_RETURN 过账 + 退款编排 Processor
 * （{@code processor-extension-pattern.md} Facade + Processor）。Facade {@code ErpSalReturnBizModel}
 * 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>跨实体：客户启用校验经 {@link IErpMdPartnerBiz}；源出库单经退货单 {@code delivery} 关系 getter；
 * 库存反向入库经 {@link IErpInvStockMoveBiz}；过账经 {@link SalReturnPostingDispatcher} →凭证聚合根 Facade；
 * 退款编排经 {@link ReturnRefundOrchestrator}。
 */
public class ErpSalReturnProcessor {

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
    SalReturnPostingDispatcher postingDispatcher;

    @Inject
    ReturnRefundOrchestrator refundOrchestrator;

    public ErpSalReturn submit(Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        validateTransitionForSubmit(returnOrder, context);
        validateBusinessRulesForSubmit(returnOrder, context);
        doSubmit(returnOrder, context);
        return returnOrder;
    }

    public ErpSalReturn withdrawSubmit(Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        validateNotCancelled(returnOrder, context);
        validateTransitionForWithdraw(returnOrder, context);
        doWithdrawSubmit(returnOrder, context);
        return returnOrder;
    }

    public ErpSalReturn approve(Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        // 幂等：已审核单据再次审核为空操作（state-machine §4），库存移动单/凭证已存在，不重复触发。
        if (isAlreadyApproved(returnOrder)) {
            return returnOrder;
        }
        validateNotCancelled(returnOrder, context);
        validateTransitionForApprove(returnOrder, context);
        validateBusinessRulesForApprove(returnOrder, context);

        // 库存反向入库移动（物理正确性硬约束，失败抛异常回滚审核）。
        ErpInvStockMove move = triggerIncomingMove(returnOrder, context);
        // 跨域 generateMove 将移动单推进至 DONE 并更新库存余额；先刷盘使 DONE 状态与余额变动落地到当前事务的 DB 连接，
        // 避免后续 REQUIRES_NEW 过账（独立会话）挂起当前会话时丢失会话内暂存的 DONE 暂态（跨域会话交互）。
        ormTemplate.flushSession();

        // SALES_RETURN 过账（跨域 REQUIRES_NEW 由 Facade 承接，失败吞异常保持终态 posted=false）。
        boolean posted = triggerPosting(returnOrder, context);
        // 退款编排：已收款退货→反向收款核销行；未收款退货→负 AR 辅助账（过账时已生成）即回减应收，无需额外动作。
        refundOrchestrator.orchestrateRefund(returnOrder);

        // 跨域调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化。
        returnOrder = returnDao().getEntityById(returnId);
        doApprove(returnOrder, posted, context);
        return returnOrder;
    }

    public ErpSalReturn reject(Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        validateNotCancelled(returnOrder, context);
        validateTransitionForReject(returnOrder, context);
        doReject(returnOrder, context);
        return returnOrder;
    }

    public ErpSalReturn reverseApprove(Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        // 幂等：已 REJECTED（曾驳回或已反审核）无更多可冲销，直接返回。
        if (isAlreadyRejected(returnOrder)) {
            return returnOrder;
        }
        validateTransitionForReverseApprove(returnOrder, context);
        ensureReversed(returnOrder, context);
        returnOrder = returnDao().getEntityById(returnId);
        doReverseApprove(returnOrder, context);
        return returnOrder;
    }

    public ErpSalReturn cancel(Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        validateTransitionForCancel(returnOrder, context);
        if (isApproved(returnOrder)) {
            ensureReversed(returnOrder, context);
            returnOrder = returnDao().getEntityById(returnId);
        }
        doCancel(returnOrder, context);
        return returnOrder;
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForSubmit(ErpSalReturn returnOrder, IServiceContext context) {
        validateNotCancelled(returnOrder, context);
        String status = returnOrder.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpSalConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(returnOrder, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpSalReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpSalReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpSalReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpSalReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(returnOrder, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpSalReturn returnOrder, IServiceContext context) {
        String docStatus = returnOrder.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(returnOrder, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpSalReturn returnOrder, IServiceContext context) {
        requireLinesNonEmpty(returnOrder, context);
        requireCustomerActive(returnOrder, context);
    }

    protected void validateBusinessRulesForApprove(ErpSalReturn returnOrder, IServiceContext context) {
        requireCustomerActive(returnOrder, context);
        requireSourceDeliveryApproved(returnOrder, context);
        List<ErpSalReturnLine> lines = loadLines(returnOrder.getId());
        requireReasonIfConfigured(returnOrder, lines, context);
        returnQtyValidator.validate(returnOrder, lines);
    }

    /**
     * 源出库单须已审核通过（{@code returns.md §状态限制}：原出库单必须已审核）。
     */
    protected void requireSourceDeliveryApproved(ErpSalReturn returnOrder, IServiceContext context) {
        ErpSalDelivery delivery = returnOrder.getDelivery();
        if (delivery == null) {
            throw new NopException(ErpSalErrors.ERR_RETURN_DELIVERY_NOT_APPROVED)
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, null);
        }
        String deliveryStatus = delivery.getApproveStatus();
        if (deliveryStatus == null || !Objects.equals(deliveryStatus, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpSalErrors.ERR_RETURN_DELIVERY_NOT_APPROVED)
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, deliveryStatus);
        }
    }

    /**
     * 退货原因必填（按配置 {@code erp-sal.return-reason-required}，默认 true）。
     */
    protected void requireReasonIfConfigured(ErpSalReturn returnOrder, List<ErpSalReturnLine> lines,
                                             IServiceContext context) {
        if (!isReasonRequired()) {
            return;
        }
        for (ErpSalReturnLine line : lines) {
            if (line.getReason() == null || line.getReason().trim().isEmpty()) {
                throw new NopException(ErpSalErrors.ERR_RETURN_REASON_REQUIRED)
                        .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode())
                        .param(ErpSalErrors.ARG_LINE_NO, line.getLineNo());
            }
        }
    }

    protected boolean isReasonRequired() {
        return readBoolConfig(ErpSalConstants.CONFIG_RETURN_REASON_REQUIRED, true);
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

    // ---------- step：执行 ----------

    protected void doSubmit(ErpSalReturn returnOrder, IServiceContext context) {
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        returnDao().updateEntity(returnOrder);
    }

    protected void doWithdrawSubmit(ErpSalReturn returnOrder, IServiceContext context) {
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        returnDao().updateEntity(returnOrder);
    }

    protected void doApprove(ErpSalReturn returnOrder, boolean posted, IServiceContext context) {
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        returnOrder.setApprovedBy(currentUserId());
        returnOrder.setApprovedAt(CoreMetrics.currentDateTime());
        applyPosted(returnOrder, posted);
        returnDao().updateEntity(returnOrder);
    }

    protected void doReject(ErpSalReturn returnOrder, IServiceContext context) {
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        returnDao().updateEntity(returnOrder);
    }

    protected void doReverseApprove(ErpSalReturn returnOrder, IServiceContext context) {
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        returnDao().updateEntity(returnOrder);
    }

    protected void doCancel(ErpSalReturn returnOrder, IServiceContext context) {
        returnOrder.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        returnDao().updateEntity(returnOrder);
    }

    /** 将过账结果接线到退货单 posted 标志。 */
    protected void applyPosted(ErpSalReturn returnOrder, boolean posted) {
        if (posted) {
            returnOrder.setPosted(true);
            returnOrder.setPostedAt(CoreMetrics.currentDateTime());
            returnOrder.setPostedBy(currentUserId());
        }
    }

    // ---------- 库存触发 + 过账接线 + 冲销 ----------

    /**
     * 审核通过后构造 {@link StockMoveRequest}(INCOMING) 调 {@link IErpInvStockMoveBiz#generateMove}
     * （业务联动自动 DONE、幂等键 {@code (ERP_SAL_RETURN, return.code)}）。
     *
     * <p>追溯挂链：解析源出库单生成的出库移动单 id，透传 {@code originReturnedMoveId}，使退货移动单可反向追溯到原出库移动单。
     */
    protected ErpInvStockMove triggerIncomingMove(ErpSalReturn returnOrder, IServiceContext context) {
        List<ErpSalReturnLine> lines = loadLines(returnOrder.getId());
        StockMoveRequest request = stockMoveBuilder.build(returnOrder, lines, context);
        request.setOriginReturnedMoveId(resolveSourceDeliveryMoveId(returnOrder, context));
        return stockMoveBiz.generateMove(request, context);
    }

    /**
     * 解析源出库单（退货单 {@code delivery} 关系）生成的库存出库移动单 id。源出库单或其移动单缺失时返回 null
     * （不阻塞退货审核，仅追溯链缺失）。
     */
    protected Long resolveSourceDeliveryMoveId(ErpSalReturn returnOrder, IServiceContext context) {
        ErpSalDelivery delivery = returnOrder.getDelivery();
        if (delivery == null) {
            return null;
        }
        ErpInvStockMove sourceMove = stockMoveBiz.findByRelatedBill(
                ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, delivery.getCode(), context);
        return sourceMove == null ? null : sourceMove.getId();
    }

    /**
     * SALES_RETURN 过账（跨域 REQUIRES_NEW 由 Facade 承接，失败吞异常保持终态 posted=false）。
     */
    protected boolean triggerPosting(ErpSalReturn returnOrder, IServiceContext context) {
        return postingDispatcher.tryPost(returnOrder);
    }

    /**
     * 反审核/作废前的内部冲销：红字冲销已过账 SALES_RETURN 凭证（幂等）+ 反向出库移动冲减库存（幂等防双冲销）。
     */
    protected void ensureReversed(ErpSalReturn returnOrder, IServiceContext context) {
        reversePostingIfAny(returnOrder, context);
        ErpInvStockMove original = stockMoveBiz.findByRelatedBill(
                ErpSalConstants.RELATED_BILL_TYPE_SAL_RETURN, returnOrder.getCode(), context);
        if (original == null) {
            throw new NopException(ErpSalErrors.ERR_MOVE_NOT_FOUND)
                    .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode());
        }
        ErpInvStockMove existingReversal = stockMoveBiz.findByRelatedBill(
                ErpSalConstants.RELATED_BILL_TYPE_REVERSAL, original.getCode(), context);
        if (existingReversal != null) {
            return;
        }
        stockMoveBiz.reverse(original.getId(), context);
    }

    /**
     * 红字冲销已过账 SALES_RETURN 凭证（幂等：未过账则跳过）+ 退款红冲恢复占位。
     * posted=false 在会话脏跟踪实例上置位，由调用方 reload 后的 updateEntity 一并持久化。
     */
    protected void reversePostingIfAny(ErpSalReturn returnOrder, IServiceContext context) {
        if (Boolean.TRUE.equals(returnOrder.getPosted())) {
            postingDispatcher.reverse(returnOrder);
            ErpSalReturn reloaded = returnDao().getEntityById(returnOrder.getId());
            reloaded.setPosted(false);
            reloaded.setPostedAt(null);
            reloaded.setPostedBy(null);
        }
        refundOrchestrator.restoreRefund(returnOrder);
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpSalReturn requireReturn(Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = returnDao().getEntityById(returnId);
        if (returnOrder == null) {
            throw new NopException(ErpSalErrors.ERR_RETURN_NOT_FOUND)
                    .param(ErpSalErrors.ARG_RETURN_ID, returnId);
        }
        return returnOrder;
    }

    protected void validateNotCancelled(ErpSalReturn returnOrder, IServiceContext context) {
        String docStatus = returnOrder.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(returnOrder, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpSalReturn returnOrder) {
        String status = returnOrder.getApproveStatus();
        return status != null && Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isApproved(ErpSalReturn returnOrder) {
        return isAlreadyApproved(returnOrder);
    }

    protected boolean isAlreadyRejected(ErpSalReturn returnOrder) {
        String status = returnOrder.getApproveStatus();
        return status != null && Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED);
    }

    protected void requireLinesNonEmpty(ErpSalReturn returnOrder, IServiceContext context) {
        if (loadLines(returnOrder.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_RETURN_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode());
        }
    }

    protected void requireCustomerActive(ErpSalReturn returnOrder, IServiceContext context) {
        if (returnOrder.getCustomerId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(returnOrder.getCustomerId(), context);
        if (partner == null || partner.getStatus() == null
                || !Objects.equals(partner.getStatus(), ErpSalConstants.PARTNER_STATUS_ACTIVE)) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, returnOrder.getCustomerId());
        }
    }

    protected List<ErpSalReturnLine> loadLines(Long returnId) {
        // D2 边界场景：同聚合子表加载，父实体已授权，子行无独立权限规则。
        IEntityDao<ErpSalReturnLine> dao = daoProvider.daoFor(ErpSalReturnLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("returnId", returnId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpSalReturn> returnDao() {
        return daoProvider.daoFor(ErpSalReturn.class);
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

    protected NopException illegalTransition(ErpSalReturn returnOrder, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_RETURN_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpSalReturn returnOrder, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
