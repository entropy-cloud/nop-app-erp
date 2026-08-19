package app.erp.mnt.service.entity;

import app.erp.mnt.biz.IErpMntEquipmentBiz;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.service.support.EquipmentStatusLinker;
import app.erp.mnt.service.support.EquipmentStatusLogWriter;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

@BizModel("ErpMntEquipment")
public class ErpMntEquipmentBizModel extends CrudBizModel<ErpMntEquipment> implements IErpMntEquipmentBiz {

    @Inject
    EquipmentStatusLogWriter statusLogWriter;

    @Inject
    EquipmentStatusLinker equipmentStatusLinker;

    public ErpMntEquipmentBizModel() {
        setEntityName(ErpMntEquipment.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntEquipment changeStatus(@Name("equipmentId") Long equipmentId,
                                         @Name("newStatus") String newStatus,
                                         IServiceContext context) {
        ErpMntEquipment equipment = requireEntity(String.valueOf(equipmentId), null, context);
        String fromStatus = equipment.getStatus();
        equipment.setStatus(newStatus);
        updateEntity(equipment, null, context);
        // RC-R1.73 / UC-MAIN-02：手动状态变更同事务追加状态日志行（来源 MANUAL），
        // 供运行时长 Σ RUNNING 段聚合消费。
        statusLogWriter.append(equipmentId, fromStatus, newStatus,
                ErpMntDaoConstants.STATUS_LOG_SOURCE_MANUAL, null);
        return equipment;
    }

    // RC-R1.77 / UC-MAIN-08：资产处置联动 Facade（assets 处置 Processor 后置调用，
    // 同 JVM 同事务异常传播回滚处置；写入经 EquipmentStatusLinker 同链记录 DISPOSAL 日志行）。

    @Override
    @BizMutation
    public int changeStatusForAssetDisposal(@Name("assetId") Long assetId,
                                             @Name("disposalCode") String disposalCode,
                                             IServiceContext context) {
        return equipmentStatusLinker.linkToDecommissionedByDisposal(assetId, disposalCode, context);
    }

    @Override
    @BizMutation
    public int restoreFromAssetDisposal(@Name("assetId") Long assetId,
                                         @Name("disposalCode") String disposalCode,
                                         IServiceContext context) {
        return equipmentStatusLinker.restoreFromDisposal(assetId, disposalCode, context);
    }
}
