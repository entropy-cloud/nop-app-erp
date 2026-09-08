package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.statemachine.ErpFinAccountingPeriodStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinAccountingPeriod openPeriod per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含期间开启编排（NEVER_OPENED→OPEN，P1-MA2-033）；共享 protected helper 单一真相源在
 * {@link ErpFinAccountingPeriodProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>状态矩阵守卫委托 {@link ErpFinAccountingPeriodStateMachine}（plan 2026-08-13-2045-1，契约 §7）：
 * Bean 直抛领域码 {@code ERR_PERIOD_ILLEGAL_TRANSITION}，本处同码补参 periodCode
 * （错误码 + 参数对外不变；plan 2026-09-07-2200-1）。
 */
public class ErpFinAccountingPeriodOpenPeriodProcessor {

    @Inject
    ErpFinAccountingPeriodProcessor facade;
    @Inject
    ErpFinAccountingPeriodStateMachine stateMachine;

    public ErpFinAccountingPeriod openPeriod(String periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = facade.requirePeriod(periodId);
        try {
            stateMachine.assertCanOpenPeriod(period.getStatus());
        } catch (NopException e) {
            // Bean 直抛领域码 ERR_PERIOD_ILLEGAL_TRANSITION（plan 2026-09-07-2200-1），本处同码补参 periodCode。
            throw e.param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode());
        }
        period.setStatus(stateMachine.openPeriodTargetStatus());
        facade.orm().flushSession();
        return period;
    }
}
