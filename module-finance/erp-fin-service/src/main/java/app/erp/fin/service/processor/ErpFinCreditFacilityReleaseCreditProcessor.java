package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinCreditFacility;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * ErpFinCreditFacility releaseCredit per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含额度释放回写编排（decrement usedAmount，下限 0；availableAmount=total−used 同步重算）。
 * 并发竞争由 version 乐观锁兜底（ORM 层版本校验）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinCreditFacilityReleaseCreditProcessor {

    @Inject
    IDaoProvider daoProvider;

    public ErpFinCreditFacility releaseCredit(Long creditFacilityId, BigDecimal amount, IServiceContext context) {
        ErpFinCreditFacility facility = requireFacility(creditFacilityId);
        BigDecimal used = nz(facility.getUsedAmount()).subtract(nz(amount));
        if (used.compareTo(BigDecimal.ZERO) < 0) {
            used = BigDecimal.ZERO;
        }
        facility.setUsedAmount(used);
        facility.setAvailableAmount(nz(facility.getTotalAmount()).subtract(used));
        daoProvider.daoFor(ErpFinCreditFacility.class).updateEntity(facility);
        return facility;
    }

    protected ErpFinCreditFacility requireFacility(Long creditFacilityId) {
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
