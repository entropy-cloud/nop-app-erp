package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.cs.service.processor.ErpCsTicketMatchAndAttachSlaProcessor;
import app.erp.cs.service.processor.ErpCsTicketReopenProcessor;
import app.erp.cs.service.processor.ErpCsTicketResolveProcessor;
import app.erp.cs.service.processor.ErpCsTicketScanOverdueTicketsProcessor;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.EntityData;

/**
 * 客服工单 BizModel。权威：{@code docs/design/customer-service/state-machine.md}、
 * {@code docs/design/customer-service/sla.md}、{@code docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md} Phase 1。
 *
 * <p>六态状态机（NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED）：assign/start/resolve/close/reopen/cancel。
 * 非法迁移抛 {@link ErpCsErrors#ERR_INVALID_TICKET_STATUS_TRANSITION}；终态抛 {@link ErpCsErrors#ERR_TICKET_ALREADY_TERMINAL}。
 * 每迁移写 {@link ErpCsTicketAction} 审计（actionType 映射见 plan Decision；fromStatus/toStatus 承载精确迁移）。
 *
 * <p>SLA：{@link #matchAndAttachSla} 匹配策略 + 算 deadline；resolve 标记 isSlaCompleted；
 * {@link #scanOverdueTickets} 超时升级（ESCALATE）；{@link #findSlaWarnings} 预警查询。
 *
 * <p>CSAT 触发：resolve 成功后（config-gated）调 {@link IErpCsSurveyBiz#createSurvey}；
 * reopen 时取消未响应的调查（删除 SENT 状态调查避免误发）。
 */
