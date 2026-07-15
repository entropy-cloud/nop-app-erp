
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import java.util.List;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntMaintenanceTeamBiz;
import app.erp.mnt.dao.entity.ErpMntMaintenanceTeam;

@BizModel("ErpMntMaintenanceTeam")
public class ErpMntMaintenanceTeamBizModel extends CrudBizModel<ErpMntMaintenanceTeam> implements IErpMntMaintenanceTeamBiz{
    public ErpMntMaintenanceTeamBizModel(){
        setEntityName(ErpMntMaintenanceTeam.class.getName());
    }

}
