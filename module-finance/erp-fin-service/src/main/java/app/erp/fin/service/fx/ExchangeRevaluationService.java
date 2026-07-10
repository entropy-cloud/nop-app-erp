package app.erp.fin.service.fx;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.close.CloseVoucherWriter;
import app.erp.fin.service.close.CloseVoucherWriter.Line;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.ne;
import static io.nop.api.core.beans.FilterBeans.notIn;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * 期末汇兑重估服务（{@code period-close.md §汇兑重估}，承接 0300-3 deferred + 0540-2 银行存款扩展）。查询外币应收应付未核销项
 * 与外币银行存款账户余额，按期末汇率重估差额，生成 EXCHANGE_GAIN_LOSS(130) 凭证。
 *
 * <p>范围（Decision）：
 * <ul>
 *   <li>外币 {@link ErpFinArApItem} 未核销项（AR/AP 往来）——按 openAmountFunctional vs openAmountSource×期末汇率；</li>
 *   <li>外币 {@link ErpFinFundAccount} 银行存款账户余额（0540-2 扩展）——按账面本位币（科目聚合）vs currentBalance×期末汇率，
 *       config-gated {@code erp-fin.bank-fx-revaluation-enabled}。</li>
 * </ul>
 * 本位币项不重估。
 *
 * <p>两类重估同业务类型（EXCHANGE_GAIN_LOSS）、同事务、同汇率源；银行存款重估凭证与 AR/AP 重估共用 billHeadCode 前缀
 * {@link #BILL_CODE_PREFIX}（反结账按此反查冲销，统一覆盖）。
 *
 * <p>差额公式：{@code diff = openAmountFunctional − (openAmountSource × 期末汇率)}。正负号按方向映射收益/损失：
 * 应收(资产) functional 升值=收益；应付(负债) functional 升值=损失。每项生成往来+汇兑损益一对分录（自平衡）。
 */
public class ExchangeRevaluationService {

    /** 汇兑重估凭证业财回链 billHeadCode 前缀（与 ErpFinAccountingPeriodBizModel.FX_BILL_CODE_PREFIX 一致）。 */
    public static final String BILL_CODE_PREFIX = "FX-REVAL-";

    @Inject
    IDaoProvider daoProvider;

    public Long revalue(ErpFinAccountingPeriod period, IServiceContext context) {
        Long functionalCurrencyId = resolveFunctionalCurrencyId();
        // 汇率延迟解析：仅当存在外币 AR/AP 或外币银行账户时才要求配置 period-end-exchange-rate
        // （无外币项的干净期间不应因汇率未配而阻断结账，保持与既有行为一致）。
        Long arApVoucherId = revalueArAp(period, functionalCurrencyId, null);
        Long bankVoucherId = null;
        if (isBankFxRevaluationEnabled()) {
            bankVoucherId = revalueBankDeposits(period, functionalCurrencyId, null);
        }
        // 返回任一非空凭证 ID（用于测试断言；两类凭证独立生成，调用方可按 billCode 反查）。
        return arApVoucherId != null ? arApVoucherId : bankVoucherId;
    }

    private BigDecimal resolvePeriodEndRate() {
        Object rateRaw = AppConfig.var(ErpFinConstants.CONFIG_PERIOD_END_EXCHANGE_RATE, null);
        BigDecimal periodEndRate = toBigDecimal(rateRaw);
        if (periodEndRate == null || periodEndRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, ErpFinConstants.CONFIG_PERIOD_END_EXCHANGE_RATE);
        }
        return periodEndRate;
    }

    /** AR/AP 外币未核销项重估（既有逻辑，抽方法）。periodEndRate 为 null 时按需延迟解析。 */
    private Long revalueArAp(ErpFinAccountingPeriod period, Long functionalCurrencyId, BigDecimal periodEndRate) {
        // 外币未核销项（status != SETTLED/CANCELLED，currencyId != 本位币）。
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(notIn("status", Arrays.asList(
                ErpFinConstants.AR_AP_STATUS_SETTLED, ErpFinConstants.AR_AP_STATUS_CANCELLED)));
        if (functionalCurrencyId != null) {
            q.addFilter(ne("currencyId", functionalCurrencyId));
        }
        List<ErpFinArApItem> items = dao.findAllByQuery(q);
        if (items.isEmpty()) {
            return null;
        }
        if (periodEndRate == null) {
            periodEndRate = resolvePeriodEndRate();
        }

        ErpMdSubject arSubject = requireSubject(ErpFinConstants.CONFIG_AR_SUBJECT_CODE);
        ErpMdSubject apSubject = requireSubject(ErpFinConstants.CONFIG_AP_SUBJECT_CODE);
        ErpMdSubject fxSubject = requireSubject(ErpFinConstants.CONFIG_FX_GAIN_LOSS_SUBJECT_CODE);

        List<Line> lines = new ArrayList<>();
        for (ErpFinArApItem item : items) {
            BigDecimal openSource = nz(item.getOpenAmountSource());
            BigDecimal openFunctional = nz(item.getOpenAmountFunctional());
            BigDecimal revaluedFunctional = openSource.multiply(periodEndRate);
            BigDecimal diff = openFunctional.subtract(revaluedFunctional);
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            boolean isReceivable = item.getDirection() != null
                    && Objects.equals(item.getDirection(), ErpFinConstants.DIRECTION_RECEIVABLE);
            ErpMdSubject counterpartSubject = isReceivable ? arSubject : apSubject;
            // diff = openFunctional − revaluedFunctional。应收(资产)：revalued↑(diff<0)=收益；应付(负债)：revalued↓(diff>0)=收益。
            boolean gain = isReceivable
                    ? diff.compareTo(BigDecimal.ZERO) < 0
                    : diff.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal abs = diff.abs();
            String counterpartDc = gain ? ErpFinConstants.DC_DEBIT : ErpFinConstants.DC_CREDIT;
            String fxDc = gain ? ErpFinConstants.DC_CREDIT : ErpFinConstants.DC_DEBIT;
            lines.add(new Line(counterpartSubject.getId(), counterpartSubject.getCode(), counterpartSubject.getName(),
                    counterpartDc, abs, item.getPartnerId()));
            lines.add(new Line(fxSubject.getId(), fxSubject.getCode(), fxSubject.getName(), fxDc, abs, null));
        }
        if (lines.isEmpty()) {
            return null;
        }

        Long acctSchemaId = resolveAcctSchemaId(period.getId());
        return CloseVoucherWriter.writeVoucher(daoProvider, "FXV", BILL_CODE_PREFIX + period.getCode(),
                ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name(), ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name(),
                period.getOrgId(), acctSchemaId, period.getId(), functionalCurrencyId, BigDecimal.ONE,
                period.getEndDate(), lines, "期末汇兑重估-AR/AP");
    }

    /**
     * 银行存款外币余额重估（0540-2 扩展）。重估 {@link ErpFinFundAccount#getCurrencyId()} ≠ 本位币 的银行存款账户：
     * 账面本位币（科目已过账分录聚合 debit−credit）vs {@code currentBalance × 期末汇率}，差额生成 EXCHANGE_GAIN_LOSS 凭证
     * （借/贷银行存款科目 / 贷/借汇兑损益）。本位币账户不重估。
     */
    private Long revalueBankDeposits(ErpFinAccountingPeriod period, Long functionalCurrencyId,
                                     BigDecimal periodEndRate) {
        IEntityDao<ErpFinFundAccount> accDao = daoProvider.daoFor(ErpFinFundAccount.class);
        QueryBean q = new QueryBean();
        if (functionalCurrencyId != null) {
            q.addFilter(ne("currencyId", functionalCurrencyId));
        }
        List<ErpFinFundAccount> accounts = accDao.findAllByQuery(q);
        if (accounts.isEmpty()) {
            return null;
        }
        if (periodEndRate == null) {
            periodEndRate = resolvePeriodEndRate();
        }
        ErpMdSubject fxSubject = requireSubject(ErpFinConstants.CONFIG_FX_GAIN_LOSS_SUBJECT_CODE);

        // 预聚合本期间各银行科目的账面本位币（debit−credit），用于与重估值比对。
        Map<Long, BigDecimal> bookBySubject = aggregateBankSubjectBookFunctional(period.getId());

        List<Line> lines = new ArrayList<>();
        for (ErpFinFundAccount acc : accounts) {
            if (acc.getSubjectId() == null || acc.getCurrencyId() == null) {
                continue;
            }
            BigDecimal sourceBalance = nz(acc.getCurrentBalance());
            if (sourceBalance.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            ErpMdSubject bankSubject = loadSubject(acc.getSubjectId());
            if (bankSubject == null) {
                continue;
            }
            BigDecimal revaluedFunctional = sourceBalance.multiply(periodEndRate);
            BigDecimal bookFunctional = bookBySubject.getOrDefault(acc.getSubjectId(), BigDecimal.ZERO);
            BigDecimal diff = revaluedFunctional.subtract(bookFunctional);
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            // 银行存款为资产：revaluedFunctional > bookFunctional（diff>0）= 升值=收益 → 借银行/贷汇兑损益。
            boolean gain = diff.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal abs = diff.abs();
            String bankDc = gain ? ErpFinConstants.DC_DEBIT : ErpFinConstants.DC_CREDIT;
            String fxDc = gain ? ErpFinConstants.DC_CREDIT : ErpFinConstants.DC_DEBIT;
            lines.add(new Line(bankSubject.getId(), bankSubject.getCode(), bankSubject.getName(),
                    bankDc, abs, null));
            lines.add(new Line(fxSubject.getId(), fxSubject.getCode(), fxSubject.getName(), fxDc, abs, null));
        }
        if (lines.isEmpty()) {
            return null;
        }
        Long acctSchemaId = resolveAcctSchemaId(period.getId());
        return CloseVoucherWriter.writeVoucher(daoProvider, "FXB", BILL_CODE_PREFIX + period.getCode(),
                ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name(), ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name(),
                period.getOrgId(), acctSchemaId, period.getId(), functionalCurrencyId, BigDecimal.ONE,
                period.getEndDate(), lines, "期末汇兑重估-银行存款");
    }

    /** 聚合本期间各银行科目已过账非红冲分录的账面本位币（debit−credit），用于银行存款重估比对。 */
    private Map<Long, BigDecimal> aggregateBankSubjectBookFunctional(Long periodId) {
        Map<Long, BigDecimal> result = new HashMap<>();
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("periodId", periodId));
        vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        vq.addFilter(eq("isReversed", Boolean.FALSE));
        // 预算凭证（postingType=BUDGET）是影子凭证，不得计入实际银行存款重估（budget.md 规则4/6/8）。
        vq.addFilter(or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET)));
        java.util.Set<Long> voucherIds = new java.util.HashSet<>();
        for (ErpFinVoucher v : vDao.findAllByQuery(vq)) {
            voucherIds.add(v.getId());
        }
        if (voucherIds.isEmpty()) {
            return result;
        }
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        for (ErpFinVoucherLine l : lineDao.findAllByQuery(new QueryBean())) {
            if (l.getSubjectId() == null || !voucherIds.contains(l.getVoucherId())) {
                continue;
            }
            String bt = l.getBusinessType();
            if (bt != null && (Objects.equals(bt, ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name())
                    || Objects.equals(bt, ErpFinBusinessType.PERIOD_CLOSE.name())
                    || Objects.equals(bt, ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS.name()))) {
                continue;
            }
            BigDecimal debit = nz(l.getDebitAmount());
            BigDecimal credit = nz(l.getCreditAmount());
            result.merge(l.getSubjectId(), debit.subtract(credit), BigDecimal::add);
        }
        return result;
    }

    private boolean isBankFxRevaluationEnabled() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_BANK_FX_REVALUATION_ENABLED, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    private ErpMdSubject loadSubject(Long id) {
        return daoProvider.daoFor(ErpMdSubject.class).getEntityById(id);
    }

    private ErpMdSubject requireSubject(String configKey) {
        String code = AppConfig.var(configKey, null);
        if (code == null || code.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, configKey);
        }
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        if (list.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, configKey);
        }
        return list.get(0);
    }

    private Long resolveFunctionalCurrencyId() {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isFunctional", Boolean.TRUE));
        q.setLimit(1);
        List<ErpMdCurrency> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    private Long resolveAcctSchemaId(Long periodId) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucher> dao = daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.setLimit(1);
        List<app.erp.fin.dao.entity.ErpFinVoucher> list = dao.findAllByQuery(q);
        if (!list.isEmpty() && list.get(0).getAcctSchemaId() != null) {
            return list.get(0).getAcctSchemaId();
        }
        return 1L;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal toBigDecimal(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigDecimal) {
            return (BigDecimal) raw;
        }
        if (raw instanceof Number) {
            return new BigDecimal(raw.toString());
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
