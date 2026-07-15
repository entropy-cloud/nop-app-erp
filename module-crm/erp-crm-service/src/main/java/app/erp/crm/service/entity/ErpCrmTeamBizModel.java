
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmTeamBiz;
import app.erp.crm.dao.entity.ErpCrmTeam;
import java.util.List;

@BizModel("ErpCrmTeam")
public class ErpCrmTeamBizModel extends CrudBizModel<ErpCrmTeam> implements IErpCrmTeamBiz{
    public ErpCrmTeamBizModel(){
        setEntityName(ErpCrmTeam.class.getName());
    }

    

}
