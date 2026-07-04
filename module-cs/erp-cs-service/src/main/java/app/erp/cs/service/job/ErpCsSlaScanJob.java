package app.erp.cs.service.job;

import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConstants;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 定时 SLA 超时扫描 Job Bean（plan 2026-07-05-0306-1 Phase 1）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每分钟）。
 *
 * <p>实际执行门控：{@code erp-cs.sla-scan-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时调 {@link IErpCsTicketBiz#scanOverdueTickets} 扫描超时工单建 ESCALATE 审计 + 升级通知。
 * SLA 总开关 {@code erp-cs.sla-enabled} 关闭时 scanOverdueTickets 自身返回空（双重门控）。
 */
public class ErpCsSlaScanJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCsSlaScanJob.class);

    @Inject
    IErpCsTicketBiz ticketBiz;

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描 SLA 超时工单并升级。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-cs-sla-scan-skipped: cron config empty (erp-cs.sla-scan-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int escalated = runSlaScan(ctx);
            LOG.info("erp-cs-sla-scan-done: escalated={}", escalated);
        } catch (Exception e) {
            LOG.error("erp-cs-sla-scan-failed", e);
        }
    }

    protected int runSlaScan(IServiceContext ctx) {
        List<ErpCsTicket> escalated = ticketBiz.scanOverdueTickets(ctx);
        return escalated.size();
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpCsConstants.CONFIG_SLA_SCAN_CRON, "");
    }
}
