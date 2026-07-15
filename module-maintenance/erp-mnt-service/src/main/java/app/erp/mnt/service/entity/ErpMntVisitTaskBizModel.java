
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import java.util.List;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntVisitTaskBiz;
import app.erp.mnt.dao.entity.ErpMntVisitTask;

@BizModel("ErpMntVisitTask")
public class ErpMntVisitTaskBizModel extends CrudBizModel<ErpMntVisitTask> implements IErpMntVisitTaskBiz{
    public ErpMntVisitTaskBizModel(){
        setEntityName(ErpMntVisitTask.class.getName());
    }

}
