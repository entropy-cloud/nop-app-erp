package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsSurveyBiz;
import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
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
import static io.nop.api.core.beans.FilterBeans.lt;
import io.nop.api.core.time.CoreMetrics;

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
    IErpCsSurveyBiz surveyBiz;
    @Inject
    IErpMdPartnerBiz mdPartnerBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    public ErpCsTicketBizModel() {
        setEntityName(ErpCsTicket.class.getName());
    }

    public void setTicketActionBiz(IErpCsTicketActionBiz ticketActionBiz) {
        this.ticketActionBiz = ticketActionBiz;
    }

    public void setSurveyBiz(IErpCsSurveyBiz surveyBiz) {
        this.surveyBiz = surveyBiz;
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
        ticket.setStartDateTime(CoreMetrics.currentDateTime());
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
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        if (!Objects.equals(from, ErpCsConstants.TICKET_STATUS_IN_PROGRESS)) {
            throw illegalTransition(ticket, from, ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        }
        LocalDateTime now = CoreMetrics.currentDateTime();
        // 停 SLA 计时算 duration（分钟）；startDateTime 为空时 duration 留空
        if (ticket.getStartDateTime() != null) {
            long minutes = SlaDeadlineCalculator.minutesBetween(ticket.getStartDateTime(), now);
            ticket.setDuration((int) minutes);
        }
        // 标记 isSlaCompleted = resolvedAt <= deadlineDateTime
        LocalDateTime deadline = ticket.getDeadlineDateTime();
        boolean completed = deadline == null || !now.isAfter(deadline);
        ticket.setIsSlaCompleted(completed);
        ticket.setStatus(ErpCsConstants.TICKET_STATUS_RESOLVED);
        if (resolution != null) {
            ticket.setRemark(resolution);
        }
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_NOTE, from, ErpCsConstants.TICKET_STATUS_RESOLVED,
                "标记解决: " + (resolution == null ? "" : resolution), context);

        // CSAT 触发（config-gated）：trigger-status 默认 RESOLVED
        if (ErpCsConfigs.isSurveyEnabled()
                && Objects.equals(ErpCsConfigs.getSurveyTriggerStatus(), ErpCsConstants.TICKET_STATUS_RESOLVED)) {
            surveyBiz.createSurvey(ticketId, context);
        }
        return ticket;
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
        ticket.setEndDateTime(CoreMetrics.currentDateTime());
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_CLOSE, from, ErpCsConstants.TICKET_STATUS_CLOSED,
                "关闭工单", context);
        return ticket;
    }

    @Override
    @BizMutation
    public ErpCsTicket reopen(@Name("ticketId") Long ticketId, IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        String from = ticket.getStatus();
        if (!Objects.equals(from, ErpCsConstants.TICKET_STATUS_RESOLVED)) {
            throw illegalTransition(ticket, from, ErpCsConstants.TICKET_STATUS_RESOLVED);
        }
        ticket.setStatus(ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        // 恢复计时：保留原 startDateTime（duration 在下次 resolve 时累加重算，因 startDateTime 不变）
        updateEntity(ticket, null, context);
        writeAction(ticket, ErpCsConstants.ACTION_TYPE_NOTE, from, ErpCsConstants.TICKET_STATUS_IN_PROGRESS,
                "驳回重开", context);

        // reopen 时取消未响应的调查（避免误发）
        cancelUnrespondedSurvey(ticketId, context);
        return ticket;
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
    public ErpCsTicket matchAndAttachSla(@Name("ticketId") Long ticketId, IServiceContext context) {
        if (!ErpCsConfigs.isSlaEnabled()) {
            return requireTicket(ticketId, context);
        }
        ErpCsTicket ticket = requireTicket(ticketId, context);
        ErpCsSlaPolicy policy = SlaPolicyMatcher.match(daoProvider(), ticket);
        if (policy == null) {
            // 无匹配策略：不挂策略，deadlineDateTime 留空
            return ticket;
        }
        ticket.setSlaPolicyId(policy.getId());
        LocalDateTime deadline = SlaDeadlineCalculator.calculate(CoreMetrics.currentDateTime(), policy);
        ticket.setDeadlineDateTime(deadline);
        // priority 变更重算时保留原 startDateTime（plan Phase 1 item 3）
        updateEntity(ticket, null, context);
        return ticket;
    }

    @Override
    @BizMutation
    public List<ErpCsTicket> scanOverdueTickets(IServiceContext context) {
        if (!ErpCsConfigs.isSlaEnabled()) {
            return new ArrayList<>();
        }
        LocalDateTime now = CoreMetrics.currentDateTime();
        QueryBean q = new QueryBean();
        // status IN (ASSIGNED, IN_PROGRESS) AND deadlineDateTime < now AND isSlaCompleted=false
        q.addFilter(in("status", java.util.Arrays.asList(
                ErpCsConstants.TICKET_STATUS_ASSIGNED, ErpCsConstants.TICKET_STATUS_IN_PROGRESS)));
        q.addFilter(lt("deadlineDateTime", now));
        q.addFilter(eq("isSlaCompleted", Boolean.FALSE));
        // deadlineDateTime 的 XMeta 仅允许 eq/in/dateBetween/dateTimeBetween（不支持 lt），
        // 内部派生查询走 doFindListByQueryDirectly 绕过 meta 限制（同 ErpCrmEventBizModel.findDueReminders 模式）
        List<ErpCsTicket> overdue = doFindListByQueryDirectly(q, context);
        List<ErpCsTicket> escalated = new ArrayList<>();
        for (ErpCsTicket ticket : overdue) {
            // 创建 ESCALATE 审计 + 通知 escalationUserId（L1，config-gated；通知占位，实际发送属 nop-notification 独立面）
            writeAction(ticket, ErpCsConstants.ACTION_TYPE_ESCALATE, ticket.getStatus(), ticket.getStatus(),
                    "SLA 超时升级通知 escalationUserId", context);
            // 通知派发（config-gated）：超时升级时通知客服主管/分派人，复用既有 erp-cs-sla-scan scheduler job 自动派发
            notifySlaOverdue(ticket, context);
            escalated.add(ticket);
        }
        return escalated;
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
            ctx.put("deadlineDateTime", ticket.getDeadlineDateTime());
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

    private void cancelUnrespondedSurvey(Long ticketId, IServiceContext context) {
        // 查找该工单未响应的调查（respondedAt 空），删除以避免误发
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.setLimit(1);
        List<ErpCsSurvey> list = surveyBiz.findList(q, null, context);
        for (ErpCsSurvey survey : list) {
            if (survey.getRespondedAt() == null) {
                surveyBiz.delete(String.valueOf(survey.getId()), context);
            }
        }
    }
}
