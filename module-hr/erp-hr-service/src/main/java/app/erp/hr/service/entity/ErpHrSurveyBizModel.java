
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSurveyBiz;
import app.erp.hr.dao.entity.ErpHrSurvey;

import java.util.List;

@BizModel("ErpHrSurvey")
public class ErpHrSurveyBizModel extends CrudBizModel<ErpHrSurvey> implements IErpHrSurveyBiz{
    public ErpHrSurveyBizModel(){
        setEntityName(ErpHrSurvey.class.getName());
    }

}
