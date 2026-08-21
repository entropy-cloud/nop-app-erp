package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

/**
 * ErpAstMaintenance approve per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Maintenance 自有工单状态机（DRAFT→SUBMITTED→IN_PROGRESS→COMPLETED→POSTED），approve 仅盖审批人
 * （前置校验 COMPLETED 态；POSTED 幂等返回）。
 * Self-contained orchestration: require → idempotency → validateCompleted → set approvedBy/approvedAt → save.
 * Domain logic via facade protected helpers (single source of truth).
 * Dormant until R5.8 rewire（BizModel Java 直调 facade.approve，不经 xbiz 委托链）。
 */
public class ErpAstMaintenanceApproveProcessor extends AbstractApproveProcessor<ErpAstMaintenance> {

    @Inject
    ErpAstMaintenanceProcessor processor;

    @Override
    public ErpAstMaintenance approve(String id, IServiceContext context) {
        ErpAstMaintenance m = processor.requireMaintenance(id, context);
        if (Objects.equals(m.getStatus(), ErpAstConstants.MAINTENANCE_STATUS_POSTED)) {
            return m;
        }
        if (!Objects.equals(m.getStatus(), ErpAstConstants.MAINTENANCE_STATUS_COMPLETED)) {
            throw processor.illegalTransition(m, m.getStatus(), "COMPLETED");
        }
        m.setApprovedBy(currentUserId());
        m.setApprovedAt(now());
        processor.maintenanceDao().updateEntity(m);
        return m;
    }

    @Override
    protected IEntityDao<ErpAstMaintenance> dao() {
        return daoProvider.daoFor(ErpAstMaintenance.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstMaintenance entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpAstMaintenance entity, String status) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected void setApprovedBy(ErpAstMaintenance entity, String userId) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected void setApprovedAt(ErpAstMaintenance entity, java.sql.Timestamp ts) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected boolean isApproved(ErpAstMaintenance entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpAstMaintenance entity) {
        return false;
    }

    @Override
    protected String submittedStatus() {
        return null;
    }

    @Override
    protected String approvedStatus() {
        return null;
    }
}
