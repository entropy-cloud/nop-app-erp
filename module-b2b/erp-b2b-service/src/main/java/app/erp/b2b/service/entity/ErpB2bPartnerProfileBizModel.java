
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bPartnerProfileBiz;
import app.erp.b2b.dao.entity.ErpB2bPartnerProfile;

@BizModel("ErpB2bPartnerProfile")
public class ErpB2bPartnerProfileBizModel extends CrudBizModel<ErpB2bPartnerProfile> implements IErpB2bPartnerProfileBiz{
    public ErpB2bPartnerProfileBizModel(){
        setEntityName(ErpB2bPartnerProfile.class.getName());
    }

}
