package app.erp.crm.service.job;

import app.erp.crm.biz.IErpCrmLeadBiz;
import app.erp.crm.biz.IErpCrmLeadScoreBiz;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.notIn;

/**
 * 定时线索评分批量重算 Job Bean（plan 2026-07-05-0306-1 Phase 1）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每日 02:00）。
 *
 * <p>实际执行门控：{@code erp-crm.lead-scoring.schedule-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时迭代 active 线索（docStatus 非终态 CONVERTED/LOST/CANCELLED）逐条调
 * {@link IErpCrmLeadScoreBiz#recalculateScore}（triggerEvent=SCHEDULED，经 {@link ErpCrmConstants} 常量派生避免魔法串）。
 * 单线索失败 try/catch 隔离不阻断后续线索。
 *
 * <p>active 线索查询经 {@link IErpCrmLeadBiz}（ICrudBiz findList），符合跨实体 I*Biz 访问约定。
 * 迭代量大时的断点续跑归 nop-batch 迁移 Deferred。
 */
public class ErpCrmLeadScoringRecalcJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCrmLeadScoringRecalcJob.class);

    /** active 线索 = docStatus 非终态（CONVERTED/LOST/CANCELLED）。 */
    private static final List<String> LEAD_TERMINAL_STATUSES = Arrays.asList(
            ErpCrmConstants.DOC_STATUS_CONVERTED,
            ErpCrmConstants.DOC_STATUS_LOST,
            ErpCrmConstants.DOC_STATUS_CANCELLED);

    @Inject
    IErpCrmLeadScoreBiz leadScoreBiz;
    @Inject
    IErpCrmLeadBiz leadBiz;

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时迭代 active 线索逐条重算评分（单线索失败隔离）。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-crm-lead-scoring-recalc-skipped: cron config empty (erp-crm.lead-scoring.schedule-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        List<ErpCrmLead> activeLeads = findActiveLeads(ctx);
        int success = 0;
        int failed = 0;
        for (ErpCrmLead lead : activeLeads) {
            try {
                runRecalculateScore(lead.getId(), ctx);
                success++;
            } catch (Exception e) {
                failed++;
                LOG.error("erp-crm-lead-scoring-recalc-lead-failed: leadId={}", lead.getId(), e);
            }
        }
        LOG.info("erp-crm-lead-scoring-recalc-done: total={} success={} failed={}",
                activeLeads.size(), success, failed);
    }

    protected void runRecalculateScore(Long leadId, IServiceContext ctx) {
        leadScoreBiz.recalculateScore(leadId, ErpCrmConstants.TRIGGER_EVENT_SCHEDULED, ctx);
    }

    protected List<ErpCrmLead> findActiveLeads(IServiceContext ctx) {
        QueryBean q = new QueryBean();
        q.addFilter(notIn("docStatus", LEAD_TERMINAL_STATUSES));
        return leadBiz.findList(q, null, ctx);
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpCrmConstants.CONFIG_LEAD_SCORING_SCHEDULE_CRON, "");
    }
}
