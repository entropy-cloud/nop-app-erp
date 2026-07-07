
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.md.biz.IErpMdSubjectBiz;
import app.erp.md.dao.entity.ErpMdSubject;

import static io.nop.api.core.beans.FilterBeans.eq;

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
        // O-5：改 findFirstByExample 为 findFirstByQuery + code 排序确保确定性
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.addOrderField("code", false);
        return dao().findFirstByQuery(q);
    }
}
