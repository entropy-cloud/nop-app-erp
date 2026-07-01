package app.erp.fin.service.posting;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
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
 * 业财过账编排服务。实现 {@code docs/design/finance/posting.md} 的过账全流程：
 * 幂等前置（按业财回链反查）→ 查 Provider → createFacts → FactsValidator 链 → 期间门控 → 借贷平衡 →
 * 写 {@link ErpFinVoucher}+{@link ErpFinVoucherLine}+{@link ErpFinVoucherBillR} + 置凭证 {@code docStatus=POSTED}。
 *
 * <p>引擎只负责凭证侧状态（凭证 + 回链 + 凭证状态）；源业务单据的 {@code posted} 标志由域调用方在
 * {@link #post(PostingEvent)} 成功返回后自行置位（引擎不持有任意源实体的 ORM 引用）。
 *
 * <p>为非 BizModel 服务：通过 {@code app-service.beans.xml} 注册为 Bean，{@link IDaoProvider}/
 * {@link ErpFinAcctDocRegistry} 经 {@code @Inject} 字段注入；{@link SingleSession}+{@link Transactional}
 * 提供 ORM Session 与事务边界（对齐平台 non-bizmodel-orm-access runbook）。
 */
public class ErpFinPostingService {

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

    @SingleSession
    @Transactional
    public Long post(PostingEvent event) {
        if (alreadyPosted(event)) {
            return null;
        }

        IErpFinAcctDocProvider provider = registry.getProvider(event.getBusinessType());
        if (provider == null) {
            throw new NopException(ErpFinPostingErrors.ERR_NO_PROVIDER)
                    .param(ErpFinPostingErrors.ARG_BUSINESS_TYPE, event.getBusinessType());
        }

        ErpFinAccountingPeriod period = resolveOpenPeriod(event.getVoucherDate());

        AcctDocContext ctx = new AcctDocContext();
        ctx.setVoucherDate(event.getVoucherDate());
        ctx.setAcctSchemaId(event.getAcctSchemaId());
        ctx.setOrgId(event.getOrgId());
        ctx.setCurrencyId(event.getCurrencyId());
        ctx.setExchangeRate(event.getExchangeRate() != null ? event.getExchangeRate() : EXCHANGE_RATE_DEFAULT);
        ctx.setPeriodId(period.getId());
        ctx.setPeriodStatus(period.getStatus());

        List<VoucherFact> facts = provider.createFacts(event, ctx);
        for (IErpFinFactsValidator validator : registry.getValidators()) {
            facts = validator.validate(facts, ctx);
        }

        resolveSubjects(facts);

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
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new NopException(ErpFinPostingErrors.ERR_UNBALANCED)
                    .param(ErpFinPostingErrors.ARG_TOTAL_DEBIT, totalDebit.toPlainString())
                    .param(ErpFinPostingErrors.ARG_TOTAL_CREDIT, totalCredit.toPlainString());
        }

        return persistVoucher(event, ctx, facts, totalDebit, totalCredit, false, null, POSTING_TYPE_NORMAL);
    }

    @SingleSession
    @Transactional
    public Long reverse(String billHeadCode, ErpFinBusinessType businessType) {
        ErpFinVoucher original = findPostedVoucher(billHeadCode, businessType);
        if (original == null) {
            throw new NopException(ErpFinPostingErrors.ERR_REVERSE_SOURCE_NOT_FOUND)
                    .param(ErpFinPostingErrors.ARG_BILL_HEAD_CODE, billHeadCode)
                    .param(ErpFinPostingErrors.ARG_BUSINESS_TYPE, businessType);
        }

        List<ErpFinVoucherLine> originalLines = loadLines(original.getId());

        ErpFinAccountingPeriod period = resolveOpenPeriod(original.getVoucherDate());

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
            ctx.setExchangeRate(parseAmount(first.getExchangeRate()));
        }

        List<VoucherFact> facts = new ArrayList<>(originalLines.size());
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (ErpFinVoucherLine ol : originalLines) {
            BigDecimal debit = parseAmount(ol.getDebitAmount());
            BigDecimal credit = parseAmount(ol.getCreditAmount());
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

        return persistVoucher(null, ctx, facts, totalDebit, totalCredit, true, original.getId(),
                POSTING_TYPE_REVERSAL, billHeadCode, businessType);
    }

    private Long persistVoucher(PostingEvent event, AcctDocContext ctx, List<VoucherFact> facts,
                                BigDecimal totalDebit, BigDecimal totalCredit, boolean isReversed,
                                Long reversalOfVoucherId, int postingType) {
        return persistVoucher(event, ctx, facts, totalDebit, totalCredit, isReversed, reversalOfVoucherId,
                postingType, null, null);
    }

    private Long persistVoucher(PostingEvent event, AcctDocContext ctx, List<VoucherFact> facts,
                                BigDecimal totalDebit, BigDecimal totalCredit, boolean isReversed,
                                Long reversalOfVoucherId, int postingType, String billHeadCode,
                                ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);

        Long acctSchemaId = ctx.getAcctSchemaId();
        Long orgId = ctx.getOrgId();
        Long periodId = ctx.getPeriodId();
        LocalDate voucherDate = ctx.getVoucherDate();
        Integer voucherType = ctx.getVoucherType() != null ? ctx.getVoucherType() : DEFAULT_VOUCHER_TYPE_TRANSFER;

        ErpFinVoucher voucher = new ErpFinVoucher();
        voucher.setCode(buildVoucherCode(businessType != null ? businessType
                : (event != null ? event.getBusinessType() : null), isReversed));
        voucher.setVoucherType(voucherType);
        voucher.setPostingType(postingType);
        voucher.setVoucherDate(voucherDate);
        voucher.setOrgId(orgId);
        voucher.setAcctSchemaId(acctSchemaId);
        voucher.setPeriodId(periodId);
        voucher.setTotalDebit(totalDebit.toPlainString());
        voucher.setTotalCredit(totalCredit.toPlainString());
        voucher.setIsReversed(isReversed);
        if (reversalOfVoucherId != null) {
            voucher.setReversalOfVoucherId(reversalOfVoucherId);
        }
        voucher.setDocStatus(VOUCHER_STATUS_POSTED);
        voucher.setPostedAt(CoreMetrics.currentDateTime());
        voucherDao.saveEntity(voucher);
        Long voucherId = voucher.getId();

        Long currencyId = ctx.getCurrencyId();
        String exchangeRate = ctx.getExchangeRate() != null
                ? ctx.getExchangeRate().toPlainString()
                : EXCHANGE_RATE_DEFAULT.toPlainString();

        int lineNo = 1;
        for (VoucherFact fact : facts) {
            BigDecimal amt = fact.getAmount() == null ? BigDecimal.ZERO : fact.getAmount();
            ErpFinVoucherLine line = new ErpFinVoucherLine();
            line.setVoucherId(voucherId);
            line.setLineNo(lineNo++);
            line.setSubjectId(fact.getSubjectId());
            line.setSubjectCode(fact.getSubjectCode());
            line.setSubjectName(fact.getSubjectName());
            line.setDcDirection(fact.getDcDirection());
            boolean isCredit = fact.getDcDirection() != null && fact.getDcDirection() == DC_CREDIT;
            line.setDebitAmount(isCredit ? BigDecimal.ZERO.toPlainString() : amt.toPlainString());
            line.setCreditAmount(isCredit ? amt.toPlainString() : BigDecimal.ZERO.toPlainString());
            line.setCurrencyId(currencyId);
            line.setExchangeRate(exchangeRate);
            line.setAmountSource(amt.toPlainString());
            line.setAmountFunctional(amt.toPlainString());
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
            ErpFinVoucherBillR billR = new ErpFinVoucherBillR();
            billR.setVoucherId(voucherId);
            billR.setBillType(resolvedType.name());
            billR.setBillCode(resolvedBillCode);
            billR.setBusinessType(resolvedType.getCode());
            billRDao.saveEntity(billR);
        }

        return voucherId;
    }

    private boolean alreadyPosted(PostingEvent event) {
        List<ErpFinVoucherBillR> links = findBillLinks(event.getBillHeadCode(), event.getBusinessType());
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

    private ErpFinVoucher findPostedVoucher(String billHeadCode, ErpFinBusinessType businessType) {
        List<ErpFinVoucherBillR> links = findBillLinks(billHeadCode, businessType);
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

    private List<ErpFinVoucherBillR> findBillLinks(String billHeadCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType.getCode())));
        return dao.findAllByQuery(q);
    }

    private ErpFinAccountingPeriod resolveOpenPeriod(LocalDate voucherDate) {
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

    private void resolveSubjects(List<VoucherFact> facts) {
        if (facts.isEmpty()) {
            return;
        }
        Map<String, ErpMdSubject> cache = new HashMap<>();
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
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
                QueryBean q = new QueryBean();
                q.addFilter(eq("code", code));
                List<ErpMdSubject> list = dao.findAllByQuery(q);
                if (list.isEmpty()) {
                    throw new NopException(ErpFinPostingErrors.ERR_SUBJECT_NOT_FOUND)
                            .param(ErpFinPostingErrors.ARG_SUBJECT_CODE, code);
                }
                subject = list.get(0);
                cache.put(code, subject);
            }
            fact.setSubjectId(subject.getId());
            if (StringHelper.isBlank(fact.getSubjectName())) {
                fact.setSubjectName(subject.getName());
            }
        }
    }

    private List<ErpFinVoucherLine> loadLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        List<ErpFinVoucherLine> lines = new ArrayList<>(dao.findAllByQuery(q));
        lines.sort(Comparator.comparingInt(l -> l.getLineNo() == null ? Integer.MAX_VALUE : l.getLineNo()));
        return lines;
    }

    private BigDecimal parseAmount(String text) {
        if (StringHelper.isBlank(text)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(text.trim());
    }

    private String buildVoucherCode(ErpFinBusinessType type, boolean reversal) {
        String prefix = reversal ? "REV-" : "PST-";
        // 用业务类型 int code（而非枚举名）以控制长度：PST-{code}-{32hex} ≤ 40 字符，适配 CODE VARCHAR(50)
        String typePart = type != null ? String.valueOf(type.getCode()) : "0";
        return prefix + typePart + "-" + StringHelper.generateUUID();
    }
}
