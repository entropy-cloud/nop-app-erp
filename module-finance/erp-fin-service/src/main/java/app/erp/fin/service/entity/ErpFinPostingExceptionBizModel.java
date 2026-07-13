
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
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.auth.IUserContext;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 过账异常工作台（{@code posting-log.md §过账异常处置}）。CRUD 之外承载三个处置动作：
 * {@link #retry}/{@link #ignore}/{@link #manualEntry}，处置状态机经 ErrorCode 守门。
 *
 * <p>期末结账前置检查经 {@link #countUnresolved} 扫描未处置（PENDING/RETRYING）记录阻止结账。
 *
 * <p>事务/会话：{@link BizMutation} 默认事务；retry 内重新触发过账经 {@link IErpFinVoucherBiz#post}
 * 的 REQUIRES_NEW 独立事务（失败回滚不污染本工作台事务）。
 */
@BizModel("ErpFinPostingException")
public class ErpFinPostingExceptionBizModel extends CrudBizModel<ErpFinPostingException>
        implements IErpFinPostingExceptionBiz {

    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Inject
    ErpFinPostingMetrics postingMetrics;

    public ErpFinPostingExceptionBizModel() {
        setEntityName(ErpFinPostingException.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinPostingException retry(@Name("exceptionId") Long exceptionId, IServiceContext context) {
        ErpFinPostingException entity = requirePending(exceptionId);
        // 翻 RETRYING 并记重试次数；重新触发过账（独立事务，失败回滚不污染本事务）。
        entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRYING);
        entity.setRetryCount((entity.getRetryCount() == null ? 0 : entity.getRetryCount()) + 1);
        entity.setResolution(ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_RETRY);
        entity.setResolvedBy(currentUserId());
        entity.setResolvedAt(CoreMetrics.currentDateTime());
        updateEntity(entity, null, context);

        if (!ErpFinConstants.POSTING_TYPE_REVERSAL.equals(entity.getPostingType())) {
            // 正向过账重试：从 eventData 重建 PostingEvent 重新过账。
            PostingEvent event = rebuildEvent(entity);
            Long voucherId = voucherBiz.post(event, context);
            if (voucherId != null) {
                entity.setVoucherId(voucherId);
                entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED);
            } else {
                // 幂等命中（源单已过账）也算重试成功。
                entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED);
            }
        } else {
            // 红冲重试：按回链重新红冲。
            ErpFinBusinessType businessType = parseBusinessType(entity.getBusinessType());
            Long voucherId = voucherBiz.reverse(entity.getBillHeadCode(), businessType, context);
            if (voucherId != null) {
                entity.setVoucherId(voucherId);
                entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRIED);
            }
        }
        updateEntity(entity, null, context);
        return entity;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinPostingException ignore(@Name("exceptionId") Long exceptionId,
                                         @Name("resolutionNote") String resolutionNote,
                                         IServiceContext context) {
        ErpFinPostingException entity = requirePending(exceptionId);
        if (resolutionNote == null || resolutionNote.trim().isEmpty()) {
            throw new NopException(ErpFinPostingErrors.ERR_POSTING_EXCEPTION_IGNORE_REASON_REQUIRED)
                    .param(ErpFinPostingErrors.ARG_EXCEPTION_ID, exceptionId);
        }
        entity.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_IGNORED);
        entity.setResolution(ErpFinConstants.POSTING_EXCEPTION_RESOLUTION_IGNORE);
        entity.setResolutionNote(resolutionNote);
        entity.setResolvedBy(currentUserId());
        entity.setResolvedAt(CoreMetrics.currentDateTime());
        updateEntity(entity, null, context);
        return entity;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinPostingException manualEntry(@Name("exceptionId") Long exceptionId,
                                              @Name("voucherId") Long voucherId,
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
        entity.setResolvedAt(CoreMetrics.currentDateTime());
        updateEntity(entity, null, context);
        return entity;
    }

    @Override
    @BizQuery
    public long countUnresolved(IServiceContext context) {
        IEntityDao<ErpFinPostingException> dao = dao();
        QueryBean q = new QueryBean();
        q.addFilter(in("status", Arrays.asList(
                ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING,
                ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRYING)));
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
    private ErpFinPostingException requirePending(Long exceptionId) {
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

    // ---------- 高价值外键名称解析（机制 D）----------

    @BizLoader(forType = ErpFinPostingException.class)
    public List<String> orgName(@ContextSource List<ErpFinPostingException> exceptions) {
        orm().batchLoadProps(exceptions, Collections.singleton("org"));
        List<String> result = new ArrayList<>(exceptions.size());
        for (ErpFinPostingException ex : exceptions) {
            result.add(ex.getOrg() != null ? ex.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinPostingException.class)
    public List<String> acctSchemaCode(@ContextSource List<ErpFinPostingException> exceptions) {
        orm().batchLoadProps(exceptions, Collections.singleton("acctSchema"));
        List<String> result = new ArrayList<>(exceptions.size());
        for (ErpFinPostingException ex : exceptions) {
            result.add(ex.getAcctSchema() != null ? ex.getAcctSchema().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinPostingException.class)
    public List<String> voucherCode(@ContextSource List<ErpFinPostingException> exceptions) {
        orm().batchLoadProps(exceptions, Collections.singleton("voucher"));
        List<String> result = new ArrayList<>(exceptions.size());
        for (ErpFinPostingException ex : exceptions) {
            result.add(ex.getVoucher() != null ? ex.getVoucher().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinPostingException.class)
    public List<String> currencyName(@ContextSource List<ErpFinPostingException> exceptions) {
        orm().batchLoadProps(exceptions, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(exceptions.size());
        for (ErpFinPostingException ex : exceptions) {
            result.add(ex.getCurrency() != null ? ex.getCurrency().getName() : null);
        }
        return result;
    }
}
