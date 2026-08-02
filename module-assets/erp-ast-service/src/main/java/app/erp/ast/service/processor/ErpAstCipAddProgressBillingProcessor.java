package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstCipProgressBilling;
import app.erp.ast.service.ErpAstErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * ErpAstCip addProgressBilling per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含进度付款记录编排；共享 protected helper 单一真相源在 {@link ErpAstCipProcessor}（slim-to-query-only facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstCipAddProgressBillingProcessor {

    @Inject
    ErpAstCipProcessor facade;

    public ErpAstCipProgressBilling addProgressBilling(Long cipId, LocalDate billingDate, String billingMilestone,
                                                        BigDecimal amountFunctional, String paymentVoucherCode,
                                                        IServiceContext context) {
        ErpAstCip cip = facade.requireCip(cipId, context);
        facade.requireInConstruction(cip);
        facade.validateAmountPositive(amountFunctional, cip);
        if (billingDate == null) {
            throw new NopException(ErpAstErrors.ERR_CIP_AMOUNT_INVALID)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode())
                    .param(ErpAstErrors.ARG_AMOUNT, null);
        }

        BigDecimal exchangeRate = ErpAstCipProcessor.nz(cip.getExchangeRate(), BigDecimal.ONE);
        BigDecimal amountSource = amountFunctional.divide(exchangeRate, ErpAstCipProcessor.SCALE, RoundingMode.HALF_UP);

        IEntityDao<ErpAstCipProgressBilling> dao = facade.progressBillingDao();
        ErpAstCipProgressBilling billing = dao.newEntity();
        billing.setCipId(cip.getId());
        billing.setOrgId(cip.getOrgId());
        billing.setLineNo(facade.nextProgressBillingLineNo(cip.getId()));
        billing.setBillingDate(billingDate);
        billing.setBillingMilestone(billingMilestone);
        billing.setAmountFunctional(amountFunctional);
        billing.setExchangeRate(exchangeRate);
        billing.setAmountSource(amountSource);
        billing.setCurrencyId(cip.getCurrencyId());
        billing.setPaymentVoucherCode(paymentVoucherCode);
        billing.setPaidFlag(true);
        dao.saveEntity(billing);
        return billing;
    }
}
