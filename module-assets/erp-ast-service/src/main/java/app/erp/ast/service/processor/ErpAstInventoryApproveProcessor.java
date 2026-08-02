package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstInventory approve per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Inventory 自有非审批状态机（DRAFT→COUNTING→RECONCILING→POSTED），approve 仅盖 approvedBy/approvedAt
 * （前置 validateReconciling 校验 RECONCILING 态）。
 * Self-contained orchestration: require → validateReconciling → set approvedBy/approvedAt → save.
 * Domain logic via facade protected helpers (single source of truth).
 * Dormant until R5.8 rewire（BizModel Java 直调 facade.approve，不经 xbiz 委托链）。
 */
public class ErpAstInventoryApproveProcessor extends AbstractApproveProcessor<ErpAstInventory> {

    @Inject
    ErpAstInventoryProcessor processor;

    @Override
    public ErpAstInventory approve(String id, IServiceContext context) {
        ErpAstInventory inv = processor.requireInventory(Long.valueOf(id), context);
        processor.validateReconciling(inv);
        inv.setApprovedBy(currentUserId());
        inv.setApprovedAt(now());
        processor.inventoryDao().updateEntity(inv);
        return inv;
    }

    @Override
    protected IEntityDao<ErpAstInventory> dao() {
        return daoProvider.daoFor(ErpAstInventory.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstInventory entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpAstInventory entity, String status) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected void setApprovedBy(ErpAstInventory entity, String userId) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected void setApprovedAt(ErpAstInventory entity, java.sql.Timestamp ts) {
        // not reached: Pattern B custom public override
    }

    @Override
    protected boolean isApproved(ErpAstInventory entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpAstInventory entity) {
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
