
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjRoleBiz;
import app.erp.prj.dao.entity.ErpPrjRole;

@BizModel("ErpPrjRole")
public class ErpPrjRoleBizModel extends CrudBizModel<ErpPrjRole> implements IErpPrjRoleBiz{
    public ErpPrjRoleBizModel(){
        setEntityName(ErpPrjRole.class.getName());
    }
}
