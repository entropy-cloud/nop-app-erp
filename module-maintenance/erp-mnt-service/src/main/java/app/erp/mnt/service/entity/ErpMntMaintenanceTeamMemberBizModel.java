
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntMaintenanceTeamMemberBiz;
import app.erp.mnt.dao.entity.ErpMntMaintenanceTeamMember;

@BizModel("ErpMntMaintenanceTeamMember")
public class ErpMntMaintenanceTeamMemberBizModel extends CrudBizModel<ErpMntMaintenanceTeamMember> implements IErpMntMaintenanceTeamMemberBiz{
    public ErpMntMaintenanceTeamMemberBizModel(){
        setEntityName(ErpMntMaintenanceTeamMember.class.getName());
    }
}
