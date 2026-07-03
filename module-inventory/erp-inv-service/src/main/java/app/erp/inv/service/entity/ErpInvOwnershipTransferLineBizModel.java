
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvOwnershipTransferLineBiz;
import app.erp.inv.dao.entity.ErpInvOwnershipTransferLine;

@BizModel("ErpInvOwnershipTransferLine")
public class ErpInvOwnershipTransferLineBizModel extends CrudBizModel<ErpInvOwnershipTransferLine> implements IErpInvOwnershipTransferLineBiz{
    public ErpInvOwnershipTransferLineBizModel(){
        setEntityName(ErpInvOwnershipTransferLine.class.getName());
    }
}
