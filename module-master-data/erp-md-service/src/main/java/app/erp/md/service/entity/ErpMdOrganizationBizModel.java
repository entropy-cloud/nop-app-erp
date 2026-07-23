
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.md.biz.IErpMdOrganizationBiz;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.spi.IErpMdOrganizationReferenceChecker;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Map;

/**
 * 组织 BizModel。F7 §3 扩展：{@link #countReferences} 删除引用阻断预览（经 SPI 跨域解耦）。
 *
 * <p>生产 SPI 实现 {@link ErpMdOrganizationReferenceChecker} 扫描 master-data 自有归属引用
 * （Employee/Warehouse 的 orgId）；默认无实现时返回空 Map。
 */
@BizModel("ErpMdOrganization")
public class ErpMdOrganizationBizModel extends CrudBizModel<ErpMdOrganization> implements IErpMdOrganizationBiz {

    /**
     * 跨域引用计数 SPI（F7 §3）。master-data 不可反向依赖下游域，
     * 默认无实现返回空 Map（删除走原 __delete 路径）。
     */
    @Inject
    @Nullable
    protected IErpMdOrganizationReferenceChecker organizationReferenceChecker;

    public ErpMdOrganizationBizModel() {
        setEntityName(ErpMdOrganization.class.getName());
    }

    /**
     * F7 §3 删除引用预览。经 SPI 跨域解耦；默认无实现返回空 Map。
     */
    @Override
    @BizQuery
    public Map<String, Long> countReferences(@Name("id") Long id, IServiceContext context) {
        if (id == null || organizationReferenceChecker == null) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = organizationReferenceChecker.countReferences(id);
        return result == null ? Collections.emptyMap() : result;
    }
}
