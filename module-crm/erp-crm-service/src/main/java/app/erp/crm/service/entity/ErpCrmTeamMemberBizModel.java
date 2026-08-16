
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmTeamMemberBiz;
import app.erp.crm.dao.entity.ErpCrmTeamMember;

@BizModel("ErpCrmTeamMember")
public class ErpCrmTeamMemberBizModel extends CrudBizModel<ErpCrmTeamMember> implements IErpCrmTeamMemberBiz{
    public ErpCrmTeamMemberBizModel(){
        setEntityName(ErpCrmTeamMember.class.getName());
    }
}