@BizModel("ErpCsTicket")
public class ErpCsTicketBizModel extends CrudBizModel<ErpCsTicket> implements IErpCsTicketBiz {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ErpCsTicketBizModel.class);

    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    IErpMdPartnerBiz mdPartnerBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    ErpCsTicketMatchAndAttachSlaProcessor matchAndAttachSlaProcessor;
    @Inject
    ErpCsTicketReopenProcessor reopenProcessor;
    @Inject
    ErpCsTicketResolveProcessor resolveProcessor;
    @Inject
    ErpCsTicketScanOverdueTicketsProcessor scanOverdueTicketsProcessor;

    public ErpCsTicketBizModel() {
        setEntityName(ErpCsTicket.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCsTicket> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpCsTicket entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

    public void setTicketActionBiz(IErpCsTicketActionBiz ticketActionBiz) {
        this.ticketActionBiz = ticketActionBiz;
    }

    public void setMdPartnerBiz(IErpMdPartnerBiz mdPartnerBiz) {
        this.mdPartnerBiz = mdPartnerBiz;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    // ---------- 状态机 ----------

    @Override
    @BizMutation
    public ErpCsTicket assign(@Name("ticketId") Long ticketId,
                              @Optional @Name("assignedToId") String assignedToId,
                              IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        if (!Objects.equals(from, ErpCsConstants.TICKET_STATUS_NEW)) {
            throw illegalTransition(ticket, from, ErpCsConstants.TICKET_STATUS_NEW);
        }
        ticket.setAssignedToId(assignedToId);
        ticket.setStatus(ErpCsConstants.TICKET_STATUS_ASSIGNED);
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_ASSIGN, from, ErpCsConstants.TICKET_STATUS_ASSIGNED,
                "分派处理人: " + assignedToId, context);
        return ticket;
    }

    @Override
    @BizMutation
    public ErpCsTicket start(@Name("ticketId") Long ticketId, IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        if (!Objects.equals(from, ErpCsConstants.TICKET_STATUS_ASSIGNED)) {
            throw illegalTransition(ticket, from, ErpCsConstants.TICKET_STATUS_ASSIGNED);
        }
        ticket.setStatus(ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        // 计时起点：首次进入 IN_PROGRESS（见 plan Decision：startDateTime=首次 IN_PROGRESS 时间）
        ticket.setStartDateTime(CoreMetrics.currentTimestamp());
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_NOTE, from, ErpCsConstants.TICKET_STATUS_IN_PROGRESS,
                "开始处理", context);
        return ticket;
    }

    @Override
    @BizMutation
    public ErpCsTicket resolve(@Name("ticketId") Long ticketId,
                               @Optional @Name("resolution") String resolution,
                               IServiceContext context) {
        return resolveProcessor.resolve(ticketId, resolution, context);
    }

    @Override
    @BizMutation
    public ErpCsTicket close(@Name("ticketId") Long ticketId, IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        if (!Objects.equals(from, ErpCsConstants.TICKET_STATUS_RESOLVED)) {
            throw illegalTransition(ticket, from, ErpCsConstants.TICKET_STATUS_RESOLVED);
        }
        // 关闭前检查：超时工单（isSlaCompleted=false）须在 remark 注明超时原因
        if (Boolean.FALSE.equals(ticket.getIsSlaCompleted())
                && (ticket.getRemark() == null || ticket.getRemark().trim().isEmpty())) {
            throw new NopException(ErpCsErrors.ERR_TICKET_CLOSE_BREACHED_NO_REASON)
                    .param(ErpCsErrors.ARG_TICKET_CODE, ticket.getCode());
        }
        ticket.setStatus(ErpCsConstants.TICKET_STATUS_CLOSED);
        ticket.setEndDateTime(CoreMetrics.currentTimestamp());
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_CLOSE, from, ErpCsConstants.TICKET_STATUS_CLOSED,
                "关闭工单", context);
        return ticket;
    }

    @Override
    @BizMutation
    public ErpCsTicket reopen(@Name("ticketId") Long ticketId, IServiceContext context) {
        return reopenProcessor.reopen(ticketId, context);
    }

    @Override
    @BizMutation
    public ErpCsTicket cancel(@Name("ticketId") Long ticketId,
                              @Optional @Name("cancelReason") String cancelReason,
                              IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        // 非终态→CANCELLED（NEW/ASSIGNED/IN_PROGRESS/RESOLVED 均可取消）
        if (Objects.equals(from, ErpCsConstants.TICKET_STATUS_CLOSED)
                || Objects.equals(from, ErpCsConstants.TICKET_STATUS_CANCELLED)) {
            throw new NopException(ErpCsErrors.ERR_TICKET_ALREADY_TERMINAL)
                    .param(ErpCsErrors.ARG_TICKET_CODE, ticket.getCode())
                    .param(ErpCsErrors.ARG_CURRENT_STATUS, from);
        }
        ticket.setStatus(ErpCsConstants.TICKET_STATUS_CANCELLED);
        if (cancelReason != null) {
            ticket.setRemark(cancelReason);
        }
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_CANCEL, from, ErpCsConstants.TICKET_STATUS_CANCELLED,
                "取消工单: " + (cancelReason == null ? "" : cancelReason), context);
        return ticket;
    }

    // ---------- SLA ----------

    @Override
    @BizMutation
    public ErpCsTicket adoptKnowledge(@Name("ticketId") Long ticketId,
                                      @Name("knowledgeBaseId") Long knowledgeBaseId,
                                      IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String current = ticket.getStatus();
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_NOTE, current, current,
                "采纳知识库文章参考: knowledgeBaseId=" + knowledgeBaseId, context);
        return ticket;
    }

    @Override
    @BizMutation
    public ErpCsTicket matchAndAttachSla(@Name("ticketId") Long ticketId, IServiceContext context) {
        return matchAndAttachSlaProcessor.matchAndAttachSla(ticketId, context);
    }

    @Override
    @BizMutation
    public List<ErpCsTicket> scanOverdueTickets(IServiceContext context) {
        return scanOverdueTicketsProcessor.scanOverdueTickets(context);
    }

    @Override
    @BizQuery
    public List<ErpCsTicket> findSlaWarnings(@Optional @Name("beforeMinutes") Integer beforeMinutes,
                                              IServiceContext context) {
        int minutes = beforeMinutes != null ? beforeMinutes : ErpCsConfigs.getSlaWarningBeforeMinutes();
        LocalDateTime now = CoreMetrics.currentDateTime();
        QueryBean q = new QueryBean();
        // deadlineDateTime BETWEEN now AND now+beforeMinutes 且未完成（供 nop-job 预警）
        q.addFilter(in("status", java.util.Arrays.asList(
                ErpCsConstants.TICKET_STATUS_ASSIGNED, ErpCsConstants.TICKET_STATUS_IN_PROGRESS)));
        q.addFilter(io.nop.api.core.beans.FilterBeans.dateTimeBetween("deadlineDateTime", now, now.plusMinutes(minutes)));
        q.addFilter(eq("isSlaCompleted", Boolean.FALSE));
        List<ErpCsTicket> warnings = findList(q, null, context);
        // 通知派发（config-gated）：临近 SLA 截止时预警通知，避免超期
        for (ErpCsTicket ticket : warnings) {
            notifySlaOverdue(ticket, context);
        }
        return warnings;
    }

    // ---------- helpers ----------

    @Override
    @BizQuery
    public Map<String, Object> findBoardData(@Optional @Name("customerId") Long customerId, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.setLimit(200);
        if (customerId != null) {
            query.addFilter(eq("customerId", customerId));
        }
        List<ErpCsTicket> tickets = findList(query, null, context);

        String[] statuses = {ErpCsConstants.TICKET_STATUS_NEW, ErpCsConstants.TICKET_STATUS_ASSIGNED,
                ErpCsConstants.TICKET_STATUS_IN_PROGRESS, ErpCsConstants.TICKET_STATUS_RESOLVED,
                ErpCsConstants.TICKET_STATUS_CLOSED, ErpCsConstants.TICKET_STATUS_CANCELLED};
        String[] titles = {"新建", "已分派", "处理中", "已解决", "已关闭", "已取消"};

        Map<String, Object> board = new LinkedHashMap<>();
        List<String> rootChildren = new ArrayList<>();
        for (int i = 0; i < statuses.length; i++) {
            rootChildren.add("col-" + statuses[i]);
        }
        board.put("root", boardNode("root", "root", null, rootChildren, null));

        for (int i = 0; i < statuses.length; i++) {
            String colId = "col-" + statuses[i];
            List<String> cardIds = new ArrayList<>();
            for (ErpCsTicket t : tickets) {
                if (Objects.equals(t.getStatus(), statuses[i])) {
                    cardIds.add("card-" + t.getId());
                }
            }
            Map<String, Object> colData = new LinkedHashMap<>();
            colData.put("title", titles[i]);
            colData.put("status", statuses[i]);
            board.put(colId, boardNode(colId, "column", null, cardIds, colData));
        }

        for (ErpCsTicket t : tickets) {
            String cardId = "card-" + t.getId();
            String colId = "col-" + t.getStatus();
            Map<String, Object> cardData = new LinkedHashMap<>();
            cardData.put("title", (t.getCode() != null ? t.getCode() : "") + " " + (t.getSubject() != null ? t.getSubject() : ""));
            cardData.put("ticketId", t.getId());
            cardData.put("code", t.getCode());
            cardData.put("subject", t.getSubject());
            cardData.put("priority", t.getPriority());
            cardData.put("status", t.getStatus());
            cardData.put("deadlineDateTime", t.getDeadlineDateTime());
            cardData.put("isSlaCompleted", t.getIsSlaCompleted());
            cardData.put("assignedToId", t.getAssignedToId());
            board.put(cardId, boardNode(cardId, "card", colId, new ArrayList<>(), cardData));
        }
        return board;
    }

    private static Map<String, Object> boardNode(String id, String type, String parentId,
                                                   List<String> children, Map<String, Object> data) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("type", type);
        if (parentId != null) node.put("parentId", parentId);
        node.put("children", children);
        if (data != null) node.put("data", data);
        return node;
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

    private ErpCsTicket requireTicket(Long ticketId, IServiceContext context) {
        if (ticketId == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND).param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        return requireEntity(String.valueOf(ticketId), null, context);
    }

    private NopException illegalTransition(ErpCsTicket ticket, String current, String expected) {
        return new NopException(ErpCsErrors.ERR_INVALID_TICKET_STATUS_TRANSITION)
                .param(ErpCsErrors.ARG_TICKET_CODE, ticket.getCode())
                .param(ErpCsErrors.ARG_CURRENT_STATUS, current)
                .param(ErpCsErrors.ARG_EXPECTED_STATUS, expected);
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

    

}
