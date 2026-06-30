
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrEmploymentContractBiz;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;

@BizModel("ErpHrEmploymentContract")
public class ErpHrEmploymentContractBizModel extends CrudBizModel<ErpHrEmploymentContract> implements IErpHrEmploymentContractBiz{
    public ErpHrEmploymentContractBizModel(){
        setEntityName(ErpHrEmploymentContract.class.getName());
    }
}
