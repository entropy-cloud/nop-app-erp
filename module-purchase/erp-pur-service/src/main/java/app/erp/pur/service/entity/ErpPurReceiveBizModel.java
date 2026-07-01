
package app.erp.pur.service.entity;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.biz.IErpPurReceiveBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.annotations.core.Name;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购入库单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机（对齐
 * {@code docs/design/purchase/state-machine.md}）+ 入库审核触发库存移动（对齐
 * {@code docs/design/inventory/cross-domain.md} 的 {@code generateMove} 调用方契约）。
 *
 * <ul>
 *   <li>审核轴：UNSUBMITTED→SUBMITTED→APPROVED/REJECTED，驳回→重提，反审核 APPROVED→REJECTED。</li>
 *   <li>单据轴：任意非终态→docStatus=CANCELLED（已 APPROVED 者须先冲销入库移动单）。</li>
 *   <li>{@link #approve} 通过后调 {@link IErpInvStockMoveBiz#generateMove} 生成入库移动单（业务联动自动 DONE、幂等），
 *       {@code receive.posted = move.posted}，并回写 {@code receiveStatus} 与源订单收货进度。</li>
 *   <li>{@link #reverseApprove}/{@link #cancel} 内部冲销已生成入库移动单（按 {@code (relatedBillType,relatedBillCode)}
 *       纯查询定位原单，按 {@code (REVERSAL,原单.code)} 幂等防双冲销）。</li>
 * </ul>
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 * {@code @SingleSession}+{@code @Transactional} 提供 ORM Session 与事务边界（同 inventory 基线）。
 */
@BizModel("ErpPurReceive")
public class ErpPurReceiveBizModel extends CrudBizModel<ErpPurReceive> implements IErpPurReceiveBiz {

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Inject
    ReceiveStockMoveBuilder stockMoveBuilder;

    @Inject
    IOrmTemplate ormTemplate;

    public ErpPurReceiveBizModel() {
        setEntityName(ErpPurReceive.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurReceive submit(@Name("receiveId") Long receiveId, IServiceContext context) {
        ErpPurReceive receive = requireReceive(receiveId);
        requireNotCancelled(receive);
        Integer status = receive.getApproveStatus();
        if (status == null) {
            status = ErpPurConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpPurConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpPurConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(receive, status, "UNSUBMITTED 或 REJECTED");
        }
        requireLinesNonEmpty(receive);
        requireSupplierActive(receive);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(receive);
        return receive;
    }

    @Override
    @BizMutation
    public ErpPurReceive withdrawSubmit(@Name("receiveId") Long receiveId, IServiceContext context) {
        ErpPurReceive receive = requireReceive(receiveId);
        requireNotCancelled(receive);
        Integer status = receive.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(receive, status, "SUBMITTED");
        }
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(receive);
        return receive;
    }

    @Override
    @BizMutation
    public ErpPurReceive approve(@Name("receiveId") Long receiveId, IServiceContext context) {
        ErpPurReceive receive = requireReceive(receiveId);
        Integer status = receive.getApproveStatus();
        // 幂等：已审核单据再次审核为空操作（state-machine §4），库存移动单已存在，不重复触发。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_APPROVED) {
            return receive;
        }
        requireNotCancelled(receive);
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(receive, status, "SUBMITTED");
        }
        requireSupplierActive(receive);

        ErpInvStockMove move = triggerIncomingMove(receive, context);
        // 跨域 generateMove 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化。
        receive = dao().getEntityById(receiveId);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        receive.setApprovedBy(currentUserId());
        receive.setApprovedAt(CoreMetrics.currentDateTime());
        applyPostingResult(receive, move);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_RECEIVED);
        dao().updateEntity(receive);
        rollupOrderReceiveStatus(receive);
        return receive;
    }

    @Override
    @BizMutation
    public ErpPurReceive reject(@Name("receiveId") Long receiveId, IServiceContext context) {
        ErpPurReceive receive = requireReceive(receiveId);
        requireNotCancelled(receive);
        Integer status = receive.getApproveStatus();
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(receive, status, "SUBMITTED");
        }
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(receive);
        return receive;
    }

    @Override
    @BizMutation
    public ErpPurReceive reverseApprove(@Name("receiveId") Long receiveId, IServiceContext context) {
        ErpPurReceive receive = requireReceive(receiveId);
        Integer status = receive.getApproveStatus();
        // 幂等：已 REJECTED（曾驳回或已反审核）无更多可冲销，直接返回。
        if (status != null && status == ErpPurConstants.APPROVE_STATUS_REJECTED) {
            return receive;
        }
        if (status == null || status != ErpPurConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(receive, status, "APPROVED");
        }
        ensureReversed(receive, context);
        receive = dao().getEntityById(receiveId);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(receive);
        return receive;
    }

    @Override
    @BizMutation
    public ErpPurReceive cancel(@Name("receiveId") Long receiveId, IServiceContext context) {
        ErpPurReceive receive = requireReceive(receiveId);
        Integer docStatus = receive.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(receive, docStatus, "非已作废");
        }
        Integer approveStatus = receive.getApproveStatus();
        if (approveStatus != null && approveStatus == ErpPurConstants.APPROVE_STATUS_APPROVED) {
            ensureReversed(receive, context);
            receive = dao().getEntityById(receiveId);
        }
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(receive);
        return receive;
    }

    private void flush() {
        ormTemplate.flushSession();
    }

    // ---------- Phase 2: 库存触发 + 过账接线 + 冲销 ----------

    /**
     * 审核通过后构造 {@link StockMoveRequest}(INCOMING) 调 {@link IErpInvStockMoveBiz#generateMove}（业务联动自动 DONE、幂等），
     * 返回生成的入库移动单。移动单 DONE 后库存域内部触发存货过账（PURCHASE_INPUT），{@code billHeadCode}=移动单 code。
     */
    ErpInvStockMove triggerIncomingMove(ErpPurReceive receive, IServiceContext context) {
        List<ErpPurReceiveLine> lines = loadLines(receive.getId());
        StockMoveRequest request = stockMoveBuilder.build(receive, lines);
        return stockMoveBiz.generateMove(request, context);
    }

    /**
     * 将移动单过账结果接线到入库单：{@code receive.posted = move.posted}、{@code postedAt}/{@code postedBy} 落地。
     * 存货估值过账由库存域独占（InvAcctDocProvider，PURCHASE_INPUT 非 Provider 冲突），purchase 不注册过账 Provider。
     */
    private void applyPostingResult(ErpPurReceive receive, ErpInvStockMove move) {
        receive.setPosted(Boolean.TRUE.equals(move.getPosted()));
        if (Boolean.TRUE.equals(receive.getPosted())) {
            receive.setPostedAt(CoreMetrics.currentDateTime());
            receive.setPostedBy(currentUserId());
        }
    }

    /**
     * 反审核/作废前的内部冲销（Design A）：
     * <ol>
     *   <li>按 {@code (ERP_PUR_RECEIVE, receive.code)} 纯查询定位原入库移动单（无 ORM FK，不改 model）；缺失抛
     *       {@link ErpPurErrors#ERR_MOVE_NOT_FOUND}（APPROVED 却无移动单为数据不一致）。</li>
     *   <li>按 {@code (REVERSAL, 原单.code)} 查是否已存在反向冲销移动单——已存在则跳过（幂等防双冲销）；
     *       不存在则调 {@link IErpInvStockMoveBiz#reverse}（库存域 DONE 时冲销流水/余额/红字凭证）。</li>
     * </ol>
     */
    void ensureReversed(ErpPurReceive receive, IServiceContext context) {
        ErpInvStockMove original = findMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receive.getCode());
        if (original == null) {
            throw new NopException(ErpPurErrors.ERR_MOVE_NOT_FOUND)
                    .param(ErpPurErrors.ARG_RECEIVE_CODE, receive.getCode());
        }
        ErpInvStockMove existingReversal = findMove(ErpPurConstants.RELATED_BILL_TYPE_REVERSAL, original.getCode());
        if (existingReversal != null) {
            return;
        }
        stockMoveBiz.reverse(original.getId(), context);
    }

    /**
     * 回写源订单 {@code receiveStatus}：按「累计已收 / 订单数量」按行比较（BigDecimal），全收清→RECEIVED、
     * 部分收→PARTIAL、未收→UNRECEIVED。当前入库单（正在审核）的行始终计入（避免依赖同事务内查询可见性）；
     * 其他已审核入库单经查询累加。
     */
    void rollupOrderReceiveStatus(ErpPurReceive currentReceive) {
        Long orderId = currentReceive.getOrderId();
        if (orderId == null) {
            return;
        }
        ErpPurOrder order = daoFor(ErpPurOrder.class).getEntityById(orderId);
        if (order == null) {
            return;
        }
        List<ErpPurOrderLine> orderLines = loadOrderLines(orderId);
        if (orderLines.isEmpty()) {
            return;
        }

        Map<Long, BigDecimal> receivedByOrderLine = new HashMap<>();
        addLineQuantities(receivedByOrderLine, loadLines(currentReceive.getId()));
        for (ErpPurReceive r : findApprovedReceives(orderId)) {
            if (r.getId().equals(currentReceive.getId())) {
                continue;
            }
            addLineQuantities(receivedByOrderLine, loadLines(r.getId()));
        }

        boolean anyReceived = false;
        boolean allFullyReceived = true;
        for (ErpPurOrderLine ol : orderLines) {
            BigDecimal ordered = ol.getQuantity() == null ? BigDecimal.ZERO : ol.getQuantity();
            BigDecimal received = receivedByOrderLine.getOrDefault(ol.getId(), BigDecimal.ZERO);
            if (received.signum() > 0) {
                anyReceived = true;
            }
            if (received.compareTo(ordered) < 0) {
                allFullyReceived = false;
            }
        }
        int rolled;
        if (allFullyReceived) {
            rolled = ErpPurConstants.RECEIVE_STATUS_RECEIVED;
        } else if (anyReceived) {
            rolled = ErpPurConstants.RECEIVE_STATUS_PARTIAL;
        } else {
            rolled = ErpPurConstants.RECEIVE_STATUS_UNRECEIVED;
        }
        order.setReceiveStatus(rolled);
        daoFor(ErpPurOrder.class).updateEntity(order);
    }

    private void addLineQuantities(Map<Long, BigDecimal> map, List<ErpPurReceiveLine> lines) {
        for (ErpPurReceiveLine rl : lines) {
            if (rl.getOrderLineId() == null) {
                continue;
            }
            BigDecimal qty = rl.getQuantity() == null ? BigDecimal.ZERO : rl.getQuantity();
            map.merge(rl.getOrderLineId(), qty, BigDecimal::add);
        }
    }

    private List<ErpPurReceive> findApprovedReceives(Long orderId) {
        ormTemplate.flushSession();
        IEntityDao<ErpPurReceive> receiveDao = dao();
        QueryBean rq = new QueryBean();
        rq.addFilter(and(eq("orderId", orderId), eq("approveStatus", ErpPurConstants.APPROVE_STATUS_APPROVED)));
        return receiveDao.findAllByQuery(rq);
    }

    // ---------- validation helpers ----------

    private ErpPurReceive requireReceive(Long receiveId) {
        ErpPurReceive receive = dao().getEntityById(receiveId);
        if (receive == null) {
            throw new NopException(ErpPurErrors.ERR_RECEIVE_NOT_FOUND)
                    .param(ErpPurErrors.ARG_RECEIVE_ID, receiveId);
        }
        return receive;
    }

    private void requireNotCancelled(ErpPurReceive receive) {
        Integer docStatus = receive.getDocStatus();
        if (docStatus != null && docStatus == ErpPurConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(receive, docStatus, "非已作废");
        }
    }

    private void requireLinesNonEmpty(ErpPurReceive receive) {
        if (loadLines(receive.getId()).isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_RECEIVE_LINES_EMPTY)
                    .param(ErpPurErrors.ARG_RECEIVE_CODE, receive.getCode());
        }
    }

    private void requireSupplierActive(ErpPurReceive receive) {
        if (receive.getSupplierId() == null) {
            return;
        }
        // 供应商启用校验为纯实体读（master-data 机制 B，无跨域 I*Biz 业务逻辑），故用 daoFor 直接加载。
        ErpMdPartner partner = daoFor(ErpMdPartner.class).getEntityById(receive.getSupplierId());
        if (partner == null || partner.getStatus() == null
                || partner.getStatus() != ErpPurConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpPurErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpPurErrors.ARG_SUPPLIER_ID, receive.getSupplierId());
        }
    }

    // ---------- query helpers ----------

    List<ErpPurReceiveLine> loadLines(Long receiveId) {
        IEntityDao<ErpPurReceiveLine> dao = daoFor(ErpPurReceiveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("receiveId", receiveId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    private List<ErpPurOrderLine> loadOrderLines(Long orderId) {
        IEntityDao<ErpPurOrderLine> dao = daoFor(ErpPurOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    private ErpInvStockMove findMove(String relatedBillType, String relatedBillCode) {
        ormTemplate.flushSession();
        IEntityDao<ErpInvStockMove> dao = daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("relatedBillType", relatedBillType), eq("relatedBillCode", relatedBillCode)));
        List<ErpInvStockMove> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
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

    private NopException illegalTransition(ErpPurReceive receive, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RECEIVE_CODE, receive.getCode())
                .param(ErpPurErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpPurReceive receive, Integer current, String expected) {
        return new NopException(ErpPurErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RECEIVE_CODE, receive.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
