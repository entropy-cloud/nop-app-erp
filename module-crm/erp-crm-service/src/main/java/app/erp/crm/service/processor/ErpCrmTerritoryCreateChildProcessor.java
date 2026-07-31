package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmTerritory;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.exceptions.UnknownEntityException;
import jakarta.inject.Inject;

/**
 * ErpCrmTerritory createChild per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含区域树建子节点编排（回填 level/fullPath/isLeaf + 父节点 isLeaf 翻转 + 深度校验）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCrmTerritoryCreateChildProcessor {

    @Inject
    IDaoProvider daoProvider;

    public ErpCrmTerritory createChild(Long parentId, String code, String name, String territoryType,
                                       Long managerId, IServiceContext context) {
        ErpCrmTerritory parent = requireTerritory(parentId);
        int maxDepth = maxDepth();
        int childLevel = (parent.getLevel() != null ? parent.getLevel() : 0) + 1;
        if (childLevel > maxDepth) {
            throw new NopException(ErpCrmErrors.ERR_TERRITORY_MAX_DEPTH_EXCEEDED)
                    .param(ErpCrmErrors.ARG_CURRENT_LEVEL, childLevel)
                    .param(ErpCrmErrors.ARG_MAX_LEVEL, maxDepth)
                    .param(ErpCrmErrors.ARG_PARENT_ID, parentId);
        }

        ErpCrmTerritory child = dao().newEntity();
        child.setCode(code);
        child.setName(name);
        child.setTerritoryType(territoryType);
        child.setManagerId(managerId);
        child.setOrgId(parent.getOrgId());
        child.setParentId(parent.getId());
        child.setLevel(childLevel);
        child.setFullPath(buildFullPath(parent.getFullPath(), code));
        child.setIsLeaf(true);
        child.setIsActive(true);
        child.setSortOrder(0);
        dao().saveEntity(child);

        if (Boolean.FALSE.equals(parent.getIsLeaf())) {
            // already non-leaf; nothing to flip
        } else {
            parent.setIsLeaf(false);
            dao().updateEntity(parent);
        }
        return child;
    }

    // ---------- 内部辅助 ----------

    protected ErpCrmTerritory requireTerritory(Long territoryId) {
        ErpCrmTerritory territory = dao().getEntityById(territoryId);
        if (territory == null) {
            throw new UnknownEntityException(ErpCrmTerritory.class.getName(), territoryId);
        }
        return territory;
    }

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

    private IEntityDao<ErpCrmTerritory> dao() {
        return daoProvider.daoFor(ErpCrmTerritory.class);
    }
}
