package app.erp.cs.service.job;

import app.erp.cs.biz.IErpCsSurveyBiz;
import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 定时 CSAT 调查到期提醒 Job Bean（plan 2026-07-06-0642-1 §Phase 2）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每日 02:00）。
 *
 * <p>实际执行门控：{@code erp-cs.csat-reminder-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时调 {@link IErpCsSurveyBiz#findSurveyReminders}（未响应超期）+
 * {@link IErpCsSurveyBiz#findExpiredSurveys}（已过期未响应）取目标调查，
 * 逐调查调 {@link IErpSysNotificationBiz#notify}（{@code cs.csat-reminder}）。
 * 单条失败隔离（try/catch per survey，不阻断后续）。
 *
 * <p>接收人=客服 agent（工单 assignedToId）；若工单未分派则由模板 ROLE resolver 兜底。
 */
public class ErpCsCsatReminderJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCsCsatReminderJob.class);

    @Inject
    IErpCsSurveyBiz surveyBiz;
    @Inject
    IErpCsTicketBiz ticketBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IDaoProvider daoProvider;

    public void setSurveyBiz(IErpCsSurveyBiz surveyBiz) {
        this.surveyBiz = surveyBiz;
    }

    public void setTicketBiz(IErpCsTicketBiz ticketBiz) {
        this.ticketBiz = ticketBiz;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时查未响应/已过期调查并派发提醒通知。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-cs-csat-reminder-skipped: cron config empty (erp-cs.csat-reminder-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int notified = runReminders(ctx);
            LOG.info("erp-cs-csat-reminder-done: notified={}", notified);
        } catch (Exception e) {
            LOG.error("erp-cs-csat-reminder-failed", e);
        }
    }

    /**
     * 查询未响应/过期调查并逐条派发通知（单条失败隔离）。返回成功派发通知的调查条数。
     */
    protected int runReminders(IServiceContext ctx) {
        List<ErpCsSurvey> reminders = surveyBiz.findSurveyReminders(null, ctx);
        List<ErpCsSurvey> expired = surveyBiz.findExpiredSurveys(null, ctx);
        int count = 0;
        count += notifyAll(reminders, "REMINDER", ctx);
        count += notifyAll(expired, "EXPIRED", ctx);
        return count;
    }

    /** 派发列表内每条调查的提醒通知，单条失败隔离。protected 以允许测试覆盖。 */
    protected int notifyAll(List<ErpCsSurvey> surveys, String state, IServiceContext ctx) {
        if (surveys == null || surveys.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ErpCsSurvey survey : surveys) {
            try {
                notifySurvey(survey, state, ctx);
                count++;
            } catch (Exception e) {
                LOG.warn("erp-cs-csat-reminder: 单条调查通知失败（隔离继续）：surveyId={}, reason={}",
                        survey.getId(), e.getMessage());
            }
        }
        return count;
    }

    /** 派发单条调查提醒通知。protected 以允许测试覆盖（如计数器替代实际 notificationBiz 调用）。 */
    protected void notifySurvey(ErpCsSurvey survey, String state, IServiceContext ctx) {
        if (notificationBiz == null) {
            return;
        }
        ErpCsTicket ticket = loadTicket(survey.getTicketId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("surveyId", survey.getId());
        map.put("ticketCode", ticket == null ? null : ticket.getCode());
        map.put("customerName", ticket == null ? null : ticket.getSubject());
        map.put("state", state);
        notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_CSAT_REMINDER, map, ctx);
    }

    private ErpCsTicket loadTicket(Long ticketId) {
        if (ticketId == null) {
            return null;
        }
        try {
            QueryBean q = new QueryBean();
            q.addFilter(eq("id", ticketId));
            q.setLimit(1);
            List<ErpCsTicket> list = daoProvider.daoFor(ErpCsTicket.class).findAllByQuery(q);
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpCsConstants.CONFIG_CSAT_REMINDER_CRON, "");
    }
}
