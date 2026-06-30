
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrTimesheetLineBiz;
import app.erp.hr.dao.entity.ErpHrTimesheetLine;

@BizModel("ErpHrTimesheetLine")
public class ErpHrTimesheetLineBizModel extends CrudBizModel<ErpHrTimesheetLine> implements IErpHrTimesheetLineBiz{
    public ErpHrTimesheetLineBizModel(){
        setEntityName(ErpHrTimesheetLine.class.getName());
    }
}
