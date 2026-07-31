
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import app.erp.inv.biz.IErpInvTransferOrderBiz;
import app.erp.inv.dao.entity.ErpInvTransferOrder;
import app.erp.inv.service.processor.ErpInvTransferOrderConfirmProcessor;

@BizModel("ErpInvTransferOrder")
public class ErpInvTransferOrderBizModel extends CrudBizModel<ErpInvTransferOrder> implements IErpInvTransferOrderBiz {
    public ErpInvTransferOrderBizModel(){
        setEntityName(ErpInvTransferOrder.class.getName());
    }

    @Inject
    ErpInvTransferOrderConfirmProcessor confirmProcessor;

    @Override
    @BizMutation
    public ErpInvTransferOrder confirm(@Name("transferOrderId") Long transferOrderId, IServiceContext context) {
        return confirmProcessor.confirm(transferOrderId, context);
    }
}
