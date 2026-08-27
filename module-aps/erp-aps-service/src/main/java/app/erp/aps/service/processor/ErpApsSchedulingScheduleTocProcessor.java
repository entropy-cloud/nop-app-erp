package app.erp.aps.service.processor;

import app.erp.aps.biz.SchedulingResult;
import app.erp.aps.dao.entity.ErpApsSchedule;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpApsScheduling scheduleToc per-mutation Processor（E3.4 TOC 瓶颈驱动排产试点，
 * `constraint-based-planning.md` §2）。自包含编排（拉取方案 + 瓶颈识别 + TOC 求解）；
 * 共享 protected helper 单一真相源在 {@link ErpApsSchedulingProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpApsSchedulingScheduleTocProcessor {

    @Inject
    ErpApsSchedulingProcessor facade;

    public SchedulingResult scheduleToc(String scheduleId, IServiceContext context) {
        ErpApsSchedule schedule = facade.requireSchedule(scheduleId, context);
        return facade.runToc(schedule, context);
    }
}
