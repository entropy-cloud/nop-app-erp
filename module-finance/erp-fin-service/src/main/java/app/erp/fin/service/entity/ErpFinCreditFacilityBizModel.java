
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinCreditFacilityBiz;
import app.erp.fin.dao.entity.ErpFinCreditFacility;
import app.erp.fin.service.processor.ErpFinCreditFacilityAccrueInterestProcessor;
import app.erp.fin.service.processor.ErpFinCreditFacilityReleaseCreditProcessor;
import app.erp.fin.service.processor.ErpFinCreditFacilityReserveCreditProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 银行授信额度 BizModel（{@code treasury.md §关键业务规则 1}）。额度占用回写（reserveCredit/releaseCredit）
 * 与利息计提（accrueInterest）分别委派对应 per-mutation Processor。
 *
 * <p>并发竞争由 {@code version} 乐观锁兜底（{@code ErpFinCreditFacility} 含标准 version 审计列，
 * ORM 层版本校验，失败抛 StaleObjectException）。
 */
@BizModel("ErpFinCreditFacility")
public class ErpFinCreditFacilityBizModel extends CrudBizModel<ErpFinCreditFacility> implements IErpFinCreditFacilityBiz {

    @Inject
    ErpFinCreditFacilityReserveCreditProcessor reserveCreditProcessor;
    @Inject
    ErpFinCreditFacilityReleaseCreditProcessor releaseCreditProcessor;
    @Inject
    ErpFinCreditFacilityAccrueInterestProcessor accrueInterestProcessor;

    public ErpFinCreditFacilityBizModel() {
        setEntityName(ErpFinCreditFacility.class.getName());
    }

    @Override
    @BizMutation
    public ErpFinCreditFacility reserveCredit(@Name("creditFacilityId") Long creditFacilityId,
                                              @Name("amount") BigDecimal amount,
                                              IServiceContext context) {
        return reserveCreditProcessor.reserveCredit(creditFacilityId, amount, context);
    }

    @Override
    @BizMutation
    public ErpFinCreditFacility releaseCredit(@Name("creditFacilityId") Long creditFacilityId,
                                              @Name("amount") BigDecimal amount,
                                              IServiceContext context) {
        return releaseCreditProcessor.releaseCredit(creditFacilityId, amount, context);
    }

    @Override
    @BizMutation
    public Long accrueInterest(@Name("creditFacilityId") Long creditFacilityId,
                               @Name("fromDate") LocalDate fromDate,
                               @Name("toDate") LocalDate toDate,
                               IServiceContext context) {
        return accrueInterestProcessor.accrueInterest(creditFacilityId, fromDate, toDate, context);
    }
}
