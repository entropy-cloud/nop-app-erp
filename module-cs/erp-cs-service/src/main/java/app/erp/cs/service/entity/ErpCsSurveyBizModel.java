
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsSurveyBiz;
import app.erp.cs.dao.entity.ErpCsSurvey;

@BizModel("ErpCsSurvey")
public class ErpCsSurveyBizModel extends CrudBizModel<ErpCsSurvey> implements IErpCsSurveyBiz{
    public ErpCsSurveyBizModel(){
        setEntityName(ErpCsSurvey.class.getName());
    }
}
