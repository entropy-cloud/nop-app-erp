package app.erp.mnt.service.job;

import app.erp.mnt.biz.IErpMntScheduleBiz;
import app.erp.mnt.service.ErpMntConstants;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * 定时到期访问生成 Job Bean（plan 2026-07-05-0306-1 Phase 1）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每日凌晨）。
 *
 * <p>实际执行门控：{@code erp-mnt.due-visit-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时以今天为基准调 {@link IErpMntScheduleBiz#generateDueVisits} 扫描 active 计划生成 DRAFT 访问并推进 nextDueDate。
 */
public class ErpMntDueVisitJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpMntDueVisitJob.class);

    @Inject
    IErpMntScheduleBiz scheduleBiz;

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时以 LocalDate.now() 为基准生成到期访问。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-mnt-due-visit-skipped: cron config empty (erp-mnt.due-visit-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        LocalDate asOfDate = LocalDate.now();
        try {
            Integer generated = runGenerateDueVisits(asOfDate, ctx);
            LOG.info("erp-mnt-due-visit-done: asOfDate={} generated={}", asOfDate, generated);
        } catch (Exception e) {
            LOG.error("erp-mnt-due-visit-failed: asOfDate={}", asOfDate, e);
        }
    }

    protected Integer runGenerateDueVisits(LocalDate asOfDate, IServiceContext ctx) {
        return scheduleBiz.generateDueVisits(asOfDate, ctx);
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpMntConstants.CONFIG_DUE_VISIT_CRON, "");
    }
}
