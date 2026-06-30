
package app.erp.drp.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpInvDrpSafetyStockCalcBiz;
import app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc;

@BizModel("ErpInvDrpSafetyStockCalc")
public class ErpInvDrpSafetyStockCalcBizModel extends CrudBizModel<ErpInvDrpSafetyStockCalc> implements IErpInvDrpSafetyStockCalcBiz{
    public ErpInvDrpSafetyStockCalcBizModel(){
        setEntityName(ErpInvDrpSafetyStockCalc.class.getName());
    }
}
