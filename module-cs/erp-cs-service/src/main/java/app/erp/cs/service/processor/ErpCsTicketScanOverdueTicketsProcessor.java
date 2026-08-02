package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.lt;

/**
 * ErpCsTicket scanOverdueTickets per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 SLA 超时批量升级编排（查询超时工单 + 幂等去重 + ESCALATE 审计 + 通知派发）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsTicketScanOverdueTicketsProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpCsTicketScanOverdueTicketsProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    IErpMdPartnerBiz mdPartnerBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    public List<ErpCsTicket> scanOverdueTickets(IServiceContext context) {
        if (!ErpCsConfigs.isSlaEnabled()) {
            return new ArrayList<>();
        }
        LocalDateTime now = io.nop.api.core.time.CoreMetrics.currentDateTime();
        QueryBean q = new QueryBean();
        // status IN (ASSIGNED, IN_PROGRESS) AND deadlineDateTime < now AND isSlaCompleted=false
        q.addFilter(in("status", Arrays.asList(
                ErpCsConstants.TICKET_STATUS_ASSIGNED, ErpCsConstants.TICKET_STATUS_IN_PROGRESS)));
        q.addFilter(lt("deadlineDateTime", now));
        q.addFilter(eq("isSlaCompleted", Boolean.FALSE));
        // deadlineDateTime 的 XMeta 仅允许 eq/in/dateBetween/dateTimeBetween（不支持 lt），
        // 内部派生查询走 entity dao 绕过 meta 限制（同 ErpCrmEventBizModel.findDueReminders 模式）
        List<ErpCsTicket> overdue = dao().findAllByQuery(q);
        List<ErpCsTicket> escalated = new ArrayList<>();
        for (ErpCsTicket ticket : overdue) {
            // 幂等去重（plan 2026-07-30-0841-2 R1.28 P1-MA2-086）：已存在 ESCALATE 审计则跳过，
            // 避免每分钟重复 ESCALATE 审计行 + 通知噪音（isSlaCompleted 仅 resolve 翻转，不随升级翻转）。
            if (hasEscalationAction(ticket.getId())) {
                continue;
            }
            // 创建 ESCALATE 审计 + 通知 escalationUserId（L1，config-gated；通知占位，实际发送属 nop-notification 独立面）
            writeAction(ticket, ErpCsConstants.ACTION_TYPE_ESCALATE, ticket.getStatus(), ticket.getStatus(),
                    "SLA 超时升级通知 escalationUserId", context);
            // 通知派发（config-gated）：超时升级时通知客服主管/分派人，复用既有 erp-cs-sla-scan scheduler job 自动派发
            notifySlaOverdue(ticket, context);
            escalated.add(ticket);
        }
        return escalated;
    }

    @SuppressWarnings("unchecked")
    private boolean hasEscalationAction(Long ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("actionType", ErpCsConstants.ACTION_TYPE_ESCALATE));
        q.setLimit(1);
        return !daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q).isEmpty();
    }

    /**
     * SLA 通知派发（config-gated by {@link ErpCsConfigs#isSlaNotifyEnabled}）。
     *
     * <p>复用既有 {@code cs.sla-overdue} 模板（plan 2026-07-06-0504-1 Phase 4 已种子）。
     * 接收人由模板 ROLE resolver 解析（客服主管）；上下文含 ticketId/ticketCode/customerName/deadlineDateTime
     * + escalationUserId（取分派人，模板可选用）。模板缺失或 notify 失败时静默降级（不阻断业务）。
     */
    private void notifySlaOverdue(ErpCsTicket ticket, IServiceContext context) {
        if (!ErpCsConfigs.isSlaNotifyEnabled()) {
            return;
        }
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("ticketId", ticket.getId());
            ctx.put("ticketCode", ticket.getCode());
            ctx.put("customerName", resolveCustomerName(ticket.getCustomerId(), context));
            ctx.put("escalationUserId", ticket.getAssignedToId());
            notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_SLA_OVERDUE, ctx, context);
        } catch (Exception e) {
            // 通知派发失败不阻断 SLA 升级主流程（config-gated 降级语义）
            LOG.warn("SLA notify 派发失败（降级，主升级流程继续）：ticketId={}, reason={}",
                    ticket.getId(), e.getMessage());
        }
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

    private void writeAction(ErpCsTicket ticket, String actionType, String fromStatus, String toStatus,
                             String content, IServiceContext context) {
        ErpCsTicketAction action = ticketActionBiz.newEntity();
        action.setTicketId(ticket.getId());
        action.setActionType(actionType);
        action.setFromStatus(fromStatus);
        action.setToStatus(toStatus);
        action.setContent(content);
        action.setOperatorId(context.getUserId());
        ticketActionBiz.saveEntity(action, null, context);
    }

    private IEntityDao<ErpCsTicket> dao() {
        return daoProvider.daoFor(ErpCsTicket.class);
    }
}
