
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.md.biz.IErpMdSubjectBiz;
import app.erp.md.dao.entity.ErpMdSubject;

@BizModel("ErpMdSubject")
public class ErpMdSubjectBizModel extends CrudBizModel<ErpMdSubject> implements IErpMdSubjectBiz{
    public ErpMdSubjectBizModel(){
        setEntityName(ErpMdSubject.class.getName());
    }

    @Override
    @BizAction
    public ErpMdSubject findByCode(@Name("code") String code, IServiceContext context) {
        if (code == null) {
            return null;
        }
        ErpMdSubject example = dao().newEntity();
        example.setCode(code);
        return dao().findFirstByExample(example);
    }
}
