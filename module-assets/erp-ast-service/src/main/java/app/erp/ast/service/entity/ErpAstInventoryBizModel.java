
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstInventoryBiz;
import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.processor.ErpAstInventoryProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 资产盘点 BizModel（Facade）。所有动作经 xbiz 委托 {@link ErpAstInventoryProcessor} 全权处理。
 * 详见 owner doc {@code docs/design/assets/inventory.md}。
 */
@BizModel("ErpAstInventory")
public class ErpAstInventoryBizModel extends CrudBizModel<ErpAstInventory> implements IErpAstInventoryBiz {

    @Inject
    ErpAstInventoryProcessor inventoryProcessor;

    public ErpAstInventoryBizModel() {
        setEntityName(ErpAstInventory.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstInventory createInventory(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.createInventory(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstInventory submitForCount(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.submitForCount(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstInventory reconcile(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.reconcile(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstInventory processVariance(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.processVariance(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstInventory approve(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.approve(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstInventory post(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.post(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstInventory cancel(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.cancel(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstInventory reverse(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.reverse(id, context);
    }
}
