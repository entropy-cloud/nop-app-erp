
package app.erp.b2b.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bAsnBiz;
import app.erp.b2b.dao.entity.ErpB2bAsn;

@BizModel("ErpB2bAsn")
public class ErpB2bAsnBizModel extends CrudBizModel<ErpB2bAsn> implements IErpB2bAsnBiz{
    public ErpB2bAsnBizModel(){
        setEntityName(ErpB2bAsn.class.getName());
    }
}
