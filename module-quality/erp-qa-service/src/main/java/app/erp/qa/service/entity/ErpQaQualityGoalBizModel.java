
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaQualityGoalBiz;
import app.erp.qa.dao.entity.ErpQaQualityGoal;

import java.util.List;

@BizModel("ErpQaQualityGoal")
public class ErpQaQualityGoalBizModel extends CrudBizModel<ErpQaQualityGoal> implements IErpQaQualityGoalBiz{
    public ErpQaQualityGoalBizModel(){
        setEntityName(ErpQaQualityGoal.class.getName());
    }

}
