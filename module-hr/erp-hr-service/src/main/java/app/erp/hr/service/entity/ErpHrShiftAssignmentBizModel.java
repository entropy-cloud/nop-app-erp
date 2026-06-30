
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;

@BizModel("ErpHrShiftAssignment")
public class ErpHrShiftAssignmentBizModel extends CrudBizModel<ErpHrShiftAssignment> implements IErpHrShiftAssignmentBiz{
    public ErpHrShiftAssignmentBizModel(){
        setEntityName(ErpHrShiftAssignment.class.getName());
    }
}
