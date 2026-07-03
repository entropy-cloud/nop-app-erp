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
                                 @Name("newStatus") Integer newStatus,
                                 IServiceContext context);
}
