
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;

@BizModel("ErpHrEmployee")
public class ErpHrEmployeeBizModel extends CrudBizModel<ErpHrEmployee> implements IErpHrEmployeeBiz{
    public ErpHrEmployeeBizModel(){
        setEntityName(ErpHrEmployee.class.getName());
    }
}
