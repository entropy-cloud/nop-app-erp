
package app.erp.sal.service.entity;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售出库单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现三轴审批状态机（对齐
 * {@code docs/design/sales/state-machine.md}，与采购域镜像对称）+ 出库审核触发库存移动（对齐
 * {@code docs/design/inventory/cross-domain.md} 的 {@code generateMove} 调用方契约 + 销售独有可用量校验）。
 *
 * <ul>
 *   <li>审核轴：UNSUBMITTED→SUBMITTED→APPROVED/REJECTED，驳回→重提，反审核 APPROVED→REJECTED。</li>
 *   <li>单据轴：任意非终态→docStatus=CANCELLED（已 APPROVED 者须先冲销出库移动单）。</li>
 *   <li>{@link #approve} 通过后调 {@link IErpInvStockMoveBiz#generateMove}(OUTGOING)——可用量不足由库存域
 *       CONFIRM 内 {@code validateAvailable} 抛 {@link NopException} 致整个出库单审核回滚（销售独有，state-machine §4）；
 *       {@code delivery.posted = move.posted}，并回写源订单 {@code deliveryStatus}。</li>
 *   <li>{@link #reverseApprove}/{@link #cancel} 内部冲销已生成出库移动单（按 {@code (relatedBillType,relatedBillCode)}
 *       纯查询定位原单，按 {@code (REVERSAL,原单.code)} 幂等防双冲销）。</li>
 * </ul>
 *
 * <p>状态机迁移校验前置 {@code approveStatus}/{@code docStatus}，违反抛 {@link NopException}。
 * {@code @SingleSession}+{@code @Transactional} 提供 ORM Session 与事务边界（同 inventory/purchase 基线）。
 */
@BizModel("ErpSalDelivery")
public class ErpSalDeliveryBizModel extends CrudBizModel<ErpSalDelivery> implements IErpSalDeliveryBiz {

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Inject
    DeliveryStockMoveBuilder stockMoveBuilder;

    @Inject
    IOrmTemplate ormTemplate;

    public ErpSalDeliveryBizModel() {
        setEntityName(ErpSalDelivery.class.getName());
    }

    @SingleSession
    @Transactional
    @Override
    public ErpSalDelivery submit(Long deliveryId) {
        ErpSalDelivery delivery = requireDelivery(deliveryId);
        requireNotCancelled(delivery);
        Integer status = delivery.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (status != ErpSalConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpSalConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(delivery, status, "UNSUBMITTED 或 REJECTED");
        }
        requireLinesNonEmpty(delivery);
        requireCustomerActive(delivery);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(delivery);
        return delivery;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpSalDelivery withdrawSubmit(Long deliveryId) {
        ErpSalDelivery delivery = requireDelivery(deliveryId);
        requireNotCancelled(delivery);
        Integer status = delivery.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        dao().updateEntity(delivery);
        return delivery;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpSalDelivery approve(Long deliveryId) {
        ErpSalDelivery delivery = requireDelivery(deliveryId);
        Integer status = delivery.getApproveStatus();
        // 幂等：已审核单据再次审核为空操作（state-machine §4），出库移动单已存在，不重复触发。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_APPROVED) {
            return delivery;
        }
        requireNotCancelled(delivery);
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
        requireCustomerActive(delivery);

        ErpInvStockMove move = triggerOutgoingMove(delivery);
        // 跨域 generateMove 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化。
        delivery = dao().getEntityById(deliveryId);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        delivery.setApprovedBy(currentUserId());
        delivery.setApprovedAt(CoreMetrics.currentDateTime());
        applyPostingResult(delivery, move);
        dao().updateEntity(delivery);
        rollupOrderDeliveryStatus(delivery);
        return delivery;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpSalDelivery reject(Long deliveryId) {
        ErpSalDelivery delivery = requireDelivery(deliveryId);
        requireNotCancelled(delivery);
        Integer status = delivery.getApproveStatus();
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(delivery, status, "SUBMITTED");
        }
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(delivery);
        return delivery;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpSalDelivery reverseApprove(Long deliveryId) {
        ErpSalDelivery delivery = requireDelivery(deliveryId);
        Integer status = delivery.getApproveStatus();
        // 幂等：已 REJECTED（曾驳回或已反审核）无更多可冲销，直接返回。
        if (status != null && status == ErpSalConstants.APPROVE_STATUS_REJECTED) {
            return delivery;
        }
        if (status == null || status != ErpSalConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(delivery, status, "APPROVED");
        }
        ensureReversed(delivery);
        delivery = dao().getEntityById(deliveryId);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(delivery);
        return delivery;
    }

    @SingleSession
    @Transactional
    @Override
    public ErpSalDelivery cancel(Long deliveryId) {
        ErpSalDelivery delivery = requireDelivery(deliveryId);
        Integer docStatus = delivery.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(delivery, docStatus, "非已作废");
        }
        Integer approveStatus = delivery.getApproveStatus();
        if (approveStatus != null && approveStatus == ErpSalConstants.APPROVE_STATUS_APPROVED) {
            ensureReversed(delivery);
            delivery = dao().getEntityById(deliveryId);
        }
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(delivery);
        return delivery;
    }

    // ---------- Phase 2: 出库触发 + 过账接线 + 冲销 ----------

    /**
     * 审核通过后构造 {@link StockMoveRequest}(OUTGOING) 调 {@link IErpInvStockMoveBiz#generateMove}。
     * 出库类在库存域 CONFIRM 校验可用量（{@code availableQuantity ≥ quantity}），不足抛 {@link NopException}
     * 致整个出库单审核回滚（对齐 state-machine §4 销售独有 + cross-domain「不足拒绝+审核回滚」）。
     * 业务联动自动 DONE 后库存域内部触发存货过账（SALES_OUTPUT，借主营业务成本/贷存货），{@code billHeadCode}=移动单 code。
     */
    ErpInvStockMove triggerOutgoingMove(ErpSalDelivery delivery) {
        List<ErpSalDeliveryLine> lines = loadLines(delivery.getId());
        StockMoveRequest request = stockMoveBuilder.build(delivery, lines);
        return stockMoveBiz.generateMove(request);
    }

    /**
     * 将移动单过账结果接线到出库单：{@code delivery.posted = move.posted}、{@code postedAt}/{@code postedBy} 落地。
     * 存货估值过账（结转成本）由库存域独占（InvAcctDocProvider，SALES_OUTPUT 非默认 Provider），
     * sales 不注册过账 Provider（避免与库存域非默认声明冲突致 ERR_DUPLICATE_PROVIDER）。
     */
    private void applyPostingResult(ErpSalDelivery delivery, ErpInvStockMove move) {
        delivery.setPosted(Boolean.TRUE.equals(move.getPosted()));
        if (Boolean.TRUE.equals(delivery.getPosted())) {
            delivery.setPostedAt(CoreMetrics.currentDateTime());
            delivery.setPostedBy(currentUserId());
        }
    }

    /**
     * 反审核/作废前的内部冲销（Design A）：
     * <ol>
     *   <li>按 {@code (ERP_SAL_DELIVERY, delivery.code)} 纯查询定位原出库移动单（无 ORM FK，不改 model）；缺失抛
     *       {@link ErpSalErrors#ERR_MOVE_NOT_FOUND}（APPROVED 却无移动单为数据不一致）。</li>
     *   <li>按 {@code (REVERSAL, 原单.code)} 查是否已存在反向冲销移动单——已存在则跳过（幂等防双冲销）；
     *       不存在则调 {@link IErpInvStockMoveBiz#reverse}（库存域 DONE 时冲销流水/余额/红字凭证）。</li>
     * </ol>
     */
    void ensureReversed(ErpSalDelivery delivery) {
        ErpInvStockMove original = findMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, delivery.getCode());
        if (original == null) {
            throw new NopException(ErpSalErrors.ERR_MOVE_NOT_FOUND)
                    .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode());
        }
        ErpInvStockMove existingReversal = findMove(ErpSalConstants.RELATED_BILL_TYPE_REVERSAL, original.getCode());
        if (existingReversal != null) {
            return;
        }
        stockMoveBiz.reverse(original.getId());
    }

    /**
     * 回写源订单 {@code deliveryStatus}：按「累计已发 / 订单数量」按行比较（BigDecimal），全发清→DELIVERED、
     * 部分发→PARTIAL、未发→UNDELIVERED。当前出库单（正在审核）的行始终计入（避免依赖同事务内查询可见性）；
     * 其他已审核出库单经查询累加。
     */
    void rollupOrderDeliveryStatus(ErpSalDelivery currentDelivery) {
        Long orderId = currentDelivery.getOrderId();
        if (orderId == null) {
            return;
        }
        ErpSalOrder order = daoFor(ErpSalOrder.class).getEntityById(orderId);
        if (order == null) {
            return;
        }
        List<ErpSalOrderLine> orderLines = loadOrderLines(orderId);
        if (orderLines.isEmpty()) {
            return;
        }

        Map<Long, BigDecimal> deliveredByOrderLine = new HashMap<>();
        addLineQuantities(deliveredByOrderLine, loadLines(currentDelivery.getId()));
        for (ErpSalDelivery d : findApprovedDeliveries(orderId)) {
            if (d.getId().equals(currentDelivery.getId())) {
                continue;
            }
            addLineQuantities(deliveredByOrderLine, loadLines(d.getId()));
        }

        boolean anyDelivered = false;
        boolean allFullyDelivered = true;
        for (ErpSalOrderLine ol : orderLines) {
            BigDecimal ordered = ol.getQuantity() == null ? BigDecimal.ZERO : ol.getQuantity();
            BigDecimal delivered = deliveredByOrderLine.getOrDefault(ol.getId(), BigDecimal.ZERO);
            if (delivered.signum() > 0) {
                anyDelivered = true;
            }
            if (delivered.compareTo(ordered) < 0) {
                allFullyDelivered = false;
            }
        }
        int rolled;
        if (allFullyDelivered) {
            rolled = ErpSalConstants.DELIVERY_STATUS_DELIVERED;
        } else if (anyDelivered) {
            rolled = ErpSalConstants.DELIVERY_STATUS_PARTIAL;
        } else {
            rolled = ErpSalConstants.DELIVERY_STATUS_UNDELIVERED;
        }
        order.setDeliveryStatus(rolled);
        daoFor(ErpSalOrder.class).updateEntity(order);
    }

    private void addLineQuantities(Map<Long, BigDecimal> map, List<ErpSalDeliveryLine> lines) {
        for (ErpSalDeliveryLine dl : lines) {
            if (dl.getOrderLineId() == null) {
                continue;
            }
            BigDecimal qty = dl.getQuantity() == null ? BigDecimal.ZERO : dl.getQuantity();
            map.merge(dl.getOrderLineId(), qty, BigDecimal::add);
        }
    }

    private List<ErpSalDelivery> findApprovedDeliveries(Long orderId) {
        ormTemplate.flushSession();
        IEntityDao<ErpSalDelivery> deliveryDao = dao();
        QueryBean rq = new QueryBean();
        rq.addFilter(and(eq("orderId", orderId), eq("approveStatus", ErpSalConstants.APPROVE_STATUS_APPROVED)));
        return deliveryDao.findAllByQuery(rq);
    }

    // ---------- validation helpers ----------

    private ErpSalDelivery requireDelivery(Long deliveryId) {
        ErpSalDelivery delivery = dao().getEntityById(deliveryId);
        if (delivery == null) {
            throw new NopException(ErpSalErrors.ERR_DELIVERY_NOT_FOUND)
                    .param(ErpSalErrors.ARG_DELIVERY_ID, deliveryId);
        }
        return delivery;
    }

    private void requireNotCancelled(ErpSalDelivery delivery) {
        Integer docStatus = delivery.getDocStatus();
        if (docStatus != null && docStatus == ErpSalConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(delivery, docStatus, "非已作废");
        }
    }

    private void requireLinesNonEmpty(ErpSalDelivery delivery) {
        if (loadLines(delivery.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_DELIVERY_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode());
        }
    }

    private void requireCustomerActive(ErpSalDelivery delivery) {
        if (delivery.getCustomerId() == null) {
            return;
        }
        // 客户启用校验为纯实体读（master-data 机制 B，无跨域 I*Biz 业务逻辑），故用 daoFor 直接加载。
        ErpMdPartner partner = daoFor(ErpMdPartner.class).getEntityById(delivery.getCustomerId());
        if (partner == null || partner.getStatus() == null
                || partner.getStatus() != ErpSalConstants.PARTNER_STATUS_ACTIVE) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, delivery.getCustomerId());
        }
    }

    // ---------- query helpers ----------

    List<ErpSalDeliveryLine> loadLines(Long deliveryId) {
        IEntityDao<ErpSalDeliveryLine> dao = daoFor(ErpSalDeliveryLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("deliveryId", deliveryId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    private List<ErpSalOrderLine> loadOrderLines(Long orderId) {
        IEntityDao<ErpSalOrderLine> dao = daoFor(ErpSalOrderLine.class);
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

    private NopException illegalTransition(ErpSalDelivery delivery, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpSalDelivery delivery, Integer current, String expected) {
        return new NopException(ErpSalErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_DELIVERY_CODE, delivery.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
