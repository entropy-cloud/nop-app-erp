package app.erp.mnt.service.processor;

import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.time.CoreMetrics;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;

/**
 * ErpMntDowntimeEntry complete per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含停机结束编排：已开始守卫 + 未结束守卫 + endTime/totalMinutes 计算 + 落库 + 设备状态恢复（RUNNING）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntDowntimeEntryProcessor}。
 */
public class ErpMntDowntimeEntryCompleteProcessor extends AbstractErpMntDowntimeEntryProcessor {

    public ErpMntDowntimeEntry complete(Long downtimeId, IServiceContext context) {
        ErpMntDowntimeEntry downtime = requireDowntime(downtimeId, context);
        validateRecorded(downtime, context);
        validateNotCompleted(downtime, context);
        doComplete(downtime, context);
        equipmentStatusLinker.restoreToRunning(downtime.getEquipmentId(), context);
        return downtime;
    }

    protected void doComplete(ErpMntDowntimeEntry downtime, IServiceContext context) {
        Timestamp endTime = downtime.getEndTime() == null ? CoreMetrics.currentTimestamp() : downtime.getEndTime();
        downtime.setEndTime(endTime);
        if (downtime.getStartTime() != null) {
            long minutes = Duration.between(downtime.getStartTime().toLocalDateTime(), endTime.toLocalDateTime()).toMinutes();
            downtime.setTotalMinutes(BigDecimal.valueOf(minutes));
        }
        downtimeDao().updateEntity(downtime);
    }
}
