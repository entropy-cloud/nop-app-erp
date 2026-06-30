
package app.erp.aps.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.aps.biz.IErpApsDispatchLogBiz;
import app.erp.aps.dao.entity.ErpApsDispatchLog;

@BizModel("ErpApsDispatchLog")
public class ErpApsDispatchLogBizModel extends CrudBizModel<ErpApsDispatchLog> implements IErpApsDispatchLogBiz{
    public ErpApsDispatchLogBizModel(){
        setEntityName(ErpApsDispatchLog.class.getName());
    }
}
