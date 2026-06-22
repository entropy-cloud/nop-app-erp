
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdSettlementMethodBiz;
import app.erp.md.dao.entity.ErpMdSettlementMethod;

@BizModel("ErpMdSettlementMethod")
public class ErpMdSettlementMethodBizModel extends CrudBizModel<ErpMdSettlementMethod> implements IErpMdSettlementMethodBiz{
    public ErpMdSettlementMethodBizModel(){
        setEntityName(ErpMdSettlementMethod.class.getName());
    }
}
