
package app.erp.mnt.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;

import java.util.List;

/**
 * 停机记录业务接口。除标准 CRUD 外，定义停机生命周期：
 * record（设备→DOWN + 记录 startTime）/ complete（endTime + totalMinutes 计算 + 设备恢复）。
 *
 * <p>record/complete 经 {@code EquipmentStatusLinker} 联动设备状态（DOWN/恢复），经
 * {@code erp-mnt.equipment-status-link-enabled} 门控。
 *
 * <p>RC-R1.76 / UC-MAIN-06：record/complete 后置 notify 计划员双事件（模板种子 7208/7209，
 * {@code erp-mnt.downtime-notify-enabled} 门控，失败静默降级）；本接口另暴露开放停机窗口只读查询
 * 供制造域排产消费（拉取模型：mfg job card 生成前拉取，开放停机工作中心的受影响工单跳过生成）。
 */
public interface IErpMntDowntimeEntryBiz extends ICrudBiz<ErpMntDowntimeEntry> {

    @BizMutation
    ErpMntDowntimeEntry record(@Name("downtimeId") Long downtimeId, IServiceContext context);

    @BizMutation
    ErpMntDowntimeEntry complete(@Name("downtimeId") Long downtimeId, IServiceContext context);

    /**
     * 开放停机窗口查询（矩阵 :109「maintenance 被 manufacturing 查」预期方向落地）：
     * endTime null 且设备 status=DOWN 的停机记录，经 equipment.workcenterId 桥接暴露
     * 「工作中心→开放停机窗口」。工作中心未映射的设备不出窗。
     */
    @BizQuery
    List<MntOpenDowntimeWindow> findOpenDowntimeEquipmentWorkcenters(IServiceContext context);
}
