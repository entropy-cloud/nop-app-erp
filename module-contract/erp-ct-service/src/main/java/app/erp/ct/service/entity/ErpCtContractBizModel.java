
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtContractBiz;
import app.erp.contract.dao.entity.ErpCtContract;

@BizModel("ErpCtContract")
public class ErpCtContractBizModel extends CrudBizModel<ErpCtContract> implements IErpCtContractBiz{
    public ErpCtContractBizModel(){
        setEntityName(ErpCtContract.class.getName());
    }
}
