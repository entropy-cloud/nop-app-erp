
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrDepartmentBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;

@BizModel("ErpHrDepartment")
public class ErpHrDepartmentBizModel extends CrudBizModel<ErpHrDepartment> implements IErpHrDepartmentBiz{
    public ErpHrDepartmentBizModel(){
        setEntityName(ErpHrDepartment.class.getName());
    }
}
