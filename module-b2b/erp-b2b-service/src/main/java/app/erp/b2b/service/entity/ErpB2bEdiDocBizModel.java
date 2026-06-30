
package app.erp.b2b.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiDoc;

@BizModel("ErpB2bEdiDoc")
public class ErpB2bEdiDocBizModel extends CrudBizModel<ErpB2bEdiDoc> implements IErpB2bEdiDocBiz{
    public ErpB2bEdiDocBizModel(){
        setEntityName(ErpB2bEdiDoc.class.getName());
    }
}
