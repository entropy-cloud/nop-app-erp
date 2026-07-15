
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdCostCenterBiz;
import app.erp.md.dao.entity.ErpMdCostCenter;

@BizModel("ErpMdCostCenter")
public class ErpMdCostCenterBizModel extends CrudBizModel<ErpMdCostCenter> implements IErpMdCostCenterBiz{
    public ErpMdCostCenterBizModel(){
        setEntityName(ErpMdCostCenter.class.getName());
    }
}
