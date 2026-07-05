package app.erp.crm.service.job;

import app.erp.crm.biz.IErpCrmEventBiz;
import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时活动到期提醒 Job Bean（plan 2026-07-06-0642-1 §Phase 2）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每 15 分钟）。
 *
 * <p>实际执行门控：{@code erp-crm.event-reminder-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时调 {@link IErpCrmEventBiz#findDueReminders} 取到期窗口内 PLANNED 事件，
 * 逐事件调 {@link IErpSysNotificationBiz#notify}（{@code crm.event-reminder}）。
 * 单条失败隔离（try/catch per event，不阻断后续）。
 *
 * <p>复用 {@code 0306-1} job bean 范式（cron 门控 + 委托 + try/catch 单条隔离）。
 */
public class ErpCrmEventReminderJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCrmEventReminderJob.class);

    /** 默认查询窗口（分钟），{@code findDueReminders} 未传参时使用。 */
    static final int DEFAULT_WINDOW_MINUTES = 60;

    @Inject
    IErpCrmEventBiz eventBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    public void setEventBiz(IErpCrmEventBiz eventBiz) {
        this.eventBiz = eventBiz;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时查到期事件并派发提醒通知。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-crm-event-reminder-skipped: cron config empty (erp-crm.event-reminder-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int notified = runReminders(ctx);
            LOG.info("erp-crm-event-reminder-done: notified={}", notified);
        } catch (Exception e) {
            LOG.error("erp-crm-event-reminder-failed", e);
        }
    }

    /**
     * 查询到期事件并逐条派发通知（单条失败隔离）。
     * 返回成功派发通知的事件条数。
     */
    protected int runReminders(IServiceContext ctx) {
        List<ErpCrmEvent> events = eventBiz.findDueReminders(DEFAULT_WINDOW_MINUTES, ctx);
        if (events == null || events.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ErpCrmEvent event : events) {
            try {
                notifyEvent(event, ctx);
                count++;
            } catch (Exception e) {
                // 单条失败隔离：不影响后续事件提醒派发
                LOG.warn("erp-crm-event-reminder: 单条事件通知失败（隔离继续）：eventId={}, reason={}",
                        event.getId(), e.getMessage());
            }
        }
        return count;
    }

    /**
     * 派发单条事件提醒通知。protected 以允许测试/下游覆盖（如注入 mock notificationBiz）。
     */
    protected void notifyEvent(ErpCrmEvent event, IServiceContext ctx) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventId", event.getId());
        map.put("title", event.getSubject());
        map.put("ownerUserId", event.getOwnerId());
        map.put("dueTime", event.getStartDateTime());
        map.put("leadName", event.getRelatedBillCode());
        notificationBiz.notify(ErpCrmConstants.NOTIFY_EVENT_EVENT_REMINDER, map, ctx);
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpCrmConstants.CONFIG_EVENT_REMINDER_CRON, "");
    }
}
