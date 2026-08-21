
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstInventoryBiz;
import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.processor.ErpAstInventoryApproveProcessor;
import app.erp.ast.service.processor.ErpAstInventoryCreateInventoryProcessor;
import app.erp.ast.service.processor.ErpAstInventoryPostProcessor;
import app.erp.ast.service.processor.ErpAstInventoryProcessVarianceProcessor;
import app.erp.ast.service.processor.ErpAstInventoryProcessor;
import app.erp.ast.service.processor.ErpAstInventoryReconcileProcessor;
import app.erp.ast.service.processor.ErpAstInventoryReverseProcessor;
import app.erp.ast.service.processor.ErpAstInventorySubmitForCountProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 资产盘点 BizModel（Facade）。D-mutation 委托对应 per-mutation Processor（R6.3 拆分）；
 * approve（S-mutation）委托 {@link ErpAstInventoryApproveProcessor}；cancel（`:45` 单步状态翻转豁免）保留委托
 * {@link ErpAstInventoryProcessor}。详见 owner doc {@code docs/design/assets/inventory.md}。
 */
@BizModel("ErpAstInventory")
public class ErpAstInventoryBizModel extends CrudBizModel<ErpAstInventory> implements IErpAstInventoryBiz {

    @Inject
    ErpAstInventoryProcessor inventoryProcessor;

    @Inject
    ErpAstInventoryApproveProcessor approveProcessor;

    @Inject
    ErpAstInventoryCreateInventoryProcessor createInventoryProcessor;

    @Inject
    ErpAstInventorySubmitForCountProcessor submitForCountProcessor;

    @Inject
    ErpAstInventoryReconcileProcessor reconcileProcessor;

    @Inject
    ErpAstInventoryProcessVarianceProcessor processVarianceProcessor;

    @Inject
    ErpAstInventoryPostProcessor postProcessor;

    @Inject
    ErpAstInventoryReverseProcessor reverseProcessor;

    public ErpAstInventoryBizModel() {
        setEntityName(ErpAstInventory.class.getName());
    }

    @Override
    @BizMutation
    public ErpAstInventory createInventory(@Name("id") String id, IServiceContext context) {
        return createInventoryProcessor.createInventory(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory submitForCount(@Name("id") String id, IServiceContext context) {
        return submitForCountProcessor.submitForCount(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory reconcile(@Name("id") String id, IServiceContext context) {
        return reconcileProcessor.reconcile(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory processVariance(@Name("id") String id, IServiceContext context) {
        return processVarianceProcessor.processVariance(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory approve(@Name("id") String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory post(@Name("id") String id, IServiceContext context) {
        return postProcessor.post(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory cancel(@Name("id") String id, IServiceContext context) {
        return inventoryProcessor.cancel(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory reverse(@Name("id") String id, IServiceContext context) {
        return reverseProcessor.reverse(id, context);
    }
}
