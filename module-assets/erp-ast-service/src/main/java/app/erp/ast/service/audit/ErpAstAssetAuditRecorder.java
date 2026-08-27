package app.erp.ast.service.audit;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetActionLog;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * E3.8 资产操作审计记录器（`audit-trail-and-custom-fieldsets.md` §1，Actionlog 模式）。
 *
 * <p>独立实体 {@link ErpAstAssetActionLog} 承载（Phase 1 裁决）：资产**实物/生命周期面**事件
 * （CREATE/UPDATE/STATUS_CHANGE/TRANSFER/MAINTENANCE/VALUATION/DISPOSAL），与会计日志（财务面）互补，
 * 不重复记录业务 action（BizModel 动作审计已有），聚焦资产状态与归属变化视角。
 *
 * <p>调用约定：在资产状态/归属变化的同一事务内调用（审计行与业务变更原子提交）；
 * from* 值由调用方从变更前实体快照传入（处理器/BizModel diff 持有旧值），to* 值取实体当前值。
 * 同域审计实体插入经 daoFor 直查（审计写路径非跨实体业务读取，E3 daoFor 豁免：域内审计追加）。
 */
public class ErpAstAssetAuditRecorder {

    @Inject
    IDaoProvider daoProvider;

    /** 变更前快照（from* 值来源；CREATE 时传 null）。 */
    public static class Before {
        /** 包级可见字段（域内调用方 diff 读取变更前状态摘要）。 */
        final String status;
        final String departmentId;
        final String locationId;
        final String employeeId;

        public Before(String status, String departmentId, String locationId, String employeeId) {
            this.status = status;
            this.departmentId = departmentId;
            this.locationId = locationId;
            this.employeeId = employeeId;
        }

        public static Before of(ErpAstAsset asset) {
            return new Before(asset.getStatus(), asset.getDepartmentId(), asset.getLocationId(), asset.getEmployeeId());
        }
    }

    public void record(ErpAstAsset asset, String eventType, Before before,
                       String refEntityName, String refEntityId, String summary) {
        IEntityDao<ErpAstAssetActionLog> dao = daoProvider.daoFor(ErpAstAssetActionLog.class);
        ErpAstAssetActionLog log = dao.newEntity();
        log.setAssetId(asset.getId());
        log.setEventType(eventType);
        if (before != null) {
            log.setFromStatus(before.status);
            log.setFromDepartmentId(before.departmentId);
            log.setFromLocationId(before.locationId);
            log.setFromStaffId(before.employeeId);
        }
        log.setToStatus(asset.getStatus());
        log.setToDepartmentId(asset.getDepartmentId());
        log.setToLocationId(asset.getLocationId());
        log.setToStaffId(asset.getEmployeeId());
        log.setRefEntityName(refEntityName);
        log.setRefEntityId(refEntityId);
        log.setSummary(summary);
        dao.saveEntity(log);
    }
}
