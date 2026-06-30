
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsCannedResponseBiz;
import app.erp.cs.dao.entity.ErpCsCannedResponse;

@BizModel("ErpCsCannedResponse")
public class ErpCsCannedResponseBizModel extends CrudBizModel<ErpCsCannedResponse> implements IErpCsCannedResponseBiz{
    public ErpCsCannedResponseBizModel(){
        setEntityName(ErpCsCannedResponse.class.getName());
    }
}
