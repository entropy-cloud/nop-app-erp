
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrRoleCompetencyBiz;
import app.erp.hr.dao.entity.ErpHrRoleCompetency;

@BizModel("ErpHrRoleCompetency")
public class ErpHrRoleCompetencyBizModel extends CrudBizModel<ErpHrRoleCompetency> implements IErpHrRoleCompetencyBiz{
    public ErpHrRoleCompetencyBizModel(){
        setEntityName(ErpHrRoleCompetency.class.getName());
    }
}
