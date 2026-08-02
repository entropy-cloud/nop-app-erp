package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmQuota;
import app.erp.crm.service.support.QuotaRollupCalculator;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * ErpCrmQuota distributeAnnualQuota per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含年度配额按季/月均分编排，委托 {@link QuotaRollupCalculator}。{@code finalizeQuota}/{@code unfinalizeQuota} 不在本期范围，仍内联于 BizModel。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmQuotaDistributeAnnualQuotaProcessor {

    @Inject
    QuotaRollupCalculator quotaRollupCalculator;

    public List<ErpCrmQuota> distributeAnnualQuota(Long quotaId, String periodType, IServiceContext context) {
        return quotaRollupCalculator.distributeAnnual(quotaId, periodType);
    }
}
