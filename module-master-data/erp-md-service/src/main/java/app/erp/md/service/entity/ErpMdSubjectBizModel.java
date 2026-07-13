
package app.erp.md.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
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

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpMdSubject.class)
    public List<String> parentName(@ContextSource List<ErpMdSubject> rows) {
        orm().batchLoadProps(rows, Collections.singleton("parent"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdSubject row : rows) {
            result.add(row.orm_attached() && row.getParent() != null ? row.getParent().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMdSubject.class)
    public List<String> currencyName(@ContextSource List<ErpMdSubject> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdSubject row : rows) {
            result.add(row.orm_attached() && row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }

}
