
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSocialInsuranceBaseBiz;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceBase;

@BizModel("ErpHrSocialInsuranceBase")
public class ErpHrSocialInsuranceBaseBizModel extends CrudBizModel<ErpHrSocialInsuranceBase> implements IErpHrSocialInsuranceBaseBiz{
    public ErpHrSocialInsuranceBaseBizModel(){
        setEntityName(ErpHrSocialInsuranceBase.class.getName());
    }
}
