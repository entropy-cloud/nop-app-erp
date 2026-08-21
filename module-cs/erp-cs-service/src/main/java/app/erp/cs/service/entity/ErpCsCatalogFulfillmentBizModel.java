package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsCatalogFulfillmentBiz;
import app.erp.cs.biz.IErpCsTicketFulfillmentStepBiz;
import app.erp.cs.dao.entity.ErpCsCatalogFulfillment;
import app.erp.cs.dao.entity.ErpCsTicketFulfillmentStep;
import app.erp.cs.service.processor.ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 目录项履行映射 BizModel（{@code docs/design/customer-service/service-catalog.md §三/§9.1}）。
 *
 * <p>{@link #executeFulfillmentSteps} 委派 {@link ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor}
 * ——RC-R1.71 实化：per-ticket 物化执行行（UK 幂等）→ sequence 升序 actionConfig 驱动五动作推进，
 * 失败暂停 + 管理员通知（7206），末步前 ensureInProgress 铺底终态推进（L1 UC-CS-12 ②③④）。
 *
 * <p>{@link #retryFulfillment}（手动重试，refresh 模板配置）/ {@link #approveFulfillmentStep}（cs-local
 * 轻量审批）承载 UC-CS-12 异常「可重试最多 3 次」；{@link #findFulfillmentProgress} 承载后置「状态可跟踪」。
 */
@BizModel("ErpCsCatalogFulfillment")
public class ErpCsCatalogFulfillmentBizModel extends CrudBizModel<ErpCsCatalogFulfillment>
        implements IErpCsCatalogFulfillmentBiz {

    @Inject
    ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor executeFulfillmentStepsProcessor;

    @Inject
    IErpCsTicketFulfillmentStepBiz ticketFulfillmentStepBiz;

    public ErpCsCatalogFulfillmentBizModel() {
        setEntityName(ErpCsCatalogFulfillment.class.getName());
    }

    @Override
    @BizMutation
    public List<ErpCsTicketFulfillmentStep> executeFulfillmentSteps(@Name("catalogItemId") String catalogItemId,
                                                                    @Name("ticketId") String ticketId,
                                                                    IServiceContext context) {
        return executeFulfillmentStepsProcessor.executeFulfillmentSteps(catalogItemId, ticketId, context);
    }

    @Override
    @BizMutation
    public List<ErpCsTicketFulfillmentStep> retryFulfillment(@Name("ticketId") String ticketId,
                                                             IServiceContext context) {
        return executeFulfillmentStepsProcessor.retryFulfillment(ticketId, context);
    }

    @Override
    @BizMutation
    public ErpCsTicketFulfillmentStep approveFulfillmentStep(@Name("stepId") String stepId,
                                                             @Name("approved") boolean approved,
                                                             @Optional @Name("comment") String comment,
                                                             IServiceContext context) {
        return executeFulfillmentStepsProcessor.approveFulfillmentStep(stepId, approved, comment, context);
    }

    /** 履行进度投影（D6）：sequence 升序 step 行 → map（sequence/actionType/status/retryCount/lastError/executedAt/executedBy）。 */
    @Override
    @BizQuery
    public List<Map<String, Object>> findFulfillmentProgress(@Name("ticketId") String ticketId,
                                                             IServiceContext context) {
        if (ticketId == null) {
            return new ArrayList<>();
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        List<ErpCsTicketFulfillmentStep> steps = ticketFulfillmentStepBiz.findList(q, null, context);
        steps.sort(java.util.Comparator.comparingInt(s -> s.getSequence() == null ? 0 : s.getSequence()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ErpCsTicketFulfillmentStep step : steps) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stepId", step.getId());
            row.put("sequence", step.getSequence());
            row.put("actionType", step.getActionType());
            row.put("status", step.getStatus());
            row.put("retryCount", step.getRetryCount());
            row.put("lastError", step.getLastError());
            row.put("executedAt", step.getExecutedAt());
            row.put("executedBy", step.getExecutedBy());
            result.add(row);
        }
        return result;
    }
}
