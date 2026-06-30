
package app.erp.b2b.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bEdiLogBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiLog;

@BizModel("ErpB2bEdiLog")
public class ErpB2bEdiLogBizModel extends CrudBizModel<ErpB2bEdiLog> implements IErpB2bEdiLogBiz{
    public ErpB2bEdiLogBizModel(){
        setEntityName(ErpB2bEdiLog.class.getName());
    }
}
