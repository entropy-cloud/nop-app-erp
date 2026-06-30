
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSalaryBiz;
import app.erp.hr.dao.entity.ErpHrSalary;

@BizModel("ErpHrSalary")
public class ErpHrSalaryBizModel extends CrudBizModel<ErpHrSalary> implements IErpHrSalaryBiz{
    public ErpHrSalaryBizModel(){
        setEntityName(ErpHrSalary.class.getName());
    }
}
