package app.erp.hr.service.job;

import app.erp.hr.biz.IErpHrEmploymentContractBiz;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.service.ErpHrConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
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
 * 定时合同到期扫描 Job Bean（use-cases.md UC-HR-07）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code scheduler.yaml} 的 cronExpr 决定（设计默认每日 01:00）。
 *
 * <p>实际执行门控：{@code erp-hr.contract-expiry-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时执行两步：
 * <ul>
 *   <li>到期预警：调 {@link IErpHrEmploymentContractBiz#scanExpiringContracts}（窗口默认 30 天）
 *       经 {@link IErpSysNotificationBiz#notify}（{@code hr.contract-expiry-warning}）派发提醒。</li>
 *   <li>过期推进：调 {@link IErpHrEmploymentContractBiz#expireOverdueContracts} 将 endDate&lt;now 的 ACTIVE 合同置 EXPIRED。</li>
 * </ul>
 * 镜像 {@code ErpCsEntitlementExpiryJob} 范式（双层门控 + 单条失败隔离）。
 */
public class ErpHrContractExpiryJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpHrContractExpiryJob.class);

    @Inject
    IErpHrEmploymentContractBiz contractBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    public void setContractBiz(IErpHrEmploymentContractBiz contractBiz) {
        this.contractBiz = contractBiz;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描到期预警合同派发提醒 + 推进已过期合同状态。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-hr-contract-expiry-skipped: cron config empty (erp-hr.contract-expiry-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int warned = runExpiryWarnings(ctx);
            int expired = runExpirations(ctx);
            LOG.info("erp-hr-contract-expiry-done: warned={}, expired={}", warned, expired);
        } catch (Exception e) {
            LOG.error("erp-hr-contract-expiry-failed", e);
        }
    }

    /** 派发到期预警通知；返回成功派发的合同条数。 */
    protected int runExpiryWarnings(IServiceContext ctx) {
        List<ErpHrEmploymentContract> expiring = contractBiz.scanExpiringContracts(null, ctx);
        if (expiring == null || expiring.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ErpHrEmploymentContract c : expiring) {
            try {
                notifyExpiry(c, ctx);
                count++;
            } catch (Exception ex) {
                LOG.warn("erp-hr-contract-expiry: 单条合同预警失败（隔离继续）：contractId={}, reason={}",
                        c.getId(), ex.getMessage());
            }
        }
        return count;
    }

    /** 推进已过期合同状态 ACTIVE→EXPIRED；返回推进条数。 */
    protected int runExpirations(IServiceContext ctx) {
        List<ErpHrEmploymentContract> expired = contractBiz.expireOverdueContracts(ctx);
        return expired == null ? 0 : expired.size();
    }

    protected void notifyExpiry(ErpHrEmploymentContract contract, IServiceContext ctx) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contractId", contract.getId());
        map.put("contractCode", contract.getCode());
        map.put("employeeId", contract.getEmployeeId());
        map.put("endDate", contract.getEndDate());
        map.put("contractType", contract.getContractType());
        notificationBiz.notify(ErpHrConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING, map, ctx);
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpHrConstants.CONFIG_CONTRACT_EXPIRY_CRON, "");
    }
}
