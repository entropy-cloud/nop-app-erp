package app.erp.qa.service.spc;

import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.service.ErpQaConfigs;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * SPC 定时采样 Job（{@code docs/design/quality/spc.md §关键流程 1}，plan 2026-07-07-0305-2 Phase 2）。
 *
 * <p>双层门控（镜像 {@code ErpPrjPnlCalcJob} 范式）：
 * <ol>
 *   <li>Layer 1：{@code erp-qa.spc-sampling-cron} 空 → 跳过。</li>
 *   <li>Layer 2：{@code erp-qa.spc-enabled}（默认 false）→ 跳过。</li>
 * </ol>
 *
 * <p>由 nop-job-local scheduler.yaml 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 扫描 isActive 控制图批量执行 {@link SpcSamplingService#collectSamples} 与
 * {@link SpcControlLimitCalculator#recalculate}（采样后立即尝试重算）。
 */
public class ErpQaSpcSamplingJob {

    private static final Logger LOG = LoggerFactory.getLogger(ErpQaSpcSamplingJob.class);

    @Inject
    SpcSamplingService spcSamplingService;
    @Inject
    SpcControlLimitCalculator spcControlLimitCalculator;
    @Inject
    IDaoProvider daoProvider;

    public void setSpcSamplingService(SpcSamplingService spcSamplingService) {
        this.spcSamplingService = spcSamplingService;
    }

    public void setSpcControlLimitCalculator(SpcControlLimitCalculator spcControlLimitCalculator) {
        this.spcControlLimitCalculator = spcControlLimitCalculator;
    }

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void execute() {
        String cron = ErpQaConfigs.getSpcSamplingCron();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-qa-spc-sampling-skipped: cron config empty (erp-qa.spc-sampling-cron)");
            return;
        }
        if (!ErpQaConfigs.isSpcEnabled()) {
            LOG.info("erp-qa-spc-sampling-skipped: spc disabled (erp-qa.spc-enabled=false)");
            return;
        }

        IServiceContext ctx = new ServiceContextImpl();
        int processed = 0;
        int failed = 0;
        try {
            for (ErpQaSpcChart chart : findActiveCharts()) {
                try {
                    spcSamplingService.collectSamples(chart.getId(), ctx);
                    spcControlLimitCalculator.recalculate(chart.getId());
                    processed++;
                } catch (Exception e) {
                    failed++;
                    LOG.warn("erp-qa-spc-sampling-failed: chartId={}", chart.getId(), e);
                }
            }
            LOG.info("erp-qa-spc-sampling-done: processed={} failed={}", processed, failed);
        } catch (Exception e) {
            LOG.error("erp-qa-spc-sampling-aborted", e);
        }
    }

    private List<ErpQaSpcChart> findActiveCharts() {
        IEntityDao<ErpQaSpcChart> dao = daoProvider.daoFor(ErpQaSpcChart.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        return dao.findAllByQuery(q);
    }
}
