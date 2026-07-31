package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.dao.entity.ErpCsCannedResponse;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.CannedResponseRenderer;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ErpCsCannedResponse applyCannedResponse per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含预设应答渲染 + usageCount+1 + TicketAction NOTE 审计。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsCannedResponseApplyCannedResponseProcessor {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCsTicketBiz ticketBiz;
    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    public String applyCannedResponse(Long cannedResponseId, Long ticketId,
                                      Map<String, String> customVariables, IServiceContext context) {
        ErpCsCannedResponse resp = requireCannedResponse(cannedResponseId, context);
        assertActive(resp);
        Map<String, String> systemVars = resolveSystemVars(resp, ticketId, context);
        String rendered = CannedResponseRenderer.render(resp.getContent(), resp.getVariableDefs(), systemVars, customVariables);

        // usageCount +1 持久化
        Integer cur = resp.getUsageCount();
        resp.setUsageCount(cur == null ? 1 : cur + 1);
        dao().updateEntity(resp);

        // 写 TicketAction NOTE 审计
        writeNoteAction(ticketId, rendered, cannedResponseId, context);

        return rendered;
    }

    private ErpCsCannedResponse requireCannedResponse(Long id, IServiceContext context) {
        if (id == null) {
            throw new NopException(ErpCsErrors.ERR_CANNED_RESPONSE_NOT_FOUND)
                    .param(ErpCsErrors.ARG_CANNED_RESPONSE_ID, id);
        }
        ErpCsCannedResponse resp = dao().getEntityById(id);
        if (resp == null) {
            throw new NopException(ErpCsErrors.ERR_CANNED_RESPONSE_NOT_FOUND)
                    .param(ErpCsErrors.ARG_CANNED_RESPONSE_ID, id);
        }
        return resp;
    }

    private void assertActive(ErpCsCannedResponse resp) {
        if (!Boolean.TRUE.equals(resp.getIsActive())) {
            throw new NopException(ErpCsErrors.ERR_CANNED_RESPONSE_INACTIVE)
                    .param(ErpCsErrors.ARG_CANNED_RESPONSE_ID, resp.getId());
        }
    }

    private Map<String, String> resolveSystemVars(ErpCsCannedResponse resp, Long ticketId, IServiceContext context) {
        Map<String, String> vars = new LinkedHashMap<>();
        LocalDate today = CoreMetrics.currentDate();
        LocalDateTime now = CoreMetrics.currentDateTime();
        vars.put("{today}", today.format(DATE_FMT));
        vars.put("{now}", now.format(DATETIME_FMT));
        if (context != null && context.getUserId() != null) {
            vars.put("{agent_name}", context.getUserId());
        }

        if (ticketId != null) {
            ErpCsTicket ticket = loadTicket(ticketId, context);
            if (ticket != null) {
                if (ticket.getCode() != null) {
                    vars.put("{ticket_id}", ticket.getCode());
                }
                String customerName = resolveCustomerName(ticket.getCustomerId(), context);
                if (customerName != null) {
                    vars.put("{customer_name}", customerName);
                }
            }
        }
        return vars;
    }

    private ErpCsTicket loadTicket(Long ticketId, IServiceContext context) {
        if (ticketId == null) {
            return null;
        }
        return ticketBiz.get(String.valueOf(ticketId), false, context);
    }

    private String resolveCustomerName(Long customerId, IServiceContext context) {
        if (customerId == null) {
            return null;
        }
        try {
            ErpMdPartner partner = mdPartnerBiz.findById(customerId, context);
            return partner == null ? null : partner.getName();
        } catch (Exception e) {
            return null;
        }
    }

    private void writeNoteAction(Long ticketId, String content, Long cannedResponseId, IServiceContext context) {
        if (ticketId == null) {
            return;
        }
        ErpCsTicketAction action = ticketActionBiz.newEntity();
        action.setTicketId(ticketId);
        action.setActionType(ErpCsConstants.ACTION_TYPE_NOTE);
        action.setContent(content);
        action.setOperatorId(context == null ? null : context.getUserId());
        ticketActionBiz.saveEntity(action, null, context);
    }

    private IEntityDao<ErpCsCannedResponse> dao() {
        return daoProvider.daoFor(ErpCsCannedResponse.class);
    }
}
