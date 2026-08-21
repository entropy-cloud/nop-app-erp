package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.posting.AssetInventoryPostingDispatcher;
import app.erp.ast.service.statemachine.ErpAstInventoryStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpAstInventory reverse per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含盘点红冲编排（红冲凭证 + posted/状态回退）；共享 protected helper 单一真相源在
 * {@link ErpAstInventoryProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstInventoryReverseProcessor {

    @Inject
    ErpAstInventoryProcessor facade;

    @Inject
    AssetInventoryPostingDispatcher postingDispatcher;

    @Inject
    ErpAstInventoryStateMachine stateMachine;

    public ErpAstInventory reverse(String id, IServiceContext context) {
        ErpAstInventory inv = facade.requireInventory(id, context);
        // posted boolean 为动态过账契约守卫（§11.2 M4 (iii) 不入轴），保留原位
        if (!Boolean.TRUE.equals(inv.getPosted())) {
            throw facade.illegalTransition(inv, inv.getStatus(), "POSTED + posted=true");
        }
        // 固定来源态守卫委托 StateMachine Bean（M4.52，契约 §4/§7；Bean 抛 common 层码 → cause-chain 领域码）
        try {
            stateMachine.assertCanReverse(inv.getStatus());
        } catch (NopException e) {
            throw facade.mapIllegalTransition(e, inv, ErpAstConstants.INVENTORY_STATUS_POSTED);
        }
        postingDispatcher.reverse(inv);
        inv = facade.reload(id);
        inv.setPosted(false);
        inv.setPostedAt(null);
        inv.setPostedBy(null);
        inv.setStatus(stateMachine.reverseTargetStatus());
        facade.inventoryDao().updateEntity(inv);
        return inv;
    }
}
