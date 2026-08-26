
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.mfg.biz.IErpMfgJobCardTimeLogBiz;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;

@BizModel("ErpMfgJobCardTimeLog")
public class ErpMfgJobCardTimeLogBizModel extends AbstractErpCrudBizModel<ErpMfgJobCardTimeLog> implements IErpMfgJobCardTimeLogBiz{
    public ErpMfgJobCardTimeLogBizModel(){
        setEntityName(ErpMfgJobCardTimeLog.class.getName());
    }
}
