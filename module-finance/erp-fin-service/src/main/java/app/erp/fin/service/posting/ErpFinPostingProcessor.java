package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.biz.IErpMdSubjectBiz;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.api.IBizObjectManager;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    static final int DC_DEBIT = 10;
    static final int DC_CREDIT = 20;

    static final int VOUCHER_STATUS_DRAFT = 10;
    static final int VOUCHER_STATUS_POSTED = 20;

    static final int PERIOD_STATUS_OPEN = 10;

    static final int POSTING_TYPE_NORMAL = 10;
    static final int POSTING_TYPE_REVERSAL = 50;

    static final int DEFAULT_VOUCHER_TYPE_TRANSFER = 30;
    static final BigDecimal EXCHANGE_RATE_DEFAULT = new BigDecimal("1");

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpFinAcctDocRegistry registry;

    @Inject
    IBizObjectManager bizObjectManager;

    /**
     * 正向过账编排。幂等命中（源单已过账）返回 {@code null}。
     */
    @SingleSession
    public Long process(PostingEvent event, IServiceContext context) {
        if (alreadyPosted(event, context)) {
            return null;
        }

        IErpFinAcctDocProvider provider = resolveProvider(event, context);
        ErpFinAccountingPeriod period = resolveOpenPeriod(event.getVoucherDate(), context);
        AcctDocContext ctx = prepareContext(event, period, context);

        List<VoucherFact> facts = generateFacts(event, provider, ctx, context);
        resolveSubjects(facts, context);

        BigDecimal[] totals = balanceTotals(facts, context);
        assertBalanced(totals[0], totals[1], context);

        return persistVoucher(event, ctx, facts, totals[0], totals[1], false, null, POSTING_TYPE_NORMAL, context);
    }

    /**
     * 红冲编排。按业财回链反查原已过账凭证，生成红字冲销凭证。
     */
    @SingleSession
    public Long reverseProcess(String billHeadCode, ErpFinBusinessType businessType, IServiceContext context) {
        ErpFinVoucher original = findPostedVoucher(billHeadCode, businessType, context);
        if (original == null) {
            throw new NopException(ErpFinPostingErrors.ERR_REVERSE_SOURCE_NOT_FOUND)
                    .param(ErpFinPostingErrors.ARG_BILL_HEAD_CODE, billHeadCode)
                    .param(ErpFinPostingErrors.ARG_BUSINESS_TYPE, businessType);
        }

        List<ErpFinVoucherLine> originalLines = loadLines(original.getId(), context);
        ErpFinAccountingPeriod period = resolveOpenPeriod(original.getVoucherDate(), context);
        AcctDocContext ctx = prepareReversalContext(original, period, originalLines, context);

        ReversalDraft draft = buildReversalDraft(originalLines, businessType, context);

        return persistVoucher(null, ctx, draft.facts, draft.totalDebit, draft.totalCredit, true,
                original.getId(), POSTING_TYPE_REVERSAL, billHeadCode, businessType, context);
    }

    // ---------- 步骤（protected + IServiceContext 末参，供派生覆盖） ----------

    protected boolean alreadyPosted(PostingEvent event, IServiceContext context) {
        List<ErpFinVoucherBillR> links = findBillLinks(event.getBillHeadCode(), event.getBusinessType(), context);
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher voucher = voucherDao.getEntityById(link.getVoucherId());
            if (voucher != null && Integer.valueOf(VOUCHER_STATUS_POSTED).equals(voucher.getDocStatus())
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
        if (!Integer.valueOf(PERIOD_STATUS_OPEN).equals(period.getStatus())) {
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
            if (DC_CREDIT == fact.getDcDirection()) {
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
            fact.setAmount(ol.getDcDirection() != null && ol.getDcDirection() == DC_CREDIT ? negCredit : negDebit);
            fact.setMemo(ol.getMemo());
            fact.setBusinessType(businessType.getCode());
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
                                  Long reversalOfVoucherId, int postingType, IServiceContext context) {
        return persistVoucher(event, ctx, facts, totalDebit, totalCredit, isReversed, reversalOfVoucherId,
                postingType, null, null, context);
    }

    protected Long persistVoucher(PostingEvent event, AcctDocContext ctx, List<VoucherFact> facts,
                                  BigDecimal totalDebit, BigDecimal totalCredit, boolean isReversed,
                                  Long reversalOfVoucherId, int postingType, String billHeadCode,
                                  ErpFinBusinessType businessType, IServiceContext context) {
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);

        Long acctSchemaId = ctx.getAcctSchemaId();
        Long orgId = ctx.getOrgId();
        Long periodId = ctx.getPeriodId();
        LocalDate voucherDate = ctx.getVoucherDate();
        Integer voucherType = ctx.getVoucherType() != null ? ctx.getVoucherType() : DEFAULT_VOUCHER_TYPE_TRANSFER;

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
            boolean isCredit = fact.getDcDirection() != null && fact.getDcDirection() == DC_CREDIT;
            line.setDebitAmount(isCredit ? BigDecimal.ZERO : amt);
            line.setCreditAmount(isCredit ? amt : BigDecimal.ZERO);
            line.setCurrencyId(currencyId);
            line.setExchangeRate(exchangeRate);
            line.setAmountSource(amt);
            line.setAmountFunctional(amt);
            line.setAcctSchemaId(acctSchemaId);
            line.setMemo(fact.getMemo());
            line.setBusinessType(businessType != null ? businessType.getCode()
                    : (event != null ? event.getBusinessType().getCode() : null));
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
            billR.setBusinessType(resolvedType.getCode());
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
            if (voucher != null && Integer.valueOf(VOUCHER_STATUS_POSTED).equals(voucher.getDocStatus())
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
        q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType.getCode())));
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
