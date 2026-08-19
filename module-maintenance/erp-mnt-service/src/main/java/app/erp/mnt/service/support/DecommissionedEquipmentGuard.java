package app.erp.mnt.service.support;

import app.erp.mnt.biz.IErpMntEquipmentBiz;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.service.ErpMntErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * DECOMMISSIONED 设备引用守卫（RC-R1.77 / UC-MAIN-08：「设备不可再被新维护计划/工单引用」）。
 *
 * <p>消费点：Schedule/Request/Visit 三 BizModel 的 save/update 钩子（新增行或 equipmentId 变更时）、
 * Visit 排程迁移（{@code ErpMntVisitScheduleProcessor}）、报修受理（{@code ErpMntRequestAcceptProcessor}，
 * 对已处置设备的新维护工作开访问正是 L1 禁止语义）、到期访问批量生成（{@code ScheduleDueGenerator}
 * 查询侧排除 + warn 跳过）。
 */
public class DecommissionedEquipmentGuard {

    @Inject
    IErpMntEquipmentBiz equipmentBiz;

    /**
     * 新引用守卫：设备 status=DECOMMISSIONED 时抛 {@link ErpMntErrors#ERR_EQUIPMENT_DECOMMISSIONED}。
     * equipmentId 为 null 或设备不存在时放行（存在性归平台 FK 校验）。
     */
    public void rejectIfDecommissioned(Long equipmentId, IServiceContext context) {
        if (equipmentId == null) {
            return;
        }
        ErpMntEquipment equipment = equipmentBiz.get(String.valueOf(equipmentId), false, context);
        if (equipment == null) {
            return;
        }
        if (ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED.equals(equipment.getStatus())) {
            throw new NopException(ErpMntErrors.ERR_EQUIPMENT_DECOMMISSIONED)
                    .param(ErpMntErrors.ARG_EQUIPMENT_CODE, equipment.getCode())
                    .param(ErpMntErrors.ARG_EQUIPMENT_ID, equipmentId);
        }
    }

    /** 批量路径豁免判定：到期访问日批等无 per-schedule try/catch 的路径查询侧排除消费。 */
    public boolean isDecommissioned(Long equipmentId, IServiceContext context) {
        if (equipmentId == null) {
            return false;
        }
        ErpMntEquipment equipment = equipmentBiz.get(String.valueOf(equipmentId), false, context);
        return equipment != null
                && ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED.equals(equipment.getStatus());
    }
}
