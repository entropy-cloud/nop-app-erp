
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntVisitBiz;
import app.erp.mnt.dao.entity.ErpMntVisit;

@BizModel("ErpMntVisit")
public class ErpMntVisitBizModel extends CrudBizModel<ErpMntVisit> implements IErpMntVisitBiz{
    public ErpMntVisitBizModel(){
        setEntityName(ErpMntVisit.class.getName());
    }
}
