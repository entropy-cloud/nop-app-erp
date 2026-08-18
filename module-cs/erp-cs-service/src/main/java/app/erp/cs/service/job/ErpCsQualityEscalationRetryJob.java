package app.erp.cs.service.job;

import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.processor.ErpCsTicketEscalateToQualityProcessor;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 质量升级后台重试 Job Bean（RC-R1.68，P1-RC-057，UC-CS-06 异常条款「quality 域服务不可用 →
 * 延迟创建 NCR，工单先保留状态，后台自动重试」）。
 *
 * <p>R1.37 简单 job 范式：由 nop-job-local 的 scheduler.yaml 经 BeanMethodJobInvoker 反射调用
 * {@link #execute()}；双层门控 = job.yaml 调度级 enabled/cron-expr + bean 内
 * {@code erp-cs.quality-retry-cron} 空值跳过（「不调度」语义）。扫描 QUALITY_ESCALATE 审计行中
 * content 以 {@code PENDING:} 开头的重试队列载体（limit 200，updateTime desc 使新近失败行优先），
 * 逐条经 {@link ErpCsTicketEscalateToQualityProcessor#retryPendingEscalation} 重试
 * （创建前反查既有 NCR 防重复；重试计数超 {@code erp-cs.quality-retry-max} 跳过并 WARN）；
 * 单条失败隔离（try/catch per record，不阻断后续）。
 */
public class ErpCsQualityEscalationRetryJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCsQualityEscalationRetryJob.class);

    /** 单次扫描最大条数（分页 limit 保护，对齐 ErpLogDraftEscalationJob 先例）。 */
    static final int SCAN_LIMIT = 200;

    @Inject
    IErpCsTicketBiz ticketBiz;
    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    ErpCsTicketEscalateToQualityProcessor escalateToQualityProcessor;
    @Inject
    IOrmTemplate ormTemplate;

    public void setTicketBiz(IErpCsTicketBiz ticketBiz) {
        this.ticketBiz = ticketBiz;
    }

    public void setTicketActionBiz(IErpCsTicketActionBiz ticketActionBiz) {
        this.ticketActionBiz = ticketActionBiz;
    }

    public void setEscalateToQualityProcessor(ErpCsTicketEscalateToQualityProcessor escalateToQualityProcessor) {
        this.escalateToQualityProcessor = escalateToQualityProcessor;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描 PENDING 质量升级审计行并逐条重试。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-cs-quality-retry-skipped: cron config empty (erp-cs.quality-retry-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int retried = runRetry(ctx);
            LOG.info("erp-cs-quality-retry-done: retried={}", retried);
        } catch (Exception e) {
            LOG.error("erp-cs-quality-retry-failed", e);
        }
    }

    /**
     * 扫描 PENDING 行并重试；返回成功修正为 NCR:{code} 的条数。
     * 扫描与读取在同一 ORM session 内完成（实体保持 MANAGED 供 content 原位修正）。
     */
    protected int runRetry(IServiceContext ctx) {
        return ormTemplate.runInSession(session -> {
            List<ErpCsTicketAction> pendingActions = findPendingActions(ctx);
            int count = 0;
            for (ErpCsTicketAction action : pendingActions) {
                try {
                    ErpCsTicket ticket = ticketBiz.get(String.valueOf(action.getTicketId()), true, ctx);
                    if (ticket == null) {
                        LOG.warn("erp-cs-quality-retry: 工单不存在（跳过）：ticketId={}", action.getTicketId());
                        continue;
                    }
                    if (escalateToQualityProcessor.retryPendingEscalation(ticket, action, ctx)) {
                        count++;
                    }
                } catch (Exception e) {
                    LOG.warn("erp-cs-quality-retry: 单条重试失败（隔离继续）：ticketId={}, reason={}",
                            action.getTicketId(), e.getMessage());
                }
            }
            return count;
        });
    }

    /** PENDING 队列扫描：actionType=QUALITY_ESCALATE + content 前缀 Java 过滤（updateTime desc 新近失败优先）。 */
    protected List<ErpCsTicketAction> findPendingActions(IServiceContext ctx) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("actionType", ErpCsConstants.ACTION_TYPE_QUALITY_ESCALATE));
        q.addOrderField("updateTime", true);
        q.setLimit(SCAN_LIMIT);
        return ticketActionBiz.findList(q, null, ctx).stream()
                .filter(a -> a.getContent() != null
                        && a.getContent().startsWith(ErpCsTicketEscalateToQualityProcessor.CONTENT_PENDING_PREFIX))
                .collect(java.util.stream.Collectors.toList());
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpCsConstants.CONFIG_QUALITY_RETRY_CRON, "");
    }
}
