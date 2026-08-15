package app.erp.ct.service.job;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.ct.biz.IErpCtContractBiz;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.auth.biz.INopAuthDeptBiz;
import io.nop.auth.biz.INopAuthUserBiz;
import io.nop.auth.dao.entity.NopAuthDept;
import io.nop.auth.dao.entity.NopAuthUser;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 定时合同到期扫描 Job Bean（use-cases.md UC-CT-05，RC-R1.35）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code erp-ct-contract-expiry.job.yaml} 的 cronExpr 决定（默认每日 01:00）。
 *
 * <p>实际执行门控（D5 单键模式）：{@code erp-ct.contract-expiry-cron} 配置为空时跳过
 * （"不调度"语义，job.yaml cronExpr 与 bean 空值跳过共用本键）；非空时执行三步：
 * <ul>
 *   <li>30/15/7 分级提醒：调 {@link IErpCtContractBiz#scanExpiringContracts}（窗口 = 30 天档 config）
 *       按剩余天数分档——≤7 天升级通知经办人上级（D2：{@code NopAuthUser.managerId} → 兜底
 *       {@code NopAuthDept.managerId}，双 null LOG.warn 跳过）/ ≤15 天再次通知经办人「即将到期」/
 *       其余 30 天档通知经办人——经 {@link IErpSysNotificationBiz#notify} 派发。</li>
 *   <li>到期推进：调 {@link IErpCtContractBiz#expireOverdueContracts} 将 endDate&lt;today 的 ACTIVE
 *       合同批量 expire（D3 未完成开票先完成 + D4 config-gated 续期草稿，BizModel 内逐条失败隔离）。</li>
 * </ul>
 * 镜像 {@code ErpHrContractExpiryJob} + {@code ErpCtApprovalTimeoutEscalationJob} 范式
 * （双层门控 + 单条失败隔离 + 跨实体经 I*Biz 注入）。
 */
public class ErpCtContractExpiryJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCtContractExpiryJob.class);

    @Inject
    IErpCtContractBiz contractBiz;
    @Inject
    INopAuthUserBiz authUserBiz;
    @Inject
    INopAuthDeptBiz authDeptBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IOrmTemplate ormTemplate;

    public void setContractBiz(IErpCtContractBiz contractBiz) {
        this.contractBiz = contractBiz;
    }

    public void setAuthUserBiz(INopAuthUserBiz authUserBiz) {
        this.authUserBiz = authUserBiz;
    }

    public void setAuthDeptBiz(INopAuthDeptBiz authDeptBiz) {
        this.authDeptBiz = authDeptBiz;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描分级提醒 + 推进已过期合同。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-ct-contract-expiry-skipped: cron config empty (erp-ct.contract-expiry-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int warned = runExpiryWarnings(ctx);
            int expired = runExpirations(ctx);
            LOG.info("erp-ct-contract-expiry-done: warned={}, expired={}", warned, expired);
        } catch (Exception e) {
            LOG.error("erp-ct-contract-expiry-failed", e);
        }
    }

    /**
     * 派发 30/15/7 分级到期提醒；返回成功派发的合同条数（7 天档无上级解析时跳过不计数）。
     * 扫描窗口取 30 天档 config（默认 30），逐合同按剩余天数分档。
     */
    protected int runExpiryWarnings(IServiceContext ctx) {
        int window = resolveWindowDays(ErpCtConfigs.CFG_CONTRACT_EXPIRY_WARNING_DAYS_30,
                ErpCtConfigs.DEFAULT_CONTRACT_EXPIRY_WARNING_DAYS_30);
        List<ErpCtContract> expiring = contractBiz.scanExpiringContracts(window, ctx);
        if (expiring == null || expiring.isEmpty()) {
            return 0;
        }
        int days15 = resolveWindowDays(ErpCtConfigs.CFG_CONTRACT_EXPIRY_WARNING_DAYS_15,
                ErpCtConfigs.DEFAULT_CONTRACT_EXPIRY_WARNING_DAYS_15);
        int days7 = resolveWindowDays(ErpCtConfigs.CFG_CONTRACT_EXPIRY_WARNING_DAYS_7,
                ErpCtConfigs.DEFAULT_CONTRACT_EXPIRY_WARNING_DAYS_7);
        int count = 0;
        for (ErpCtContract contract : expiring) {
            try {
                if (notifyByTier(contract, days15, days7, ctx)) {
                    count++;
                }
            } catch (Exception ex) {
                LOG.warn("erp-ct-contract-expiry: 单条合同预警失败（隔离继续）：contractId={}, reason={}",
                        contract.getId(), ex.getMessage());
            }
        }
        return count;
    }

    /**
     * 按剩余天数分档派发通知（每合同每运行至多一条——随时间推进逐档穿越，对齐 L1
     * 「30 天通知 / 15 天再次通知 / 7 天升级」）。返回 true 表示已派发。
     */
    protected boolean notifyByTier(ErpCtContract contract, int days15, int days7, IServiceContext ctx) {
        LocalDate today = CoreMetrics.today();
        if (contract.getEndDate() == null) {
            return false;
        }
        long remaining = ChronoUnit.DAYS.between(today, contract.getEndDate());
        if (remaining <= days7) {
            return notifyEscalation(contract, ctx);
        }
        if (remaining <= days15) {
            return notifyWarning(contract, ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_15, ctx);
        }
        return notifyWarning(contract, ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_30, ctx);
    }

    /**
     * 派发到期提醒（30/15 天档，接收人 = 合同经办人 createdBy）。无 ACTIVE 模板时 notify 内部
     * config-gated 静默跳过（R1.4 范式）。
     */
    protected boolean notifyWarning(ErpCtContract contract, String eventType, IServiceContext ctx) {
        if (notificationBiz == null) {
            return false;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contractId", contract.getId());
        map.put("contractCode", contract.getCode());
        map.put("ownerUserId", contract.getCreatedBy());
        map.put("endDate", contract.getEndDate());
        notificationBiz.notify(eventType, map, ctx);
        return true;
    }

    /**
     * 派发 7 天升级通知（接收人 = 经办人上级）。上级解析链（D2 选项 A）：{@code NopAuthUser.managerId}
     * （直接上级）→ 兜底 {@code NopAuthUser.deptId → NopAuthDept.managerId}（部门负责人）；
     * 双 null 时 LOG.warn 跳过（R1.4 范式）。
     */
    protected boolean notifyEscalation(ErpCtContract contract, IServiceContext ctx) {
        String escalationUserId = resolveEscalationUserId(contract.getCreatedBy(), ctx);
        if (escalationUserId == null) {
            LOG.warn("erp-ct-contract-expiry: 无经办人上级（无直接上级且无部门负责人），跳过升级通知：contractId={}",
                    contract.getId());
            return false;
        }
        if (notificationBiz == null) {
            return false;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contractId", contract.getId());
        map.put("contractCode", contract.getCode());
        map.put("ownerUserId", contract.getCreatedBy());
        map.put("escalationUserId", escalationUserId);
        map.put("endDate", contract.getEndDate());
        notificationBiz.notify(ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_ESCALATION_7, map, ctx);
        return true;
    }

    /**
     * 经办人上级解析：{@code NopAuthUser.managerId}（直接上级）→ 兜底部门负责人
     * （{@code NopAuthUser.deptId → NopAuthDept.managerId}）；均缺失返回 null。
     */
    protected String resolveEscalationUserId(String ownerUserId, IServiceContext ctx) {
        if (StringHelper.isEmpty(ownerUserId)) {
            return null;
        }
        NopAuthUser user = authUserBiz.get(ownerUserId, true, ctx);
        if (user == null) {
            return null;
        }
        if (!StringHelper.isEmpty(user.getManagerId())) {
            return user.getManagerId();
        }
        if (!StringHelper.isEmpty(user.getDeptId())) {
            NopAuthDept dept = authDeptBiz.get(user.getDeptId(), true, ctx);
            if (dept != null && !StringHelper.isEmpty(dept.getManagerId())) {
                return dept.getManagerId();
            }
        }
        return null;
    }

    /**
     * 批量推进已过期合同（BizModel 内逐条失败隔离 + D3/D4 副作用）；返回推进条数。
     * runInSession 原因：job 非 GraphQL 上下文，经 IBiz 代理调用时 @SingleSession 注解
     * 不经代理生效（对齐 hr 先例 TestErpHrContractExpiry:87 同型包裹），须显式开 session
     * 使 expireOverdueContracts 内 findList 返回实体保持 MANAGED 供 updateEntity 落库。
     */
    protected int runExpirations(IServiceContext ctx) {
        return ormTemplate.runInSession(session -> {
            List<ErpCtContract> expired = contractBiz.expireOverdueContracts(ctx);
            return expired == null ? 0 : expired.size();
        });
    }

    protected int resolveWindowDays(String configKey, int defaultValue) {
        Integer v = AppConfig.var(configKey, defaultValue);
        return v == null || v < 0 ? defaultValue : v;
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpCtConfigs.CFG_CONTRACT_EXPIRY_CRON, "");
    }
}
