package app.erp.ast.service.processor;

import app.erp.ast.dao.ErpAstDaoConstants;
import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.audit.ErpAstAssetAuditRecorder;
import app.erp.ast.service.statemachine.ErpAstMaintenanceStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpAstMaintenance completeWork per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含维修完工编排；共享 protected helper 单一真相源在 {@link ErpAstMaintenanceProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 * E3.8：完工时落 MAINTENANCE 审计事件（资产生命周期面，回链维修工单）。
 */
public class ErpAstMaintenanceCompleteWorkProcessor {

    @Inject
    ErpAstMaintenanceProcessor facade;

    @Inject
    ErpAstMaintenanceStateMachine stateMachine;

    @Inject
    ErpAstAssetAuditRecorder auditRecorder;

    public ErpAstMaintenance completeWork(String id, IServiceContext context) {
        ErpAstMaintenance m = facade.requireMaintenance(id, context);
        // 固定来源态守卫委托 StateMachine Bean（M4.53，契约 §4；Bean 直抛领域码 + 调用点同码补参，plan 2026-09-07-2200-1）
        try {
            stateMachine.assertCanCompleteWork(m.getStatus());
        } catch (NopException e) {
            throw e.param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
        }
        m.setStatus(stateMachine.completeWorkTargetStatus());
        facade.maintenanceDao().updateEntity(m);
        if (m.getAsset() != null) {
            auditRecorder.record(m.getAsset(), ErpAstDaoConstants.AUDIT_EVENT_TYPE_MAINTENANCE,
                    ErpAstAssetAuditRecorder.Before.of(m.getAsset()),
                    "ErpAstMaintenance", m.getId(), "Maintenance completed (" + m.getCode() + ")");
        }
        return m;
    }
}
