package app.erp.inv.service.entity;

import app.erp.inv.biz.IErpInvOwnershipTransferBiz;
import app.erp.inv.dao.entity.ErpInvOwnershipTransfer;
import app.erp.inv.service.processor.ErpInvOwnershipTransferProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 所有权转移单 BizModel（Facade）。状态机迁移（DRAFT→CONFIRMED→DONE/CANCELLED）、DONE 同库位调账 + 业财过账派发
 * 委托 {@link ErpInvOwnershipTransferProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>权威设计见 {@code docs/design/inventory/consignment.md}（所有权转移单 + 状态机 + 同库位调账）。
 */
@BizModel("ErpInvOwnershipTransfer")
public class ErpInvOwnershipTransferBizModel extends CrudBizModel<ErpInvOwnershipTransfer>
        implements IErpInvOwnershipTransferBiz {

    @Inject
    ErpInvOwnershipTransferProcessor ownershipTransferProcessor;

    public ErpInvOwnershipTransferBizModel() {
        setEntityName(ErpInvOwnershipTransfer.class.getName());
    }

    @Override
    @BizMutation
    public ErpInvOwnershipTransfer confirm(@Name("transferId") Long transferId, IServiceContext context) {
        return ownershipTransferProcessor.confirm(transferId, context);
    }

    @Override
    @BizMutation
    public ErpInvOwnershipTransfer done(@Name("transferId") Long transferId, IServiceContext context) {
        return ownershipTransferProcessor.done(transferId, context);
    }

    @Override
    @BizMutation
    public ErpInvOwnershipTransfer cancel(@Name("transferId") Long transferId, IServiceContext context) {
        return ownershipTransferProcessor.cancel(transferId, context);
    }
}
