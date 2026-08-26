
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.crm.biz.IErpCrmForecastAccuracyBiz;
import app.erp.crm.dao.entity.ErpCrmForecastAccuracy;
import java.util.List;

@BizModel("ErpCrmForecastAccuracy")
public class ErpCrmForecastAccuracyBizModel extends AbstractErpCrudBizModel<ErpCrmForecastAccuracy> implements IErpCrmForecastAccuracyBiz{
    public ErpCrmForecastAccuracyBizModel(){
        setEntityName(ErpCrmForecastAccuracy.class.getName());
    }

    

}
