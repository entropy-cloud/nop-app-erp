package app.erp.mfg.service.job;

import app.erp.mfg.biz.IErpMfgCrpLoadBiz;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 定时 CRP 产能负荷计算 Job Bean（plan 2026-07-05-0306-1 Phase 1）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每日凌晨）。
 *
 * <p>实际执行门控：{@code erp-mfg.crp-run-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时按 config-gated 窗口（默认当自然月）派生 [periodFrom, periodTo] 调
 * {@link IErpMfgCrpLoadBiz#calculateLoad}（workcenterIds=null 全工作中心，重算前清区间再写新行）。
 *
 * <p>负荷来源本期取 WorkOrder 计划日期（fallback，无 APS）；APS OperationOrder 接线归计划 2026-07-05-0306-2。
 */
public class ErpMfgCrpRunJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpMfgCrpRunJob.class);

    @Inject
    IErpMfgCrpLoadBiz crpLoadBiz;

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时按窗口计算 CRP 负荷快照。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-mfg-crp-run-skipped: cron config empty (erp-mfg.crp-run-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        LocalDate today = LocalDate.now();
        int windowMonths = resolveWindowMonths();
        LocalDate periodFrom = today.withDayOfMonth(1);
        LocalDate periodTo = YearMonth.from(today).plusMonths(windowMonths).atEndOfMonth();
        try {
            int rows = runCalculateLoad(periodFrom, periodTo, ctx);
            LOG.info("erp-mfg-crp-run-done: from={} to={} rows={}", periodFrom, periodTo, rows);
        } catch (Exception e) {
            LOG.error("erp-mfg-crp-run-failed: from={} to={}", periodFrom, periodTo, e);
        }
    }

    protected int runCalculateLoad(LocalDate periodFrom, LocalDate periodTo, IServiceContext ctx) {
        Integer rows = crpLoadBiz.calculateLoad(periodFrom, periodTo, null, ctx);
        return rows == null ? 0 : rows;
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpMfgConstants.CONFIG_CRP_RUN_CRON, "");
    }

    protected int resolveWindowMonths() {
        String raw = AppConfig.var(ErpMfgConstants.CONFIG_CRP_RUN_DEFAULT_WINDOW_MONTHS,
                String.valueOf(ErpMfgConstants.DEFAULT_CRP_RUN_WINDOW_MONTHS));
        if (StringHelper.isEmpty(raw)) {
            return ErpMfgConstants.DEFAULT_CRP_RUN_WINDOW_MONTHS;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return ErpMfgConstants.DEFAULT_CRP_RUN_WINDOW_MONTHS;
        }
    }
}
