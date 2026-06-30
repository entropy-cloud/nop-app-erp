
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrRecruitmentBiz;
import app.erp.hr.dao.entity.ErpHrRecruitment;

@BizModel("ErpHrRecruitment")
public class ErpHrRecruitmentBizModel extends CrudBizModel<ErpHrRecruitment> implements IErpHrRecruitmentBiz{
    public ErpHrRecruitmentBizModel(){
        setEntityName(ErpHrRecruitment.class.getName());
    }
}
