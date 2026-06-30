
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrAttendanceBiz;
import app.erp.hr.dao.entity.ErpHrAttendance;

@BizModel("ErpHrAttendance")
public class ErpHrAttendanceBizModel extends CrudBizModel<ErpHrAttendance> implements IErpHrAttendanceBiz{
    public ErpHrAttendanceBizModel(){
        setEntityName(ErpHrAttendance.class.getName());
    }
}
