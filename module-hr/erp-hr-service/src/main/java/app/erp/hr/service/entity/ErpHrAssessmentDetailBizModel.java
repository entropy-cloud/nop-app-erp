
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrAssessmentDetailBiz;
import app.erp.hr.dao.entity.ErpHrAssessmentDetail;

@BizModel("ErpHrAssessmentDetail")
public class ErpHrAssessmentDetailBizModel extends CrudBizModel<ErpHrAssessmentDetail> implements IErpHrAssessmentDetailBiz{
    public ErpHrAssessmentDetailBizModel(){
        setEntityName(ErpHrAssessmentDetail.class.getName());
    }
}
