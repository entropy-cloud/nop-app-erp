package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.statemachine.ErpAstInventoryStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpAstInventory submitForCount per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含提交盘点编排；共享 protected helper 单一真相源在 {@link ErpAstInventoryProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstInventorySubmitForCountProcessor {

    @Inject
    ErpAstInventoryProcessor facade;

    @Inject
    ErpAstInventoryStateMachine stateMachine;

    public ErpAstInventory submitForCount(String id, IServiceContext context) {
        ErpAstInventory inv = facade.requireInventory(id, context);
        // 固定来源态守卫委托 StateMachine Bean（M4.52，契约 §4/§7；Bean 抛 common 层码 → cause-chain 领域码）
        try {
            stateMachine.assertCanSubmitForCount(inv.getStatus());
        } catch (NopException e) {
            throw facade.mapIllegalTransition(e, inv, ErpAstConstants.INVENTORY_STATUS_DRAFT);
        }
        if (facade.findLines(inv.getId()).isEmpty()) {
            throw new NopException(ErpAstErrors.ERR_AST_INVENTORY_RANGE_EMPTY)
                    .param(ErpAstErrors.ARG_INVENTORY_CODE, inv.getCode());
        }
        inv.setStatus(stateMachine.submitForCountTargetStatus());
        facade.inventoryDao().updateEntity(inv);
        return inv;
    }
}
