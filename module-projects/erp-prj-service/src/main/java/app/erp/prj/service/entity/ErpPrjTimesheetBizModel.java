
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjTimesheetBiz;
import app.erp.prj.dao.entity.ErpPrjTimesheet;

@BizModel("ErpPrjTimesheet")
public class ErpPrjTimesheetBizModel extends CrudBizModel<ErpPrjTimesheet> implements IErpPrjTimesheetBiz{
    public ErpPrjTimesheetBizModel(){
        setEntityName(ErpPrjTimesheet.class.getName());
    }
}
