package app.erp.mnt.service.processor;

import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.time.CoreMetrics;

import java.sql.Timestamp;

/**
 * ErpMntDowntimeEntry record per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含停机记录编排：未结束守卫 + startTime 兜底 + 落库 + 设备状态联动（DOWN）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntDowntimeEntryProcessor}。
 */
public class ErpMntDowntimeEntryRecordProcessor extends AbstractErpMntDowntimeEntryProcessor {

    public ErpMntDowntimeEntry record(Long downtimeId, IServiceContext context) {
        ErpMntDowntimeEntry downtime = requireDowntime(downtimeId, context);
        validateNotCompleted(downtime, context);
        doRecord(downtime, context);
        equipmentStatusLinker.linkToDown(downtime.getEquipmentId(), context);
        return downtime;
    }

    protected void doRecord(ErpMntDowntimeEntry downtime, IServiceContext context) {
        if (downtime.getStartTime() == null) {
            downtime.setStartTime(CoreMetrics.currentTimestamp());
        }
        downtimeDao().updateEntity(downtime);
    }
}
