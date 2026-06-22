
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntScheduleBiz;
import app.erp.mnt.dao.entity.ErpMntSchedule;

@BizModel("ErpMntSchedule")
public class ErpMntScheduleBizModel extends CrudBizModel<ErpMntSchedule> implements IErpMntScheduleBiz{
    public ErpMntScheduleBizModel(){
        setEntityName(ErpMntSchedule.class.getName());
    }
}
