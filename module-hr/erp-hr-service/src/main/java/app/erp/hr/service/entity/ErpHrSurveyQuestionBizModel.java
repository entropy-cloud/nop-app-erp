
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSurveyQuestionBiz;
import app.erp.hr.dao.entity.ErpHrSurveyQuestion;

import java.util.List;

@BizModel("ErpHrSurveyQuestion")
public class ErpHrSurveyQuestionBizModel extends CrudBizModel<ErpHrSurveyQuestion> implements IErpHrSurveyQuestionBiz{
    public ErpHrSurveyQuestionBizModel(){
        setEntityName(ErpHrSurveyQuestion.class.getName());
    }

}
