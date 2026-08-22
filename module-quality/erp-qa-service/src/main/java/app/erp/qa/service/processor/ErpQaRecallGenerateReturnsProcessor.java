package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.dao.entity.ErpQaRecallTarget;
import app.erp.qa.service.ErpQaConstants;
import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.biz.IErpSalReturnBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalReturn;
import io.nop.api.core.convert.ConvertHelper;
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

    public ErpQaRecall generateReturns(String recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_IN_PROGRESS, "IN_PROGRESS");
        for (ErpQaRecallTarget target : loadTargets(recallId, null, context)) {
            if (target.getReturnStatus() != null
                    && Objects.equals(target.getReturnStatus(), ErpQaConstants.RECALL_TARGET_RETURN_RETURNED)) {
                continue;
            }
            ErpSalReturn salReturn = createSalesReturnFor(recall, target, context);
            // bridge-main-100/102: sal return.id 仍 Long（sal 未迁移），qa target 已 String——sal 翻转时退役（owner M2.6）
            target.setGeneratedReturnId(ConvertHelper.toString(salReturn.getId()));
            target.setReturnStatus(ErpQaConstants.RECALL_TARGET_RETURN_RETURNED);
            recallTargetBiz.updateEntity(target, null, context);
        }
        return recall;
    }

    @SuppressWarnings("unchecked")
    private ErpSalReturn createSalesReturnFor(ErpQaRecall recall, ErpQaRecallTarget target, IServiceContext context) {
        // bridge-main-101: 平台 ICrudBiz.get(String id) 签名本身为 String，qa target.salesDeliveryId 直传
        ErpSalDelivery delivery = target.getSalesDeliveryId() == null ? null
                : salDeliveryBiz.get(target.getSalesDeliveryId(), false, context);
        Long warehouseId = delivery != null ? delivery.getWarehouseId() : null;
        Long currencyId = delivery != null ? delivery.getCurrencyId() : null;
        // bridge-main-099: qa recall.materialId 已 String，sal deliveryLine.materialId 仍 Long——比较前转 Long（owner M2.6）
        Long uoMId = pickUoMId(delivery, ConvertHelper.toLong(recall.getMaterialId()));

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("lineNo", 1);
        // bridge-main-099: qa String materialId → sal Long 列（owner M2.6）
        line.put("materialId", ConvertHelper.toLong(recall.getMaterialId()));
        line.put("uoMId", uoMId);
        line.put("quantity", target.getShippedQty() != null ? target.getShippedQty() : BigDecimal.ZERO);
        line.put("reason", "recall:" + recall.getCode());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "RMA-" + recall.getCode() + "-" + target.getId());
        // bridge-main-098: qa target.partnerId/salesDeliveryId 已 String，sal customerId/deliveryId 仍 Long（owner M2.6）
        data.put("customerId", ConvertHelper.toLong(target.getPartnerId()));
        data.put("deliveryId", ConvertHelper.toLong(target.getSalesDeliveryId()));
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
