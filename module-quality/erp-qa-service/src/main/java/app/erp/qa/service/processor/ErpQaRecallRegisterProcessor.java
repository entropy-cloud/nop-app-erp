package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
import java.util.Map;

/**
 * ErpQaRecall register per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含召回事件登记编排：字段映射 → 状态/approve/notify 初始化 → 保存（{@code docs/design/quality/recall.md §召回状态机`}）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaRecallProcessor}。
 */
public class ErpQaRecallRegisterProcessor extends AbstractErpQaRecallProcessor {

    public ErpQaRecall register(Map<String, Object> data, IServiceContext context) {
        ErpQaRecall recall = recallDao().newEntity();
        if (data != null) {
            applyRecallFields(recall, data);
        }
        recall.setStatus(ErpQaConstants.RECALL_STATUS_OPEN);
        recall.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
        recall.setNotifyCustomer(Boolean.FALSE);
        if (recall.getBusinessDate() == null) {
            recall.setBusinessDate(CoreMetrics.today());
        }
        recallDao().saveEntity(recall);
        return recall;
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
                    recall.setTriggerType(asString(value));
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
                    recall.setSeverityLevel(asString(value));
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
