package app.erp.prj.service.job;

import app.erp.prj.biz.IErpPrjProjectPnlBiz;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.pnl.ProjectPnlCalculator;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 项目损益汇总定时计算 Job（{@code profitability.md §关键流程 1}，plan 2026-07-07-0305-1 Phase 2）。
 *
 * <p>双层门控（镜像 {@code ErpMfgJobCardAutoGenJob} 范式）：
 * <ol>
 *   <li>Layer 1：{@code erp-prj.pnl-calc-cron} 空 → 跳过（本类 {@link #execute()}）。</li>
 *   <li>Layer 2：{@code erp-prj.pnl-auto-calc-enabled}（默认 false）→ 跳过批量计算。</li>
 * </ol>
 *
 * <p>由 nop-job-local scheduler.yaml 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 扫描活跃状态（DRAFT/OPEN/ON_HOLD）项目，逐项目调用 {@link ProjectPnlCalculator#refreshPnl} 全期间汇总。
 */
public class ErpPrjPnlCalcJob {

    private static final Logger LOG = LoggerFactory.getLogger(ErpPrjPnlCalcJob.class);

    private static final List<String> ACTIVE_STATUSES = Arrays.asList(
            ErpPrjConstants.PROJECT_STATUS_DRAFT,
            ErpPrjConstants.PROJECT_STATUS_OPEN,
            ErpPrjConstants.PROJECT_STATUS_ON_HOLD);

    @Inject
    IErpPrjProjectPnlBiz pnlBiz;
    @Inject
    IDaoProvider daoProvider;

    public void execute() {
        String cron = ErpPrjConfigs.pnlCalcCron();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-prj-pnl-calc-skipped: cron config empty (erp-prj.pnl-calc-cron)");
            return;
        }
        if (!ErpPrjConfigs.pnlAutoCalcEnabled()) {
            LOG.info("erp-prj-pnl-calc-skipped: auto-calc disabled (erp-prj.pnl-auto-calc-enabled=false)");
            return;
        }

        IServiceContext ctx = new ServiceContextImpl();
        int processed = 0;
        int failed = 0;
        try {
            for (ErpPrjProject project : findActiveProjects()) {
                try {
                    pnlBiz.refreshPnl(project.getId(), null, null, ctx);
                    processed++;
                } catch (Exception e) {
                    failed++;
                    LOG.warn("erp-prj-pnl-calc-failed: projectId={}", project.getId(), e);
                }
            }
            LOG.info("erp-prj-pnl-calc-done: processed={} failed={}", processed, failed);
        } catch (Exception e) {
            LOG.error("erp-prj-pnl-calc-aborted", e);
        }
    }

    private List<ErpPrjProject> findActiveProjects() {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("status", ACTIVE_STATUSES));
        return dao.findAllByQuery(q);
    }
}
