
package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmTerritoryBiz;
import app.erp.crm.dao.entity.ErpCrmTerritory;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.processor.ErpCrmTerritoryCreateChildProcessor;
import app.erp.crm.service.processor.ErpCrmTerritoryMoveTerritoryProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;

/**
 * 销售区域 BizModel。在标准 CRUD 之上扩展区域树维护：建子节点（回填 level/fullPath/isLeaf）、
 * 移动子树（递归重算 + 成环校验）、有子节点禁删、子树查询。
 * {@code createChild} / {@code moveTerritory} 各自委托独立 per-mutation Processor（R6.6，{@code processor-extension-pattern.md}）。
 *
 * <p>对齐 {@code docs/design/crm/territory.md §业务规则 1 区域树 / §实现注记}。
 */
@BizModel("ErpCrmTerritory")
public class ErpCrmTerritoryBizModel extends CrudBizModel<ErpCrmTerritory> implements IErpCrmTerritoryBiz {

    @Inject
    ErpCrmTerritoryCreateChildProcessor createChildProcessor;

    @Inject
    ErpCrmTerritoryMoveTerritoryProcessor moveTerritoryProcessor;

    public ErpCrmTerritoryBizModel() {
        setEntityName(ErpCrmTerritory.class.getName());
    }

    @Override
    @BizMutation
    public ErpCrmTerritory createChild(@Name("parentId") Long parentId,
                                        @Name("code") String code,
                                        @Name("name") String name,
                                        @Name("territoryType") String territoryType,
                                        @Optional @Name("managerId") Long managerId,
                                        IServiceContext context) {
        return createChildProcessor.createChild(parentId, code, name, territoryType, managerId, context);
    }

    @Override
    @BizMutation
    public ErpCrmTerritory moveTerritory(@Name("territoryId") Long territoryId,
                                          @Name("newParentId") Long newParentId,
                                          IServiceContext context) {
        return moveTerritoryProcessor.moveTerritory(territoryId, newParentId, context);
    }

    @Override
    @BizQuery
    public List<ErpCrmTerritory> getTerritoryTree(@Optional @Name("parentId") Long parentId,
                                                    IServiceContext context) {
        QueryBean q = new QueryBean();
        if (parentId == null) {
            q.addFilter(isNull("parentId"));
        } else {
            q.addFilter(eq("parentId", parentId));
        }
        q.addOrderField("sortOrder", false);
        return findList(q, null, context);
    }

    @Override
    protected void defaultPrepareDelete(ErpCrmTerritory entity, IServiceContext context) {
        super.defaultPrepareDelete(entity, context);
        // 有子节点禁止删除
        QueryBean q = new QueryBean();
        q.addFilter(eq("parentId", entity.getId()));
        q.setLimit(1);
        if (!findList(q, null, context).isEmpty()) {
            throw new NopException(ErpCrmErrors.ERR_TERRITORY_HAS_CHILDREN)
                    .param(ErpCrmErrors.ARG_TERRITORY_ID, entity.getId());
        }
    }

    // ---------- 内部辅助 ----------

    protected String buildFullPath(String parentFullPath, String code) {
        if (parentFullPath == null || parentFullPath.isEmpty() || "/".equals(parentFullPath)) {
            return "/" + code;
        }
        return parentFullPath + "/" + code;
    }

    protected int maxDepth() {
        return io.nop.api.core.config.AppConfig.var(
                ErpCrmConstants.CONFIG_TERRITORY_MAX_DEPTH, 4);
    }

    

}
