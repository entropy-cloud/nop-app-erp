package app.erp.inv.service.entity;

import app.erp.inv.biz.IErpInvOwnershipTransferBiz;
import app.erp.inv.dao.entity.ErpInvOwnershipTransfer;
import app.erp.inv.service.processor.ErpInvOwnershipTransferConfirmProcessor;
import app.erp.inv.service.processor.ErpInvOwnershipTransferDoneProcessor;
import app.erp.inv.service.processor.ErpInvOwnershipTransferProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 所有权转移单 BizModel（Facade）。状态机迁移 DRAFT→CONFIRMED→DONE 经 per-mutation Processor 编排
 * （{@link ErpInvOwnershipTransferConfirmProcessor}/{@link ErpInvOwnershipTransferDoneProcessor}，
 * protected step 方法，下游可逐 step 覆盖）。{@code cancel}（{@code :46} 单步状态翻转豁免）委托 facade。
 *
 * <p>权威设计见 {@code docs/design/inventory/consignment.md}（所有权转移单 + 状态机 + 同库位调账）。
 */
@BizModel("ErpInvOwnershipTransfer")
public class ErpInvOwnershipTransferBizModel extends CrudBizModel<ErpInvOwnershipTransfer>
        implements IErpInvOwnershipTransferBiz {

    @Inject
    ErpInvOwnershipTransferProcessor ownershipTransferProcessor;

    @Inject
    ErpInvOwnershipTransferConfirmProcessor confirmProcessor;

    @Inject
    ErpInvOwnershipTransferDoneProcessor doneProcessor;

    public ErpInvOwnershipTransferBizModel() {
        setEntityName(ErpInvOwnershipTransfer.class.getName());
    }

    @Override
    @BizMutation
    public ErpInvOwnershipTransfer confirm(@Name("transferId") Long transferId, IServiceContext context) {
        return confirmProcessor.confirm(transferId, context);
    }

    @Override
    @BizMutation
    public ErpInvOwnershipTransfer done(@Name("transferId") Long transferId, IServiceContext context) {
        return doneProcessor.done(transferId, context);
    }

    @Override
    @BizMutation
    public ErpInvOwnershipTransfer cancel(@Name("transferId") Long transferId, IServiceContext context) {
        return ownershipTransferProcessor.cancel(transferId, context);
    }
}
