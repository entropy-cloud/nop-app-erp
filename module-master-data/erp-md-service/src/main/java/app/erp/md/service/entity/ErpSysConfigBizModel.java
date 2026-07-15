
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpSysConfigBiz;
import app.erp.md.dao.entity.ErpSysConfig;

@BizModel("ErpSysConfig")
public class ErpSysConfigBizModel extends CrudBizModel<ErpSysConfig> implements IErpSysConfigBiz{
    public ErpSysConfigBizModel(){
        setEntityName(ErpSysConfig.class.getName());
    }
}
