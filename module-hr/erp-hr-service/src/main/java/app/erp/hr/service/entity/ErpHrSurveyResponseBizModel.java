
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSurveyResponseBiz;
import app.erp.hr.dao.entity.ErpHrSurveyResponse;

@BizModel("ErpHrSurveyResponse")
public class ErpHrSurveyResponseBizModel extends CrudBizModel<ErpHrSurveyResponse> implements IErpHrSurveyResponseBiz{
    public ErpHrSurveyResponseBizModel(){
        setEntityName(ErpHrSurveyResponse.class.getName());
    }
}
