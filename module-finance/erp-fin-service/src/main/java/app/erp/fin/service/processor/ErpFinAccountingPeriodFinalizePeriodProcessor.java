package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinAccountingPeriod finalizePeriod per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含最终锁定编排；共享 protected helper 单一真相源在 {@link ErpFinAccountingPeriodProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinAccountingPeriodFinalizePeriodProcessor {

    @Inject
    ErpFinAccountingPeriodProcessor facade;

    public ErpFinAccountingPeriod finalizePeriod(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = facade.requirePeriod(periodId);
        facade.assertPeriodStatus(period, ErpFinConstants.PERIOD_STATUS_CLOSED, "最终锁定");
        period.setStatus(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL);
        facade.orm().flushSession();
        return period;
    }
}
