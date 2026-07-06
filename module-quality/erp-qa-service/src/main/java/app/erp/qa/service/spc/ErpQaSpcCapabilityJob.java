package app.erp.qa.service.spc;

import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.service.ErpQaConfigs;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;

/**
 * SPC 过程能力分析定时 Job（{@code docs/design/quality/spc.md §关键流程 4}，plan 2026-07-07-0305-2 Phase 4）。
 *
 * <p>双层门控：
 * <ol>
 *   <li>Layer 1：{@code erp-qa.spc-capability-cron} 空 → 跳过。</li>
 *   <li>Layer 2：{@code erp-qa.spc-enabled}（默认 false）→ 跳过。</li>
 * </ol>
 *
 * <p>周期性（月/周）扫描 active 控制图批量调用 {@link SpcCapabilityCalculator#calculateCapability}
 * （默认周期=过去 30 天）。
 */
public class ErpQaSpcCapabilityJob {

    private static final Logger LOG = LoggerFactory.getLogger(ErpQaSpcCapabilityJob.class);

    private static final int DEFAULT_PERIOD_DAYS = 30;

    @Inject
    SpcCapabilityCalculator spcCapabilityCalculator;
    @Inject
    IDaoProvider daoProvider;

    public void setSpcCapabilityCalculator(SpcCapabilityCalculator spcCapabilityCalculator) {
        this.spcCapabilityCalculator = spcCapabilityCalculator;
    }

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void execute() {
        String cron = ErpQaConfigs.getSpcCapabilityCron();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-qa-spc-capability-skipped: cron config empty (erp-qa.spc-capability-cron)");
            return;
        }
        if (!ErpQaConfigs.isSpcEnabled()) {
            LOG.info("erp-qa-spc-capability-skipped: spc disabled (erp-qa.spc-enabled=false)");
            return;
        }

        IServiceContext ctx = new ServiceContextImpl();
        LocalDate periodTo = LocalDate.now();
        LocalDate periodFrom = periodTo.minusDays(DEFAULT_PERIOD_DAYS);
        int processed = 0;
        int failed = 0;
        try {
            for (ErpQaSpcChart chart : findActiveCharts()) {
                try {
                    spcCapabilityCalculator.calculateCapability(chart.getId(), periodFrom, periodTo, ctx);
                    processed++;
                } catch (Exception e) {
                    failed++;
                    LOG.warn("erp-qa-spc-capability-failed: chartId={}", chart.getId(), e);
                }
            }
            LOG.info("erp-qa-spc-capability-done: processed={} failed={}", processed, failed);
        } catch (Exception e) {
            LOG.error("erp-qa-spc-capability-aborted", e);
        }
    }

    private List<ErpQaSpcChart> findActiveCharts() {
        IEntityDao<ErpQaSpcChart> dao = daoProvider.daoFor(ErpQaSpcChart.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        return dao.findAllByQuery(q);
    }
}
