
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSocialInsuranceConfigBiz;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceConfig;

import java.util.List;

@BizModel("ErpHrSocialInsuranceConfig")
public class ErpHrSocialInsuranceConfigBizModel extends CrudBizModel<ErpHrSocialInsuranceConfig> implements IErpHrSocialInsuranceConfigBiz{
    public ErpHrSocialInsuranceConfigBizModel(){
        setEntityName(ErpHrSocialInsuranceConfig.class.getName());
    }

}
