
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.pur.biz.IErpPurSupplierScorecardCriteriaBiz;
import app.erp.pur.dao.entity.ErpPurSupplierScorecardCriteria;

@BizModel("ErpPurSupplierScorecardCriteria")
public class ErpPurSupplierScorecardCriteriaBizModel extends AbstractErpCrudBizModel<ErpPurSupplierScorecardCriteria> implements IErpPurSupplierScorecardCriteriaBiz{
    public ErpPurSupplierScorecardCriteriaBizModel(){
        setEntityName(ErpPurSupplierScorecardCriteria.class.getName());
    }
}
