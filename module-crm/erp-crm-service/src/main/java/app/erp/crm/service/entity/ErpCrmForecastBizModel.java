
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmForecastBiz;
import app.erp.crm.dao.entity.ErpCrmForecast;

@BizModel("ErpCrmForecast")
public class ErpCrmForecastBizModel extends CrudBizModel<ErpCrmForecast> implements IErpCrmForecastBiz{
    public ErpCrmForecastBizModel(){
        setEntityName(ErpCrmForecast.class.getName());
    }
}
