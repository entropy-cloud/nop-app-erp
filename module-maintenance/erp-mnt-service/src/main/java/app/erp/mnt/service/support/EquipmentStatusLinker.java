package app.erp.mnt.service.support;

import app.erp.mnt.biz.IErpMntEquipmentBiz;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.service.ErpMntConfigs;
import app.erp.mnt.service.ErpMntErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备状态联动器。访问 start→设备 UNDER_MAINTENANCE、complete/cancel→恢复；
 * 停机 record→设备 DOWN、complete→恢复。经 {@code erp-mnt.equipment-status-link-enabled} 门控。
 *
 * <p>恢复目标双分支（P2-RC-061 / UC-MAIN-03 收敛）：UNDER_MAINTENANCE/DOWN 为维护期临时态，
 * 恢复时默认回到运行态 RUNNING；visit 路径维护开始前设备为 IDLE 时（经内部 transient 前态缓存
 * {@link #priorStatusCache} 捕获）恢复 IDLE——owner doc equipment-integration.md §3.3
 * 「恢复为 RUNNING 或 IDLE（根据之前状态）」运行时成立。停机路径（linkToDown）不捕获前态，
 * 恢复恒 RUNNING（owner doc §4.3「更新设备状态为 RUNNING」字面语义，行为零变化）。
 *
 * <p>状态日志（RC-R1.73 / UC-MAIN-02）：三迁移方法在同一事务经 {@link EquipmentStatusLogWriter}
 * 追加 {@code ErpMntEquipmentStatusLog} 行（来源 VISIT/DOWNTIME，由调用路径显式传入 restore 侧），
 * 作为运行时长 Σ RUNNING 段聚合的唯一数据源。
 *
 * <p>残余风险（watch-only，详见 plan 2026-08-15-1605-2 Deferred But Adjudicated）：缓存为 JVM
 * 内存态——①容器重启/多实例部署缓存丢失 → 回退 RUNNING（= 现状已接受行为，非新退化）；②缓存写入
 * 非事务性——linkToUnderMaintenance 所在事务回滚后 IDLE 条目残留，污染该设备下一次 restore
 * （恢复 IDLE 而非 RUNNING），方向保守（IDLE 是更保守可用态）且条目在下一次 linkTo* 覆盖或
 * restore 消费时清除；③异常路径（维护开始后未走 restore）悬挂条目由下一次 restore 消费或
 * linkTo* 覆盖清除，无永久泄漏；④缓存超 {@link #MAX_CACHE_ENTRIES} 清空全表回退 RUNNING
 * （fail-safe）；⑤并发同设备双维护由既有 @Version 乐观锁兜底。
 */
public class EquipmentStatusLinker {

    static final int MAX_CACHE_ENTRIES = 1024;

    @Inject
    IErpMntEquipmentBiz equipmentBiz;

    @Inject
    EquipmentStatusLogWriter statusLogWriter;

    /** visit 路径前态缓存：linkToUnderMaintenance 仅捕获 IDLE（覆盖写），restoreToRunning 消费移除。包级可见供测试观察。 */
    transient ConcurrentHashMap<Long, String> priorStatusCache = new ConcurrentHashMap<>();

    public void linkToUnderMaintenance(Long equipmentId, IServiceContext context) {
        if (!ErpMntConfigs.equipmentStatusLinkEnabled() || equipmentId == null) {
            return;
        }
        capturePriorStatus(equipmentId, context);
        changeEquipmentStatus(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE,
                ErpMntDaoConstants.STATUS_LOG_SOURCE_VISIT, context);
    }

    public void linkToDown(Long equipmentId, IServiceContext context) {
        if (!ErpMntConfigs.equipmentStatusLinkEnabled() || equipmentId == null) {
            return;
        }
        changeEquipmentStatus(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN,
                ErpMntDaoConstants.STATUS_LOG_SOURCE_DOWNTIME, context);
    }

    public void restoreToRunning(Long equipmentId, IServiceContext context) {
        restoreToRunning(equipmentId, ErpMntDaoConstants.STATUS_LOG_SOURCE_VISIT, context);
    }

    /** 带日志来源的恢复：visit 路径传 VISIT，停机路径传 DOWNTIME（RC-R1.73 状态日志来源区分）。 */
    public void restoreToRunning(Long equipmentId, String logSource, IServiceContext context) {
        if (!ErpMntConfigs.equipmentStatusLinkEnabled() || equipmentId == null) {
            return;
        }
        String priorStatus = consumePriorStatus(equipmentId);
        String targetStatus = ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE.equals(priorStatus)
                ? ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE
                : ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING;
        changeEquipmentStatus(equipmentId, targetStatus, logSource, context);
    }

    /**
     * visit 开始前捕获设备前态：仅 IDLE 入缓存（覆盖写）；非 IDLE（RUNNING/DOWN 等）移除既有条目，
     * 保证非 IDLE 来源恢复目标恒 RUNNING（D2 裁决，同时清除异常路径悬挂残留）。
     */
    protected void capturePriorStatus(Long equipmentId, IServiceContext context) {
        ErpMntEquipment equipment = equipmentBiz.get(String.valueOf(equipmentId), false, context);
        if (equipment == null) {
            return; // 守卫由 changeEquipmentStatus 抛 ERR_EQUIPMENT_NOT_FOUND
        }
        if (ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE.equals(equipment.getStatus())) {
            guardCacheSize();
            priorStatusCache.put(equipmentId, equipment.getStatus());
        } else {
            priorStatusCache.remove(equipmentId);
        }
    }

    /** 消费前态：remove 在恢复前执行，防并发重复消费同一条目。 */
    protected String consumePriorStatus(Long equipmentId) {
        return priorStatusCache.remove(equipmentId);
    }

    /** 缓存超限清空全表（回退 RUNNING = 现状行为，fail-safe）。 */
    protected void guardCacheSize() {
        if (priorStatusCache.size() >= MAX_CACHE_ENTRIES) {
            priorStatusCache.clear();
        }
    }

    protected void changeEquipmentStatus(Long equipmentId, String newStatus, String logSource,
                                         IServiceContext context) {
        ErpMntEquipment equipment = equipmentBiz.get(String.valueOf(equipmentId), false, context);
        if (equipment == null) {
            throw new NopException(ErpMntErrors.ERR_EQUIPMENT_NOT_FOUND)
                    .param(ErpMntErrors.ARG_EQUIPMENT_ID, equipmentId);
        }
        String fromStatus = equipment.getStatus();
        equipment.setStatus(newStatus);
        equipmentBiz.updateEntity(equipment, null, context);
        statusLogWriter.append(equipmentId, fromStatus, newStatus, logSource, null);
    }
}
