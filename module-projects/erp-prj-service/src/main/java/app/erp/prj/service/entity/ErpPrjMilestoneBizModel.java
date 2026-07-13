
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjMilestoneBiz;
import app.erp.prj.dao.entity.ErpPrjMilestone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPrjMilestone")
public class ErpPrjMilestoneBizModel extends CrudBizModel<ErpPrjMilestone> implements IErpPrjMilestoneBiz{
    public ErpPrjMilestoneBizModel(){
        setEntityName(ErpPrjMilestone.class.getName());
    }

    @BizLoader(forType = ErpPrjMilestone.class)
    public List<String> projectName(@ContextSource List<ErpPrjMilestone> milestones) {
        orm().batchLoadProps(milestones, Collections.singleton("project"));
        List<String> result = new ArrayList<>(milestones.size());
        for (ErpPrjMilestone milestone : milestones) {
            result.add(milestone.getProject() != null ? milestone.getProject().getName() : null);
        }
        return result;
    }
}
