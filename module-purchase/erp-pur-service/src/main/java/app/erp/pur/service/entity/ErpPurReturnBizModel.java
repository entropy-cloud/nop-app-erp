
package app.erp.pur.service.entity;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.biz.IErpPurReturnBiz;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.dao.entity.ErpPurReturnLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.posting.PurReturnPostingDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.dao.api.IEntityDao;
import io.nop.core.context.IServiceContext;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购退货单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机（对齐
 * {@code docs/design/purchase/returns.md §退货单状态机} 与 {@code state-machine.md}；复用现有
 * {@code docStatus}+{@code approveStatus} 两轴，不新增 returnStatus 字段——见 plan Task Route Decision）
 * + 退货审核触发库存反向出库移动（{@code IErpInvStockMoveBiz.generateMove}，{@code relatedBillType=PUR_RETURN}）
 * + PURCHASE_RETURN 过账（红字冲减暂估应付/存货，{@code posted=true}）。
 *
 * <p>跨实体访问对齐 {@code ai-defaults.md}：供应商启用校验经 {@link IErpMdPartnerBiz}；源入库单经退货单
 * {@code receive} 关系 getter；库存反向出库经 {@link IErpInvStockMoveBiz}；过账经 {@link PurReturnPostingDispatcher}
 * →凭证聚合根 Facade {@code IErpFinVoucherBiz}。
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 */
@BizModel("ErpPurReturn")
public class ErpPurReturnBizModel extends CrudBizModel<ErpPurReturn> implements IErpPurReturnBiz {

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
    IOrmTemplate ormTemplate;

