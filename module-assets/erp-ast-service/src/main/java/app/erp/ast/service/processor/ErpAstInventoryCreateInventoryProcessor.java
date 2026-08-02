package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.ErpAstConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpAstInventory createInventory per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含盘点建单编排（范围展开）；共享 protected helper 单一真相源在 {@link ErpAstInventoryProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstInventoryCreateInventoryProcessor {

    @Inject
    ErpAstInventoryProcessor facade;

    public ErpAstInventory createInventory(Long id, IServiceContext context) {
        ErpAstInventory inv = facade.requireInventory(id, context);
        facade.validateTransition(inv, ErpAstConstants.INVENTORY_STATUS_DRAFT, "createInventory");
        facade.expandAssetsToLines(inv, context);
        inv.setStatus(ErpAstConstants.INVENTORY_STATUS_DRAFT);
        facade.inventoryDao().updateEntity(inv);
        return inv;
    }
}
