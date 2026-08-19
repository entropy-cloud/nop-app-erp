package app.erp.mnt.service.support;

import app.erp.mnt.dao.entity.ErpMntEquipmentStatusLog;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.sql.Timestamp;

/**
 * 设备状态日志追加器（RC-R1.73 / UC-MAIN-02 运行时长触发）：设备状态每次迁移在同一事务追加一行
 * {@link ErpMntEquipmentStatusLog}，作为运行时长查询时聚合（Σ RUNNING 段）的唯一数据源。
 *
 * <p>写点：{@link EquipmentStatusLinker} 三迁移方法（来源 VISIT/DOWNTIME）+
 * {@code ErpMntEquipmentBizModel.changeStatus}（来源 MANUAL）。
 *
 * <p>changeAt 取 {@link CoreMetrics#currentDateTime()}（秒级语义即可满足小时粒度聚合，
 * 且对冻结时钟测试确定性友好）。
 */
public class EquipmentStatusLogWriter {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 追加状态变更日志行（与状态迁移同一事务）。
     * daoFor 直写说明（E3）：StatusLog 为域内日志子实体，仅作聚合数据源，无业务管道/权限语义需求。
     */
    public void append(Long equipmentId, String fromStatus, String toStatus, String source, String sourceBillCode) {
        IEntityDao<ErpMntEquipmentStatusLog> dao = daoProvider.daoFor(ErpMntEquipmentStatusLog.class);
        ErpMntEquipmentStatusLog log = dao.newEntity();
        log.setEquipmentId(equipmentId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setChangeAt(Timestamp.valueOf(CoreMetrics.currentDateTime()));
        log.setSource(source);
        log.setSourceBillCode(sourceBillCode);
        dao.saveEntity(log);
    }
}
