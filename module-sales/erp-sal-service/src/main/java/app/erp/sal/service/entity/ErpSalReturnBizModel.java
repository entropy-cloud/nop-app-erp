
package app.erp.sal.service.entity;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.IErpSalReturnBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.sal.service.posting.SalReturnPostingDispatcher;
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
 * 销售退货单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机（对齐
 * {@code docs/design/sales/returns.md §退货单状态机} 与 {@code state-machine.md}；复用现有
 * {@code docStatus}+{@code approveStatus} 两轴，不新增 returnStatus 字段——见 plan Task Route Decision）
 * + 退货审核触发库存反向入库移动（{@code IErpInvStockMoveBiz.generateMove}，{@code relatedBillType=SAL_RETURN}，库存增加）
 * + SALES_RETURN 过账（反向 SALES_OUTPUT：借库存商品/贷主营业务成本，{@code posted=true}）。
 *
 * <p>跨实体访问对齐 {@code ai-defaults.md}：客户启用校验经 {@link IErpMdPartnerBiz}；源出库单经退货单
 * {@code delivery} 关系 getter；库存反向入库经 {@link IErpInvStockMoveBiz}；过账经
 * {@code SalReturnPostingDispatcher} →凭证聚合根 Facade {@code IErpFinVoucherBiz}。
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 */
@BizModel("ErpSalReturn")
public class ErpSalReturnBizModel extends CrudBizModel<ErpSalReturn> implements IErpSalReturnBiz {

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

    @Inject
    IOrmTemplate ormTemplate;

