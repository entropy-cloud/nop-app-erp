
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrDevelopmentPlanItemBiz;
import app.erp.hr.dao.entity.ErpHrDevelopmentPlanItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrDevelopmentPlanItem")
public class ErpHrDevelopmentPlanItemBizModel extends CrudBizModel<ErpHrDevelopmentPlanItem> implements IErpHrDevelopmentPlanItemBiz{
    public ErpHrDevelopmentPlanItemBizModel(){
        setEntityName(ErpHrDevelopmentPlanItem.class.getName());
    }

    @BizLoader(forType = ErpHrDevelopmentPlanItem.class)
    public List<String> planName(@ContextSource List<ErpHrDevelopmentPlanItem> rows) {
        orm().batchLoadProps(rows, Collections.singleton("plan"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrDevelopmentPlanItem row : rows) {
            result.add(row.orm_attached() && row.getPlan() != null ? row.getPlan().getPlanName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrDevelopmentPlanItem.class)
    public List<String> competencyName(@ContextSource List<ErpHrDevelopmentPlanItem> rows) {
        orm().batchLoadProps(rows, Collections.singleton("competency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrDevelopmentPlanItem row : rows) {
            result.add(row.orm_attached() && row.getCompetency() != null ? row.getCompetency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrDevelopmentPlanItem.class)
    public List<String> mentorDisplayName(@ContextSource List<ErpHrDevelopmentPlanItem> rows) {
        orm().batchLoadProps(rows, Collections.singleton("mentor"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrDevelopmentPlanItem row : rows) {
            result.add(row.orm_attached() && row.getMentor() != null ? row.getMentor().getFullName() : null);
        }
        return result;
    }
}
