
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdPartnerAddressBiz;
import app.erp.md.dao.entity.ErpMdPartnerAddress;

@BizModel("ErpMdPartnerAddress")
public class ErpMdPartnerAddressBizModel extends CrudBizModel<ErpMdPartnerAddress> implements IErpMdPartnerAddressBiz{
    public ErpMdPartnerAddressBizModel(){
        setEntityName(ErpMdPartnerAddress.class.getName());
    }
}
