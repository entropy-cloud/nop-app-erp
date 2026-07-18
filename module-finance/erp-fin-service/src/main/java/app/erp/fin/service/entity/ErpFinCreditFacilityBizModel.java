
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinCreditFacilityBiz;
import app.erp.fin.dao.entity.ErpFinCreditFacility;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.treasury.CreditFacilityInterestVoucherBuilder;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 银行授信额度 BizModel（{@code treasury.md §关键业务规则 1}）。承载额度占用回写：
 * {@link #reserveCredit} 强一致校验 availableAmount>=amount 并 increment usedAmount；
 * {@link #releaseCredit} decrement usedAmount。availableAmount=total−used 每次同步重算。
 *
 * <p>并发竞争由 {@code version} 乐观锁兜底（{@code ErpFinCreditFacility} 含标准 version 审计列，
 * {@code updateEntity} 走 CrudBizModel 管道自动版本校验，失败抛 StaleObjectException）。
 *
 * <p>{@link #accrueInterest}（plan 2026-07-18-0718-1）：按区间计提授信利息并委派
 * {@link CreditFacilityInterestVoucherBuilder} 生成 {@code CREDIT_FACILITY_INTEREST} 凭证。
 */
@BizModel("ErpFinCreditFacility")
public class ErpFinCreditFacilityBizModel extends CrudBizModel<ErpFinCreditFacility> implements IErpFinCreditFacilityBiz {

    @Inject
    CreditFacilityInterestVoucherBuilder interestVoucherBuilder;

    public ErpFinCreditFacilityBizModel() {
        setEntityName(ErpFinCreditFacility.class.getName());
    }

    @Override
    @BizMutation
    public ErpFinCreditFacility reserveCredit(@Name("creditFacilityId") Long creditFacilityId,
                                              @Name("amount") BigDecimal amount,
                                              IServiceContext context) {
        ErpFinCreditFacility facility = requireFacility(creditFacilityId, context);
        BigDecimal amt = nz(amount);
        BigDecimal available = nz(facility.getAvailableAmount());
        if (available.compareTo(amt) < 0) {
            throw new NopException(ErpFinErrors.ERR_CREDIT_FACILITY_INSUFFICIENT)
                    .param(ErpFinErrors.ARG_CREDIT_FACILITY_ID, creditFacilityId)
                    .param(ErpFinErrors.ARG_AVAILABLE_AMOUNT, available)
                    .param(ErpFinErrors.ARG_FACE_AMOUNT, amt);
        }
        BigDecimal used = nz(facility.getUsedAmount()).add(amt);
        facility.setUsedAmount(used);
        facility.setAvailableAmount(nz(facility.getTotalAmount()).subtract(used));
        updateEntity(facility, null, context);
        return facility;
    }

    @Override
    @BizMutation
    public ErpFinCreditFacility releaseCredit(@Name("creditFacilityId") Long creditFacilityId,
                                              @Name("amount") BigDecimal amount,
                                              IServiceContext context) {
        ErpFinCreditFacility facility = requireFacility(creditFacilityId, context);
        BigDecimal used = nz(facility.getUsedAmount()).subtract(nz(amount));
        if (used.compareTo(BigDecimal.ZERO) < 0) {
            used = BigDecimal.ZERO;
        }
        facility.setUsedAmount(used);
        facility.setAvailableAmount(nz(facility.getTotalAmount()).subtract(used));
        updateEntity(facility, null, context);
        return facility;
    }

    @Override
    @BizMutation
    public Long accrueInterest(@Name("creditFacilityId") Long creditFacilityId,
                               @Name("fromDate") LocalDate fromDate,
                               @Name("toDate") LocalDate toDate,
                               IServiceContext context) {
        ErpFinCreditFacility facility = requireFacility(creditFacilityId, context);
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

    // ---------- helpers ----------

    private ErpFinCreditFacility requireFacility(Long creditFacilityId, IServiceContext context) {
        ErpFinCreditFacility facility = get(String.valueOf(creditFacilityId), true, context);
        if (facility == null) {
            throw new NopException(ErpFinErrors.ERR_CREDIT_FACILITY_NOT_FOUND)
                    .param(ErpFinErrors.ARG_CREDIT_FACILITY_ID, creditFacilityId);
        }
        return facility;
    }

    private static final BigDecimal BD_360 = new BigDecimal("360");

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
