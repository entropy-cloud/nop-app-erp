
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bCertificationChecklistBiz;
import app.erp.b2b.dao.entity.ErpB2bCertificationChecklist;

@BizModel("ErpB2bCertificationChecklist")
public class ErpB2bCertificationChecklistBizModel extends CrudBizModel<ErpB2bCertificationChecklist> implements IErpB2bCertificationChecklistBiz{
    public ErpB2bCertificationChecklistBizModel(){
        setEntityName(ErpB2bCertificationChecklist.class.getName());
    }

}
