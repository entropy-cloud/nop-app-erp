package app.erp.fin.service.fx;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;
import static io.nop.api.core.beans.FilterBeans.notIn;

/**
 * 期末汇兑重估服务（{@code period-close.md §汇兑重估}，承接 0300-3 deferred）。查询外币应收应付未核销项，
 * 按期末汇率重估差额，生成 EXCHANGE_GAIN_LOSS(130) 凭证。
 *
 * <p>范围（Decision）：仅重估外币 {@link ErpFinArApItem} 未核销项（AR/AP 往来），不重估货币性科目余额
 * （需科目级币种标记，超范围）。本位币项不重估。
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

        Object rateRaw = AppConfig.var(ErpFinConstants.CONFIG_PERIOD_END_EXCHANGE_RATE, null);
        BigDecimal periodEndRate = toBigDecimal(rateRaw);
        if (periodEndRate == null || periodEndRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, ErpFinConstants.CONFIG_PERIOD_END_EXCHANGE_RATE);
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
                    && item.getDirection() == ErpFinConstants.DIRECTION_RECEIVABLE;
            ErpMdSubject counterpartSubject = isReceivable ? arSubject : apSubject;
            // diff = openFunctional − revaluedFunctional。应收(资产)：revalued↑(diff<0)=收益；应付(负债)：revalued↓(diff>0)=收益。
            boolean gain = isReceivable
                    ? diff.compareTo(BigDecimal.ZERO) < 0
                    : diff.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal abs = diff.abs();
            int counterpartDc = gain ? ErpFinConstants.DC_DEBIT : ErpFinConstants.DC_CREDIT;
            int fxDc = gain ? ErpFinConstants.DC_CREDIT : ErpFinConstants.DC_DEBIT;
            lines.add(new Line(counterpartSubject.getId(), counterpartSubject.getCode(), counterpartSubject.getName(),
                    counterpartDc, abs, item.getPartnerId()));
            lines.add(new Line(fxSubject.getId(), fxSubject.getCode(), fxSubject.getName(), fxDc, abs, null));
        }
        if (lines.isEmpty()) {
            return null;
        }

        Long acctSchemaId = resolveAcctSchemaId(period.getId());
        return CloseVoucherWriter.writeVoucher(daoProvider, "FXV", BILL_CODE_PREFIX + period.getCode(),
                ErpFinBusinessType.EXCHANGE_GAIN_LOSS.getCode(), ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name(),
                period.getOrgId(), acctSchemaId, period.getId(), functionalCurrencyId, BigDecimal.ONE,
                period.getEndDate(), lines, "期末汇兑重估");
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
