package app.erp.pur.service.processor;

import app.erp.fin.biz.IErpFinBudgetCommitmentBiz;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.dao.entity.ErpPurReturnLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.common.service.SoDGuard;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购退货单审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 */
public class ErpPurReturnProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpPurReturnProcessor.class);

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

    @Inject
    IErpFinBudgetCommitmentBiz budgetCommitmentBiz;

    @Inject
    ErpPurReturnSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpPurReturnApproveProcessor approveProcessor;

    @Inject
    ErpPurReturnRejectProcessor rejectProcessor;

    @Inject
    ErpPurReturnReverseApproveProcessor reverseApproveProcessor;

    @Inject
    ErpPurReturnWithdrawApprovalProcessor withdrawApprovalProcessor;

    @Inject
    ErpPurReturnCancelProcessor cancelProcessor;

    public ErpPurReturn submitForApproval(String id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpPurReturn withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpPurReturn approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    public ErpPurReturn reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    public ErpPurReturn reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    public ErpPurReturn cancel(String id, IServiceContext context) {
        return cancelProcessor.cancel(id, context);
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
            throw illegalTransition(returnOrder, status, ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForApprove(ErpPurReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(returnOrder, status, ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReject(ErpPurReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(returnOrder, status, ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReverseApprove(ErpPurReturn returnOrder, IServiceContext context) {
        String status = returnOrder.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(returnOrder, status, ErpPurConstants.APPROVE_STATUS_APPROVED);
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
        List<ErpPurReturnLine> lines = loadLines(returnOrder);
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
        SoDGuard.assertApproverNotCreator(returnOrder.getCreatedBy(), currentUserId(), ErpPurErrors.ERR_PUR_APPROVER_IS_CREATOR);
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        returnOrder.setApprovedBy(currentUserId());
        returnOrder.setApprovedAt(CoreMetrics.currentTimestamp());
        if (posted) {
            returnOrder.setPosted(true);
            returnOrder.setPostedAt(CoreMetrics.currentTimestamp());
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
        returnOrder.setApprovedBy(null);
        returnOrder.setApprovedAt(null);
        returnDao().updateEntity(returnOrder);
    }

    protected void doCancel(ErpPurReturn returnOrder, IServiceContext context) {
        returnOrder.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        returnDao().updateEntity(returnOrder);
    }

    // ---------- 库存触发 + 冲销 ----------

    protected ErpInvStockMove triggerOutgoingMove(ErpPurReturn returnOrder, IServiceContext context) {
        List<ErpPurReturnLine> lines = loadLines(returnOrder);
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

    // ---------- 承付释放（release-on-return, budget.md §承付会计 §3 接入点 #4） ----------

    /**
     * 采购退货审核后置承付释放钩子（P1-MA2-082，budget.md §承付会计 §3 接入点 #4 release-on-return）。
     *
     * <p>退货减少实际采购量 → 同步释放原 PO 承付凭证（红冲 COMMITMENT）。config-gated
     * （{@code erp-fin.commitment-release-on-return} 默认 false）+ 依赖承付总开关
     * {@code erp-fin.budget-commitment-enabled}；调 {@link IErpFinBudgetCommitmentBiz#releaseIfPresent}
     * 容错（无原承付凭证静默跳过，对齐 release-on-cancel 范式 budget.md §release hook 容错对称性）。
     *
     * <p><b>全额释放语义</b>：{@code releaseIfPresent} 走 {@code reverseCommitment} 全额红冲（无 amount 入参），
     * 故<b>部分退货亦全额红冲整张 PO 承付</b>——剩余未开票数量失去承付保护，可能允许超预算放行新订单。
     * 按比例部分释放归 successor（与 P1-MA2-081 方案B 同 successor）。声明见 budget.md §3。
     */
    protected void runCommitmentReleaseOnReturnHook(ErpPurReturn returnOrder, IServiceContext context) {
        if (!Boolean.TRUE.equals(AppConfig.var(
                ErpFinConstants.CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN, Boolean.FALSE))) {
            return;
        }
        String poCode = resolvePurchaseOrderCode(returnOrder);
        if (poCode == null || poCode.isEmpty()) {
            return;
        }
        try {
            budgetCommitmentBiz.releaseIfPresent(
                    ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, poCode, context);
        } catch (NopException e) {
            // 容错：无原承付凭证静默跳过，不阻断退货审核流（对齐 release-on-cancel 容错范式）
            LOG.debug("commitment release-on-return skipped for return {}: {}", returnOrder.getCode(), e.getMessage());
        }
    }

    /** 经 return.receiveId → receive.orderId → order.code 解析源采购订单 code（无法解析返回 null）。 */
    protected String resolvePurchaseOrderCode(ErpPurReturn returnOrder) {
        ErpPurReceive receive = returnOrder.getReceive();
        if (receive == null) {
            return null;
        }
        ErpPurOrder order = receive.getOrder();
        return order == null ? null : order.getCode();
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpPurReturn requireReturn(String id, IServiceContext context) {
        ErpPurReturn returnOrder = returnDao().getEntityById(id);
        if (returnOrder == null) {
            throw new NopException(ErpPurErrors.ERR_RETURN_NOT_FOUND)
                    .param(ErpPurErrors.ARG_RETURN_ID, id);
        }
        return returnOrder;
    }

    protected void validateNotCancelled(ErpPurReturn returnOrder, IServiceContext context) {
        if (returnOrder.isCancelled()) {
            throw illegalDocTransition(returnOrder, returnOrder.getDocStatus(), "非已作废");
        }
    }

    protected void requireLinesNonEmpty(ErpPurReturn returnOrder, IServiceContext context) {
        if (returnOrder.getLines().isEmpty()) {
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

    /**
     * 通过 ORM to-many 关系 {@code ErpPurReturn.lines} 加载行（懒加载，复用主实体 session）。
     * 关系已在 {@code app-erp-purchase.orm.xml} 声明。
     */
    protected List<ErpPurReturnLine> loadLines(ErpPurReturn returnOrder) {
        return new ArrayList<>(returnOrder.getLines());
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
