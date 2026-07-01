package app.erp.sal.service.entity;

import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 退货数量上限校验器。每行 {@code quantity} ≤ 对应 {@link ErpSalDeliveryLine} 已出库量 − 该出库行
 * <b>已审核</b>退货行 SUM（聚合查询 {@code ErpSalReturnLine} 关联 {@code ErpSalReturn} 过滤
 * {@code approveStatus=APPROVED}，排除当前退货单）。
 *
 * <p>权威：{@code docs/design/sales/returns.md §退货数量限制}（超额退货：拒绝，提示最大可退数量）。
 *
 * <p>Decision（见 plan Phase 1）：选择聚合查询（无 ORM 变更，保持 implementation-only），不在
 * {@code ErpSalDeliveryLine} 加 {@code returnedQuantity} 列。残留风险：跨退货单并发超额由退货单自身
 * {@code version} 乐观锁 + 审核时重查聚合兜底。
 */
public class ReturnQtyValidator {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 校验退货单所有行数量上限。超限抛 {@link ErpSalErrors#ERR_RETURN_QTY_EXCEED}（提示最大可退量）。
     *
     * @param returnOrder 当前退货单（审核中）
     * @param lines       当前退货单行
     */
    public void validate(ErpSalReturn returnOrder, List<ErpSalReturnLine> lines) {
        Map<Long, BigDecimal> approvedReturned = sumApprovedReturnedByDeliveryLine(returnOrder);
        for (ErpSalReturnLine line : lines) {
            Long deliveryLineId = line.getDeliveryLineId();
            if (deliveryLineId == null) {
                continue;
            }
            BigDecimal delivered = loadDeliveredQuantity(deliveryLineId);
            BigDecimal alreadyReturned = approvedReturned.getOrDefault(deliveryLineId, BigDecimal.ZERO);
            BigDecimal maxReturnable = delivered.subtract(alreadyReturned);
            BigDecimal qty = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
            if (qty.compareTo(maxReturnable) > 0) {
                throw new NopException(ErpSalErrors.ERR_RETURN_QTY_EXCEED)
                        .param(ErpSalErrors.ARG_RETURN_CODE, returnOrder.getCode())
                        .param(ErpSalErrors.ARG_LINE_NO, line.getLineNo())
                        .param(ErpSalErrors.ARG_RETURN_QTY, qty)
                        .param(ErpSalErrors.ARG_MAX_RETURN_QTY, maxReturnable)
                        .param(ErpSalErrors.ARG_DELIVERED_QTY_RETURN, delivered);
            }
        }
    }

    /**
     * 按出库行聚合「已审核退货量」，排除当前退货单（避免审核自身时把自身行算进上限）。
     * 范围限定在同一源出库单（{@code deliveryId}）下，缩小查询面。
     */
    private Map<Long, BigDecimal> sumApprovedReturnedByDeliveryLine(ErpSalReturn current) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (current.getDeliveryId() == null) {
            return result;
        }
        IEntityDao<ErpSalReturn> returnDao = daoProvider.daoFor(ErpSalReturn.class);
        QueryBean rq = new QueryBean();
        rq.addFilter(and(
                eq("deliveryId", current.getDeliveryId()),
                eq("approveStatus", ErpSalConstants.APPROVE_STATUS_APPROVED),
                current.getId() != null ? ne("id", current.getId()) : eq("id", null)));
        List<ErpSalReturn> approvedReturns = returnDao.findAllByQuery(rq);
        if (approvedReturns.isEmpty()) {
            return result;
        }
        List<Long> returnIds = approvedReturns.stream().map(ErpSalReturn::getId)
                .collect(java.util.stream.Collectors.toList());

        IEntityDao<ErpSalReturnLine> lineDao = daoProvider.daoFor(ErpSalReturnLine.class);
        QueryBean lq = new QueryBean();
        lq.addFilter(in("returnId", returnIds));
        for (ErpSalReturnLine rl : lineDao.findAllByQuery(lq)) {
            if (rl.getDeliveryLineId() == null) {
                continue;
            }
            BigDecimal qty = rl.getQuantity() == null ? BigDecimal.ZERO : rl.getQuantity();
            result.merge(rl.getDeliveryLineId(), qty, BigDecimal::add);
        }
        return result;
    }

    private BigDecimal loadDeliveredQuantity(Long deliveryLineId) {
        IEntityDao<ErpSalDeliveryLine> dao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        ErpSalDeliveryLine deliveryLine = dao.getEntityById(deliveryLineId);
        if (deliveryLine == null || deliveryLine.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return deliveryLine.getQuantity();
    }
}
