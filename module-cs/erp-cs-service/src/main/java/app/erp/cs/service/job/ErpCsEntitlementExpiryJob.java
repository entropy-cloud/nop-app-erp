package app.erp.cs.service.job;

import app.erp.cs.biz.IErpCsEntitlementBiz;
import app.erp.cs.dao.entity.ErpCsEntitlement;
import app.erp.cs.service.ErpCsConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时服务权益到期扫描 Job Bean（plan 2026-07-07-1430-1 §Phase 1）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每日 01:30）。
 *
 * <p>实际执行门控：{@code erp-cs.entitlement-expiry-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时执行两步：
 * <ul>
 *   <li>到期预警：调 {@link IErpCsEntitlementBiz#scanExpiringEntitlements}（窗口默认 30 天）
 *       经 {@link IErpSysNotificationBiz#notify}（{@code cs.entitlement-expiry}）派发提醒。</li>
 *   <li>自动停用：调 {@link IErpCsEntitlementBiz#deactivateExpiredEntitlements} 将 endDate&lt;now 的 active 权益置 inactive。</li>
 * </ul>
 * 双层门控对齐 0306-1 范式（镜像既有 {@code ErpCsSlaScanJob}）。
 */
public class ErpCsEntitlementExpiryJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCsEntitlementExpiryJob.class);

    @Inject
    IErpCsEntitlementBiz entitlementBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    public void setEntitlementBiz(IErpCsEntitlementBiz entitlementBiz) {
        this.entitlementBiz = entitlementBiz;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    public void setMdPartnerBiz(IErpMdPartnerBiz mdPartnerBiz) {
        this.mdPartnerBiz = mdPartnerBiz;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描到期前权益派发提醒 + 自动停用已到期权益。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-cs-entitlement-expiry-skipped: cron config empty (erp-cs.entitlement-expiry-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int warned = runExpiryWarnings(ctx);
            int deactivated = runDeactivations(ctx);
            LOG.info("erp-cs-entitlement-expiry-done: warned={}, deactivated={}", warned, deactivated);
        } catch (Exception e) {
            LOG.error("erp-cs-entitlement-expiry-failed", e);
        }
    }

    /** 派发到期预警通知；返回成功派发的权益条数。 */
    protected int runExpiryWarnings(IServiceContext ctx) {
        List<ErpCsEntitlement> expiring = entitlementBiz.scanExpiringEntitlements(null, ctx);
        if (expiring == null || expiring.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ErpCsEntitlement e : expiring) {
            try {
                notifyExpiry(e, ctx);
                count++;
            } catch (Exception ex) {
                LOG.warn("erp-cs-entitlement-expiry: 单条权益预警失败（隔离继续）：entitlementId={}, reason={}",
                        e.getId(), ex.getMessage());
            }
        }
        return count;
    }

    /** 自动停用已到期权益；返回停用条数。 */
    protected int runDeactivations(IServiceContext ctx) {
        List<ErpCsEntitlement> deactivated = entitlementBiz.deactivateExpiredEntitlements(ctx);
        return deactivated == null ? 0 : deactivated.size();
    }

    protected void notifyExpiry(ErpCsEntitlement entitlement, IServiceContext ctx) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("entitlementId", entitlement.getId());
        map.put("entitlementCode", entitlement.getCode());
        map.put("partnerId", entitlement.getPartnerId());
        map.put("partnerName", resolvePartnerName(entitlement.getPartnerId(), ctx));
        map.put("endDate", entitlement.getEndDate());
        map.put("serviceType", entitlement.getServiceType());
        map.put("usedTickets", entitlement.getUsedTickets());
        map.put("maxTickets", entitlement.getMaxTickets());
        notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_ENTITLEMENT_EXPIRY, map, ctx);
    }

    private String resolvePartnerName(Long partnerId, IServiceContext ctx) {
        if (partnerId == null || mdPartnerBiz == null) {
            return null;
        }
        try {
            ErpMdPartner partner = mdPartnerBiz.findById(partnerId, ctx);
            return partner == null ? null : partner.getName();
        } catch (Exception e) {
            return null;
        }
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpCsConstants.CONFIG_ENTITLEMENT_EXPIRY_CRON, "");
    }
}
