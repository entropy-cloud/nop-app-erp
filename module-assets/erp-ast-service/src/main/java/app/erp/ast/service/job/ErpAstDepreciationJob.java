package app.erp.ast.service.job;

import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.service.ErpAstConstants;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;

/**
 * 定时批量折旧 Job Bean（plan 2026-07-05-0306-1 Phase 1）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每月 1 日 02:00）。
 *
 * <p>实际执行门控：{@code erp-ast.depreciation-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时派生当前月 period（YYYY-MM）调 {@link IErpAstDepreciationScheduleBiz#executeBatchDepreciation}，
 * 单资产失败由 Processor 内部错误隔离（depreciation-and-posting.md §5.3）。
 */
public class ErpAstDepreciationJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpAstDepreciationJob.class);

    @Inject
    IErpAstDepreciationScheduleBiz depreciationScheduleBiz;

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时按当前自然月批量计提折旧。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-ast-depreciation-skipped: cron config empty (erp-ast.depreciation-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        String period = YearMonth.now().toString();
        try {
            int processed = runBatchDepreciation(period, ctx);
            LOG.info("erp-ast-depreciation-done: period={} assets={}", period, processed);
        } catch (Exception e) {
            LOG.error("erp-ast-depreciation-failed: period={}", period, e);
        }
    }

    protected int runBatchDepreciation(String period, IServiceContext ctx) {
        return depreciationScheduleBiz.executeBatchDepreciation(period, ctx);
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpAstConstants.CONFIG_DEPRECIATION_CRON, "");
    }
}
