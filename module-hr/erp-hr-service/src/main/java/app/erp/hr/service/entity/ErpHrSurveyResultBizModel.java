
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSurveyResultBiz;
import app.erp.hr.dao.entity.ErpHrSurveyResult;

import java.util.List;

@BizModel("ErpHrSurveyResult")
public class ErpHrSurveyResultBizModel extends CrudBizModel<ErpHrSurveyResult> implements IErpHrSurveyResultBiz{
    public ErpHrSurveyResultBizModel(){
        setEntityName(ErpHrSurveyResult.class.getName());
    }

}
