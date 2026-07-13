
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaQualityGoalBiz;
import app.erp.qa.dao.entity.ErpQaQualityGoal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpQaQualityGoal")
public class ErpQaQualityGoalBizModel extends CrudBizModel<ErpQaQualityGoal> implements IErpQaQualityGoalBiz{
    public ErpQaQualityGoalBizModel(){
        setEntityName(ErpQaQualityGoal.class.getName());
    }

    @BizLoader(forType = ErpQaQualityGoal.class)
    public List<String> responsiblePersonName(@ContextSource List<ErpQaQualityGoal> list) {
        orm().batchLoadProps(list, Collections.singleton("responsiblePerson"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaQualityGoal entity : list) {
            result.add(entity.getResponsiblePerson() != null ? entity.getResponsiblePerson().getName() : null);
        }
        return result;
    }
}
