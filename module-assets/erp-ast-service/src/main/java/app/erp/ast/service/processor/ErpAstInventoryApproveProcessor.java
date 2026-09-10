package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpAstInventory）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
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
        ErpAstInventory inv = processor.requireInventory(id, context);
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

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_AST_INVENTORY_ILLEGAL_STATUS_TRANSITION（参数形态与 facade 组装一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpAstInventory entity, String current, String... expected) {
        return new NopException(ErpAstErrors.ERR_AST_INVENTORY_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_INVENTORY_CODE, entity.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
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
