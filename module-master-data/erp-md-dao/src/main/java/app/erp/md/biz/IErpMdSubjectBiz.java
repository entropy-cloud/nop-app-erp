
package app.erp.md.biz;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.md.dao.entity.ErpMdSubject;

public interface IErpMdSubjectBiz extends ICrudBiz<ErpMdSubject>{

    /**
     * 按科目代码解析会计科目，不存在返回 null。供 finance 过账引擎按 subjectCode 解析 subjectId。
     */
    @BizAction
    ErpMdSubject findByCode(@Name("code") String code, IServiceContext context);
}
