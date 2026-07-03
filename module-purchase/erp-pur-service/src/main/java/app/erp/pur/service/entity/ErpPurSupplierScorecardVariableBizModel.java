
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurSupplierScorecardVariableBiz;
import app.erp.pur.dao.entity.ErpPurSupplierScorecardVariable;

@BizModel("ErpPurSupplierScorecardVariable")
public class ErpPurSupplierScorecardVariableBizModel extends CrudBizModel<ErpPurSupplierScorecardVariable> implements IErpPurSupplierScorecardVariableBiz{
    public ErpPurSupplierScorecardVariableBizModel(){
        setEntityName(ErpPurSupplierScorecardVariable.class.getName());
    }
}
