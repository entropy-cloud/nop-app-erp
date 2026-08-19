package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.service.ErpMntConfigs;
import app.erp.mnt.service.ErpMntErrors;
import app.erp.mnt.service.support.EquipmentStatusLinker;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 停机记录 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 record/complete 两个 per-mutation Processor 共用的加载、状态守卫、计划员通知辅助（单一真相源）。
 * 子类只编排单 mutation 步骤顺序。
 *
 * <p>RC-R1.76 / UC-MAIN-06 计划员通知（L2「通知计划员」辅助语义）：record → 7208
 * {@code mnt.equipment-downtime}（设备/工作中心/原因/起始时间）/ complete → 7209
 * {@code mnt.equipment-recovered}，经 {@code erp-mnt.downtime-notify-enabled} 门控（默认 true），
 * try/catch 静默降级不阻断停机主流程（镜像 cs resolve 通知范式）。
 */
public abstract class AbstractErpMntDowntimeEntryProcessor {

    static final Logger LOG = LoggerFactory.getLogger(AbstractErpMntDowntimeEntryProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    EquipmentStatusLinker equipmentStatusLinker;

    @Inject
    IErpSysNotificationBiz notificationBiz;

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

    /**
     * 停机事件通知（config-gated + try/catch 静默降级）。context 字段对齐模板种子 7208/7209：
     * equipmentCode/workcenterId/reason/startTime（7208）/ endTime（7209）。无 ACTIVE 模板时
     * notify 契约 config-gated 静默跳过（不阻断）。
     */
    protected void notifyDowntimeEvent(ErpMntDowntimeEntry downtime, String eventType,
                                       IServiceContext context) {
        if (!ErpMntConfigs.downtimeNotifyEnabled()) {
            return;
        }
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("downtimeId", downtime.getId());
            ErpMntEquipment equipment = downtime.getEquipment();
            if (equipment != null) {
                ctx.put("equipmentId", equipment.getId());
                ctx.put("equipmentCode", equipment.getCode());
                ctx.put("workcenterId", equipment.getWorkcenterId());
            }
            ctx.put("reason", downtime.getReason());
            ctx.put("startTime", downtime.getStartTime() != null
                    ? downtime.getStartTime().toString() : null);
            ctx.put("endTime", downtime.getEndTime() != null
                    ? downtime.getEndTime().toString() : null);
            notificationBiz.notify(eventType, ctx, context);
        } catch (Exception e) {
            LOG.warn("downtime notify failed (degraded, main flow continues): eventType={}, downtimeId={}, reason={}",
                    eventType, downtime.getId(), e.getMessage());
        }
    }
}
