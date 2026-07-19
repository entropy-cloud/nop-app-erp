
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.inv.biz.IErpInvStockTakeBiz;
import app.erp.inv.dao.entity.ErpInvStockTake;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;

import java.util.Objects;

@BizModel("ErpInvStockTake")
public class ErpInvStockTakeBizModel extends CrudBizModel<ErpInvStockTake> implements IErpInvStockTakeBiz {
    public ErpInvStockTakeBizModel(){
        setEntityName(ErpInvStockTake.class.getName());
    }

    @Override
    @BizMutation
    public ErpInvStockTake startTake(@Name("takeId") Long takeId, IServiceContext context) {
        ErpInvStockTake take = requireEntity(String.valueOf(takeId), null, context);
        if (!Objects.equals(take.getDocStatus(), ErpInvConstants.DOC_STATUS_DRAFT)) {
            throw new NopException(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION)
                    .param(ErpInvErrors.ARG_TAKE_ID, takeId)
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, take.getDocStatus());
        }
        take.setDocStatus(ErpInvConstants.DOC_STATUS_CONFIRMED);
        updateEntity(take, null, context);
        return take;
    }

    @Override
    @BizMutation
    public ErpInvStockTake completeTake(@Name("takeId") Long takeId, IServiceContext context) {
        ErpInvStockTake take = requireEntity(String.valueOf(takeId), null, context);
        if (!Objects.equals(take.getDocStatus(), ErpInvConstants.DOC_STATUS_CONFIRMED)) {
            throw new NopException(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION)
                    .param(ErpInvErrors.ARG_TAKE_ID, takeId)
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, take.getDocStatus());
        }
        take.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
        updateEntity(take, null, context);
        return take;
    }

    @Override
    @BizMutation
    public ErpInvStockTake cancelTake(@Name("takeId") Long takeId, IServiceContext context) {
        ErpInvStockTake take = requireEntity(String.valueOf(takeId), null, context);
        String status = take.getDocStatus();
        if (Objects.equals(status, ErpInvConstants.DOC_STATUS_DONE)
                || Objects.equals(status, ErpInvConstants.DOC_STATUS_CANCELLED)) {
            throw new NopException(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION)
                    .param(ErpInvErrors.ARG_TAKE_ID, takeId)
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status);
        }
        take.setDocStatus(ErpInvConstants.DOC_STATUS_CANCELLED);
        updateEntity(take, null, context);
        return take;
    }
}
