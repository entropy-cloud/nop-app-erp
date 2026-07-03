package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntDowntimeEntryBiz;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.service.ErpMntErrors;
import app.erp.mnt.service.support.EquipmentStatusLinker;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.time.Duration;
import java.time.LocalDateTime;

import java.math.BigDecimal;

@BizModel("ErpMntDowntimeEntry")
public class ErpMntDowntimeEntryBizModel extends CrudBizModel<ErpMntDowntimeEntry> implements IErpMntDowntimeEntryBiz {

    @jakarta.inject.Inject
    EquipmentStatusLinker equipmentStatusLinker;

    public ErpMntDowntimeEntryBizModel() {
        setEntityName(ErpMntDowntimeEntry.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntDowntimeEntry record(@Name("downtimeId") Long downtimeId, IServiceContext context) {
        ErpMntDowntimeEntry downtime = requireDowntime(downtimeId, context);
        validateNotCompleted(downtime, context);
        doRecord(downtime, context);
        equipmentStatusLinker.linkToDown(downtime.getEquipmentId(), context);
        return downtime;
    }

    @Override
    @BizMutation
    public ErpMntDowntimeEntry complete(@Name("downtimeId") Long downtimeId, IServiceContext context) {
        ErpMntDowntimeEntry downtime = requireDowntime(downtimeId, context);
        validateRecorded(downtime, context);
        validateNotCompleted(downtime, context);
        doComplete(downtime, context);
        equipmentStatusLinker.restoreToRunning(downtime.getEquipmentId(), context);
        return downtime;
    }

    // ---------- step：校验 ----------

    protected ErpMntDowntimeEntry requireDowntime(Long downtimeId, IServiceContext context) {
        ErpMntDowntimeEntry downtime = get(String.valueOf(downtimeId), false, context);
        if (downtime == null) {
            throw new NopException(ErpMntErrors.ERR_DOWNTIME_NOT_FOUND).param(ErpMntErrors.ARG_DOWNTIME_ID, downtimeId);
        }
        return downtime;
    }

    protected void validateNotCompleted(ErpMntDowntimeEntry downtime, IServiceContext context) {
        if (downtime.getEndTime() != null) {
            throw new NopException(ErpMntErrors.ERR_DOWNTIME_ALREADY_COMPLETED)
                    .param(ErpMntErrors.ARG_DOWNTIME_ID, downtime.getId());
        }
    }

    protected void validateRecorded(ErpMntDowntimeEntry downtime, IServiceContext context) {
        if (downtime.getStartTime() == null) {
            throw new NopException(ErpMntErrors.ERR_DOWNTIME_NOT_STARTED)
                    .param(ErpMntErrors.ARG_DOWNTIME_ID, downtime.getId());
        }
    }

    // ---------- step：执行 ----------

    protected void doRecord(ErpMntDowntimeEntry downtime, IServiceContext context) {
        if (downtime.getStartTime() == null) {
            downtime.setStartTime(CoreMetrics.currentDateTime());
        }
        updateEntity(downtime, null, context);
    }

    protected void doComplete(ErpMntDowntimeEntry downtime, IServiceContext context) {
        LocalDateTime endTime = downtime.getEndTime() == null ? CoreMetrics.currentDateTime() : downtime.getEndTime();
        downtime.setEndTime(endTime);
        if (downtime.getStartTime() != null) {
            long minutes = Duration.between(downtime.getStartTime(), endTime).toMinutes();
            downtime.setTotalMinutes(BigDecimal.valueOf(minutes));
        }
        updateEntity(downtime, null, context);
    }
}
