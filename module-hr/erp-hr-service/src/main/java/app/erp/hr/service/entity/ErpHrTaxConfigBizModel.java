
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrTaxConfigBiz;
import app.erp.hr.dao.entity.ErpHrTaxConfig;

import java.util.List;

@BizModel("ErpHrTaxConfig")
public class ErpHrTaxConfigBizModel extends CrudBizModel<ErpHrTaxConfig> implements IErpHrTaxConfigBiz{
    public ErpHrTaxConfigBizModel(){
        setEntityName(ErpHrTaxConfig.class.getName());
    }

}
