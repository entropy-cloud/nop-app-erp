
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsTeamBiz;
import app.erp.cs.dao.entity.ErpCsTeam;

@BizModel("ErpCsTeam")
public class ErpCsTeamBizModel extends CrudBizModel<ErpCsTeam> implements IErpCsTeamBiz{
    public ErpCsTeamBizModel(){
        setEntityName(ErpCsTeam.class.getName());
    }
}