    public ErpPurReturnBizModel() {
        setEntityName(ErpPurReturn.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurReturn submit(@Name("returnId") Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        requireNotCancelled(returnOrder);
        Integer status = returnOrder.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpPurConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpPurConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(returnOrder, status, "UNSUBMITTED 或 REJECTED");
        }
        requireLinesNonEmpty(returnOrder);
        requireSupplierActive(returnOrder, context);
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    @Override
    @BizMutation
    public ErpPurReturn withdrawSubmit(@Name("returnId") Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        requireNotCancelled(returnOrder);
        Integer status = returnOrder.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    @Override
    @BizMutation
    public ErpPurReturn approve(@Name("returnId") Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        Integer status = returnOrder.getApproveStatus();
        // 幂等：已审核单据再次审核为空操作（state-machine §4），库存移动单/凭证已存在，不重复触发。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_APPROVED) {
            return returnOrder;
        }
        requireNotCancelled(returnOrder);
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
        requireSupplierActive(returnOrder, context);
        requireSourceReceiveApproved(returnOrder);
        List<ErpPurReturnLine> lines = loadLines(returnOrder.getId());
        requireReasonIfConfigured(returnOrder, lines);
        returnQtyValidator.validate(returnOrder, lines);

        // 库存反向出库移动（物理正确性硬约束，失败抛异常回滚审核，对齐 1132-1 入库触发模式）。
        ErpInvStockMove move = triggerOutgoingMove(returnOrder, lines, context);
        // 跨域 generateMove 将移动单推进至 DONE 并更新库存余额；先刷盘使 DONE 状态与余额变动落地到当前事务的 DB 连接，
        // 避免后续 REQUIRES_NEW 过账（独立会话）挂起当前会话时丢失会话内暂存的 DONE 暂态（跨域会话交互，对齐 lessons 经验）。
        ormTemplate.flushSession();

        // PURCHASE_RETURN 过账（跨域 REQUIRES_NEW 由 Facade 承接，失败吞异常保持终态 posted=false）。
        boolean posted = postingDispatcher.tryPost(returnOrder);

        // 跨域调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化（对齐 ErpPurReceiveBizModel.approve）。
        returnOrder = dao().getEntityById(returnId);
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        returnOrder.setApprovedBy(currentUserId());
        returnOrder.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            returnOrder.setPosted(true);
            returnOrder.setPostedAt(CoreMetrics.currentDateTime());
            returnOrder.setPostedBy(currentUserId());
        }
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    @Override
    @BizMutation
    public ErpPurReturn reject(@Name("returnId") Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        requireNotCancelled(returnOrder);
        Integer status = returnOrder.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    @Override
    @BizMutation
    public ErpPurReturn reverseApprove(@Name("returnId") Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        Integer status = returnOrder.getApproveStatus();
        // 幂等：已 REJECTED（曾驳回或已反审核）无更多可冲销，直接返回。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_REJECTED) {
            return returnOrder;
        }
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(returnOrder, status, "APPROVED");
        }
        ensureReversed(returnOrder, context);
        returnOrder = dao().getEntityById(returnId);
        returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    @Override
    @BizMutation
    public ErpPurReturn cancel(@Name("returnId") Long returnId, IServiceContext context) {
        ErpPurReturn returnOrder = requireReturn(returnId, context);
        Integer docStatus = returnOrder.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(returnOrder, docStatus, "非已作废");
        }
        Integer approveStatus = returnOrder.getApproveStatus();
        if (approveStatus != null && approveStatus == ErpPurConstants.APPROVE_STATUS_APPROVED) {
            ensureReversed(returnOrder, context);
            returnOrder = dao().getEntityById(returnId);
        }
        returnOrder.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    // ---------- 库存触发 + 冲销 ----------

    /**
     * 审核通过后构造 {@link StockMoveRequest}(OUTGOING) 调 {@link IErpInvStockMoveBiz#generateMove}（业务联动自动
     * DONE、幂等键 {@code (ERP_PUR_RETURN, return.code)}），返回生成的出库移动单。库存余额随 DONE 减少。
     */
    ErpInvStockMove triggerOutgoingMove(ErpPurReturn returnOrder, List<ErpPurReturnLine> lines,
                                        IServiceContext context) {
        StockMoveRequest request = stockMoveBuilder.build(returnOrder, lines, context);
        return stockMoveBiz.generateMove(request, context);
    }

    /**
     * 反审核/作废前的内部冲销（对齐 ErpPurReceiveBizModel.ensureReversal 模式）：
     * <ol>
     *   <li>红字冲销已过账 PURCHASE_RETURN 凭证（幂等：未过账则跳过）。</li>
     *   <li>按 {@code (ERP_PUR_RETURN, return.code)} 反查出库移动单；缺失抛
     *       {@link ErpPurErrors#ERR_MOVE_NOT_FOUND}（APPROVED 却无移动单为数据不一致）。</li>
     *   <li>按 {@code (REVERSAL, 原单.code)} 查是否已存在反向冲销移动单——已存在则跳过（幂等防双冲销）；
     *       不存在则调 {@link IErpInvStockMoveBiz#reverse}（生成反向入库移动恢复库存）。</li>
     * </ol>
     */
    void ensureReversed(ErpPurReturn returnOrder, IServiceContext context) {
        if (Boolean.TRUE.equals(returnOrder.getPosted())) {
            postingDispatcher.reverse(returnOrder);
            returnOrder = dao().getEntityById(returnOrder.getId());
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

    // ---------- validation helpers ----------

    private ErpPurReturn requireReturn(Long returnId, IServiceContext context) {
        return requireEntity(String.valueOf(returnId), null, context);
    }

    private void requireNotCancelled(ErpPurReturn returnOrder) {
        Integer docStatus = returnOrder.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(returnOrder, docStatus, "非已作废");
        }
    }

    private void requireLinesNonEmpty(ErpPurReturn returnOrder) {
        if (loadLines(returnOrder.getId()).isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_RETURN_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_RETURN_CODE, returnOrder.getCode());
        }
    }

    private void requireSupplierActive(ErpPurReturn returnOrder, IServiceContext context) {
        if (returnOrder.getSupplierId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(returnOrder.getSupplierId(), context);
        if (partner == null || partner.getStatus() == null
                || partner.getStatus() != ErpPurConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpPurErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpPurErrors.ARG_SUPPLIER_ID, returnOrder.getSupplierId());
        }
    }

    /**
     * 源入库单须已审核通过（{@code returns.md §状态限制}：原入库单必须已审核）。
     */
    private void requireSourceReceiveApproved(ErpPurReturn returnOrder) {
        ErpPurReceive receive = returnOrder.getReceive();
        if (receive == null) {
            throw new NopException(ErpPurErrors.ERR_RETURN_RECEIVE_NOT_APPROVED)
                    .param(ErpPurErrors.ARG_CURRENT_STATUS, null);
        }
        Integer receiveStatus = receive.getApproveStatus();
        if (receiveStatus == null || receiveStatus != ErpPurConstants.APPROVE_STATUS_APPROVED) {
            throw new NopException(ErpPurErrors.ERR_RETURN_RECEIVE_NOT_APPROVED)
                    .param(ErpPurErrors.ARG_CURRENT_STATUS, receiveStatus);
        }
    }

    /**
     * 退货原因必填（按配置 {@code erp-pur.return-reason-required}，默认 true，{@code returns.md §配置项}）。
     */
    private void requireReasonIfConfigured(ErpPurReturn returnOrder, List<ErpPurReturnLine> lines) {
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

    private boolean isReasonRequired() {
        return readBoolConfig(ErpPurConstants.CONFIG_RETURN_REASON_REQUIRED, true);
    }

    private boolean readBoolConfig(String key, boolean defaultValue) {
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

    // ---------- query helpers ----------

    List<ErpPurReturnLine> loadLines(Long returnId) {
        IEntityDao<ErpPurReturnLine> dao = daoFor(ErpPurReturnLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("returnId", returnId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- misc helpers ----------

    private String currentUserId() {
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

    private NopException illegalTransition(ErpPurReturn returnOrder, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_RETURN_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RETURN_CODE, returnOrder.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpPurReturn returnOrder, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RETURN_CODE, returnOrder.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
