
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntMaintenanceTeamBiz;
import app.erp.mnt.dao.entity.ErpMntMaintenanceTeam;

@BizModel("ErpMntMaintenanceTeam")
public class ErpMntMaintenanceTeamBizModel extends CrudBizModel<ErpMntMaintenanceTeam> implements IErpMntMaintenanceTeamBiz{
    public ErpMntMaintenanceTeamBizModel(){
        setEntityName(ErpMntMaintenanceTeam.class.getName());
    }

    @BizLoader(forType = ErpMntMaintenanceTeam.class)
    public List<String> orgName(@ContextSource List<ErpMntMaintenanceTeam> list) {
        orm().batchLoadProps(list, Collections.singleton("org"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntMaintenanceTeam entity : list) {
            result.add(entity.getOrg() != null ? entity.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMntMaintenanceTeam.class)
    public List<String> leaderName(@ContextSource List<ErpMntMaintenanceTeam> list) {
        orm().batchLoadProps(list, Collections.singleton("leader"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntMaintenanceTeam entity : list) {
            result.add(entity.getLeader() != null ? entity.getLeader().getName() : null);
        }
        return result;
    }
}
