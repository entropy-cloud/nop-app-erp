package app.erp.fin.service.job;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.dto.AutoReconResult;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定时自动核销 Job Bean（plan 2026-07-05-0115-1 Phase 2）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（默认每日凌晨）。
 *
 * <p>实际执行门控：{@code erp-fin.ar-ap-auto-recon-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时对 RECEIVABLE + PAYABLE 两方向全 partner FIFO 自动核销。
 *
 * <p>调用 {@link IErpFinReconciliationBiz#runAutoReconciliation}（@BizMutation，事务由 BizModel 承接）。
 * nop-batch 分 chunk 迁移留 Deferred（触发条件：单次匹配量 ≥ 数万）。
 */
public class ErpFinAutoReconJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpFinAutoReconJob.class);

    @Inject
    IErpFinReconciliationBiz reconciliationBiz;

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时执行 RECEIVABLE + PAYABLE 全 partner FIFO 自动核销。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-fin-auto-recon-skipped: cron config empty (erp-fin.ar-ap-auto-recon-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        runDirection(ErpFinConstants.DIRECTION_RECEIVABLE, ctx);
        runDirection(ErpFinConstants.DIRECTION_PAYABLE, ctx);
    }

    protected void runDirection(String direction, IServiceContext ctx) {
        try {
            AutoReconResult result = reconciliationBiz.runAutoReconciliation(
                    direction, null, ErpFinConstants.AUTO_RECON_STRATEGY_FIFO, ctx);
            LOG.info("erp-fin-auto-recon-done: direction={} reconciliations={} unmatched={}",
                    direction, result.getReconciliationIds().size(), result.getUnmatched().size());
        } catch (Exception e) {
            LOG.error("erp-fin-auto-recon-failed: direction={}", direction, e);
        }
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpFinConstants.CONFIG_AR_AP_AUTO_RECON_CRON, "");
    }
}
