package app.erp.qa.service.processor;

import app.erp.qa.biz.IErpQaRecallBiz;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ErpQaNonConformance upgradeToRecall per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 NCR→ESCALATED_TO_RECALL 状态迁移 + 召回事件登记编排（继承 NCR 物料/严重程度）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaNonConformanceProcessor}。
 */
public class ErpQaNonConformanceUpgradeToRecallProcessor extends AbstractErpQaNonConformanceProcessor {

    @Inject
    IErpQaRecallBiz recallBiz;

    public ErpQaRecall upgradeToRecall(Long ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        requireNcrStatus(ncr, ErpQaConstants.NCR_STATUS_IN_REVIEW, "IN_REVIEW");
        // NCR→ESCALATED_TO_RECALL（字典值已存在），并生成召回事件（继承 NCR 物料/严重程度）
        ncr.setStatus(ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL);
        ncrDao().updateEntity(ncr);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "RC-FROM-NCR-" + ncr.getId());
        data.put("recallName", "NCR升级召回:" + ncr.getCode());
        data.put("triggerType", ErpQaConstants.RECALL_TRIGGER_BATCH_NCR_UPGRADE);
        data.put("sourceNcrId", ncr.getId());
        if (ncr.getMaterialId() != null) {
            data.put("materialId", ncr.getMaterialId());
        }
        // NCR severity(LOW/NORMAL/HIGH/CRITICAL=10/20/30/40) 与 recall severity(LOW/MEDIUM/HIGH/CRITICAL=10/20/30/40) 码值对齐
        String severity = ncr.getSeverity() != null ? ncr.getSeverity() : ErpQaConstants.RECALL_SEVERITY_MEDIUM;
        data.put("severityLevel", severity);
        data.put("businessDate", CoreMetrics.today().toString());
        data.put("rootCause", ncr.getDescription());
        return recallBiz.register(data, context);
    }
}