    public ErpSalReturnBizModel() {
        setEntityName(ErpSalReturn.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalReturn submit(@Name("returnId") Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        requireNotCancelled(returnOrder);
        Integer status = returnOrder.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpSalConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpSalConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(returnOrder, status, "UNSUBMITTED 或 REJECTED");
        }
        requireLinesNonEmpty(returnOrder);
        requireCustomerActive(returnOrder, context);
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    @Override
    @BizMutation
    public ErpSalReturn withdrawSubmit(@Name("returnId") Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        requireNotCancelled(returnOrder);
        Integer status = returnOrder.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    @Override
    @BizMutation
    public ErpSalReturn approve(@Name("returnId") Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        Integer status = returnOrder.getApproveStatus();
        // 幂等：已审核单据再次审核为空操作（state-machine §4），库存移动单/凭证已存在，不重复触发。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_APPROVED) {
            return returnOrder;
        }
        requireNotCancelled(returnOrder);
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
        requireCustomerActive(returnOrder, context);
        requireSourceDeliveryApproved(returnOrder);
        List<ErpSalReturnLine> lines = loadLines(returnOrder.getId());
        requireReasonIfConfigured(returnOrder, lines);
        returnQtyValidator.validate(returnOrder, lines);

        // 库存反向入库移动（物理正确性硬约束，失败抛异常回滚审核，对齐 1132-2 出库触发模式）。
        ErpInvStockMove move = triggerIncomingMove(returnOrder, lines, context);
        // 跨域 generateMove 将移动单推进至 DONE 并更新库存余额；先刷盘使 DONE 状态与余额变动落地到当前事务的 DB 连接，
        // 避免后续 REQUIRES_NEW 过账（独立会话）挂起当前会话时丢失会话内暂存的 DONE 暂态（跨域会话交互，对齐 lessons 经验）。
        ormTemplate.flushSession();

        // SALES_RETURN 过账（跨域 REQUIRES_NEW 由 Facade 承接，失败吞异常保持终态 posted=false）。
        boolean posted = triggerPosting(returnOrder);

        // 退款编排：已收款退货→反向收款核销行（回写发票 receivedStatus/receivedAmount、收款 writtenOffStatus）；
        // 未收款退货→负 AR 辅助账（过账时已生成）即回减应收，无需额外动作。
        refundOrchestrator.orchestrateRefund(returnOrder);

        // 跨域调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化（对齐 ErpSalDeliveryBizModel.approve）。
        returnOrder = dao().getEntityById(returnId);
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        returnOrder.setApprovedBy(currentUserId());
        returnOrder.setApprovedAt(CoreMetrics.currentDateTime());
        applyPosted(returnOrder, posted);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    @Override
    @BizMutation
    public ErpSalReturn reject(@Name("returnId") Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        requireNotCancelled(returnOrder);
        Integer status = returnOrder.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(returnOrder, status, "SUBMITTED");
        }
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    @Override
    @BizMutation
    public ErpSalReturn reverseApprove(@Name("returnId") Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        Integer status = returnOrder.getApproveStatus();
        // 幂等：已 REJECTED（曾驳回或已反审核）无更多可冲销，直接返回。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_REJECTED) {
            return returnOrder;
        }
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(returnOrder, status, "APPROVED");
        }
        ensureReversed(returnOrder, context);
        returnOrder = dao().getEntityById(returnId);
        returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    @Override
    @BizMutation
    public ErpSalReturn cancel(@Name("returnId") Long returnId, IServiceContext context) {
        ErpSalReturn returnOrder = requireReturn(returnId, context);
        Integer docStatus = returnOrder.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(returnOrder, docStatus, "非已作废");
        }
        Integer approveStatus = returnOrder.getApproveStatus();
        if (approveStatus != null && approveStatus == ErpSalConstants.APPROVE_STATUS_APPROVED) {
            ensureReversed(returnOrder, context);
            returnOrder = dao().getEntityById(returnId);
        }
        returnOrder.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(returnOrder);
        return returnOrder;
    }

    // ---------- 库存触发 + 冲销 ----------

    /**
     * 审核通过后构造 {@link StockMoveRequest}(INCOMING) 调 {@link IErpInvStockMoveBiz#generateMove}（业务联动自动
     * DONE、幂等键 {@code (ERP_SAL_RETURN, return.code)}），返回生成的入库移动单。库存余额随 DONE 增加。
     *
     * <p>追溯挂链：解析源出库单（{@code delivery}）生成的出库移动单 id，透传
     * {@code originReturnedMoveId}，使退货移动单可反向追溯到原出库移动单（解除 0456-2 Deferred「追溯」语义）。
     */
    ErpInvStockMove triggerIncomingMove(ErpSalReturn returnOrder, List<ErpSalReturnLine> lines,
                                        IServiceContext context) {
        StockMoveRequest request = stockMoveBuilder.build(returnOrder, lines, context);
        request.setOriginReturnedMoveId(resolveSourceDeliveryMoveId(returnOrder, context));
        return stockMoveBiz.generateMove(request, context);
    }

    /**
     * 解析源出库单（退货单 {@code delivery} 关系）生成的库存出库移动单 id（经
     * {@code (ERP_SAL_DELIVERY, delivery.code)} 反查）。供退货移动单挂退货追溯上链 {@code originReturnedMoveId}。
     * 源出库单或其移动单缺失时返回 null（不阻塞退货审核，仅追溯链缺失）。
     */
    private Long resolveSourceDeliveryMoveId(ErpSalReturn returnOrder, IServiceContext context) {
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
     * 过账成功后由辅助账生成器产出 DIRECTION_RECEIVABLE 负 openAmount 项（credit memo）→ receivableBalance 回减。
     */
    boolean triggerPosting(ErpSalReturn returnOrder) {
        return postingDispatcher.tryPost(returnOrder);
    }

    /** 将过账结果接线到退货单 posted 标志。 */
    private void applyPosted(ErpSalReturn returnOrder, boolean posted) {
        if (posted) {
            returnOrder.setPosted(true);
            returnOrder.setPostedAt(CoreMetrics.currentDateTime());
            returnOrder.setPostedBy(currentUserId());
        }
    }

    /**
     * 反审核/作废前的内部冲销（对齐 ErpSalDeliveryBizModel.ensureReversed 模式）：
     * <ol>
     *   <li>红字冲销已过账 SALES_RETURN 凭证（幂等：未过账则跳过）。</li>
     *   <li>按 {@code (ERP_SAL_RETURN, return.code)} 反查入库移动单；缺失抛
     *       {@link ErpSalErrors#ERR_MOVE_NOT_FOUND}（APPROVED 却无移动单为数据不一致）。</li>
     *   <li>按 {@code (REVERSAL, 原单.code)} 查是否已存在反向冲销移动单——已存在则跳过（幂等防双冲销）；
     *       不存在则调 {@link IErpInvStockMoveBiz#reverse}（生成反向出库移动冲减库存）。</li>
     * </ol>
     */
    void ensureReversed(ErpSalReturn returnOrder, IServiceContext context) {
        reversePostingIfAny(returnOrder);
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
     * 红冲同事务内由 finance cancelOnReverse 取消退货自身的负 AR 辅助账，receivableBalance 恢复。
     */
    void reversePostingIfAny(ErpSalReturn returnOrder) {
        if (Boolean.TRUE.equals(returnOrder.getPosted())) {
            postingDispatcher.reverse(returnOrder);
            returnOrder = dao().getEntityById(returnOrder.getId());
            returnOrder.setPosted(false);
            returnOrder.setPostedAt(null);
            returnOrder.setPostedBy(null);
        }
        refundOrchestrator.restoreRefund(returnOrder);
    }

    // ---------- validation helpers ----------

    private ErpSalReturn requireReturn(Long returnId, IServiceContext context) {
        return requireEntity(String.valueOf(returnId), null, context);
    }

    private void requireNotCancelled(ErpSalReturn returnOrder) {
        Integer docStatus = returnOrder.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(returnOrder, docStatus, "非已作废");
        }
    }

    private void requireLinesNonEmpty(ErpSalReturn returnOrder) {
        if (loadLines(returnOrder.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_RETURN_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode());
        }
    }

    private void requireCustomerActive(ErpSalReturn returnOrder, IServiceContext context) {
        if (returnOrder.getCustomerId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(returnOrder.getCustomerId(), context);
        if (partner == null || partner.getStatus() == null
                || partner.getStatus() != ErpSalConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, returnOrder.getCustomerId());
        }
    }

    /**
     * 源出库单须已审核通过（{@code returns.md §状态限制}：原出库单必须已审核）。
     */
    private void requireSourceDeliveryApproved(ErpSalReturn returnOrder) {
        ErpSalDelivery delivery = returnOrder.getDelivery();
        if (delivery == null) {
            throw new NopException(ErpSalErrors.ERR_RETURN_DELIVERY_NOT_APPROVED)
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, null);
        }
        Integer deliveryStatus = delivery.getApproveStatus();
        if (deliveryStatus == null || deliveryStatus != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw new NopException(ErpSalErrors.ERR_RETURN_DELIVERY_NOT_APPROVED)
                    .param(ErpSalErrors.ARG_CURRENT_STATUS, deliveryStatus);
        }
    }

    /**
     * 退货原因必填（按配置 {@code erp-sal.return-reason-required}，默认 true，{@code returns.md §配置项}）。
     */
    private void requireReasonIfConfigured(ErpSalReturn returnOrder, List<ErpSalReturnLine> lines) {
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

    private boolean isReasonRequired() {
        return readBoolConfig(ErpSalConstants.CONFIG_RETURN_REASON_REQUIRED, true);
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

    List<ErpSalReturnLine> loadLines(Long returnId) {
        IEntityDao<ErpSalReturnLine> dao = daoFor(ErpSalReturnLine.class);
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

    private NopException illegalTransition(ErpSalReturn returnOrder, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_RETURN_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpSalReturn returnOrder, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_RETURN_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
