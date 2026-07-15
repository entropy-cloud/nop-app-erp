
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtDocumentBiz;
import app.erp.contract.dao.entity.ErpCtDocument;

@BizModel("ErpCtDocument")
public class ErpCtDocumentBizModel extends CrudBizModel<ErpCtDocument> implements IErpCtDocumentBiz{
    public ErpCtDocumentBizModel(){
        setEntityName(ErpCtDocument.class.getName());
    }

}
