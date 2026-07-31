package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
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

    public ErpAstInventory submitForCount(Long id, IServiceContext context) {
        ErpAstInventory inv = facade.requireInventory(id, context);
        facade.validateTransition(inv, ErpAstConstants.INVENTORY_STATUS_DRAFT, "submitForCount");
        if (facade.findLines(inv.getId()).isEmpty()) {
            throw new NopException(ErpAstErrors.ERR_AST_INVENTORY_RANGE_EMPTY)
                    .param(ErpAstErrors.ARG_INVENTORY_CODE, inv.getCode());
        }
        inv.setStatus(ErpAstConstants.INVENTORY_STATUS_COUNTING);
        facade.inventoryDao().updateEntity(inv);
        return inv;
    }
}
