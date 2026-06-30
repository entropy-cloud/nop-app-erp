
package app.erp.drp.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpInvDrpCrossDockBiz;
import app.erp.drp.dao.entity.ErpInvDrpCrossDock;

@BizModel("ErpInvDrpCrossDock")
public class ErpInvDrpCrossDockBizModel extends CrudBizModel<ErpInvDrpCrossDock> implements IErpInvDrpCrossDockBiz{
    public ErpInvDrpCrossDockBizModel(){
        setEntityName(ErpInvDrpCrossDock.class.getName());
    }
}
