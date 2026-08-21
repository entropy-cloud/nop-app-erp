package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinCreditFacility;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.treasury.CreditFacilityInterestVoucherBuilder;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * ErpFinCreditFacility accrueInterest per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含授信利息计提编排：按区间计提利息并委派 {@link CreditFacilityInterestVoucherBuilder} 生成
 * {@code CREDIT_FACILITY_INTEREST} 凭证。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinCreditFacilityAccrueInterestProcessor {

    private static final BigDecimal BD_360 = new BigDecimal("360");

    @Inject
    IDaoProvider daoProvider;
    @Inject
    CreditFacilityInterestVoucherBuilder interestVoucherBuilder;

    public String accrueInterest(String creditFacilityId, LocalDate fromDate, LocalDate toDate, IServiceContext context) {
        ErpFinCreditFacility facility = requireFacility(creditFacilityId);
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new NopException(ErpFinErrors.ERR_CREDIT_FACILITY_INTEREST_INVALID_DATE_RANGE)
                    .param(ErpFinErrors.ARG_CREDIT_FACILITY_ID, creditFacilityId)
                    .param(ErpFinErrors.ARG_FROM_DATE, fromDate)
                    .param(ErpFinErrors.ARG_TO_DATE, toDate);
        }
        BigDecimal usedAmount = nz(facility.getUsedAmount());
        if (usedAmount.signum() <= 0) {
            return null;
        }
        BigDecimal rate = AppConfig.var(ErpFinConstants.CONFIG_CREDIT_FACILITY_DEFAULT_INTEREST_RATE, BigDecimal.ZERO);
        if (rate == null || rate.signum() <= 0) {
            throw new NopException(ErpFinErrors.ERR_CREDIT_FACILITY_INTEREST_RATE_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CREDIT_FACILITY_ID, creditFacilityId)
                    .param(ErpFinErrors.ARG_CONFIG_KEY,
                            ErpFinConstants.CONFIG_CREDIT_FACILITY_DEFAULT_INTEREST_RATE);
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        BigDecimal interest = usedAmount
                .multiply(rate)
                .multiply(BigDecimal.valueOf(days))
                .divide(BD_360, 4, RoundingMode.HALF_UP);
        return interestVoucherBuilder.post(facility, fromDate, toDate, interest, context);
    }

    protected ErpFinCreditFacility requireFacility(String creditFacilityId) {
        IEntityDao<ErpFinCreditFacility> dao = daoProvider.daoFor(ErpFinCreditFacility.class);
        ErpFinCreditFacility facility = dao.getEntityById(creditFacilityId);
        if (facility == null) {
            throw new NopException(ErpFinErrors.ERR_CREDIT_FACILITY_NOT_FOUND)
                    .param(ErpFinErrors.ARG_CREDIT_FACILITY_ID, creditFacilityId);
        }
        return facility;
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
