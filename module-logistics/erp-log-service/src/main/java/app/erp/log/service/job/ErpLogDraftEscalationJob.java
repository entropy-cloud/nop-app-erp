package app.erp.log.service.job;

import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.ErpLogConfigs;
import app.erp.log.service.ErpLogConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.dateTimeBetween;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * DRAFT 发运单超阈值升级 Job Bean（RC-R1.37，P1-RC-084，UC-LOG-01「超过 24 小时未确认的 DRAFT 发运单触发升级通知」）。
 *
 * <p>R1.4 简单 job bean 范式：由 nop-job-local 的 {@code scheduler.yaml} 经
 * BeanMethodJobInvoker 反射调用 {@link #execute()}，触发频率由
 * {@code erp-log-draft-escalation.job.yaml} 的 cronExpr 决定（默认每日 01:30）。
 *
 * <p>双层门控：job.yaml {@code nop.job.erp-log-draft-escalation.enabled|cron-expr}（调度级）+
 * bean 内 {@code erp-log.draft-escalation-cron} 配置空值跳过（「不调度」语义，对齐
 * {@code ErpCtApprovalTimeoutEscalationJob} 双键范式）。扫描 DRAFT 且 updateTime 超
 * {@code erp-log.draft-escalation-hours}（默认 24）的发运单，逐条派发升级通知
 * （context 键 {@code submitterUserId} 承载 {@code createdBy} 发货员，对齐 D2 裁决
 * USER_LIST 模板插值）；单条失败隔离（try/catch per record，不阻断后续）。
 */
public class ErpLogDraftEscalationJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpLogDraftEscalationJob.class);

    /** 单次扫描最大条数（分页 limit 保护，对齐 ErpCtApprovalTimeoutEscalationJob 先例）。 */
    static final int SCAN_LIMIT = 200;

    @Inject
    IErpLogShipmentBiz shipmentBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IOrmTemplate ormTemplate;

    public void setShipmentBiz(IErpLogShipmentBiz shipmentBiz) {
        this.shipmentBiz = shipmentBiz;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描超阈值 DRAFT 发运单并逐条派发升级通知。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-log-draft-escalation-skipped: cron config empty (erp-log.draft-escalation-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int escalated = runDraftEscalation(ctx);
            LOG.info("erp-log-draft-escalation-done: escalated={}", escalated);
        } catch (Exception e) {
            LOG.error("erp-log-draft-escalation-failed", e);
        }
    }

    /**
     * 扫描超阈值 DRAFT 发运单并逐条派发升级通知；返回成功派发条数。
     * 扫描与读取在同一 ORM session 内完成（findList 返回的实体保持 MANAGED）。
     */
    protected int runDraftEscalation(IServiceContext ctx) {
        long timeoutHours = resolveTimeoutHours();
        LocalDateTime cutoff = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(CoreMetrics.currentTimeMillis() - timeoutHours * 3600_000L),
                java.time.ZoneId.systemDefault());
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("status", ErpLogConstants.SHIPMENT_STATUS_DRAFT));
            // updateTime < now - timeoutHours 语义：以 dateTimeBetween(epoch, cutoff) 表达
            // （对齐 ErpCtApprovalTimeoutEscalationJob:113-117 先例，XMeta 过滤操作集不支持 lt）。
            q.addFilter(dateTimeBetween("updateTime",
                    LocalDateTime.of(1970, 1, 1, 0, 0), cutoff));
            q.setLimit(SCAN_LIMIT);
            List<ErpLogShipment> shipments = shipmentBiz.findList(q, null, ctx);
            if (shipments == null || shipments.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (ErpLogShipment shipment : shipments) {
                try {
                    if (escalateShipment(shipment, timeoutHours, ctx)) {
                        count++;
                    }
                } catch (Exception e) {
                    LOG.warn("erp-log-draft-escalation: 单条升级失败（隔离继续）：shipmentId={}, reason={}",
                            shipment.getId(), e.getMessage());
                }
            }
            return count;
        });
    }

    /**
     * 单条 DRAFT 超阈值升级：经 {@code IErpSysNotificationBiz.notify} 派发
     * {@code log.draft-escalation} 事件（无 ACTIVE 模板时 notify 内部静默跳过）。返回 true 表示已派发。
     * context 键对齐 D2 裁决 {@code log.draft-escalation} 模板约定：shipmentId/shipmentCode/
     * submitterUserId（=createdBy 发货员）/elapsedHours，USER_LIST 接收人经 ${submitterUserId} 插值。
     */
    protected boolean escalateShipment(ErpLogShipment shipment, long timeoutHours, IServiceContext ctx) {
        if (notificationBiz == null) {
            return false;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("shipmentId", shipment.getId());
        map.put("shipmentCode", shipment.getCode());
        map.put("submitterUserId", shipment.getCreatedBy());
        map.put("elapsedHours", timeoutHours);
        notificationBiz.notify(ErpLogConstants.NOTIFY_EVENT_DRAFT_ESCALATION, map, ctx);
        return true;
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpLogConfigs.CONFIG_DRAFT_ESCALATION_CRON, "");
    }

    protected long resolveTimeoutHours() {
        return AppConfig.var(ErpLogConfigs.CONFIG_DRAFT_ESCALATION_HOURS,
                ErpLogConfigs.DEFAULT_DRAFT_ESCALATION_HOURS);
    }
}
