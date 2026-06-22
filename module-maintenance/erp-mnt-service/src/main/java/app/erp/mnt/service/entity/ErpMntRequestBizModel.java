
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntRequestBiz;
import app.erp.mnt.dao.entity.ErpMntRequest;

@BizModel("ErpMntRequest")
public class ErpMntRequestBizModel extends CrudBizModel<ErpMntRequest> implements IErpMntRequestBiz{
    public ErpMntRequestBizModel(){
        setEntityName(ErpMntRequest.class.getName());
    }
}
