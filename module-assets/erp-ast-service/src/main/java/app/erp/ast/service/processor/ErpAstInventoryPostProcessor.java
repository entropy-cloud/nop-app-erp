package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.AssetInventoryPostingDispatcher;
import app.erp.ast.service.statemachine.ErpAstInventoryStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.sql.Timestamp;

/**
 * ErpAstInventory post per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含盘点过账编排（INVENTORY 业财过账 + posted 审计字段）；共享 protected helper 单一真相源在
 * {@link ErpAstInventoryProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstInventoryPostProcessor {

    @Inject
    ErpAstInventoryProcessor facade;

    @Inject
    AssetInventoryPostingDispatcher postingDispatcher;

    @Inject
    ErpAstInventoryStateMachine stateMachine;

    public ErpAstInventory post(Long id, IServiceContext context) {
        ErpAstInventory inv = facade.requireInventory(id, context);
        // 固定来源态守卫委托 StateMachine Bean（M4.52，契约 §4/§7；Bean 抛 common 层码 → cause-chain 领域码）
        try {
            stateMachine.assertCanPost(inv.getStatus());
        } catch (NopException e) {
            throw facade.mapIllegalTransition(e, inv, ErpAstConstants.INVENTORY_STATUS_RECONCILING);
        }
        if (facade.isApprovalRequired() && inv.getApprovedAt() == null) {
            throw new NopException(ErpAstErrors.ERR_AST_INVENTORY_NOT_RECONCILED)
                    .param(ErpAstErrors.ARG_INVENTORY_CODE, inv.getCode())
                    .param(ErpAstErrors.ARG_CURRENT_STATUS, inv.getStatus());
        }
        facade.validateAllVarianceProcessed(inv);
        facade.validateShortageBlocks(inv);

        Long voucherId = postingDispatcher.tryPost(inv);
        inv = facade.reload(id);
        if (voucherId != null) {
            Timestamp now = CoreMetrics.currentTimestamp();
            inv.setPosted(true);
            inv.setPostedAt(now);
            inv.setPostedBy(facade.currentUserId());
            inv.setStatus(stateMachine.postTargetStatus());
        }
        facade.inventoryDao().updateEntity(inv);
        return inv;
    }
}
