
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdPartnerContactBiz;
import app.erp.md.dao.entity.ErpMdPartnerContact;

@BizModel("ErpMdPartnerContact")
public class ErpMdPartnerContactBizModel extends CrudBizModel<ErpMdPartnerContact> implements IErpMdPartnerContactBiz{
    public ErpMdPartnerContactBizModel(){
        setEntityName(ErpMdPartnerContact.class.getName());
    }
}
