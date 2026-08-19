
package app.erp.drp.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpInvDrpSupplierScoreBiz;
import app.erp.drp.dao.entity.ErpInvDrpSupplierScore;

@BizModel("ErpInvDrpSupplierScore")
public class ErpInvDrpSupplierScoreBizModel extends CrudBizModel<ErpInvDrpSupplierScore> implements IErpInvDrpSupplierScoreBiz{
    public ErpInvDrpSupplierScoreBizModel(){
        setEntityName(ErpInvDrpSupplierScore.class.getName());
    }
}
