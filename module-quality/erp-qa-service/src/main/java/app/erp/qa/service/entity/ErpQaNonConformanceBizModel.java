
package app.erp.qa.service.entity;

import app.erp.qa.biz.IErpQaNonConformanceBiz;
import app.erp.qa.biz.IErpQaRecallBiz;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import java.util.Objects;

import java.util.LinkedHashMap;
import java.util.Map;
import io.nop.api.core.time.CoreMetrics;

/**
 * NCR BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现 NCR 5 态状态机
 * （{@code docs/design/quality/state-machine.md §适用对象二`}）。
 *
 * <p>迁移：submitReview（OPEN→IN_REVIEW）、resolve（IN_REVIEW→RESOLVED，须全部 CAPA COMPLETED + 验证）、
 * escalateToRecall（IN_REVIEW→ESCALATED_TO_RECALL 终态）、cancel（OPEN/IN_REVIEW→CANCELLED）。
 * 非法迁移抛 {@link ErpQaErrors#ERR_INVALID_NCR_STATUS_TRANSITION}。
 *
 * <p>resolve 门控经 {@link NcrLifecycleService#requireResolveGate}（CAPA 效果验证闭环）。
 */
@BizModel("ErpQaNonConformance")
public class ErpQaNonConformanceBizModel extends CrudBizModel<ErpQaNonConformance> implements IErpQaNonConformanceBiz {

    @Inject
    NcrLifecycleService ncrLifecycleService;
    @Inject
    IErpQaRecallBiz recallBiz;

    public ErpQaNonConformanceBizModel() {
        setEntityName(ErpQaNonConformance.class.getName());
    }

    public void setNcrLifecycleService(NcrLifecycleService ncrLifecycleService) {
        this.ncrLifecycleService = ncrLifecycleService;
    }

    public void setRecallBiz(IErpQaRecallBiz recallBiz) {
        this.recallBiz = recallBiz;
    }

    @Override
    @BizMutation
    public ErpQaNonConformance submitReview(@Name("ncrId") Long ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        requireNcrStatus(ncr, ErpQaConstants.NCR_STATUS_OPEN, "OPEN");
        ncr.setStatus(ErpQaConstants.NCR_STATUS_IN_REVIEW);
        dao().updateEntity(ncr);
        return ncr;
    }

    @Override
    @BizMutation
    public ErpQaNonConformance resolve(@Name("ncrId") Long ncrId,
                                       @Name("resolution") String resolution,
                                       IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        requireNcrStatus(ncr, ErpQaConstants.NCR_STATUS_IN_REVIEW, "IN_REVIEW");
        // CAPA 闭环门控：全部措施 COMPLETED + 验证人/验证日期已填
        ncrLifecycleService.requireResolveGate(ncrId, ncr.getCode());
        ncr.setStatus(ErpQaConstants.NCR_STATUS_RESOLVED);
        if (resolution != null) {
            ncr.setResolution(resolution);
        }
        ncr.setResolvedAt(CoreMetrics.currentDateTime());
        dao().updateEntity(ncr);
        return ncr;
    }

    @Override
    @BizMutation
    public ErpQaNonConformance escalateToRecall(@Name("ncrId") Long ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        requireNcrStatus(ncr, ErpQaConstants.NCR_STATUS_IN_REVIEW, "IN_REVIEW");
        // 升级为召回（终态，仅状态迁移占位；不建召回实体。真正建召回用 upgradeToRecall）
        ncr.setStatus(ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL);
        dao().updateEntity(ncr);
        return ncr;
    }

    @Override
    @BizMutation
    public ErpQaRecall upgradeToRecall(@Name("ncrId") Long ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        requireNcrStatus(ncr, ErpQaConstants.NCR_STATUS_IN_REVIEW, "IN_REVIEW");
        // NCR→ESCALATED_TO_RECALL（字典值已存在），并生成召回事件（继承 NCR 物料/严重程度）
        ncr.setStatus(ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL);
        dao().updateEntity(ncr);

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

    @Override
    @BizMutation
    public ErpQaNonConformance cancel(@Name("ncrId") Long ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        String current = ncr.getStatus();
        if (current == null || (!Objects.equals(current, ErpQaConstants.NCR_STATUS_OPEN)
                && !Objects.equals(current, ErpQaConstants.NCR_STATUS_IN_REVIEW))) {
            throw illegalNcrTransition(ncr, current, "OPEN 或 IN_REVIEW");
        }
        ncr.setStatus(ErpQaConstants.NCR_STATUS_CANCELLED);
        dao().updateEntity(ncr);
        return ncr;
    }

    // ---------- helpers ----------

    private ErpQaNonConformance requireNcr(Long ncrId, IServiceContext context) {
        if (ncrId == null) {
            throw new NopException(ErpQaErrors.ERR_NCR_NOT_FOUND).param(ErpQaErrors.ARG_NCR_ID, ncrId);
        }
        return requireEntity(String.valueOf(ncrId), null, context);
    }

    private void requireNcrStatus(ErpQaNonConformance ncr, String expected, String expectedLabel) {
        String current = ncr.getStatus();
        if (current == null || !Objects.equals(current, expected)) {
            throw illegalNcrTransition(ncr, current, expectedLabel);
        }
    }

    private NopException illegalNcrTransition(ErpQaNonConformance ncr, String current, String expected) {
        return new NopException(ErpQaErrors.ERR_INVALID_NCR_STATUS_TRANSITION)
                .param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode())
                .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                .param(ErpQaErrors.ARG_EXPECTED_STATUS, expected);
    }
}
