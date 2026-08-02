package app.erp.mnt.service.processor;

import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.service.ErpMntErrors;
import app.erp.mnt.service.support.EquipmentStatusLinker;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * 停机记录 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 record/complete 两个 per-mutation Processor 共用的加载、状态守卫辅助（单一真相源）。子类只编排单 mutation 步骤顺序。
 */
public abstract class AbstractErpMntDowntimeEntryProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    EquipmentStatusLinker equipmentStatusLinker;

    protected IEntityDao<ErpMntDowntimeEntry> downtimeDao() {
        return daoProvider.daoFor(ErpMntDowntimeEntry.class);
    }

    protected ErpMntDowntimeEntry requireDowntime(Long downtimeId, IServiceContext context) {
        ErpMntDowntimeEntry downtime = downtimeDao().getEntityById(downtimeId);
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
}
