
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvSerialNumberBiz;
import app.erp.inv.dao.entity.ErpInvSerialNumber;

@BizModel("ErpInvSerialNumber")
public class ErpInvSerialNumberBizModel extends CrudBizModel<ErpInvSerialNumber> implements IErpInvSerialNumberBiz{
    public ErpInvSerialNumberBizModel(){
        setEntityName(ErpInvSerialNumber.class.getName());
    }
}
