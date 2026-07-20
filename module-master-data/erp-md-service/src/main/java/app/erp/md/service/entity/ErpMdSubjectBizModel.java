
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.md.biz.IErpMdSubjectBiz;
import app.erp.md.dao.entity.ErpMdSubject;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

@BizModel("ErpMdSubject")
public class ErpMdSubjectBizModel extends CrudBizModel<ErpMdSubject> implements IErpMdSubjectBiz {
    public ErpMdSubjectBizModel() {
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

    /**
     * F7 §3 编码唯一性前置校验。Add 模式 excludeId 传 null；Edit 模式传自身 id 排除自身。
     *
     * <p>实现说明：直接经 {@code dao().findAllByQuery(query)} 查询避免 CrudBizModel 管道的
     * objMeta filter 校验（默认仅允许 eq/in/date-between，ne 在 id 上不被允许）。
     */
    @Override
    @BizQuery
    public boolean isCodeUnique(@Name("code") String code,
                                @Optional @Name("excludeId") Long excludeId,
                                IServiceContext context) {
        if (code == null || code.isEmpty()) {
            return true;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("code", code));
        if (excludeId != null) {
            query.addFilter(ne("id", excludeId));
        }
        return dao().findAllByQuery(query).isEmpty();
    }
}
