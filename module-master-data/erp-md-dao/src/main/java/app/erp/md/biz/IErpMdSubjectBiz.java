
package app.erp.md.biz;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.md.dao.entity.ErpMdSubject;

public interface IErpMdSubjectBiz extends ICrudBiz<ErpMdSubject> {

    /**
     * 按科目代码解析会计科目，不存在返回 null。供 finance 过账引擎按 subjectCode 解析 subjectId。
     */
    @BizAction
    ErpMdSubject findByCode(@Name("code") String code, IServiceContext context);

    /**
     * F7 §3 编码唯一性前置校验（async validator on blur 调用入口）。
     *
     * @param code       待校验编码
     * @param excludeId  edit 模式排除自身 ID（add 模式传 null）
     * @return true 表示编码可用（无冲突）；false 表示已被其他记录占用
     */
    @BizQuery
    boolean isCodeUnique(@Name("code") String code,
                         @Optional @Name("excludeId") Long excludeId,
                         IServiceContext context);
}
