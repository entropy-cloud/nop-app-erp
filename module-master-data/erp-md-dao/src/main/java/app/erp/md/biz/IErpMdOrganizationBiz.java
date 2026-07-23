
package app.erp.md.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.md.dao.entity.ErpMdOrganization;

import java.util.Map;

public interface IErpMdOrganizationBiz extends ICrudBiz<ErpMdOrganization>{

    /**
     * F7 §3 删除引用预览（row-delete-button 点击时调用入口）。
     *
     * <p>跨域引用经 SPI（{@code IErpMdOrganizationReferenceChecker}）解耦：默认无实现返回空 Map（删除走原 __delete 路径）。
     *
     * @param id 组织 ID
     * @return key=引用域名，value=引用行数。无引用或无 SPI 实现返回空 Map
     */
    @BizQuery
    Map<String, Long> countReferences(@Name("id") Long id, IServiceContext context);
}
