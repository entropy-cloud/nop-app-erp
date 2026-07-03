
package app.erp.qa.service.entity;

import app.erp.inv.biz.IErpInvBatchBiz;
import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.TraceChainResult;
import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.qa.biz.IErpQaRecallTargetBiz;
import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.dao.entity.ErpQaRecallTarget;
import app.erp.qa.service.ErpQaConfigs;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.orm.IOrmEntitySet;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 召回目标定位器：经库存批次追溯（{@link IErpInvStockMoveBiz#batchTrace}）反查受影响销售出库 → 客户/发货数量，
 * 生成 {@link ErpQaRecallTarget}（PENDING）。
 *
 * <p>定位算法（{@code docs/design/quality/recall.md §目标定位算法`}，以 batchTrace 为准）：
 * <ol>
 *   <li>追溯链开关 {@code erp-inv.trace-chain-enabled=false} → 抛 {@link ErpQaErrors#ERR_TRACE_CHAIN_DISABLED}。</li>
 *   <li>类型桥：{@code recall.batchId}(Long) → 经 {@link IErpInvBatchBiz} 解析 {@code batchNo}(String)。</li>
 *   <li>{@code batchTrace(batchNo)} 聚合全部相关移动单。</li>
 *   <li>筛选销售出库移动单（{@code moveType=OUTGOING + relatedBillType=SALES_DELIVERY + docStatus=DONE}）。</li>
 *   <li>按 {@code relatedBillCode}(出库单号) 反查出库单 → 客户(partnerId)/出库ID/发货数量。</li>
 * </ol>
 *
 * <p>serialNo 单件追溯本期不覆盖（batchTrace 按批次聚合）——属 Non-Goal，待单件追溯查询就绪补齐。
 */
public class RecallTargetLocator {

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;
    @Inject
    IErpInvBatchBiz batchBiz;
    @Nullable
    @Inject
    IErpSalDeliveryBiz deliveryBiz;
    @Inject
    IErpQaRecallTargetBiz recallTargetBiz;

    public void setStockMoveBiz(IErpInvStockMoveBiz stockMoveBiz) {
        this.stockMoveBiz = stockMoveBiz;
    }

    public void setBatchBiz(IErpInvBatchBiz batchBiz) {
        this.batchBiz = batchBiz;
    }

    public void setDeliveryBiz(IErpSalDeliveryBiz deliveryBiz) {
        this.deliveryBiz = deliveryBiz;
    }

    public void setRecallTargetBiz(IErpQaRecallTargetBiz recallTargetBiz) {
        this.recallTargetBiz = recallTargetBiz;
    }

    /**
     * 定位召回目标并持久化 PENDING 目标记录。返回生成的目标列表（recall→IN_PROGRESS 由调用方设置）。
     */
    public List<ErpQaRecallTarget> locate(ErpQaRecall recall, IServiceContext context) {
        if (!ErpQaConfigs.isTraceChainEnabled()) {
            throw new NopException(ErpQaErrors.ERR_TRACE_CHAIN_DISABLED)
                    .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode())
                    .param(ErpQaErrors.ARG_BATCH_NO, recall.getBatchId());
        }
        String batchNo = resolveBatchNo(recall, context);

        TraceChainResult result = stockMoveBiz.batchTrace(batchNo, context);
        List<ErpQaRecallTarget> targets = new ArrayList<>();
        if (result == null || result.getNodes() == null) {
            return targets;
        }
        for (ErpInvStockMove move : result.getNodes()) {
            if (!isSalesOutbound(move)) {
                continue;
            }
            ErpSalDelivery delivery = findDeliveryByCode(move.getRelatedBillCode(), context);
            if (delivery == null) {
                continue;
            }
            BigDecimal shippedQty = sumShippedQty(move, batchNo);

            ErpQaRecallTarget target = recallTargetBiz.newEntity();
            target.setRecallId(recall.getId());
            target.setPartnerId(delivery.getCustomerId());
            target.setBatchNo(batchNo);
            target.setSalesDeliveryId(delivery.getId());
            target.setShippedQty(shippedQty);
            target.setReturnStatus(ErpQaConstants.RECALL_TARGET_RETURN_PENDING);
            recallTargetBiz.saveEntity(target, null, context);
            targets.add(target);
        }
        return targets;
    }

    private boolean isSalesOutbound(ErpInvStockMove move) {
        Integer moveType = move.getMoveType();
        Integer docStatus = move.getDocStatus();
        return moveType != null && moveType == ErpQaConstants.INV_OPERATION_TYPE_OUTGOING
                && docStatus != null && docStatus == ErpQaConstants.INV_MOVE_STATUS_DONE
                && ErpQaConstants.RELATED_BILL_TYPE_SAL_DELIVERY_INV.equals(move.getRelatedBillType())
                && move.getRelatedBillCode() != null;
    }

    private String resolveBatchNo(ErpQaRecall recall, IServiceContext context) {
        Long batchId = recall.getBatchId();
        if (batchId == null) {
            throw new NopException(ErpQaErrors.ERR_RECALL_LOCATE_NO_BATCH)
                    .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode());
        }
        ErpInvBatch batch = batchBiz.get(String.valueOf(batchId), false, context);
        if (batch == null || batch.getBatchNo() == null) {
            throw new NopException(ErpQaErrors.ERR_RECALL_LOCATE_NO_BATCH)
                    .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode());
        }
        return batch.getBatchNo();
    }

    private ErpSalDelivery findDeliveryByCode(String code, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        return deliveryBiz.findFirst(q, null, context);
    }

    private BigDecimal sumShippedQty(ErpInvStockMove move, String batchNo) {
        BigDecimal sum = BigDecimal.ZERO;
        IOrmEntitySet<ErpInvStockMoveLine> lines = move.getLines();
        if (lines == null || lines.isEmpty()) {
            return sum;
        }
        for (ErpInvStockMoveLine line : lines) {
            if (batchNo == null || batchNo.equals(line.getBatchNo())) {
                BigDecimal qty = line.getQuantity();
                if (qty != null) {
                    sum = sum.add(qty);
                }
            }
        }
        return sum;
    }
}
