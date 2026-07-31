package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.dao.entity.ErpCsCatalogFulfillment;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.ErpCsConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCsCatalogFulfillment executeFulfillmentSteps per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含目录项履行步骤编排（按 sequence 升序执行，写 TicketAction 审计）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCsTicketActionBiz ticketActionBiz;

    public List<ErpCsCatalogFulfillment> executeFulfillmentSteps(Long catalogItemId, Long ticketId,
                                                                  IServiceContext context) {
        if (catalogItemId == null || ticketId == null) {
            return new ArrayList<>();
        }
        List<ErpCsCatalogFulfillment> steps = loadStepsByCatalogItem(catalogItemId);
        // 按 sequence 升序执行（null 视为 0）
        steps.sort(Comparator.comparingInt(this::sequenceOf));
        List<ErpCsCatalogFulfillment> processed = new ArrayList<>();
        for (ErpCsCatalogFulfillment step : steps) {
            String result = executeStep(step, ticketId, context);
            LOG.debug("fulfillment-step: catalogItemId={}, ticketId={}, actionType={}, result={}",
                    catalogItemId, ticketId, step.getActionType(), result);
            processed.add(step);
        }
        return processed;
    }

    /**
     * 执行单步履行。返回执行结果标识（DONE / SKIPPED）。
     * protected 以允许下游覆盖（产品化扩展点：实现真实 ASSIGN_AGENT 技能匹配等）。
     */
    protected String executeStep(ErpCsCatalogFulfillment step, Long ticketId, IServiceContext context) {
        String actionType = step.getActionType();
        if (actionType == null) {
            return ErpCsConstants.FULFILLMENT_RESULT_SKIPPED;
        }
        switch (actionType) {
            case ErpCsConstants.FULFILLMENT_ACTION_CREATE_TICKET:
                // 工单已由 createFromCatalog 创建，登记 DONE
                writeAudit(ticketId, actionType, "DONE: 工单已建", context);
                return ErpCsConstants.FULFILLMENT_RESULT_DONE;
            case ErpCsConstants.FULFILLMENT_ACTION_ASSIGN_TEAM:
            case ErpCsConstants.FULFILLMENT_ACTION_ASSIGN_AGENT:
                // 本期登记执行结果占位（实际分派逻辑归 successor）
                writeAudit(ticketId, actionType,
                        "DONE: assignToRole=" + step.getAssignToRole() + " (本期占位登记)", context);
                return ErpCsConstants.FULFILLMENT_RESULT_DONE;
            case ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER:
                writeAudit(ticketId, actionType, "DONE: 客户通知已登记", context);
                return ErpCsConstants.FULFILLMENT_RESULT_DONE;
            case ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS:
                writeAudit(ticketId, actionType, "DONE: 状态更新已登记", context);
                return ErpCsConstants.FULFILLMENT_RESULT_DONE;
            case ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL:
                writeAudit(ticketId, actionType, "DONE: 审批请求已登记", context);
                return ErpCsConstants.FULFILLMENT_RESULT_DONE;
            case ErpCsConstants.FULFILLMENT_ACTION_CLOSE_TICKET:
                writeAudit(ticketId, actionType, "DONE: 关闭动作已登记", context);
                return ErpCsConstants.FULFILLMENT_RESULT_DONE;
            case ErpCsConstants.FULFILLMENT_ACTION_INVOKE_WORKFLOW:
            case ErpCsConstants.FULFILLMENT_ACTION_CREATE_CHILD_TICKET:
                // 归 Non-Goal successor（plan §Non-Goals 已声明）
                writeAudit(ticketId, actionType, "SKIPPED: 归 successor (跨域编排/子工单)", context);
                return ErpCsConstants.FULFILLMENT_RESULT_SKIPPED;
            default:
                writeAudit(ticketId, actionType, "SKIPPED: 未知 actionType", context);
                return ErpCsConstants.FULFILLMENT_RESULT_SKIPPED;
        }
    }

    private void writeAudit(Long ticketId, String actionType, String content, IServiceContext context) {
        if (ticketActionBiz == null) {
            return;
        }
        ErpCsTicketAction action = ticketActionBiz.newEntity();
        action.setTicketId(ticketId);
        action.setActionType(actionType);
        action.setContent(content);
        action.setOperatorId(context.getUserId());
        ticketActionBiz.saveEntity(action, null, context);
    }

    private List<ErpCsCatalogFulfillment> loadStepsByCatalogItem(Long catalogItemId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("catalogItemId", catalogItemId));
        IEntityDao<ErpCsCatalogFulfillment> dao = daoProvider.daoFor(ErpCsCatalogFulfillment.class);
        return dao.findAllByQuery(q);
    }

    private int sequenceOf(ErpCsCatalogFulfillment step) {
        Integer seq = step.getSequence();
        return seq == null ? 0 : seq;
    }
}
