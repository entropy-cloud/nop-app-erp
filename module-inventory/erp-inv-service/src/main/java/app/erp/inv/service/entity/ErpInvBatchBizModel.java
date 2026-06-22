
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvBatchBiz;
import app.erp.inv.dao.entity.ErpInvBatch;

@BizModel("ErpInvBatch")
public class ErpInvBatchBizModel extends CrudBizModel<ErpInvBatch> implements IErpInvBatchBiz{
    public ErpInvBatchBizModel(){
        setEntityName(ErpInvBatch.class.getName());
    }
}
