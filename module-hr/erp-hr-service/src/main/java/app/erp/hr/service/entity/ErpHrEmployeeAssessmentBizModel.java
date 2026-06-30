
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrEmployeeAssessmentBiz;
import app.erp.hr.dao.entity.ErpHrEmployeeAssessment;

@BizModel("ErpHrEmployeeAssessment")
public class ErpHrEmployeeAssessmentBizModel extends CrudBizModel<ErpHrEmployeeAssessment> implements IErpHrEmployeeAssessmentBiz{
    public ErpHrEmployeeAssessmentBizModel(){
        setEntityName(ErpHrEmployeeAssessment.class.getName());
    }
}
