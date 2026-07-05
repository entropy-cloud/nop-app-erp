package app.erp.mfg.service.job;

import app.erp.mfg.biz.IErpMfgWorkOrderBiz;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定时 APS 排程→工序卡自动生成 Job Bean（plan 2026-07-05-0427-3 Phase 3，参照 0306-1 三件套范式）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每日凌晨）。
 *
 * <p>双层门控（同 0306-1 范式）：
 * <ol>
 *   <li>{@code erp-mfg.jobcard-auto-generate-cron} 配置为空时跳过（"不调度"语义，本类负责）；</li>
 *   <li>{@code erp-mfg.jobcard-auto-generate-on-schedule} 总开关（默认 false），false 时
 *       {@link IErpMfgWorkOrderBiz#generatePendingJobCards} 内部直接返回 0（Processor 负责）。</li>
 * </ol>
 * 任一层关闭即不建卡；双层开启才实际生成。单工单失败隔离（Processor 内 try/catch 继续后续工单）。
 */
public class ErpMfgJobCardAutoGenJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpMfgJobCardAutoGenJob.class);

    @Inject
    IErpMfgWorkOrderBiz workOrderBiz;

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时调 {@link IErpMfgWorkOrderBiz#generatePendingJobCards}（内部按总开关再门控）。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-mfg-jobcard-auto-gen-skipped: cron config empty (erp-mfg.jobcard-auto-generate-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            Integer count = runGenerate(ctx);
            LOG.info("erp-mfg-jobcard-auto-gen-done: generatedWorkOrders={}", count);
        } catch (Exception e) {
            LOG.error("erp-mfg-jobcard-auto-gen-failed", e);
        }
    }

    protected Integer runGenerate(IServiceContext ctx) {
        Integer count = workOrderBiz.generatePendingJobCards(ctx);
        return count == null ? 0 : count;
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpMfgConstants.CONFIG_JOBCARD_AUTO_GENERATE_CRON, "");
    }
}
