
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.inv.biz.IErpInvTransferOrderBiz;
import app.erp.inv.dao.entity.ErpInvTransferOrder;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;

import java.util.Objects;

@BizModel("ErpInvTransferOrder")
public class ErpInvTransferOrderBizModel extends CrudBizModel<ErpInvTransferOrder> implements IErpInvTransferOrderBiz {
    public ErpInvTransferOrderBizModel(){
        setEntityName(ErpInvTransferOrder.class.getName());
    }

    @Override
    @BizMutation
    public ErpInvTransferOrder confirm(@Name("transferOrderId") Long transferOrderId, IServiceContext context) {
        ErpInvTransferOrder order = requireEntity(String.valueOf(transferOrderId), null, context);
        if (!Objects.equals(order.getDocStatus(), ErpInvConstants.DOC_STATUS_DRAFT)) {
            throw new NopException(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION)
                    .param(ErpInvErrors.ARG_TAKE_ID, transferOrderId)
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, order.getDocStatus());
        }
        order.setDocStatus(ErpInvConstants.DOC_STATUS_CONFIRMED);
        updateEntity(order, null, context);
        return order;
    }
}
