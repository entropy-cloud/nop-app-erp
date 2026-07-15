
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdEmployeeBiz;
import app.erp.md.dao.entity.ErpMdEmployee;

@BizModel("ErpMdEmployee")
public class ErpMdEmployeeBizModel extends CrudBizModel<ErpMdEmployee> implements IErpMdEmployeeBiz{
    public ErpMdEmployeeBizModel(){
        setEntityName(ErpMdEmployee.class.getName());
    }
}
