
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmForecastPeriodBiz;
import app.erp.crm.dao.entity.ErpCrmForecastPeriod;

@BizModel("ErpCrmForecastPeriod")
public class ErpCrmForecastPeriodBizModel extends CrudBizModel<ErpCrmForecastPeriod> implements IErpCrmForecastPeriodBiz{
    public ErpCrmForecastPeriodBizModel(){
        setEntityName(ErpCrmForecastPeriod.class.getName());
    }
}
