package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinAccountingPeriod openPeriod per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含期间开启编排（NEVER_OPENED→OPEN，P1-MA2-033）；共享 protected helper 单一真相源在
 * {@link ErpFinAccountingPeriodProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinAccountingPeriodOpenPeriodProcessor {

    @Inject
    ErpFinAccountingPeriodProcessor facade;

    public ErpFinAccountingPeriod openPeriod(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = facade.requirePeriod(periodId);
        facade.assertPeriodStatus(period, ErpFinConstants.PERIOD_STATUS_NEVER_OPENED, "开启");
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        facade.orm().flushSession();
        return period;
    }
}
