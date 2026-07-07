package app.erp.crm.service.job;

import app.erp.crm.biz.IErpCrmLeadFunnelBiz;
import app.erp.crm.service.ErpCrmConfigs;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * 定时漏斗聚合 Job Bean（plan 2026-07-07-1430-3 §Phase 2）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每日 03:30）。
 *
 * <p>实际执行门控：{@code erp-crm.funnel.aggregation-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时按 {@code retention-period-months} 计算最近一期完整月份（当月）全量刷新漏斗快照
 * （维度 territoryId/teamId/sourceId 均为 null 表示全量）。
 *
 * <p>复用 {@code 0306-1/0642-1} job bean 范式（cron 门控 + 委托）。
 */
public class ErpCrmFunnelAggregationJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCrmFunnelAggregationJob.class);

    @Inject
    IErpCrmLeadFunnelBiz funnelBiz;

    public void setFunnelBiz(IErpCrmLeadFunnelBiz funnelBiz) {
        this.funnelBiz = funnelBiz;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时按当月窗口全量刷新漏斗快照。
     */
    public void execute() {
        String cron = ErpCrmConfigs.funnelAggregationCron();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-crm-funnel-aggregation-skipped: cron config empty (erp-crm.funnel.aggregation-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        LocalDate today = CoreMetrics.today();
        LocalDate periodStart = today.withDayOfMonth(1);
        LocalDate periodEnd = today.withDayOfMonth(today.lengthOfMonth());
        try {
            funnelBiz.refreshFunnel(periodStart, periodEnd, null, null, null, ctx);
            LOG.info("erp-crm-funnel-aggregation-done: periodStart={} periodEnd={}", periodStart, periodEnd);
        } catch (Exception e) {
            LOG.error("erp-crm-funnel-aggregation-failed: periodStart={} periodEnd={}", periodStart, periodEnd, e);
        }
    }
}
