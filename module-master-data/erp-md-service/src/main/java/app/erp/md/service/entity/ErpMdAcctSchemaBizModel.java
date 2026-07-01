
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.md.biz.IErpMdAcctSchemaBiz;
import app.erp.md.dao.entity.ErpMdAcctSchema;

@BizModel("ErpMdAcctSchema")
public class ErpMdAcctSchemaBizModel extends CrudBizModel<ErpMdAcctSchema> implements IErpMdAcctSchemaBiz{
    public ErpMdAcctSchemaBizModel(){
        setEntityName(ErpMdAcctSchema.class.getName());
    }

    @Override
    @BizAction
    public ErpMdAcctSchema findFirstByOrg(@Name("orgId") Long orgId, IServiceContext context) {
        if (orgId == null) {
            return null;
        }
        ErpMdAcctSchema example = dao().newEntity();
        example.setOrgId(orgId);
        return dao().findFirstByExample(example);
    }
}
