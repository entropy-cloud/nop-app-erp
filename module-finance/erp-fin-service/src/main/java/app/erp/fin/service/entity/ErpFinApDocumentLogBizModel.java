
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinApDocumentLogBiz;
import app.erp.fin.dao.entity.ErpFinApDocumentLog;

@BizModel("ErpFinApDocumentLog")
public class ErpFinApDocumentLogBizModel extends CrudBizModel<ErpFinApDocumentLog> implements IErpFinApDocumentLogBiz{
    public ErpFinApDocumentLogBizModel(){
        setEntityName(ErpFinApDocumentLog.class.getName());
    }
}
