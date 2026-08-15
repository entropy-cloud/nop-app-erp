package app.erp.b2b.service.job;

import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.biz.IErpB2bEdiFormatBiz;
import app.erp.b2b.biz.IErpB2bPartnerProfileBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiFormat;
import app.erp.b2b.dao.entity.ErpB2bPartnerProfile;
import app.erp.b2b.service.ErpB2bConfigs;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.core.lang.json.JsonTool;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.dateTimeBetween;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * B2B 伙伴 24h 上线监控 Job Bean（RC-R1.36，P1-RC-080 ④，UC-B2B-007「上线监控 24 小时」）。
 *
 * <p>R1.4 简单 job bean 范式（D4 裁决）：由 nop-job-local 的 {@code scheduler.yaml} 经
 * BeanMethodJobInvoker 反射调用 {@link #execute()}，触发频率由
 * {@code erp-b2b-onboarding-monitor.job.yaml} 的 cronExpr 决定（默认每小时）。
 *
 * <p>双层门控：job.yaml {@code nop.job.erp-b2b-onboarding-monitor.enabled}（调度级）+
 * bean 内 {@code erp-b2b.onboarding-monitor-cron} 配置空值跳过（单键模式，对齐 R1.35 裁决）。
 *
 * <p>扫描语义（D4 选项 A 间接路径锚点）：扫描 PRODUCTION 且 {@code goLiveDate ∈
 * [today - ⌈monitor-hours/24⌉, today]} 的伙伴（上线后监控窗口内），逐伙伴：
 * <ol>
 *   <li>解析 {@code allowedFormats} JSON（formatCode 数组）→ 按 code 查 {@code ErpB2bEdiFormat}
 *       收 formatId 集（空集跳过——无法锚定该伙伴 EDI 流量）；</li>
 *   <li>统计 {@code ErpB2bEdiDoc}：{@code formatId ∈ ids AND orgId = profile.orgId AND
 *       createTime ≥ goLiveDate}（org 级 scope 兜底防跨伙伴误计）总件数与 state=ERROR 件数；</li>
 *   <li>失败率 = ERROR 件数 / 总件数，超 {@code erp-b2b.onboarding-monitor-failure-rate}
 *       （默认 0.05，对齐 partner-onboarding.md §生产监控表「EDI 发送失败率 > 5%」）→
 *       notify 派发 {@code b2b.onboarding-monitor-alert}（ROLE「B2B 管理员」，无 ACTIVE 模板
 *       notify 内部静默跳过 R1.4 范式）。</li>
 * </ol>
 *
 * <p>单条失败隔离：逐伙伴 try/catch WARN，不阻断其余伙伴（R1.4 简单 job bean 范式，
 * 否决 batch-task REQUIRES_NEW helper 的 R10 基线漂移——R1.34 D6 同型裁决）。
 *
 * <p>已知行为（非缺陷）：窗口内超阈值伙伴在每次扫描（每小时）都会重复告警，直至窗口结束
 * （无「已通知」标记列——去重须 ORM 列 = ask-first，登记 successor = 伙伴级 EDI 统计需求立项）。
 */
public class ErpB2bOnboardingMonitorJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpB2bOnboardingMonitorJob.class);

    /** 单次扫描最大伙伴数（分页 limit 保护）。 */
    static final int SCAN_LIMIT = 200;

    @Inject
    IErpB2bPartnerProfileBiz partnerProfileBiz;
    @Inject
    IErpB2bEdiFormatBiz ediFormatBiz;
    @Inject
    IErpB2bEdiDocBiz ediDocBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IOrmTemplate ormTemplate;

    public void setPartnerProfileBiz(IErpB2bPartnerProfileBiz partnerProfileBiz) {
        this.partnerProfileBiz = partnerProfileBiz;
    }

    public void setEdiFormatBiz(IErpB2bEdiFormatBiz ediFormatBiz) {
        this.ediFormatBiz = ediFormatBiz;
    }

    public void setEdiDocBiz(IErpB2bEdiDocBiz ediDocBiz) {
        this.ediDocBiz = ediDocBiz;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描监控窗口内 PRODUCTION 伙伴并逐条派发超阈值告警。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-b2b-onboarding-monitor-skipped: cron config empty (erp-b2b.onboarding-monitor-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int alerted = runMonitor(ctx);
            LOG.info("erp-b2b-onboarding-monitor-done: alerted={}", alerted);
        } catch (Exception e) {
            LOG.error("erp-b2b-onboarding-monitor-failed", e);
        }
    }

    /**
     * 扫描监控窗口内 PRODUCTION 伙伴并逐条评估失败率；返回超阈值告警派发条数。
     * 扫描与读取在同一 ORM session 内完成（findList 返回的实体保持 MANAGED）。
     */
    protected int runMonitor(IServiceContext ctx) {
        long monitorHours = resolveMonitorHours();
        int monitorDays = Math.max(1, (int) Math.ceil(monitorHours / 24.0));
        LocalDate today = CoreMetrics.currentDate();
        LocalDate windowStart = today.minusDays(monitorDays);
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("status", ErpB2bConstants.PARTNER_STATUS_PRODUCTION));
            // 窗口语义（D4）：goLiveDate ∈ [today - ⌈monitorHours/24⌉, today]
            // （XMeta 过滤算子白名单 [eq, in, dateBetween, dateTimeBetween] 无 ge/le，经 dateBetween 表达）
            q.addFilter(dateBetween("goLiveDate", windowStart, today));
            q.setLimit(SCAN_LIMIT);
            List<ErpB2bPartnerProfile> partners = partnerProfileBiz.findList(q, null, ctx);
            if (partners == null || partners.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (ErpB2bPartnerProfile profile : partners) {
                try {
                    if (monitorPartner(profile, ctx)) {
                        count++;
                    }
                } catch (Exception e) {
                    LOG.warn("erp-b2b-onboarding-monitor: 单伙伴监控失败（隔离继续）：partnerCode={}, reason={}",
                            profile.getCode(), e.getMessage());
                }
            }
            return count;
        });
    }

    /**
     * 单伙伴窗口失败率评估（D4 选项 A 间接路径锚点）：allowedFormats→formatId 聚合窗口内
     * ERROR 失败率，超阈值 notify 派发告警。返回 true 表示已派发。
     */
    protected boolean monitorPartner(ErpB2bPartnerProfile profile, IServiceContext ctx) {
        double threshold = resolveFailureRate();
        long monitorHours = resolveMonitorHours();
        List<Long> formatIds = resolveFormatIds(profile, ctx);
        if (formatIds.isEmpty()) {
            return false;
        }
        long total = countEdiDocs(profile, formatIds, false, ctx);
        if (total <= 0) {
            return false;
        }
        long errors = countEdiDocs(profile, formatIds, true, ctx);
        double rate = (double) errors / total;
        if (rate + 1e-9 <= threshold) {
            return false;
        }
        notifyAlert(profile, total, errors, rate, monitorHours, ctx);
        return true;
    }

    /** 解析伙伴 allowedFormats JSON（formatCode 数组）→ EdiFormat.id 集；空/非法返回空集（跳过该伙伴）。 */
    protected List<Long> resolveFormatIds(ErpB2bPartnerProfile profile, IServiceContext ctx) {
        String allowedFormats = profile.getAllowedFormats();
        if (StringHelper.isBlank(allowedFormats)) {
            return Collections.emptyList();
        }
        List<String> codes;
        try {
            codes = JsonTool.parseBeanFromText(allowedFormats, List.class);
        } catch (Exception e) {
            LOG.warn("erp-b2b-onboarding-monitor: allowedFormats 解析失败（跳过）：partnerCode={}, reason={}",
                    profile.getCode(), e.getMessage());
            return Collections.emptyList();
        }
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (String code : codes) {
            QueryBean q = new QueryBean();
            q.addFilter(eq("code", code));
            List<ErpB2bEdiFormat> formats = ediFormatBiz.findList(q, null, ctx);
            if (formats != null) {
                for (ErpB2bEdiFormat format : formats) {
                    ids.add(format.getId());
                }
            }
        }
        return ids;
    }

    /** 统计伙伴窗口内 EDI 事务件数（formatId 锚点 + org 级 scope 兜底 + createTime ∈ [goLiveDate, now]）。 */
    protected long countEdiDocs(ErpB2bPartnerProfile profile, List<Long> formatIds, boolean onlyErrors, IServiceContext ctx) {
        QueryBean q = new QueryBean();
        q.addFilter(in("formatId", formatIds));
        if (profile.getOrgId() != null) {
            q.addFilter(eq("orgId", profile.getOrgId()));
        }
        // createTime ≥ goLiveDate 且 ≤ now（XMeta 白名单无 ge/le，经 dateTimeBetween 表达；上界兜底防未来事务）
        q.addFilter(dateTimeBetween("createTime",
                profile.getGoLiveDate().atStartOfDay(), LocalDateTime.now()));
        if (onlyErrors) {
            q.addFilter(eq("state", ErpB2bConstants.EDI_DOC_STATE_ERROR));
        }
        return ediDocBiz.findCount(q, ctx);
    }

    /**
     * 派发上线监控告警（无 ACTIVE 模板时 notify 内部静默跳过）。
     * context 键对齐 {@code b2b.onboarding-monitor-alert} 模板约定：partnerCode/partnerName/
     * profileId/totalCount/errorCount/failureRate/goLiveDate/monitorHours。
     */
    protected void notifyAlert(ErpB2bPartnerProfile profile, long total, long errors,
                               double rate, long monitorHours, IServiceContext ctx) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("partnerCode", profile.getCode());
        map.put("partnerName", profile.getPartnerName());
        map.put("profileId", profile.getId());
        map.put("totalCount", total);
        map.put("errorCount", errors);
        map.put("failureRate", Math.round(rate * 10000d) / 10000d);
        map.put("goLiveDate", profile.getGoLiveDate());
        map.put("monitorHours", monitorHours);
        notificationBiz.notify(ErpB2bConstants.NOTIFY_EVENT_ONBOARDING_MONITOR_ALERT, map, ctx);
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpB2bConfigs.CONFIG_ONBOARDING_MONITOR_CRON, "");
    }

    protected long resolveMonitorHours() {
        return AppConfig.var(ErpB2bConfigs.CONFIG_ONBOARDING_PRODUCTION_MONITOR_HOURS,
                ErpB2bConfigs.DEFAULT_ONBOARDING_PRODUCTION_MONITOR_HOURS);
    }

    protected double resolveFailureRate() {
        return AppConfig.var(ErpB2bConfigs.CONFIG_ONBOARDING_MONITOR_FAILURE_RATE,
                ErpB2bConfigs.DEFAULT_ONBOARDING_MONITOR_FAILURE_RATE);
    }
}
