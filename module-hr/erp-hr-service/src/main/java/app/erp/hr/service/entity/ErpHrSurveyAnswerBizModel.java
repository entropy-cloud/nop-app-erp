
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSurveyAnswerBiz;
import app.erp.hr.dao.entity.ErpHrSurveyAnswer;

import java.util.List;

@BizModel("ErpHrSurveyAnswer")
public class ErpHrSurveyAnswerBizModel extends CrudBizModel<ErpHrSurveyAnswer> implements IErpHrSurveyAnswerBiz{
    public ErpHrSurveyAnswerBizModel(){
        setEntityName(ErpHrSurveyAnswer.class.getName());
    }

}
