package app.erp.log.service.job;

import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.service.ErpLogConfigs;
import io.nop.api.core.config.AppConfig;
import io.nop.orm.IOrmTemplate;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 追踪轮询兜底 Job Bean（RC-R1.38，P1-RC-085，UC-LOG-03「定时轮询间隔可配置（默认 4 小时）」）。
 *
 * <p>R1.4 简单 job bean 范式（D1 裁决选项 A）：由 nop-job-local 的 {@code scheduler.yaml} 经
 * BeanMethodJobInvoker 反射调用 {@link #execute()}，触发频率由
 * {@code erp-log-tracking-poll.job.yaml} 的 cronExpr 决定（默认每 4 小时）。
 *
 * <p>双层门控：job.yaml {@code nop.job.erp-log-tracking-poll.enabled|cron-expr}（调度级）+
 * bean 内 {@code erp-log.tracking-poll-cron} 配置空值跳过（「不调度」语义，对齐 R1.4 双键范式）。
 * 触发时调既有 {@code IErpLogShipmentBiz.scanForPolling} 一次完成全量推进（DISPATCHED/IN_TRANSIT →
 * trackShipment 推进；DELIVERED 翻转后逐单 onDelivered 运费过账/到岸成本编排，失败隔离由既有
 * {@code ErpLogShipmentScanForPollingProcessor} 内部保证）；本类零业务逻辑，仅接线。
 */
public class ErpLogTrackingPollJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpLogTrackingPollJob.class);

    @Inject
    IErpLogShipmentBiz shipmentBiz;
    @Inject
    IOrmTemplate ormTemplate;

    public void setShipmentBiz(IErpLogShipmentBiz shipmentBiz) {
        this.shipmentBiz = shipmentBiz;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时调 scanForPolling 推进 DISPATCHED/IN_TRANSIT 运单。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-log-tracking-poll-skipped: cron config empty (erp-log.tracking-poll-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            // 与 manual @BizMutation 路径一致：scanForPolling 全链（查询 + 状态推进 + 落库）
            // 在同一 ORM session 内执行，避免跨 session 的 MANAGED 实体保存冲突。
            int advanced = ormTemplate.runInSession(session -> shipmentBiz.scanForPolling(ctx));
            LOG.info("erp-log-tracking-poll-done: advanced={}", advanced);
        } catch (Exception e) {
            LOG.error("erp-log-tracking-poll-failed", e);
        }
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpLogConfigs.CONFIG_TRACKING_POLLING_CRON, "");
    }
}
