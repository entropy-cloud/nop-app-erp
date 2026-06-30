
package app.erp.b2b.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bPartnerCredentialBiz;
import app.erp.b2b.dao.entity.ErpB2bPartnerCredential;

@BizModel("ErpB2bPartnerCredential")
public class ErpB2bPartnerCredentialBizModel extends CrudBizModel<ErpB2bPartnerCredential> implements IErpB2bPartnerCredentialBiz{
    public ErpB2bPartnerCredentialBizModel(){
        setEntityName(ErpB2bPartnerCredential.class.getName());
    }
}
