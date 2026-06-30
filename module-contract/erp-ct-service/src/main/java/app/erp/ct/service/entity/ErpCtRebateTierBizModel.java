
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtRebateTierBiz;
import app.erp.contract.dao.entity.ErpCtRebateTier;

@BizModel("ErpCtRebateTier")
public class ErpCtRebateTierBizModel extends CrudBizModel<ErpCtRebateTier> implements IErpCtRebateTierBiz{
    public ErpCtRebateTierBizModel(){
        setEntityName(ErpCtRebateTier.class.getName());
    }
}
