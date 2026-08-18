package app.erp.cs.service.job;

import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.processor.ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 履行链自动重试 Job Bean（RC-R1.71，P1-RC-061，UC-CS-12 后置「异常可重试」+ 异常「超时自动审批」）。
 *
 * <p>R1.37 简单 job 范式：由 nop-job-local 的 scheduler.yaml 经 BeanMethodJobInvoker 反射调用
 * {@link #execute()}；双层门控 = job.yaml 调度级 enabled/cron-expr + bean 内
 * {@code erp-cs.fulfillment-retry-cron} 空值跳过（「不调度」语义）。
 *
 * <p>两段扫描（Processor {@code SCAN_LIMIT} 单批上限，逐条 try/catch 失败隔离，{@code ormTemplate.runInSession}
 * 包裹保持实体 MANAGED 原位修正）：
 * <ol>
 *   <li><b>REQUEST_APPROVAL 超时自动审批</b>——IN_PROGRESS + REQUEST_APPROVAL + now - executedAt &gt;
 *       timeoutHours（actionConfig.timeoutHours 覆盖 &gt; {@code erp-cs.fulfillment-approval-timeout-hours} 兜底）
 *       → 自动 DONE + 审计「超时自动审批」+ 链恢复推进；</li>
 *   <li><b>FAILED 未超限自动重试</b>——存在 FAILED 且 retryCount &lt; {@code erp-cs.fulfillment-retry-max}
 *       步骤的工单逐张重试（刷新模板 actionConfig 后重执行；审批驳回终局行 retryCount 已置 max 天然排除）；
 *       超限 → 终态保留 + 管理员通知人工介入（L1「超出后通知管理员人工介入」）。</li>
 * </ol>
 */
public class ErpCsFulfillmentRetryJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCsFulfillmentRetryJob.class);

    @Inject
    ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor fulfillmentProcessor;
    @Inject
    IOrmTemplate ormTemplate;

    public void setFulfillmentProcessor(
            ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor fulfillmentProcessor) {
        this.fulfillmentProcessor = fulfillmentProcessor;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时先超时自动审批（解除 IN_PROGRESS 阻塞），再自动重试 FAILED 未超限链。
     */
    public void execute() {
        String cron = ErpCsConfigs.getFulfillmentRetryCron();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-cs-fulfillment-retry-skipped: cron config empty (erp-cs.fulfillment-retry-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int[] result = ormTemplate.runInSession(session -> runRetry(ctx));
            LOG.info("erp-cs-fulfillment-retry-done: autoApproved={}, retriedSteps={}", result[0], result[1]);
        } catch (Exception e) {
            LOG.error("erp-cs-fulfillment-retry-failed", e);
        }
    }

    /**
     * 两段扫描 + 恢复；返回 [超时自动审批数, 自动重试步骤数]。
     * 扫描与修正同一 ORM session 内完成（实体保持 MANAGED 供状态原位修正，session 提交落库）。
     */
    protected int[] runRetry(IServiceContext ctx) {
        int autoApproved = fulfillmentProcessor.autoApproveTimedOut(ctx);
        int retried = 0;
        List<Long> candidates = fulfillmentProcessor.findRetryCandidateTicketIds(ctx);
        for (Long ticketId : candidates) {
            try {
                retried += fulfillmentProcessor.retryForJob(ticketId, ctx);
            } catch (Exception e) {
                LOG.warn("erp-cs-fulfillment-retry: 单张工单重试失败（隔离继续）：ticketId={}, reason={}",
                        ticketId, e.getMessage());
            }
        }
        return new int[]{autoApproved, retried};
    }
}
