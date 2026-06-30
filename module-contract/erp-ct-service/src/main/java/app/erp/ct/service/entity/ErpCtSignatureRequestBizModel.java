
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtSignatureRequestBiz;
import app.erp.contract.dao.entity.ErpCtSignatureRequest;

@BizModel("ErpCtSignatureRequest")
public class ErpCtSignatureRequestBizModel extends CrudBizModel<ErpCtSignatureRequest> implements IErpCtSignatureRequestBiz{
    public ErpCtSignatureRequestBizModel(){
        setEntityName(ErpCtSignatureRequest.class.getName());
    }
}
