
package app.erp.aps.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.aps.biz.IErpApsDispatchLogBiz;
import app.erp.aps.dao.entity.ErpApsDispatchLog;

@BizModel("ErpApsDispatchLog")
public class ErpApsDispatchLogBizModel extends AbstractErpCrudBizModel<ErpApsDispatchLog> implements IErpApsDispatchLogBiz{
    public ErpApsDispatchLogBizModel(){
        setEntityName(ErpApsDispatchLog.class.getName());
    }

}
