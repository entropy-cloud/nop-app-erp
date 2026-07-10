
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.sal.biz.IErpSalPricingRuleBiz;
import app.erp.sal.dao.entity.ErpSalPricingRule;

@BizModel("ErpSalPricingRule")
public class ErpSalPricingRuleBizModel extends CrudBizModel<ErpSalPricingRule> implements IErpSalPricingRuleBiz {
    public ErpSalPricingRuleBizModel() {
        setEntityName(ErpSalPricingRule.class.getName());
    }

    @Override
    protected void defaultPrepareQuery(QueryBean query, IServiceContext context) {
        super.defaultPrepareQuery(query, context);
    }
}
