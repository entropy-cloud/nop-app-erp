package app.erp.mnt.service.support;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntEquipmentStatusLog;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * 设备累计运行时长计算器（RC-R1.73 / UC-MAIN-02）：查询时聚合 Σ RUNNING 段，以设备状态记录
 * （{@link ErpMntEquipmentStatusLog}）为唯一真相——无采集 Job、无物化累计列，幂等可重算
 * （D1 裁决：否决「累计列物化 + 采集 Job」，job 中断/回滚下增量累加漂移需对账）。
 *
 * <p>聚合语义：
 * <ul>
 *   <li><b>有日志行</b>：按 changeAt 升序逐行扫描，行 i 之后的时段状态 = toStatus_i，持续到下一行
 *       changeAt（最后一行持续到 asOf）；toStatus=RUNNING 的时段计入。首行之前的 RUNNING 史不可知，
 *       不计入（保守 fail-safe，防无历史设备虚计触发）。当前开放段 = 最后一行 toStatus=RUNNING 时
 *       从其 changeAt 至 asOf。</li>
 *   <li><b>无日志行（遗留基线双分支）</b>：当前 status=RUNNING → 从设备 createTime 起算至 asOf；
 *       当前非 RUNNING（IDLE/DOWN/UNDER_MAINTENANCE/DECOMMISSIONED）→ 记 0 直至首条日志行。</li>
 * </ul>
 */
public class EquipmentRuntimeCalculator {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 计算设备截至 asOf 的累计运行小时数（scale 4）。
     * daoFor 直读说明（E3）：equipment/StatusLog 均为域内实体的只读聚合访问，无业务管道语义。
     */
    public BigDecimal computeRunningHours(Long equipmentId, Timestamp asOf) {
        ErpMntEquipment equipment = daoProvider.daoFor(ErpMntEquipment.class).getEntityById(equipmentId);
        if (equipment == null) {
            return BigDecimal.ZERO;
        }
        List<ErpMntEquipmentStatusLog> logs = findLogs(equipmentId);
        if (logs.isEmpty()) {
            return legacyRunningHours(equipment, asOf);
        }
        long runningSeconds = 0L;
        for (int i = 0; i < logs.size(); i++) {
            ErpMntEquipmentStatusLog row = logs.get(i);
            if (!ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING.equals(row.getToStatus())) {
                continue;
            }
            Timestamp start = row.getChangeAt();
            Timestamp end = i + 1 < logs.size() ? logs.get(i + 1).getChangeAt() : asOf;
            runningSeconds += segmentSeconds(start, end, asOf);
        }
        return toHours(runningSeconds);
    }

    /** 遗留基线：无日志历史设备。当前 RUNNING → createTime 起算；非 RUNNING → 0 直至首条日志行。 */
    protected BigDecimal legacyRunningHours(ErpMntEquipment equipment, Timestamp asOf) {
        if (!ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING.equals(equipment.getStatus())
                || equipment.getCreateTime() == null) {
            return BigDecimal.ZERO;
        }
        return toHours(segmentSeconds(equipment.getCreateTime(), asOf, asOf));
    }

    /** 段时长（秒），钳制到 [0, asOf-start]：起点晚于 asOf（未来日志行）或倒序段计 0。 */
    protected long segmentSeconds(Timestamp start, Timestamp end, Timestamp asOf) {
        if (start == null || end == null) {
            return 0L;
        }
        if (start.after(asOf)) {
            return 0L;
        }
        if (end.after(asOf)) {
            end = asOf;
        }
        long seconds = Duration.between(start.toLocalDateTime(), end.toLocalDateTime()).getSeconds();
        return Math.max(seconds, 0L);
    }

    protected List<ErpMntEquipmentStatusLog> findLogs(Long equipmentId) {
        IEntityDao<ErpMntEquipmentStatusLog> dao = daoProvider.daoFor(ErpMntEquipmentStatusLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("equipmentId", equipmentId));
        List<ErpMntEquipmentStatusLog> logs = dao.findAllByQuery(q);
        logs.sort(Comparator.comparing(ErpMntEquipmentStatusLog::getChangeAt)
                .thenComparing(ErpMntEquipmentStatusLog::getId));
        return logs;
    }

    protected BigDecimal toHours(long seconds) {
        return BigDecimal.valueOf(seconds)
                .divide(BigDecimal.valueOf(3600L), 4, RoundingMode.HALF_UP);
    }
}
