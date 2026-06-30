
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrTimesheetBiz;
import app.erp.hr.dao.entity.ErpHrTimesheet;

@BizModel("ErpHrTimesheet")
public class ErpHrTimesheetBizModel extends CrudBizModel<ErpHrTimesheet> implements IErpHrTimesheetBiz{
    public ErpHrTimesheetBizModel(){
        setEntityName(ErpHrTimesheet.class.getName());
    }
}
