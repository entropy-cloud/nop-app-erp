
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjTaskBiz;
import app.erp.prj.dao.entity.ErpPrjTask;

@BizModel("ErpPrjTask")
public class ErpPrjTaskBizModel extends CrudBizModel<ErpPrjTask> implements IErpPrjTaskBiz{
    public ErpPrjTaskBizModel(){
        setEntityName(ErpPrjTask.class.getName());
    }
}
