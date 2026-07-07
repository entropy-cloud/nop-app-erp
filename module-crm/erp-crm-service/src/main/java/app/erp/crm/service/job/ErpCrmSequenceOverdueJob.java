package app.erp.crm.service.job;

import app.erp.crm.biz.IErpCrmLeadBiz;
import app.erp.crm.biz.IErpCrmLeadSequenceProgressBiz;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.service.ErpCrmConfigs;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 定时序列逾期检查 Job Bean（plan 2026-07-07-1430-3 §Phase 1）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每日 06:00）。
 *
 * <p>实际执行门控：{@code erp-crm.sequence.overdue-check-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时调 {@link IErpCrmLeadSequenceProgressBiz#scanOverdueSteps} 取连续逾期 ≥ max-overdue-steps 的进度，
 * 逐条经 {@link IErpSysNotificationBiz#notify}（{@code crm.sequence-overdue}）派发提醒。
 * 单条失败隔离（try/catch per progress，不阻断后续）。
 *
 * <p>复用 {@code 0642-1/0306-1} job bean 范式（cron 门控 + 委托 + try/catch 单条隔离）。
 */
public class ErpCrmSequenceOverdueJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCrmSequenceOverdueJob.class);

    @Inject
    IErpCrmLeadSequenceProgressBiz progressBiz;
    @Inject
    IErpCrmLeadBiz leadBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    public void setProgressBiz(IErpCrmLeadSequenceProgressBiz progressBiz) {
        this.progressBiz = progressBiz;
    }

    public void setLeadBiz(IErpCrmLeadBiz leadBiz) {
        this.leadBiz = leadBiz;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描逾期进度并逐条派发通知。
     */
    public void execute() {
        String cron = ErpCrmConfigs.sequenceOverdueCheckCron();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-crm-sequence-overdue-skipped: cron config empty (erp-crm.sequence.overdue-check-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int notified = runScan(ctx);
            LOG.info("erp-crm-sequence-overdue-done: notified={}", notified);
        } catch (Exception e) {
            LOG.error("erp-crm-sequence-overdue-failed", e);
        }
    }

    /**
     * 扫描连续逾期进度并逐条派发通知（单条失败隔离）。返回成功派发通知的条数。
     */
    protected int runScan(IServiceContext ctx) {
        List<Map<String, Object>> overdueList = progressBiz.scanOverdueSteps(ctx);
        if (overdueList == null || overdueList.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Map<String, Object> row : overdueList) {
            try {
                notifyOverdue(row, ctx);
                count++;
            } catch (Exception e) {
                LOG.warn("erp-crm-sequence-overdue: 单条逾期通知失败（隔离继续）：progressId={}, reason={}",
                        row.get("progressId"), e.getMessage());
            }
        }
        return count;
    }

    /**
     * 派发单条序列逾期提醒通知。protected 以允许测试/下游覆盖。
     */
    protected void notifyOverdue(Map<String, Object> row, IServiceContext ctx) {
        if (notificationBiz == null) {
            return;
        }
        Long leadId = toLong(row.get("leadId"));
        String ownerId = null;
        if (leadId != null) {
            ErpCrmLead lead = leadBiz.get(String.valueOf(leadId), false, ctx);
            if (lead != null) {
                ownerId = lead.getOwnerId();
            }
        }
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("leadId", leadId);
        map.put("progressId", row.get("progressId"));
        map.put("sequenceId", row.get("sequenceId"));
        map.put("currentStepIndex", row.get("currentStepIndex"));
        map.put("overdueStepCount", row.get("overdueStepCount"));
        map.put("ownerUserId", ownerId);
        notificationBiz.notify(ErpCrmConstants.NOTIFY_EVENT_SEQUENCE_OVERDUE, map, ctx);
    }

    protected Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
