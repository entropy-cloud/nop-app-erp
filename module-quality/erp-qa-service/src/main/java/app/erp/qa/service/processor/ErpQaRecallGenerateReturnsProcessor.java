package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.dao.entity.ErpQaRecallTarget;
import app.erp.qa.service.ErpQaConstants;
import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.biz.IErpSalReturnBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalReturn;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.orm.IOrmEntitySet;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ErpQaRecall generateReturns per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含批量退货生成编排：逐未退货 target 调销售退货域创建 RMA 退货单（quality→sales 写触发）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaRecallProcessor}。
 */
public class ErpQaRecallGenerateReturnsProcessor extends AbstractErpQaRecallProcessor {

    @Nullable
    @Inject
    IErpSalReturnBiz salReturnBiz;
    @Nullable
    @Inject
    IErpSalDeliveryBiz salDeliveryBiz;

    public ErpQaRecall generateReturns(Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_IN_PROGRESS, "IN_PROGRESS");
        for (ErpQaRecallTarget target : loadTargets(recallId, null, context)) {
            if (target.getReturnStatus() != null
                    && Objects.equals(target.getReturnStatus(), ErpQaConstants.RECALL_TARGET_RETURN_RETURNED)) {
                continue;
            }
            ErpSalReturn salReturn = createSalesReturnFor(recall, target, context);
            target.setGeneratedReturnId(salReturn.getId());
            target.setReturnStatus(ErpQaConstants.RECALL_TARGET_RETURN_RETURNED);
            recallTargetBiz.updateEntity(target, null, context);
        }
        return recall;
    }

    @SuppressWarnings("unchecked")
    private ErpSalReturn createSalesReturnFor(ErpQaRecall recall, ErpQaRecallTarget target, IServiceContext context) {
        ErpSalDelivery delivery = target.getSalesDeliveryId() == null ? null
                : salDeliveryBiz.get(String.valueOf(target.getSalesDeliveryId()), false, context);
        Long warehouseId = delivery != null ? delivery.getWarehouseId() : null;
        Long currencyId = delivery != null ? delivery.getCurrencyId() : null;
        Long uoMId = pickUoMId(delivery, recall.getMaterialId());

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("lineNo", 1);
        line.put("materialId", recall.getMaterialId());
        line.put("uoMId", uoMId);
        line.put("quantity", target.getShippedQty() != null ? target.getShippedQty() : BigDecimal.ZERO);
        line.put("reason", "recall:" + recall.getCode());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "RMA-" + recall.getCode() + "-" + target.getId());
        data.put("customerId", target.getPartnerId());
        data.put("deliveryId", target.getSalesDeliveryId());
        data.put("warehouseId", warehouseId);
        data.put("currencyId", currencyId);
        data.put("businessDate", CoreMetrics.today().toString());
        data.put("docStatus", ErpQaConstants.SAL_DOC_STATUS_DRAFT);
        data.put("approveStatus", ErpQaConstants.SAL_APPROVE_STATUS_UNSUBMITTED);
        data.put("lines", java.util.Collections.singletonList(line));
        return salReturnBiz.save(data, context);
    }

    private Long pickUoMId(ErpSalDelivery delivery, Long materialId) {
        if (delivery == null) {
            return null;
        }
        IOrmEntitySet<ErpSalDeliveryLine> lines = delivery.getLines();
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        Long firstUoMId = null;
        for (ErpSalDeliveryLine line : lines) {
            if (firstUoMId == null) {
                firstUoMId = line.getUoMId();
            }
            if (materialId != null && materialId.equals(line.getMaterialId())) {
                return line.getUoMId();
            }
        }
        return firstUoMId;
    }
}
