
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntMaintenanceTeamMemberBiz;
import app.erp.mnt.dao.entity.ErpMntMaintenanceTeamMember;

@BizModel("ErpMntMaintenanceTeamMember")
public class ErpMntMaintenanceTeamMemberBizModel extends CrudBizModel<ErpMntMaintenanceTeamMember> implements IErpMntMaintenanceTeamMemberBiz{
    public ErpMntMaintenanceTeamMemberBizModel(){
        setEntityName(ErpMntMaintenanceTeamMember.class.getName());
    }

    @BizLoader(forType = ErpMntMaintenanceTeamMember.class)
    public List<String> teamName(@ContextSource List<ErpMntMaintenanceTeamMember> list) {
        orm().batchLoadProps(list, Collections.singleton("team"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntMaintenanceTeamMember entity : list) {
            result.add(entity.getTeam() != null ? entity.getTeam().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMntMaintenanceTeamMember.class)
    public List<String> employeeName(@ContextSource List<ErpMntMaintenanceTeamMember> list) {
        orm().batchLoadProps(list, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntMaintenanceTeamMember entity : list) {
            result.add(entity.getEmployee() != null ? entity.getEmployee().getName() : null);
        }
        return result;
    }
}
