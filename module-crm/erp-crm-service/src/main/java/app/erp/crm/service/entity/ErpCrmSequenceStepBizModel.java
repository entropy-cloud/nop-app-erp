
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmSequenceStepBiz;
import app.erp.crm.dao.entity.ErpCrmSequenceStep;
import java.util.List;

@BizModel("ErpCrmSequenceStep")
public class ErpCrmSequenceStepBizModel extends CrudBizModel<ErpCrmSequenceStep> implements IErpCrmSequenceStepBiz{
    public ErpCrmSequenceStepBizModel(){
        setEntityName(ErpCrmSequenceStep.class.getName());
    }

    

}
