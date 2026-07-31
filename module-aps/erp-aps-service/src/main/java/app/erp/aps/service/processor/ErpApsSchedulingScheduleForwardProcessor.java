package app.erp.aps.service.processor;

import app.erp.aps.biz.SchedulingResult;
import app.erp.aps.dao.entity.ErpApsSchedule;
import app.erp.aps.service.ErpApsConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpApsScheduling scheduleForward per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含前向排产编排（拉取方案 + 调用 run）；共享 protected helper 单一真相源在
 * {@link ErpApsSchedulingProcessor}（delete-after-extract facade，类保留为 helper 持有者）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpApsSchedulingScheduleForwardProcessor {

    @Inject
    ErpApsSchedulingProcessor facade;

    public SchedulingResult scheduleForward(Long scheduleId, IServiceContext context) {
        ErpApsSchedule schedule = facade.requireSchedule(scheduleId, context);
        return facade.run(schedule, ErpApsConstants.SCHEDULING_MODE_FORWARD, context);
    }
}
