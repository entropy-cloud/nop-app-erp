package app.erp.mnt.biz;

import io.nop.api.core.annotations.data.DataBean;

import java.sql.Timestamp;

/**
 * 开放停机窗口 DTO（{@link IErpMntDowntimeEntryBiz#findOpenDowntimeEquipmentWorkcenters} 返回元素，
 * RC-R1.76 / UC-MAIN-06 停机→制造排产消费）。
 *
 * <p>字段映射（{@code ErpMntDowntimeEntry} × {@code ErpMntEquipment} 桥接）：
 * <ul>
 *   <li>{@code equipmentId}/{@code equipmentCode} ← 停机记录关联设备</li>
 *   <li>{@code workcenterId} ← {@code ErpMntEquipment.workcenterId}（设备↔工作中心桥，D3）</li>
 *   <li>{@code startTime}/{@code reason} ← 停机记录起始/原因</li>
 * </ul>
 *
 * <p>开放语义 = endTime null 且设备 status=DOWN（工作中心未映射的设备不出窗）。
 * mfg 排产消费（job card 生成门控）拉取后按 workcenterId 判定暂停；停机 complete 后窗口
 * 自然关闭，下次排产执行时点恢复（拉取消费模型，恢复即时性 = 下次排产执行）。
 */
@DataBean
public class MntOpenDowntimeWindow {

    private Long equipmentId;
    private String equipmentCode;
    private Long workcenterId;
    private Timestamp startTime;
    private String reason;

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public void setEquipmentCode(String equipmentCode) {
        this.equipmentCode = equipmentCode;
    }

    public Long getWorkcenterId() {
        return workcenterId;
    }

    public void setWorkcenterId(Long workcenterId) {
        this.workcenterId = workcenterId;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
