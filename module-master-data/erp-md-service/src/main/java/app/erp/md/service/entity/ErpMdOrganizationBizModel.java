
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdOrganizationBiz;
import app.erp.md.dao.entity.ErpMdOrganization;

@BizModel("ErpMdOrganization")
public class ErpMdOrganizationBizModel extends CrudBizModel<ErpMdOrganization> implements IErpMdOrganizationBiz{
    public ErpMdOrganizationBizModel(){
        setEntityName(ErpMdOrganization.class.getName());
    }
}
