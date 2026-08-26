
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.sal.biz.IErpSalContractBiz;
import app.erp.sal.dao.entity.ErpSalContract;

@BizModel("ErpSalContract")
public class ErpSalContractBizModel extends AbstractErpCrudBizModel<ErpSalContract> implements IErpSalContractBiz{
    public ErpSalContractBizModel(){
        setEntityName(ErpSalContract.class.getName());
    }
}
