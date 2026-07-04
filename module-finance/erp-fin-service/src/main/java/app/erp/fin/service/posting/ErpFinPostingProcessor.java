package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.biz.IErpMdSubjectBiz;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.api.IBizObjectManager;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 业财过账编排 Processor。实现 {@code docs/design/finance/posting.md} 的过账全流程，被凭证聚合根 facade
 * {@code ErpFinVoucherBizModel} 内部调用（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 *
 * <p>编排步骤（稳定不变量，顺序锁死）：幂等前置（按业财回链反查）→ 查 Provider → createFacts → FactsValidator 链 →
 * 期间门控 → 借贷平衡 → 写 {@link ErpFinVoucher}+{@link ErpFinVoucherLine}+{@link ErpFinVoucherBillR} + 置凭证 {@code docStatus=POSTED}。
 *
 * <p>配置余地：主流程 {@link #process}/{@link #reverseProcess} 只编排步骤顺序；每个步骤是 {@code protected} 方法、
 * 单一职责、以 {@link IServiceContext} 为末参。客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，
 * 在 Delta beans.xml 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：本类**不**自带 {@code @Transactional}（跟随 Facade {@code @BizMutation} 事务；
 * 跨域失败隔离的 {@code REQUIRES_NEW} 由 Facade {@code post()} 显式声明，不下放编排层）。ORM Session 由
 * {@link SingleSession} 承接——作用域精确覆盖编排方法的 ORM 工作，在方法返回时刷新，使跨域调用方的 try/catch
 * 能稳定捕获过账异常（@SingleSession 原位于重构前的过账入口方法、现迁移至编排方法；事务/Session 分层见
 * {@code processor-extension-pattern.md}）。引擎只负责凭证侧状态（凭证 + 回链 + 凭证状态）；源业务单据的
 * {@code posted} 标志由域调用方在 {@code post()} 成功返回后自行置位。
 */
public class ErpFinPostingProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinPostingProcessor.class);

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String VOUCHER_STATUS_DRAFT = ErpFinConstants.VOUCHER_STATUS_DRAFT;
    static final String VOUCHER_STATUS_POSTED = ErpFinConstants.VOUCHER_STATUS_POSTED;

    static final String PERIOD_STATUS_OPEN = ErpFinConstants.PERIOD_STATUS_OPEN;

    static final String POSTING_TYPE_NORMAL = "NORMAL";
    static final String POSTING_TYPE_REVERSAL = "REVERSAL";

    static final String DEFAULT_VOUCHER_TYPE_TRANSFER = "TRANSFER";
    static final BigDecimal EXCHANGE_RATE_DEFAULT = new BigDecimal("1");

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpFinAcctDocRegistry registry;

    @Inject
    IBizObjectManager bizObjectManager;

    @Inject
    ErpFinArApItemGenerator arApItemGenerator;

    @Inject
    ErpFinPostingExceptionRecorder exceptionRecorder;

    @Inject
    ErpFinPostingMetrics postingMetrics;

    @Inject
    ErpFinReversalListenerRegistry reversalListenerRegistry;

    @Inject
    ITransactionTemplate transactionTemplate;

    /**
     * 正向过账编排。幂等命中（源单已过账）返回 {@code null}。
     *
     * <p>可观测性：入口解析 {@code traceId}（缺失生成），各 protected step 经 {@link #timeStage} 埋结构化日志
     * （成功含 traceId+billHeadCode+businessType+voucherId+命中 Provider/模板+各阶段耗时；失败含同一键+ErrorCode+阶段）。
     * 埋点在编排方法（非覆盖点），派生 Processor 覆盖单步时仍保留编排层埋点。
     */
    @SingleSession
    public Long process(PostingEvent event, IServiceContext context) {
        ensureTraceId(event);
        PostingRun run = PostingRun.forPost(event);
        long processBegin = CoreMetrics.nanoTime();

        if (alreadyPosted(event, context)) {
            LOG.info("过账幂等命中（源单已过账），空操作：traceId={}, billHeadCode={}, businessType={}",
                    run.traceId, run.billHeadCode, run.businessType);
            return null;
        }

        try {
            IErpFinAcctDocProvider provider = timeStage("resolveProvider", run,
                    () -> resolveProvider(event, context));
            run.providerName = provider.getClass().getSimpleName();
            run.isFallback = provider.isFallback();

            ErpFinAccountingPeriod period = timeStage("resolveOpenPeriod", run,
                    () -> resolveOpenPeriod(event.getVoucherDate(), context));
            AcctDocContext ctx = prepareContext(event, period, context);
            ctx.setTraceId(run.traceId);

            List<VoucherFact> facts = timeStage("generateFacts", run,
                    () -> generateFacts(event, provider, ctx, context));
            run.captureTemplate(facts);
            timeStageVoid("resolveSubjects", run, () -> resolveSubjects(facts, context));

            BigDecimal[] totals = timeStage("balanceTotals", run,
                    () -> balanceTotals(facts, context));
            timeStageVoid("assertBalanced", run, () -> assertBalanced(totals[0], totals[1], context));

            Long voucherId = timeStage("persistVoucher", run,
                    () -> persistVoucher(event, ctx, facts, totals[0], totals[1], false, null, POSTING_TYPE_NORMAL, context));
            // 凭证落库成功后、同事务内生成应收应付辅助账（强一致：凭证 + 辅助账同生共死）。
            timeStageVoid("generateArApItems", run, () -> arApItemGenerator.generate(event, context));

            postingMetrics.recordLatency(CoreMetrics.nanoTimeDiff(processBegin));
            LOG.info("过账成功：traceId={}, billHeadCode={}, businessType={}, voucherId={}, provider={}, fallback={}, template={}, timings(ms)={}",
                    run.traceId, run.billHeadCode, run.businessType, voucherId,
                    run.providerName, run.isFallback, run.templateDesc, run.timingsMillis());
            return voucherId;
        } catch (RuntimeException e) {
            logFailure(run, e);
            recordPostFailure(run, event, e);
            throw e;
        }
    }

    /**
     * 红冲编排。按业财回链反查原已过账凭证，生成红字冲销凭证。
     */
    @SingleSession
    public Long reverseProcess(String billHeadCode, ErpFinBusinessType businessType, IServiceContext context) {
        PostingRun run = PostingRun.forReverse(billHeadCode, businessType);
        long reverseBegin = CoreMetrics.nanoTime();

        try {
            ErpFinVoucher original = timeStage("findPostedVoucher", run,
                    () -> findPostedVoucher(billHeadCode, businessType, context));
            if (original == null) {
                throw new NopException(ErpFinPostingErrors.ERR_REVERSE_SOURCE_NOT_FOUND)
                        .param(ErpFinPostingErrors.ARG_BILL_HEAD_CODE, billHeadCode)
                        .param(ErpFinPostingErrors.ARG_BUSINESS_TYPE, businessType);
            }

            List<ErpFinVoucherLine> originalLines = timeStage("loadLines", run,
                    () -> loadLines(original.getId(), context));
            ErpFinAccountingPeriod period = timeStage("resolveOpenPeriod", run,
                    () -> resolveOpenPeriod(original.getVoucherDate(), context));
            AcctDocContext ctx = prepareReversalContext(original, period, originalLines, context);
            ctx.setTraceId(run.traceId);

            ReversalDraft draft = timeStage("buildReversalDraft", run,
                    () -> buildReversalDraft(originalLines, businessType, context));

            Long voucherId = timeStage("persistVoucher", run,
                    () -> persistVoucher(null, ctx, draft.facts, draft.totalDebit, draft.totalCredit, true,
                            original.getId(), POSTING_TYPE_REVERSAL, billHeadCode, businessType, context));
            // 红冲凭证落库后、同事务内取消原辅助账项（对齐冲销语义）。
            timeStageVoid("cancelArApOnReverse", run,
                    () -> arApItemGenerator.cancelOnReverse(billHeadCode, businessType, context));
            // 业财闭环方向二（冲销反写）：红字凭证+回链+辅助账落库后，构造 VoucherReversedEvent 派发给
            // 各域监听者回退源单状态。默认 SYNC 同事务同步通知（posting.md §实现策略 裁决3）。
            timeStageVoid("notifyReversalListeners", run,
                    () -> dispatchReversalEvent(run, voucherId, original.getId(), billHeadCode, businessType, context));

            postingMetrics.recordLatency(CoreMetrics.nanoTimeDiff(reverseBegin));
            LOG.info("红冲成功：traceId={}, billHeadCode={}, businessType={}, voucherId={}, reversalOf={}, timings(ms)={}",
                    run.traceId, run.billHeadCode, run.businessType, voucherId, original.getId(), run.timingsMillis());
            return voucherId;
        } catch (RuntimeException e) {
            logFailure(run, e);
            recordReverseFailure(run, e);
            throw e;
        }
    }

    // ---------- 可观测性埋点辅助 ----------

    /** 解析 traceId：业务域调用方已传则沿用，否则生成。 */
    protected void ensureTraceId(PostingEvent event) {
        if (StringHelper.isBlank(event.getTraceId())) {
            event.setTraceId(StringHelper.generateUUID());
        }
    }

    private <T> T timeStage(String stage, PostingRun run, java.util.function.Supplier<T> action) {
        long start = CoreMetrics.nanoTime();
        run.currentStage = stage;
        try {
            T result = action.get();
            run.recordStage(stage, CoreMetrics.nanoTimeDiff(start));
            return result;
        } catch (RuntimeException e) {
            run.recordStage(stage, CoreMetrics.nanoTimeDiff(start));
            throw e;
        }
    }

    private void timeStageVoid(String stage, PostingRun run, Runnable action) {
        long start = CoreMetrics.nanoTime();
        run.currentStage = stage;
        try {
            action.run();
            run.recordStage(stage, CoreMetrics.nanoTimeDiff(start));
        } catch (RuntimeException e) {
            run.recordStage(stage, CoreMetrics.nanoTimeDiff(start));
            throw e;
        }
    }

    private void logFailure(PostingRun run, RuntimeException e) {
        String code = e instanceof NopException ? ((NopException) e).getErrorCode() : null;
        LOG.error("过账失败：traceId={}, billHeadCode={}, businessType={}, failedStage={}, errorCode={}, errorMsg={}",
                run.traceId, run.billHeadCode, run.businessType, run.currentStage, code, e.getMessage());
    }

    /** 失败先落异常记录再抛（posting-log.md §失败不静默丢弃）：仅对 NopException 落 PENDING 记录，独立事务保证不随回滚丢失。 */
    private void recordPostFailure(PostingRun run, PostingEvent event, RuntimeException e) {
        if (!(e instanceof NopException)) {
            return;
        }
        String errorCode = ((NopException) e).getErrorCode();
        String errorMessage = ((NopException) e).getDescription();
        if (errorMessage == null) {
            errorMessage = e.getMessage();
        }
        LocalDate voucherDate = event != null ? event.getVoucherDate() : null;
        Long orgId = event != null ? event.getOrgId() : null;
        Long acctSchemaId = event != null ? event.getAcctSchemaId() : null;
        Long currencyId = event != null ? event.getCurrencyId() : null;
        java.math.BigDecimal exchangeRate = event != null ? event.getExchangeRate() : null;
        String eventData = event != null
                ? ErpFinPostingExceptionRecorder.serializeEventData(event.getBillData()) : null;
        exceptionRecorder.record(run.traceId, run.billHeadCode, run.businessType, POSTING_TYPE_NORMAL,
                errorCode, errorMessage, run.currentStage, voucherDate, orgId, acctSchemaId,
                currencyId, exchangeRate, eventData);
    }

    private void recordReverseFailure(PostingRun run, RuntimeException e) {
        if (!(e instanceof NopException)) {
            return;
        }
        String errorCode = ((NopException) e).getErrorCode();
        String errorMessage = ((NopException) e).getDescription();
        if (errorMessage == null) {
            errorMessage = e.getMessage();
        }
        // 红冲无完整 PostingEvent 快照（按回链反查），eventData 留空；重试经 reverse() 重建。
        exceptionRecorder.record(run.traceId, run.billHeadCode, run.businessType, POSTING_TYPE_REVERSAL,
                errorCode, errorMessage, run.currentStage, null, null, null,
                null, null, null);
    }

    /**
     * 构造 {@link VoucherReversedEvent} 并按配置派发给各域监听者（业财闭环方向二）。
     *
     * <p>派发模式（{@link ErpFinConstants#CONFIG_REVERSAL_DISPATCH_MODE}）：
     * <ul>
     *   <li>默认 SYNC：同事务内同步遍历监听者调用——监听者回退与红字凭证原子提交（强一致）。
     *       监听者失败经 {@link ErpFinReversalListenerRegistry#dispatch} 的 try/catch 隔离
     *       （不阻断其他监听者、不回滚红字凭证），失败记录落入 5.1 异常工作台。</li>
     *   <li>ASYNC：经 {@code txn().afterCommit} 注册 post-commit 回调，红字凭证事务提交成功后再派发
     *       （对齐 posting.md §总体架构 第②层 ASYNC 模式）。</li>
     * </ul>
     */
    protected void dispatchReversalEvent(PostingRun run, Long voucherId, Long reversalOfVoucherId,
                                          String billHeadCode, ErpFinBusinessType businessType,
                                          IServiceContext context) {
        VoucherReversedEvent event = new VoucherReversedEvent();
        event.setVoucherId(voucherId);
        event.setReversalOfVoucherId(reversalOfVoucherId);
        event.setBillHeadCode(billHeadCode);
        event.setBusinessType(businessType == null ? null : businessType.name());
        event.setBillType(businessType == null ? null : businessType.name());
        event.setTraceId(run.traceId);

        String mode = resolveDispatchMode();
        if (ErpFinConstants.REVERSAL_DISPATCH_MODE_ASYNC.equals(mode)) {
            // post-commit 派发：红字凭证事务提交成功后再通知域监听者（ASYNC 解耦）。
            transactionTemplate.afterCommit(null, () -> {
                List<ErpFinReversalListenerRegistry.ListenerFailure> failures =
                        reversalListenerRegistry.dispatch(event, context);
                recordListenerFailures(run, failures, billHeadCode, businessType);
            });
        } else {
            // SYNC 同事务派发：监听者回退与红字凭证原子提交（强一致默认）。
            List<ErpFinReversalListenerRegistry.ListenerFailure> failures =
                    reversalListenerRegistry.dispatch(event, context);
            recordListenerFailures(run, failures, billHeadCode, businessType);
        }
    }

    /** 读取派发模式配置（默认 SYNC）。 */
    protected String resolveDispatchMode() {
        String mode = AppConfig.var(ErpFinConstants.CONFIG_REVERSAL_DISPATCH_MODE,
                ErpFinConstants.REVERSAL_DISPATCH_MODE_SYNC);
        return ErpFinConstants.REVERSAL_DISPATCH_MODE_ASYNC.equals(mode)
                ? ErpFinConstants.REVERSAL_DISPATCH_MODE_ASYNC
                : ErpFinConstants.REVERSAL_DISPATCH_MODE_SYNC;
    }

    /**
     * 监听者派发失败记录落入 5.1 异常工作台（独立事务，posting-log.md §失败不静默丢弃）。
     * 凭证法律效力：红字凭证已过账不回滚；监听者失败进 PENDING 队列供人工处置。
     */
    protected void recordListenerFailures(PostingRun run,
                                            List<ErpFinReversalListenerRegistry.ListenerFailure> failures,
                                            String billHeadCode, ErpFinBusinessType businessType) {
        if (failures == null || failures.isEmpty()) {
            return;
        }
        String bizType = businessType == null ? null : businessType.name();
        for (ErpFinReversalListenerRegistry.ListenerFailure f : failures) {
            String errorCode = StringHelper.isBlank(f.getErrorCode())
                    ? ErpFinPostingErrors.ERR_REVERSAL_LISTENER_FAILED.getErrorCode() : f.getErrorCode();
            String errorMsg = "监听者=" + f.getListenerName() + "；" + (f.getErrorMessage() == null ? "" : f.getErrorMessage());
            exceptionRecorder.record(run.traceId, billHeadCode, bizType, POSTING_TYPE_REVERSAL,
                    errorCode, errorMsg, ErpFinConstants.FAILED_STAGE_NOTIFY_REVERSAL_LISTENER,
                    null, null, null, null, null, null);
        }
    }

    /** 过账运行态（traceId+路由结果+各阶段耗时），供编排层埋点与失败记录复用。 */
    protected static final class PostingRun {
        final String traceId;
        final String billHeadCode;
        final String businessType;
        String providerName;
        boolean isFallback;
        String templateDesc;
        String currentStage;
        final java.util.Map<String, Long> stageNanos = new java.util.LinkedHashMap<>();

        static PostingRun forPost(PostingEvent event) {
            return new PostingRun(event.getTraceId(), event.getBillHeadCode(),
                    event.getBusinessType() == null ? null : event.getBusinessType().name());
        }

        static PostingRun forReverse(String billHeadCode, ErpFinBusinessType businessType) {
            String traceId = StringHelper.generateUUID();
            return new PostingRun(traceId, billHeadCode, businessType == null ? null : businessType.name());
        }

        private PostingRun(String traceId, String billHeadCode, String businessType) {
            this.traceId = traceId;
            this.billHeadCode = billHeadCode;
            this.businessType = businessType;
        }

        void captureTemplate(List<VoucherFact> facts) {
            templateDesc = null;
        }

        void recordStage(String stage, long nanos) {
            stageNanos.put(stage, nanos);
        }

        java.util.Map<String, Long> timingsMillis() {
            java.util.Map<String, Long> ms = new java.util.LinkedHashMap<>();
            stageNanos.forEach((k, v) -> ms.put(k, CoreMetrics.nanoToMillis(v)));
            return ms;
        }
    }

    // ---------- 步骤（protected + IServiceContext 末参，供派生覆盖） ----------

    protected boolean alreadyPosted(PostingEvent event, IServiceContext context) {
        List<ErpFinVoucherBillR> links = findBillLinks(event.getBillHeadCode(), event.getBusinessType(), context);
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher voucher = voucherDao.getEntityById(link.getVoucherId());
            if (voucher != null && VOUCHER_STATUS_POSTED.equals(voucher.getDocStatus())
                    && !Boolean.TRUE.equals(voucher.getIsReversed())) {
                return true;
            }
        }
        return false;
    }

    protected IErpFinAcctDocProvider resolveProvider(PostingEvent event, IServiceContext context) {
        IErpFinAcctDocProvider provider = registry.getProvider(event.getBusinessType());
        if (provider == null) {
            throw new NopException(ErpFinPostingErrors.ERR_NO_PROVIDER)
                    .param(ErpFinPostingErrors.ARG_BUSINESS_TYPE, event.getBusinessType());
        }
        return provider;
    }

    protected ErpFinAccountingPeriod resolveOpenPeriod(LocalDate voucherDate, IServiceContext context) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        if (voucherDate != null) {
            q.addFilter(and(le("startDate", voucherDate), ge("endDate", voucherDate)));
        }
        List<ErpFinAccountingPeriod> periods = dao.findAllByQuery(q);
        if (periods.isEmpty()) {
            throw new NopException(ErpFinPostingErrors.ERR_PERIOD_NOT_FOUND)
                    .param(ErpFinPostingErrors.ARG_VOUCHER_DATE, voucherDate);
        }
        ErpFinAccountingPeriod period = periods.get(0);
        if (!PERIOD_STATUS_OPEN.equals(period.getStatus())) {
            throw new NopException(ErpFinPostingErrors.ERR_PERIOD_CLOSED)
                    .param(ErpFinPostingErrors.ARG_PERIOD_STATUS, period.getStatus());
        }
        return period;
    }

    protected AcctDocContext prepareContext(PostingEvent event, ErpFinAccountingPeriod period, IServiceContext context) {
        AcctDocContext ctx = new AcctDocContext();
        ctx.setVoucherDate(event.getVoucherDate());
        ctx.setAcctSchemaId(event.getAcctSchemaId());
        ctx.setOrgId(event.getOrgId());
        ctx.setCurrencyId(event.getCurrencyId());
        ctx.setExchangeRate(event.getExchangeRate() != null ? event.getExchangeRate() : EXCHANGE_RATE_DEFAULT);
        ctx.setPeriodId(period.getId());
        ctx.setPeriodStatus(period.getStatus());
        return ctx;
    }

    protected AcctDocContext prepareReversalContext(ErpFinVoucher original, ErpFinAccountingPeriod period,
                                                    List<ErpFinVoucherLine> originalLines, IServiceContext context) {
        AcctDocContext ctx = new AcctDocContext();
        ctx.setVoucherDate(original.getVoucherDate());
        ctx.setAcctSchemaId(original.getAcctSchemaId());
        ctx.setOrgId(original.getOrgId());
        ctx.setPeriodId(period.getId());
        ctx.setPeriodStatus(period.getStatus());
        ctx.setVoucherType(original.getVoucherType());
        if (!originalLines.isEmpty()) {
            ErpFinVoucherLine first = originalLines.get(0);
            ctx.setCurrencyId(first.getCurrencyId());
            ctx.setExchangeRate(first.getExchangeRate());
        }
        return ctx;
    }

    protected List<VoucherFact> generateFacts(PostingEvent event, IErpFinAcctDocProvider provider,
                                              AcctDocContext ctx, IServiceContext context) {
        List<VoucherFact> facts = provider.createFacts(event, ctx);
        for (IErpFinFactsValidator validator : registry.getValidators()) {
            facts = validator.validate(facts, ctx);
        }
        return facts;
    }

    protected void resolveSubjects(List<VoucherFact> facts, IServiceContext context) {
        if (facts.isEmpty()) {
            return;
        }
        // 科目解析经 master-data 的 IErpMdSubjectBiz（跨域只读经 I*Biz 管道，对齐 service-layer 跨实体访问规则）。
        // finance→erp-md-service 仅 test 作用域，故非 BizModel 编排 bean 经 IBizObjectManager 按名解析（运行期 app-erp-all 注入）。
        Map<String, ErpMdSubject> cache = new HashMap<>();
        IErpMdSubjectBiz mdSubjectBiz = bizObjectManager.getBizObject(ErpMdSubject.class.getSimpleName()).asProxy();
        for (VoucherFact fact : facts) {
            if (fact.getSubjectId() != null) {
                continue;
            }
            String code = fact.getSubjectCode();
            if (code == null) {
                throw new NopException(ErpFinPostingErrors.ERR_SUBJECT_NOT_FOUND).param(ErpFinPostingErrors.ARG_SUBJECT_CODE, code);
            }
            ErpMdSubject subject = cache.get(code);
            if (subject == null) {
                subject = mdSubjectBiz.findByCode(code, context);
                if (subject == null) {
                    throw new NopException(ErpFinPostingErrors.ERR_SUBJECT_NOT_FOUND)
                            .param(ErpFinPostingErrors.ARG_SUBJECT_CODE, code);
                }
                cache.put(code, subject);
            }
            fact.setSubjectId(subject.getId());
            if (StringHelper.isBlank(fact.getSubjectName())) {
                fact.setSubjectName(subject.getName());
            }
        }
    }

    protected BigDecimal[] balanceTotals(List<VoucherFact> facts, IServiceContext context) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (VoucherFact fact : facts) {
            BigDecimal amt = fact.getAmount() == null ? BigDecimal.ZERO : fact.getAmount();
            if (Objects.equals(DC_CREDIT, fact.getDcDirection())) {
                totalCredit = totalCredit.add(amt);
            } else {
                totalDebit = totalDebit.add(amt);
            }
        }
        return new BigDecimal[]{totalDebit, totalCredit};
    }

    protected void assertBalanced(BigDecimal totalDebit, BigDecimal totalCredit, IServiceContext context) {
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new NopException(ErpFinPostingErrors.ERR_UNBALANCED)
                    .param(ErpFinPostingErrors.ARG_TOTAL_DEBIT, totalDebit.toPlainString())
                    .param(ErpFinPostingErrors.ARG_TOTAL_CREDIT, totalCredit.toPlainString());
        }
    }

    protected ReversalDraft buildReversalDraft(List<ErpFinVoucherLine> originalLines,
                                               ErpFinBusinessType businessType, IServiceContext context) {
        List<VoucherFact> facts = new ArrayList<>(originalLines.size());
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (ErpFinVoucherLine ol : originalLines) {
            BigDecimal debit = ol.getDebitAmount() != null ? ol.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = ol.getCreditAmount() != null ? ol.getCreditAmount() : BigDecimal.ZERO;
            BigDecimal negDebit = debit.negate();
            BigDecimal negCredit = credit.negate();
            totalDebit = totalDebit.add(negDebit);
            totalCredit = totalCredit.add(negCredit);

            VoucherFact fact = new VoucherFact();
            fact.setSubjectId(ol.getSubjectId());
            fact.setSubjectCode(ol.getSubjectCode());
            fact.setSubjectName(ol.getSubjectName());
            fact.setDcDirection(ol.getDcDirection());
            fact.setAmount(ol.getDcDirection() != null && Objects.equals(ol.getDcDirection(), DC_CREDIT) ? negCredit : negDebit);
            fact.setMemo(ol.getMemo());
            fact.setBusinessType(businessType.name());
            fact.setPartnerId(ol.getPartnerId());
            fact.setDepartmentId(ol.getDepartmentId());
            fact.setProjectId(ol.getProjectId());
            fact.setWarehouseId(ol.getWarehouseId());
            fact.setMaterialId(ol.getMaterialId());
            fact.setCostCenterId(ol.getCostCenterId());
            facts.add(fact);
        }
        return new ReversalDraft(facts, totalDebit, totalCredit);
    }

    protected Long persistVoucher(PostingEvent event, AcctDocContext ctx, List<VoucherFact> facts,
                                  BigDecimal totalDebit, BigDecimal totalCredit, boolean isReversed,
                                  Long reversalOfVoucherId, String postingType, IServiceContext context) {
        return persistVoucher(event, ctx, facts, totalDebit, totalCredit, isReversed, reversalOfVoucherId,
                postingType, null, null, context);
    }

    protected Long persistVoucher(PostingEvent event, AcctDocContext ctx, List<VoucherFact> facts,
                                  BigDecimal totalDebit, BigDecimal totalCredit, boolean isReversed,
                                  Long reversalOfVoucherId, String postingType, String billHeadCode,
                                  ErpFinBusinessType businessType, IServiceContext context) {
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);

        Long acctSchemaId = ctx.getAcctSchemaId();
        Long orgId = ctx.getOrgId();
        Long periodId = ctx.getPeriodId();
        LocalDate voucherDate = ctx.getVoucherDate();
        String voucherType = ctx.getVoucherType() != null ? ctx.getVoucherType() : DEFAULT_VOUCHER_TYPE_TRANSFER;

        ErpFinVoucher voucher = voucherDao.newEntity();
        voucher.setCode(buildVoucherCode(businessType != null ? businessType
                : (event != null ? event.getBusinessType() : null), isReversed, context));
        voucher.setVoucherType(voucherType);
        voucher.setPostingType(postingType);
        voucher.setVoucherDate(voucherDate);
        voucher.setOrgId(orgId);
        voucher.setAcctSchemaId(acctSchemaId);
        voucher.setPeriodId(periodId);
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setIsReversed(isReversed);
        if (reversalOfVoucherId != null) {
            voucher.setReversalOfVoucherId(reversalOfVoucherId);
        }
        voucher.setDocStatus(VOUCHER_STATUS_POSTED);
        voucher.setPostedAt(CoreMetrics.currentDateTime());
        voucherDao.saveEntity(voucher);
        Long voucherId = voucher.getId();

        Long currencyId = ctx.getCurrencyId();
        BigDecimal exchangeRate = ctx.getExchangeRate() != null
                ? ctx.getExchangeRate()
                : EXCHANGE_RATE_DEFAULT;

        int lineNo = 1;
        for (VoucherFact fact : facts) {
            BigDecimal amt = fact.getAmount() == null ? BigDecimal.ZERO : fact.getAmount();
            ErpFinVoucherLine line = lineDao.newEntity();
            line.setVoucherId(voucherId);
            line.setLineNo(lineNo++);
            line.setSubjectId(fact.getSubjectId());
            line.setSubjectCode(fact.getSubjectCode());
            line.setSubjectName(fact.getSubjectName());
            line.setDcDirection(fact.getDcDirection());
            boolean isCredit = fact.getDcDirection() != null && Objects.equals(fact.getDcDirection(), DC_CREDIT);
            line.setDebitAmount(isCredit ? BigDecimal.ZERO : amt);
            line.setCreditAmount(isCredit ? amt : BigDecimal.ZERO);
            line.setCurrencyId(currencyId);
            line.setExchangeRate(exchangeRate);
            line.setAmountSource(amt);
            line.setAmountFunctional(amt);
            line.setAcctSchemaId(acctSchemaId);
            line.setMemo(fact.getMemo());
            line.setBusinessType(businessType != null ? businessType.name()
                    : (event != null ? event.getBusinessType().name() : null));
            line.setPartnerId(fact.getPartnerId());
            line.setDepartmentId(fact.getDepartmentId());
            line.setProjectId(fact.getProjectId());
            line.setWarehouseId(fact.getWarehouseId());
            line.setMaterialId(fact.getMaterialId());
            line.setCostCenterId(fact.getCostCenterId());
            lineDao.saveEntity(line);
        }

        String resolvedBillCode = event != null ? event.getBillHeadCode() : billHeadCode;
        ErpFinBusinessType resolvedType = businessType != null ? businessType
                : (event != null ? event.getBusinessType() : null);
        if (!StringHelper.isBlank(resolvedBillCode) && resolvedType != null) {
            ErpFinVoucherBillR billR = billRDao.newEntity();
            billR.setVoucherId(voucherId);
            billR.setBillType(resolvedType.name());
            billR.setBillCode(resolvedBillCode);
            billR.setBusinessType(resolvedType.name());
            billRDao.saveEntity(billR);
        }

        return voucherId;
    }

    protected ErpFinVoucher findPostedVoucher(String billHeadCode, ErpFinBusinessType businessType,
                                              IServiceContext context) {
        List<ErpFinVoucherBillR> links = findBillLinks(billHeadCode, businessType, context);
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher voucher = voucherDao.getEntityById(link.getVoucherId());
            if (voucher != null && VOUCHER_STATUS_POSTED.equals(voucher.getDocStatus())
                    && !Boolean.TRUE.equals(voucher.getIsReversed())) {
                return voucher;
            }
        }
        return null;
    }

    protected List<ErpFinVoucherBillR> findBillLinks(String billHeadCode, ErpFinBusinessType businessType,
                                                     IServiceContext context) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType.name())));
        return dao.findAllByQuery(q);
    }

    protected List<ErpFinVoucherLine> loadLines(Long voucherId, IServiceContext context) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        List<ErpFinVoucherLine> lines = new ArrayList<>(dao.findAllByQuery(q));
        lines.sort(Comparator.comparingInt(l -> l.getLineNo() == null ? Integer.MAX_VALUE : l.getLineNo()));
        return lines;
    }

    protected String buildVoucherCode(ErpFinBusinessType type, boolean reversal, IServiceContext context) {
        String prefix = reversal ? "REV-" : "PST-";
        // 用业务类型 int code（而非枚举名）以控制长度：PST-{code}-{32hex} ≤ 40 字符，适配 CODE VARCHAR(50)
        String typePart = type != null ? String.valueOf(type.getCode()) : "0";
        return prefix + typePart + "-" + StringHelper.generateUUID();
    }

    /** 红冲草稿：分录 + 借贷合计（金额取负，与原凭证对冲）。 */
    protected static final class ReversalDraft {
        final List<VoucherFact> facts;
        final BigDecimal totalDebit;
        final BigDecimal totalCredit;

        ReversalDraft(List<VoucherFact> facts, BigDecimal totalDebit, BigDecimal totalCredit) {
            this.facts = facts;
            this.totalDebit = totalDebit;
            this.totalCredit = totalCredit;
        }
    }
}
