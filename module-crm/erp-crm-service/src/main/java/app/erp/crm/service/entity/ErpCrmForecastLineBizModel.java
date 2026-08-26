
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.crm.biz.IErpCrmForecastLineBiz;
import app.erp.crm.dao.entity.ErpCrmForecastLine;
import java.util.List;

@BizModel("ErpCrmForecastLine")
public class ErpCrmForecastLineBizModel extends AbstractErpCrudBizModel<ErpCrmForecastLine> implements IErpCrmForecastLineBiz{
    public ErpCrmForecastLineBizModel(){
        setEntityName(ErpCrmForecastLine.class.getName());
    }

    

}
