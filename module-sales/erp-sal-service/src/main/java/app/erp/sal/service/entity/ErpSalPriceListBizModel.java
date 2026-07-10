
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.sal.biz.IErpSalPriceListBiz;
import app.erp.sal.dao.entity.ErpSalPriceList;

@BizModel("ErpSalPriceList")
public class ErpSalPriceListBizModel extends CrudBizModel<ErpSalPriceList> implements IErpSalPriceListBiz {
    public ErpSalPriceListBizModel() {
        setEntityName(ErpSalPriceList.class.getName());
    }

    @Override
    protected void defaultPrepareQuery(QueryBean query, IServiceContext context) {
        super.defaultPrepareQuery(query, context);
    }
}
