
package app.erp.mnt.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;

/**
 * 停机记录业务接口。除标准 CRUD 外，定义停机生命周期：
 * record（设备→DOWN + 记录 startTime）/ complete（endTime + totalMinutes 计算 + 设备恢复）。
 *
 * <p>record/complete 经 {@code EquipmentStatusLinker} 联动设备状态（DOWN/恢复），经
 * {@code erp-mnt.equipment-status-link-enabled} 门控。
 */
public interface IErpMntDowntimeEntryBiz extends ICrudBiz<ErpMntDowntimeEntry> {

    @BizMutation
    ErpMntDowntimeEntry record(@Name("downtimeId") Long downtimeId, IServiceContext context);

    @BizMutation
    ErpMntDowntimeEntry complete(@Name("downtimeId") Long downtimeId, IServiceContext context);
}
