
package app.erp.ast.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ast.biz.IErpAstCipProgressBillingBiz;
import app.erp.ast.dao.entity.ErpAstCipProgressBilling;

@BizModel("ErpAstCipProgressBilling")
public class ErpAstCipProgressBillingBizModel extends CrudBizModel<ErpAstCipProgressBilling> implements IErpAstCipProgressBillingBiz{
    public ErpAstCipProgressBillingBizModel(){
        setEntityName(ErpAstCipProgressBilling.class.getName());
    }
}
