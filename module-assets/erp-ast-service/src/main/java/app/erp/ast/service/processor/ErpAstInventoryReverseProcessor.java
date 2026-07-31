package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.posting.AssetInventoryPostingDispatcher;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

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

    public ErpAstInventory reverse(Long id, IServiceContext context) {
        ErpAstInventory inv = facade.requireInventory(id, context);
        if (!Objects.equals(inv.getStatus(), ErpAstConstants.INVENTORY_STATUS_POSTED)
                || !Boolean.TRUE.equals(inv.getPosted())) {
            throw facade.illegalTransition(inv, inv.getStatus(), "POSTED + posted=true");
        }
        postingDispatcher.reverse(inv);
        inv = facade.reload(id);
        inv.setPosted(false);
        inv.setPostedAt(null);
        inv.setPostedBy(null);
        inv.setStatus(ErpAstConstants.INVENTORY_STATUS_RECONCILING);
        facade.inventoryDao().updateEntity(inv);
        return inv;
    }
}
