package app.erp.fin.service.processor;

import app.erp.fin.dao.PeriodPreCheckReport;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinAccountingPeriod preCheck per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含期末前置检查编排；共享 protected helper 单一真相源在 {@link ErpFinAccountingPeriodProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinAccountingPeriodPreCheckProcessor {

    @Inject
    ErpFinAccountingPeriodProcessor facade;

    public PeriodPreCheckReport preCheck(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = facade.requirePeriod(periodId);
        PeriodPreCheckReport report = new PeriodPreCheckReport();
        report.setUnpostedVoucherCodes(facade.findUnpostedVoucherCodes(period));
        report.setUnsettledArApCodes(facade.findUnsettledArApCodes(period));
        report.setUnresolvedPostingExceptionKeys(facade.findUnresolvedPostingExceptionKeys(period));
        facade.populateAllowanceCheck(period, report);
        return report;
    }
}
