
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.dao.entity.ErpHrShift;

@BizModel("ErpHrShift")
public class ErpHrShiftBizModel extends CrudBizModel<ErpHrShift> implements IErpHrShiftBiz{
    public ErpHrShiftBizModel(){
        setEntityName(ErpHrShift.class.getName());
    }
}
