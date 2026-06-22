
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvTransferOrderLineBiz;
import app.erp.inv.dao.entity.ErpInvTransferOrderLine;

@BizModel("ErpInvTransferOrderLine")
public class ErpInvTransferOrderLineBizModel extends CrudBizModel<ErpInvTransferOrderLine> implements IErpInvTransferOrderLineBiz{
    public ErpInvTransferOrderLineBizModel(){
        setEntityName(ErpInvTransferOrderLine.class.getName());
    }
}
