
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinPostingExceptionBiz;
import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.ErpFinPostingMetricsSnapshot;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.ErpFinPostingErrors;
import app.erp.fin.service.posting.ErpFinPostingExceptionRecorder;
import app.erp.fin.service.posting.ErpFinPostingMetrics;
import app.erp.fin.service.metrics.ErpFinPostingExceptionBacklogGauge;
import app.erp.fin.service.processor.ErpFinPostingExceptionIgnoreProcessor;
import app.erp.fin.service.processor.ErpFinPostingExceptionRetryProcessor;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.auth.IUserContext;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.concurrent.executor.GlobalExecutors;
import io.nop.commons.metrics.GlobalMeterRegistry;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 过账异常工作台（{@code posting-log.md §过账异常处置}）。CRUD 之外承载三个处置动作：
 * {@link #retry}/{@link #ignore}/{@link #manualEntry}，处置状态机经 ErrorCode 守门。
 *
 * <p>期末结账前置检查经 {@link #countUnresolved} 扫描未处置（PENDING/RETRYING/MANUAL 未补录）记录阻止结账。
 *
 * <p>事务/会话：{@link BizMutation} 默认事务；retry 内重新触发过账经 {@link IErpFinVoucherBiz#post}
 * 的 REQUIRES_NEW 独立事务（失败回滚不污染本工作台事务）。
 */
@BizModel("ErpFinPostingException")
public class ErpFinPostingExceptionBizModel extends CrudBizModel<ErpFinPostingException>
        implements IErpFinPostingExceptionBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinPostingExceptionBizModel.class);

    /**
     * observability.md §5.1 指标 5（{@code erp_fin_posting_exception_backlog} Gauge）：
     * 进程级 once-only 注册 flag，避免多实例 / 容器重启叠加导致 Gauge 重复注册告警。
     */
    private static final AtomicBoolean BACKLOG_GAUGE_REGISTERED = new AtomicBoolean(false);

    /** 5 分钟固定间隔（observability.md §5.1 指标 5 校正：避免 scrape 时才查 DB 引入抓取延迟）。 */
    static final long BACKLOG_REFRESH_INITIAL_DELAY_MS = 30_000L;
    static final long BACKLOG_REFRESH_PERIOD_MS = 5 * 60_000L;

    /** backlog Gauge 当前缓存值（由后台刷新任务更新，Gauge 读取此值）。 */
    private final AtomicLong postingExceptionBacklog = new AtomicLong(0L);

    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Inject
    ErpFinPostingMetrics postingMetrics;

    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    ErpFinPostingExceptionRetryProcessor retryProcessor;
    @Inject
    ErpFinPostingExceptionIgnoreProcessor ignoreProcessor;

    public ErpFinPostingExceptionBizModel() {
        setEntityName(ErpFinPostingException.class.getName());
    }

    /**
     * observability.md §5.1 指标 5 注册（{@code erp_fin_posting_exception_backlog} Gauge）。
     *
     * <p>进程级 once-only：经 {@code BACKLOG_GAUGE_REGISTERED} CAS 保证仅注册一次（多 BizModel 实例
     * / Quarkus dev mode reload 场景）。Gauge 注册逻辑经 {@link ErpFinPostingExceptionBacklogGauge#register}
     * 静态助手（亦可被单测针对 SimpleMeterRegistry 调用）。后台刷新任务复用 {@link #countUnresolved}
     * 既有语义（PENDING/RETRYING/MANUAL 终态机计数），固定 5 分钟间隔。
     */
    @PostConstruct
    public void initObservability() {
        if (!BACKLOG_GAUGE_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        ErpFinPostingExceptionBacklogGauge.register(GlobalMeterRegistry.instance(), postingExceptionBacklog);
        GlobalExecutors.globalTimer().scheduleAtFixedRate(
                this::refreshPostingExceptionBacklog,
                BACKLOG_REFRESH_INITIAL_DELAY_MS,
                BACKLOG_REFRESH_PERIOD_MS,
                TimeUnit.MILLISECONDS);
        LOG.info("erp-fin-posting-exception-backlog-gauge-registered: initialDelayMs={}, periodMs={}",
                BACKLOG_REFRESH_INITIAL_DELAY_MS, BACKLOG_REFRESH_PERIOD_MS);
    }

    /** 后台刷新一次 backlog 缓存（单测亦调用以避免等待 5 分钟调度）。异常隔离不向上抛。 */
    public void refreshPostingExceptionBacklog() {
        try {
            long count = countUnresolved(new ServiceContextImpl());
            postingExceptionBacklog.set(count);
        } catch (Exception e) {
            LOG.warn("erp-fin-posting-exception-backlog-refresh-failed: reason={}", e.getMessage());
        }
    }

    /** 当前 backlog 缓存值（单测断言用）。 */
    public long currentPostingExceptionBacklog() {
        return postingExceptionBacklog.get();
    }

    @Override
    @BizMutation
    public ErpFinPostingException retry(@Name("exceptionId") String exceptionId, IServiceContext context) {
        return retryProcessor.retry(exceptionId, context);
    }

    @Override
    @BizMutation
    public ErpFinPostingException ignore(@Name("exceptionId") String exceptionId,
                                         @Name("resolutionNote") String resolutionNote,
                                         IServiceContext context) {
        return ignoreProcessor.ignore(exceptionId, resolutionNote, context);
    }

    /**
     * IGNORED 放弃态告警派发（G2 错误传播分级策略；plan 2026-07-30-0341-2 P1-MA2-032）。
     * 通知失败降级（warn）不阻断处置动作。
     */
    private void dispatchAbandonmentAlert(ErpFinPostingException entity, String resolutionNote) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("exceptionId", entity.getId());
        ctx.put("billHeadCode", entity.getBillHeadCode());
        ctx.put("businessType", entity.getBusinessType());
        ctx.put("errorCode", entity.getErrorCode());
        ctx.put("errorMessage", entity.getErrorMessage());
        ctx.put("resolutionNote", resolutionNote);
        ctx.put("postingNo", entity.getBillHeadCode());
        try {
            notificationBiz.notify(ErpFinConstants.NOTIFY_EVENT_POSTING_EXCEPTION, ctx, new io.nop.core.context.ServiceContextImpl());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ErpFinPostingExceptionBizModel.class)
                    .warn("erp-fin-posting-exception-ignored-alert-failed: exceptionId={}, reason={}",
                            entity.getId(), e.getMessage());
        }
    }

    @Override
    @BizMutation
    public ErpFinPostingException manualEntry(@Name("exceptionId") String exceptionId,
                                              @Name("voucherId") String voucherId,
                                              @Name("resolutionNote") String resolutionNote,
                                              IServiceContext context) {
        ErpFinPostingException entity = requirePending(exceptionId);
        if (voucherId == null) {
            throw new NopException(ErpFinPostingErrors.ERR_POSTING_EXCEPTION_MANUAL_VOUCHER_REQUIRED)
                    .param(ErpFinPostingErrors.ARG_EXCEPTION_ID, exceptionId);
        }
        entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_MANUAL);
        entity.setResolution(ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_MANUAL);
        entity.setVoucherId(voucherId);
        entity.setResolutionNote(resolutionNote);
        entity.setResolvedBy(currentUserId());
        entity.setResolvedAt(CoreMetrics.currentTimestamp());
        updateEntity(entity, null, context);
        return entity;
    }

    @Override
    @BizQuery
    public long countUnresolved(IServiceContext context) {
        IEntityDao<ErpFinPostingException> dao = dao();
        QueryBean q = new QueryBean();
        // MANUAL（G2 MAX_RETRY 升级）voucherId 为空即未补录，计入未处置；RETRIED/IGNORED 已决策不计入。
        q.addFilter(in("status", Arrays.asList(
                ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING,
                ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRYING,
                ErpFinConstants.POSTING_EXCEPTION_STATUS_MANUAL)));
        q.addFilter(io.nop.api.core.beans.FilterBeans.isNull("voucherId"));
        List<ErpFinPostingException> all = dao.findAllByQuery(q);
        return all.size();
    }

    /**
     * 运行监控四指标快照（{@code posting-log.md §裁决3}）。
     *
     * <p>读路径直接用 {@code daoProvider().daoFor()} 聚合 COUNT（跨实体只读统计，非业务写操作；
     * @BizQuery 自身的 auth 已是访问门控，无需 per-entity 权限管道——对齐 service-layer 跨实体只读统计约定）。
     */
    @Override
    @BizQuery
    public ErpFinPostingMetricsSnapshot getRuntimeMetrics(@Name("windowHours") Integer windowHours,
                                                          IServiceContext context) {
        int window = windowHours != null && windowHours > 0 ? windowHours
                : AppConfig.var(ErpFinConstants.CONFIG_METRIC_WINDOW_HOURS,
                        ErpFinConstants.DEFAULT_METRIC_WINDOW_HOURS);
        Timestamp since = new Timestamp(CoreMetrics.currentTimeMillis() - window * 3600_000L);

        long voucherCount = countVouchersSince(since);
        long exceptionCount = countExceptionsSince(since);
        long manualResolutionCount = countManualResolutionsSince(since);

        // 以下 ErpFinPostingMetricsSnapshot / MetricValue 为 finance-dao 跨层契约 DTO（非 ORM 实体），不适用 newEntity()
        ErpFinPostingMetricsSnapshot snapshot = new ErpFinPostingMetricsSnapshot();
        snapshot.setWindowHours(window);
        snapshot.setVoucherCount(voucherCount);
        snapshot.setExceptionCount(exceptionCount);
        snapshot.setManualResolutionCount(manualResolutionCount);
        snapshot.setLatencySampleCount(postingMetrics.sampleCount());

        double autoRateThreshold = resolveDouble(ErpFinConstants.CONFIG_METRIC_AUTO_POSTING_RATE_THRESHOLD,
                ErpFinConstants.DEFAULT_METRIC_AUTO_POSTING_RATE_THRESHOLD);
        double autoRate = computeAutoPostingRate(voucherCount, manualResolutionCount);
        snapshot.setAutoPostingRate(new ErpFinPostingMetricsSnapshot.MetricValue(
                autoRate, autoRateThreshold, autoRate >= autoRateThreshold, HIGHER_BETTER));

        long latencyP99 = postingMetrics.p99LatencyMillis();
        long latencyThreshold = AppConfig.var(
                ErpFinConstants.CONFIG_METRIC_LATENCY_P99_THRESHOLD_MILLIS,
                ErpFinConstants.DEFAULT_METRIC_LATENCY_P99_THRESHOLD_MILLIS);
        snapshot.setLatencyP99Millis(new ErpFinPostingMetricsSnapshot.MetricValue(
                (double) latencyP99, (double) latencyThreshold,
                latencyP99 < latencyThreshold, LOWER_BETTER));

        double exceptionRateThreshold = resolveDouble(ErpFinConstants.CONFIG_METRIC_EXCEPTION_RATE_THRESHOLD,
                ErpFinConstants.DEFAULT_METRIC_EXCEPTION_RATE_THRESHOLD);
        double exceptionRate = computeExceptionRate(exceptionCount, voucherCount);
        snapshot.setExceptionRate(new ErpFinPostingMetricsSnapshot.MetricValue(
                exceptionRate, exceptionRateThreshold, exceptionRate < exceptionRateThreshold, LOWER_BETTER));

        double loopbackThreshold = resolveDouble(ErpFinConstants.CONFIG_METRIC_LOOPBACK_RATE_THRESHOLD,
                ErpFinConstants.DEFAULT_METRIC_LOOPBACK_RATE_THRESHOLD);
        snapshot.setLoopbackProxyMode(true);
        snapshot.setLoopbackSuccessRate(new ErpFinPostingMetricsSnapshot.MetricValue(
                1.0, loopbackThreshold, 1.0 >= loopbackThreshold, HIGHER_BETTER));
        return snapshot;
    }

    // ---------- metrics helpers ----------

    private static final String HIGHER_BETTER = "higher_better";
    private static final String LOWER_BETTER = "lower_better";

    /** 自动化记账率 = 自动凭证数 ÷ (自动凭证数 + 手工补录异常数)。 */
    private static double computeAutoPostingRate(long voucherCount, long manualResolutionCount) {
        long denom = voucherCount + manualResolutionCount;
        if (denom == 0) {
            return 1.0;
        }
        return (double) voucherCount / denom;
    }

    /** 过账异常率 = 异常记录数 ÷ (异常记录数 + 成功凭证数)。 */
    private static double computeExceptionRate(long exceptionCount, long voucherCount) {
        long denom = exceptionCount + voucherCount;
        if (denom == 0) {
            return 0.0;
        }
        return (double) exceptionCount / denom;
    }

    private long countVouchersSince(Timestamp since) {
        // 跨实体只读 COUNT，经 daoProvider 直读（见方法注释说明）。
        IEntityDao<ErpFinVoucher> dao = daoProvider().daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(ge("createTime", since));
        return dao.findAllByQuery(q).size();
    }

    private long countExceptionsSince(Timestamp since) {
        IEntityDao<ErpFinPostingException> dao = dao();
        QueryBean q = new QueryBean();
        q.addFilter(ge("occurrenceTime", since));
        return dao.findAllByQuery(q).size();
    }

    private long countManualResolutionsSince(Timestamp since) {
        IEntityDao<ErpFinPostingException> dao = dao();
        QueryBean q = new QueryBean();
        q.addFilter(ge("occurrenceTime", since));
        q.addFilter(eq("resolution", ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_MANUAL));
        return dao.findAllByQuery(q).size();
    }

    private static double resolveDouble(String key, double defaultVal) {
        return AppConfig.var(key, defaultVal);
    }

    // ---------- helpers ----------

    /** 仅 PENDING 状态可处置，其余抛 ErrorCode 守门异常。 */
    private ErpFinPostingException requirePending(String exceptionId) {
        IEntityDao<ErpFinPostingException> dao = dao();
        ErpFinPostingException entity = dao.getEntityById(exceptionId);
        if (entity == null) {
            throw new NopException(ErpFinPostingErrors.ERR_POSTING_EXCEPTION_NOT_FOUND)
                    .param(ErpFinPostingErrors.ARG_EXCEPTION_ID, exceptionId);
        }
        if (!Objects.equals(entity.getStatus(), ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING)) {
            throw new NopException(ErpFinPostingErrors.ERR_POSTING_EXCEPTION_NOT_PENDING)
                    .param(ErpFinPostingErrors.ARG_EXCEPTION_ID, exceptionId)
                    .param(ErpFinPostingErrors.ARG_CURRENT_STATUS, entity.getStatus());
        }
        return entity;
    }

    /** 从异常记录重建 PostingEvent（重试用）。 */
    private PostingEvent rebuildEvent(ErpFinPostingException entity) {
        PostingEvent event = new PostingEvent();
        event.setTraceId(entity.getTraceId());
        event.setBillHeadCode(entity.getBillHeadCode());
        event.setBusinessType(parseBusinessType(entity.getBusinessType()));
        event.setVoucherDate(entity.getVoucherDate());
        event.setOrgId(entity.getOrgId());
        event.setAcctSchemaId(entity.getAcctSchemaId());
        event.setCurrencyId(entity.getCurrencyId());
        event.setExchangeRate(entity.getExchangeRate() != null ? entity.getExchangeRate() : BigDecimal.ONE);
        Map<String, Object> billData = ErpFinPostingExceptionRecorder.deserializeEventData(entity.getEventData());
        if (billData == null) {
            billData = new LinkedHashMap<>();
        }
        event.setBillData(billData);
        return event;
    }

    private ErpFinBusinessType parseBusinessType(String name) {
        if (name == null) {
            return null;
        }
        return ErpFinBusinessType.valueOf(name);
    }

    private String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

}
