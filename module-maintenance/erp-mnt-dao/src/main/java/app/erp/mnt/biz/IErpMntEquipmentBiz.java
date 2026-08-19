package app.erp.mnt.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mnt.dao.entity.ErpMntEquipment;

/**
 * 设备业务接口。除标准 CRUD 外，提供设备状态联动入口（访问/停机记录触发）。
 */
public interface IErpMntEquipmentBiz extends ICrudBiz<ErpMntEquipment> {

    @BizMutation
    ErpMntEquipment changeStatus(@Name("equipmentId") Long equipmentId,
                                 @Name("newStatus") String newStatus,
                                 IServiceContext context);

    /**
     * 资产处置联动（RC-R1.77 / UC-MAIN-08）：按 assetId 反查关联设备置 DECOMMISSIONED。
     * 返回联动行数（0 = 无关联设备 no-op / 已目标态幂等跳过 / config 关闭跳过）。
     * 失败语义 = 异常传播回滚调用方（处置 approve）事务——设备停用是 L1 硬断言。
     */
    @BizMutation
    int changeStatusForAssetDisposal(@Name("assetId") Long assetId,
                                     @Name("disposalCode") String disposalCode,
                                     IServiceContext context);

    /**
     * 资产处置冲销对称恢复（§1.3 资产恢复分支）：按 assetId 反查关联设备恢复 RUNNING。
     * 设备非 DECOMMISSIONED 时幂等跳过（返回 0）。
     */
    @BizMutation
    int restoreFromAssetDisposal(@Name("assetId") Long assetId,
                                 @Name("disposalCode") String disposalCode,
                                 IServiceContext context);
}
