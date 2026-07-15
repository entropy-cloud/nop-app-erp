
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtConsumptionLineBiz;
import app.erp.contract.dao.entity.ErpCtConsumptionLine;

@BizModel("ErpCtConsumptionLine")
public class ErpCtConsumptionLineBizModel extends CrudBizModel<ErpCtConsumptionLine> implements IErpCtConsumptionLineBiz{
    public ErpCtConsumptionLineBizModel(){
        setEntityName(ErpCtConsumptionLine.class.getName());
    }

}
