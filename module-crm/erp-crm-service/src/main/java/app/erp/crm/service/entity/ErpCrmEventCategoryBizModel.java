
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.crm.biz.IErpCrmEventCategoryBiz;
import app.erp.crm.dao.entity.ErpCrmEventCategory;

@BizModel("ErpCrmEventCategory")
public class ErpCrmEventCategoryBizModel extends AbstractErpCrudBizModel<ErpCrmEventCategory> implements IErpCrmEventCategoryBiz{
    public ErpCrmEventCategoryBizModel(){
        setEntityName(ErpCrmEventCategory.class.getName());
    }
}
