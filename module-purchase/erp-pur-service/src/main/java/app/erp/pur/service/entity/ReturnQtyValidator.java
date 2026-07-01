package app.erp.pur.service.entity;

import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.dao.entity.ErpPurReturnLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
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
 * 退货数量上限校验器。每行 {@code quantity} ≤ 对应 {@link ErpPurReceiveLine} 已入库量 − 该入库行
 * <b>已审核</b>退货行 SUM（聚合查询 {@code ErpPurReturnLine} 关联 {@code ErpPurReturn} 过滤
 * {@code approveStatus=APPROVED}，排除当前退货单）。
 *
 * <p>权威：{@code docs/design/purchase/returns.md §退货数量限制}（超额退货：拒绝，提示最大可退数量）。
 *
 * <p>Decision（见 plan Phase 1）：选择聚合查询（无 ORM 变更，保持 implementation-only），不在
 * {@code ErpPurReceiveLine} 加 {@code returnedQuantity} 列。残留风险：跨退货单并发超额由退货单自身
 * {@code version} 乐观锁 + 审核时重查聚合兜底。
 */
public class ReturnQtyValidator {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 校验退货单所有行数量上限。超限抛 {@link ErpPurErrors#ERR_RETURN_QTY_EXCEED}（提示最大可退量）。
     *
     * @param returnOrder 当前退货单（审核中）
     * @param lines       当前退货单行
     */
    public void validate(ErpPurReturn returnOrder, List<ErpPurReturnLine> lines) {
        Map<Long, BigDecimal> approvedReturned = sumApprovedReturnedByReceiveLine(returnOrder);
        for (ErpPurReturnLine line : lines) {
            Long receiveLineId = line.getReceiveLineId();
            if (receiveLineId == null) {
                continue;
            }
            BigDecimal received = loadReceivedQuantity(receiveLineId);
            BigDecimal alreadyReturned = approvedReturned.getOrDefault(receiveLineId, BigDecimal.ZERO);
            BigDecimal maxReturnable = received.subtract(alreadyReturned);
            BigDecimal qty = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
            if (qty.compareTo(maxReturnable) > 0) {
                throw new NopException(ErpPurErrors.ERR_RETURN_QTY_EXCEED)
                        .param(ErpPurErrors.ARG_RETURN_CODE, returnOrder.getCode())
                        .param(ErpPurErrors.ARG_LINE_NO, line.getLineNo())
                        .param(ErpPurErrors.ARG_INVOICE_QTY, qty)
                        .param(ErpPurErrors.ARG_MAX_RETURN_QTY, maxReturnable)
                        .param(ErpPurErrors.ARG_RECEIVED_QTY_RETURN, received);
            }
        }
    }

    /**
     * 按入库行聚合「已审核退货量」，排除当前退货单（避免审核自身时把自身行算进上限）。
     * 范围限定在同一源入库单（{@code receiveId}）下，缩小查询面。
     */
    private Map<Long, BigDecimal> sumApprovedReturnedByReceiveLine(ErpPurReturn current) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (current.getReceiveId() == null) {
            return result;
        }
        IEntityDao<ErpPurReturn> returnDao = daoProvider.daoFor(ErpPurReturn.class);
        QueryBean rq = new QueryBean();
        rq.addFilter(and(
                eq("receiveId", current.getReceiveId()),
                eq("approveStatus", ErpPurConstants.APPROVE_STATUS_APPROVED),
                current.getId() != null ? ne("id", current.getId()) : eq("id", null)));
        List<ErpPurReturn> approvedReturns = returnDao.findAllByQuery(rq);
        if (approvedReturns.isEmpty()) {
            return result;
        }
        List<Long> returnIds = approvedReturns.stream().map(ErpPurReturn::getId)
                .collect(java.util.stream.Collectors.toList());

        IEntityDao<ErpPurReturnLine> lineDao = daoProvider.daoFor(ErpPurReturnLine.class);
        QueryBean lq = new QueryBean();
        lq.addFilter(in("returnId", returnIds));
        for (ErpPurReturnLine rl : lineDao.findAllByQuery(lq)) {
            if (rl.getReceiveLineId() == null) {
                continue;
            }
            BigDecimal qty = rl.getQuantity() == null ? BigDecimal.ZERO : rl.getQuantity();
            result.merge(rl.getReceiveLineId(), qty, BigDecimal::add);
        }
        return result;
    }

    private BigDecimal loadReceivedQuantity(Long receiveLineId) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine receiveLine = dao.getEntityById(receiveLineId);
        if (receiveLine == null || receiveLine.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return receiveLine.getQuantity();
    }
}
