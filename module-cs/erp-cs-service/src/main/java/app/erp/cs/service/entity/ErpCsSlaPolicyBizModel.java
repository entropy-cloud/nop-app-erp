
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsSlaPolicyBiz;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;

@BizModel("ErpCsSlaPolicy")
public class ErpCsSlaPolicyBizModel extends CrudBizModel<ErpCsSlaPolicy> implements IErpCsSlaPolicyBiz{
    public ErpCsSlaPolicyBizModel(){
        setEntityName(ErpCsSlaPolicy.class.getName());
    }
}
