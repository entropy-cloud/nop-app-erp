
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.pur.biz.IErpPurSupplierScorecardVariableBiz;
import app.erp.pur.dao.entity.ErpPurSupplierScorecardVariable;

@BizModel("ErpPurSupplierScorecardVariable")
public class ErpPurSupplierScorecardVariableBizModel extends AbstractErpCrudBizModel<ErpPurSupplierScorecardVariable> implements IErpPurSupplierScorecardVariableBiz{
    public ErpPurSupplierScorecardVariableBizModel(){
        setEntityName(ErpPurSupplierScorecardVariable.class.getName());
    }
}
