package app.erp.mnt.service.support;

import app.erp.mnt.biz.IErpMntEquipmentBiz;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.service.ErpMntConfigs;
import app.erp.mnt.service.ErpMntErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 设备状态联动器。访问 start→设备 UNDER_MAINTENANCE、complete/cancel→恢复；
 * 停机 record→设备 DOWN、complete→恢复。经 {@code erp-mnt.equipment-status-link-enabled} 门控。
 *
 * <p>恢复目标：UNDER_MAINTENANCE/DOWN 为维护期临时态，恢复时回到运行态 RUNNING（设备无独立持久化的前置状态快照列，
 * 故以 RUNNING 作为标准运行态恢复；IDLE 设备恢复为 RUNNING 为已知的简化偏差，乐观锁保护并发覆盖）。
 */
public class EquipmentStatusLinker {

    @Inject
    IErpMntEquipmentBiz equipmentBiz;

    public void linkToUnderMaintenance(Long equipmentId, IServiceContext context) {
        if (!ErpMntConfigs.equipmentStatusLinkEnabled() || equipmentId == null) {
            return;
        }
        changeEquipmentStatus(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE, context);
    }

    public void linkToDown(Long equipmentId, IServiceContext context) {
        if (!ErpMntConfigs.equipmentStatusLinkEnabled() || equipmentId == null) {
            return;
        }
        changeEquipmentStatus(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN, context);
    }

    public void restoreToRunning(Long equipmentId, IServiceContext context) {
        if (!ErpMntConfigs.equipmentStatusLinkEnabled() || equipmentId == null) {
            return;
        }
        changeEquipmentStatus(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, context);
    }

    protected void changeEquipmentStatus(Long equipmentId, Integer newStatus, IServiceContext context) {
        ErpMntEquipment equipment = equipmentBiz.get(String.valueOf(equipmentId), false, context);
        if (equipment == null) {
            throw new NopException(ErpMntErrors.ERR_EQUIPMENT_NOT_FOUND)
                    .param(ErpMntErrors.ARG_EQUIPMENT_ID, equipmentId);
        }
        equipment.setStatus(newStatus);
        equipmentBiz.updateEntity(equipment, null, context);
    }
}
