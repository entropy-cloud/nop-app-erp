
package app.erp.aps.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.aps.biz.IErpApsScheduleBiz;
import app.erp.aps.dao.entity.ErpApsSchedule;

@BizModel("ErpApsSchedule")
public class ErpApsScheduleBizModel extends CrudBizModel<ErpApsSchedule> implements IErpApsScheduleBiz{
    public ErpApsScheduleBizModel(){
        setEntityName(ErpApsSchedule.class.getName());
    }
}
