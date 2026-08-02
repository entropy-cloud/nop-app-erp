package app.erp.mnt.service.processor;

import app.erp.mnt.service.support.ScheduleDueGenerator;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import java.time.LocalDate;

/**
 * ErpMntSchedule generateDueVisits per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含到期访问生成编排：委派 {@link ScheduleDueGenerator}（扫描 active 计划 nextDueDate ≤ asOfDate 生成 DRAFT 访问 +
 * 推进 nextDueDate，经 erp-mnt.auto-generate-due-visits 门控）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpMntScheduleGenerateDueVisitsProcessor {

    @Inject
    ScheduleDueGenerator scheduleDueGenerator;

    public Integer generateDueVisits(LocalDate asOfDate, IServiceContext context) {
        return scheduleDueGenerator.generateDueVisits(asOfDate, context);
    }
}
