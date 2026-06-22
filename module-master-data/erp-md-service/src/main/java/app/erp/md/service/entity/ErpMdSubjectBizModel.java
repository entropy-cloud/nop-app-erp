
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdSubjectBiz;
import app.erp.md.dao.entity.ErpMdSubject;

@BizModel("ErpMdSubject")
public class ErpMdSubjectBizModel extends CrudBizModel<ErpMdSubject> implements IErpMdSubjectBiz{
    public ErpMdSubjectBizModel(){
        setEntityName(ErpMdSubject.class.getName());
    }
}
