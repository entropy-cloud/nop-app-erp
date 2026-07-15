
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmCampaignBiz;
import app.erp.crm.dao.entity.ErpCrmCampaign;
import java.util.List;

@BizModel("ErpCrmCampaign")
public class ErpCrmCampaignBizModel extends CrudBizModel<ErpCrmCampaign> implements IErpCrmCampaignBiz{
    public ErpCrmCampaignBizModel(){
        setEntityName(ErpCrmCampaign.class.getName());
    }

    

}
