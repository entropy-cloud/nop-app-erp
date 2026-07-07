
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.md.biz.IErpMdAcctSchemaBiz;
import app.erp.md.dao.entity.ErpMdAcctSchema;

import static io.nop.api.core.beans.FilterBeans.eq;

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
        // O-5：改 findFirstByExample 为 findFirstByQuery + code 排序确保确定性
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        q.addOrderField("code", false);
        return dao().findFirstByQuery(q);
    }
}
