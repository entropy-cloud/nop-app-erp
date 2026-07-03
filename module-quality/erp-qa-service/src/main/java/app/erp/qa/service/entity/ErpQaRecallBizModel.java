
package app.erp.qa.service.entity;

import app.erp.qa.biz.IErpQaRecallBiz;
import app.erp.qa.biz.IErpQaRecallTargetBiz;
import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.dao.entity.ErpQaRecallTarget;
import app.erp.qa.service.ErpQaConfigs;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.biz.IErpSalReturnBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalReturn;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 召回事件 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现召回 5 态状态机
 * （{@code docs/design/quality/recall.md §召回状态机`}）。
 *
 * <p>迁移：register（→OPEN）、submit（→SUBMITTED）、approve（→APPROVED，强制审批）、
 * reject（→CANCELLED）、cancel（非终态→CANCELLED）、close（IN_PROGRESS→CLOSED，门控通知）。
 * 非法迁移抛 {@link ErpQaErrors#ERR_INVALID_RECALL_STATUS_TRANSITION}。
 *
 * <p>目标定位（locateTargets）/客户通知（notifyCustomers）/批量退货（generateReturns）
 * 编排由 Phase 2 引入（跨域 quality→inventory 只读 + quality→sales 写触发）。
 *
 * <p>{@link #close} 门控经 {@code erp-qua.recall-notify-required-to-close}（默认 true）：
 * 全部 target {@code returnStatus≠PENDING} 且 {@code notifyCustomer=true} 方可 CLOSED。
 */
@BizModel("ErpQaRecall")
public class ErpQaRecallBizModel extends CrudBizModel<ErpQaRecall> implements IErpQaRecallBiz {

    @Inject
    IErpQaRecallTargetBiz recallTargetBiz;
    @Inject
    RecallTargetLocator targetLocator;
    @Nullable
    @Inject
    IErpSalReturnBiz salReturnBiz;
    @Nullable
    @Inject
    IErpSalDeliveryBiz salDeliveryBiz;

    public ErpQaRecallBizModel() {
        setEntityName(ErpQaRecall.class.getName());
    }

    public void setRecallTargetBiz(IErpQaRecallTargetBiz recallTargetBiz) {
        this.recallTargetBiz = recallTargetBiz;
    }

    public void setTargetLocator(RecallTargetLocator targetLocator) {
        this.targetLocator = targetLocator;
    }

    public void setSalReturnBiz(IErpSalReturnBiz salReturnBiz) {
        this.salReturnBiz = salReturnBiz;
    }

    public void setSalDeliveryBiz(IErpSalDeliveryBiz salDeliveryBiz) {
        this.salDeliveryBiz = salDeliveryBiz;
    }

    @Override
    @BizMutation
    public ErpQaRecall register(@Name("data") Map<String, Object> data, IServiceContext context) {
        ErpQaRecall recall = newEntity();
        if (data != null) {
            applyRecallFields(recall, data);
        }
        recall.setStatus(ErpQaConstants.RECALL_STATUS_OPEN);
        recall.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
        recall.setNotifyCustomer(Boolean.FALSE);
        if (recall.getBusinessDate() == null) {
            recall.setBusinessDate(LocalDate.now());
        }
        saveEntity(recall, null, context);
        return recall;
    }

    @Override
    @BizMutation
    public ErpQaRecall submit(@Name("recallId") Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_OPEN, "OPEN");
        requireApproveStatus(recall, ErpQaConstants.APPROVE_STATUS_UNSUBMITTED, "UNSUBMITTED");
        recall.setApproveStatus(ErpQaConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(recall);
        return recall;
    }

    @Override
    @BizMutation
    public ErpQaRecall approve(@Name("recallId") Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_OPEN, "OPEN");
        // 强制审批：配置开启时须先 submit（approveStatus=SUBMITTED）
        if (ErpQaConfigs.isRecallRequireApproval()
                && (recall.getApproveStatus() == null
                || recall.getApproveStatus() != ErpQaConstants.APPROVE_STATUS_SUBMITTED)) {
            throw new NopException(ErpQaErrors.ERR_RECALL_APPROVAL_REQUIRED)
                    .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode());
        }
        recall.setStatus(ErpQaConstants.RECALL_STATUS_APPROVED);
        recall.setApproveStatus(ErpQaConstants.APPROVE_STATUS_APPROVED);
        recall.setApprovedAt(LocalDateTime.now());
        recall.setApprovedBy(context.getUserId());
        dao().updateEntity(recall);
        return recall;
    }

    @Override
    @BizMutation
    public ErpQaRecall reject(@Name("recallId") Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireApproveStatus(recall, ErpQaConstants.APPROVE_STATUS_SUBMITTED, "SUBMITTED");
        recall.setStatus(ErpQaConstants.RECALL_STATUS_CANCELLED);
        recall.setApproveStatus(ErpQaConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(recall);
        return recall;
    }

    @Override
    @BizMutation
    public ErpQaRecall cancel(@Name("recallId") Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        Integer current = recall.getStatus();
        if (current == null || (current != ErpQaConstants.RECALL_STATUS_OPEN
                && current != ErpQaConstants.RECALL_STATUS_APPROVED
                && current != ErpQaConstants.RECALL_STATUS_IN_PROGRESS)) {
            throw illegalRecallTransition(recall, current, "OPEN 或 APPROVED 或 IN_PROGRESS");
        }
        recall.setStatus(ErpQaConstants.RECALL_STATUS_CANCELLED);
        dao().updateEntity(recall);
        return recall;
    }

    @Override
    @BizMutation
    public ErpQaRecall locateTargets(@Name("recallId") Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_APPROVED, "APPROVED");
        targetLocator.locate(recall, context);
        recall.setStatus(ErpQaConstants.RECALL_STATUS_IN_PROGRESS);
        dao().updateEntity(recall);
        return recall;
    }

    @Override
    @BizMutation
    public ErpQaRecall notifyCustomers(@Name("recallId") Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_IN_PROGRESS, "IN_PROGRESS");
        for (ErpQaRecallTarget target : loadTargets(recallId, null, context)) {
            target.setNotifiedAt(LocalDateTime.now());
            target.setNotifiedBy(context.getUserId());
            target.setReturnStatus(ErpQaConstants.RECALL_TARGET_RETURN_NOTIFIED);
            recallTargetBiz.updateEntity(target, null, context);
        }
        recall.setNotifyCustomer(Boolean.TRUE);
        dao().updateEntity(recall);
        return recall;
    }

    @Override
    @BizMutation
    public ErpQaRecall generateReturns(@Name("recallId") Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_IN_PROGRESS, "IN_PROGRESS");
        for (ErpQaRecallTarget target : loadTargets(recallId, null, context)) {
            if (target.getReturnStatus() != null
                    && target.getReturnStatus() == ErpQaConstants.RECALL_TARGET_RETURN_RETURNED) {
                continue;
            }
            ErpSalReturn salReturn = createSalesReturnFor(recall, target, context);
            target.setGeneratedReturnId(salReturn.getId());
            target.setReturnStatus(ErpQaConstants.RECALL_TARGET_RETURN_RETURNED);
            recallTargetBiz.updateEntity(target, null, context);
        }
        return recall;
    }

    @Override
    @BizMutation
    public ErpQaRecall close(@Name("recallId") Long recallId, IServiceContext context) {
        ErpQaRecall recall = requireRecall(recallId, context);
        requireRecallStatus(recall, ErpQaConstants.RECALL_STATUS_IN_PROGRESS, "IN_PROGRESS");
        // 通知门控：配置开启时全部 target returnStatus≠PENDING 且 notifyCustomer=true 方可 CLOSED
        if (ErpQaConfigs.isRecallNotifyRequiredToClose()) {
            if (!Boolean.TRUE.equals(recall.getNotifyCustomer())) {
                throw new NopException(ErpQaErrors.ERR_RECALL_NOTIFY_INCOMPLETE)
                        .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode());
            }
            QueryBean q = new QueryBean();
            q.addFilter(eq("recallId", recallId));
            q.addFilter(eq("returnStatus", ErpQaConstants.RECALL_TARGET_RETURN_PENDING));
            long pending = recallTargetBiz.findCount(q, context);
            if (pending > 0) {
                throw new NopException(ErpQaErrors.ERR_RECALL_NOTIFY_INCOMPLETE)
                        .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode());
            }
        }
        recall.setStatus(ErpQaConstants.RECALL_STATUS_CLOSED);
        dao().updateEntity(recall);
        return recall;
    }

    // ---------- helpers ----------

    private List<ErpQaRecallTarget> loadTargets(Long recallId, Set<Long> targetIds, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recallId", recallId));
        if (targetIds != null && !targetIds.isEmpty()) {
            q.addFilter(io.nop.api.core.beans.FilterBeans.in("id", new ArrayList<>(targetIds)));
        }
        return recallTargetBiz.findList(q, null, context);
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
        data.put("businessDate", LocalDate.now().toString());
        data.put("docStatus", ErpQaConstants.SAL_DOC_STATUS_DRAFT);
        data.put("approveStatus", ErpQaConstants.SAL_APPROVE_STATUS_UNSUBMITTED);
        data.put("lines", java.util.Collections.singletonList(line));
        return salReturnBiz.save(data, context);
    }

    private Long pickUoMId(ErpSalDelivery delivery, Long materialId) {
        if (delivery == null) {
            return null;
        }
        io.nop.orm.IOrmEntitySet<ErpSalDeliveryLine> lines = delivery.getLines();
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

    private ErpQaRecall requireRecall(Long recallId, IServiceContext context) {
        if (recallId == null) {
            throw new NopException(ErpQaErrors.ERR_RECALL_NOT_FOUND).param(ErpQaErrors.ARG_RECALL_ID, recallId);
        }
        return requireEntity(String.valueOf(recallId), null, context);
    }

    private void requireRecallStatus(ErpQaRecall recall, int expected, String expectedLabel) {
        Integer current = recall.getStatus();
        if (current == null || current != expected) {
            throw illegalRecallTransition(recall, current, expectedLabel);
        }
    }

    private void requireApproveStatus(ErpQaRecall recall, int expected, String expectedLabel) {
        Integer current = recall.getApproveStatus();
        if (current == null || current != expected) {
            throw new NopException(ErpQaErrors.ERR_INVALID_RECALL_STATUS_TRANSITION)
                    .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode())
                    .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                    .param(ErpQaErrors.ARG_EXPECTED_STATUS, expectedLabel);
        }
    }

    private NopException illegalRecallTransition(ErpQaRecall recall, Integer current, String expected) {
        return new NopException(ErpQaErrors.ERR_INVALID_RECALL_STATUS_TRANSITION)
                .param(ErpQaErrors.ARG_RECALL_CODE, recall.getCode())
                .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                .param(ErpQaErrors.ARG_EXPECTED_STATUS, expected);
    }

    @SuppressWarnings("unchecked")
    private void applyRecallFields(ErpQaRecall recall, Map<String, Object> data) {
        for (Map.Entry<String, Object> e : data.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if (value == null) {
                continue;
            }
            switch (key) {
                case "code":
                    recall.setCode(asString(value));
                    break;
                case "recallName":
                    recall.setRecallName(asString(value));
                    break;
                case "triggerType":
                    recall.setTriggerType(asInt(value));
                    break;
                case "sourceNcrId":
                    recall.setSourceNcrId(asLong(value));
                    break;
                case "materialId":
                    recall.setMaterialId(asLong(value));
                    break;
                case "batchId":
                    recall.setBatchId(asLong(value));
                    break;
                case "serialNo":
                    recall.setSerialNo(asString(value));
                    break;
                case "rootCause":
                    recall.setRootCause(asString(value));
                    break;
                case "severityLevel":
                    recall.setSeverityLevel(asInt(value));
                    break;
                case "businessDate":
                    recall.setBusinessDate(asLocalDate(value));
                    break;
                case "remark":
                    recall.setRemark(asString(value));
                    break;
                default:
                    // 忽略未识别字段（status/approveStatus 等状态由状态机控制，不允许经 register 直接设入）
                    break;
            }
        }
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString().trim());
    }

    private static Integer asInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString().trim());
    }

    private static LocalDate asLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        return LocalDate.parse(value.toString().trim());
    }
}
