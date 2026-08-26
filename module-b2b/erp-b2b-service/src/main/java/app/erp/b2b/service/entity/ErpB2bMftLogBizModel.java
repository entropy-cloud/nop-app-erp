
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.b2b.biz.IErpB2bMftLogBiz;
import app.erp.b2b.dao.entity.ErpB2bMftLog;

@BizModel("ErpB2bMftLog")
public class ErpB2bMftLogBizModel extends AbstractErpCrudBizModel<ErpB2bMftLog> implements IErpB2bMftLogBiz{
    public ErpB2bMftLogBizModel(){
        setEntityName(ErpB2bMftLog.class.getName());
    }

}
