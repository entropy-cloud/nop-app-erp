
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsContractBiz;
import app.erp.cs.dao.entity.ErpCsContract;

@BizModel("ErpCsContract")
public class ErpCsContractBizModel extends CrudBizModel<ErpCsContract> implements IErpCsContractBiz{
    public ErpCsContractBizModel(){
        setEntityName(ErpCsContract.class.getName());
    }
}
