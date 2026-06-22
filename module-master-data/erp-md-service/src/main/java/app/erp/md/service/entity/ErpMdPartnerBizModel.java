
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;

@BizModel("ErpMdPartner")
public class ErpMdPartnerBizModel extends CrudBizModel<ErpMdPartner> implements IErpMdPartnerBiz{
    public ErpMdPartnerBizModel(){
        setEntityName(ErpMdPartner.class.getName());
    }
}
