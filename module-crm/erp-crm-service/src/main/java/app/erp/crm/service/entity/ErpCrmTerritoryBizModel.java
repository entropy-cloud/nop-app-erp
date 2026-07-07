
package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmTerritoryBiz;
import app.erp.crm.dao.entity.ErpCrmTerritory;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;

/**
 * 销售区域 BizModel。在标准 CRUD 之上扩展区域树维护：建子节点（回填 level/fullPath/isLeaf）、
 * 移动子树（递归重算 + 成环校验）、有子节点禁删、子树查询。
 *
 * <p>对齐 {@code docs/design/crm/territory.md §业务规则 1 区域树 / §实现注记}。
 */
@BizModel("ErpCrmTerritory")
public class ErpCrmTerritoryBizModel extends CrudBizModel<ErpCrmTerritory> implements IErpCrmTerritoryBiz {

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
        ErpCrmTerritory parent = requireEntity(String.valueOf(parentId), null, context);
        int maxDepth = maxDepth();
        int childLevel = (parent.getLevel() != null ? parent.getLevel() : 0) + 1;
        if (childLevel > maxDepth) {
            throw new NopException(ErpCrmErrors.ERR_TERRITORY_MAX_DEPTH_EXCEEDED)
                    .param(ErpCrmErrors.ARG_CURRENT_LEVEL, childLevel)
                    .param(ErpCrmErrors.ARG_MAX_LEVEL, maxDepth)
                    .param(ErpCrmErrors.ARG_PARENT_ID, parentId);
        }

        ErpCrmTerritory child = newEntity();
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
        saveEntity(child, null, context);

        if (Boolean.FALSE.equals(parent.getIsLeaf())) {
            // already non-leaf; nothing to flip
        } else {
            parent.setIsLeaf(false);
            updateEntity(parent, null, context);
        }
        return child;
    }

    @Override
    @BizMutation
    public ErpCrmTerritory moveTerritory(@Name("territoryId") Long territoryId,
                                          @Name("newParentId") Long newParentId,
                                          IServiceContext context) {
        ErpCrmTerritory node = requireEntity(String.valueOf(territoryId), null, context);
        if (newParentId == null) {
            // move to root
            applyMove(node, null, 0, null, context);
            return node;
        }
        if (territoryId.equals(newParentId)) {
            throw new NopException(ErpCrmErrors.ERR_TERRITORY_CYCLE)
                    .param(ErpCrmErrors.ARG_TERRITORY_ID, territoryId)
                    .param(ErpCrmErrors.ARG_PARENT_ID, newParentId);
        }
        // Cycle check: newParentId must not be in the subtree of node
        Set<Long> subtree = collectSubtreeIds(territoryId);
        if (subtree.contains(newParentId)) {
            throw new NopException(ErpCrmErrors.ERR_TERRITORY_CYCLE)
                    .param(ErpCrmErrors.ARG_TERRITORY_ID, territoryId)
                    .param(ErpCrmErrors.ARG_PARENT_ID, newParentId);
        }
        ErpCrmTerritory newParent = requireEntity(String.valueOf(newParentId), null, context);
        int maxDepth = maxDepth();
        int newLevel = (newParent.getLevel() != null ? newParent.getLevel() : 0) + 1;
        int subtreeDepth = maxSubtreeDepthFrom(territoryId);
        if (newLevel + subtreeDepth > maxDepth) {
            throw new NopException(ErpCrmErrors.ERR_TERRITORY_MAX_DEPTH_EXCEEDED)
                    .param(ErpCrmErrors.ARG_CURRENT_LEVEL, newLevel + subtreeDepth)
                    .param(ErpCrmErrors.ARG_MAX_LEVEL, maxDepth)
                    .param(ErpCrmErrors.ARG_PARENT_ID, newParentId);
        }
        applyMove(node, newParentId, newLevel, newParent.getFullPath(), context);
        return node;
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

    protected void applyMove(ErpCrmTerritory node, Long newParentId, int newLevel, String newParentFullPath,
                              IServiceContext context) {
        String oldFullPath = node.getFullPath();
        String newFullPath = buildFullPath(newParentFullPath, node.getCode());
        node.setParentId(newParentId);
        node.setLevel(newLevel);
        node.setFullPath(newFullPath);
        updateEntity(node, null, context);
        // recursively update children's level / fullPath
        if (oldFullPath != null && !oldFullPath.equals(newFullPath)) {
            relocateChildren(node.getId(), oldFullPath, newFullPath, newLevel, context);
        }
    }

    protected void relocateChildren(Long parentId, String oldPrefix, String newPrefix, int parentLevel,
                                     IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("parentId", parentId));
        List<ErpCrmTerritory> children = findList(q, null, context);
        for (ErpCrmTerritory child : children) {
            String oldChildPath = child.getFullPath();
            String newChildPath = oldChildPath != null && oldChildPath.startsWith(oldPrefix)
                    ? newPrefix + oldChildPath.substring(oldPrefix.length())
                    : buildFullPath(newPrefix, child.getCode());
            child.setFullPath(newChildPath);
            child.setLevel(parentLevel + 1);
            updateEntity(child, null, context);
            relocateChildren(child.getId(), oldChildPath, newChildPath, parentLevel + 1, context);
        }
    }

    protected Set<Long> collectSubtreeIds(Long rootId) {
        Set<Long> ids = new HashSet<>();
        collectSubtreeIds(rootId, ids);
        return ids;
    }

    protected void collectSubtreeIds(Long rootId, Set<Long> acc) {
        acc.add(rootId);
        QueryBean q = new QueryBean();
        q.addFilter(eq("parentId", rootId));
        for (ErpCrmTerritory child : dao().findAllByQuery(q)) {
            collectSubtreeIds(child.getId(), acc);
        }
    }

    protected int maxSubtreeDepthFrom(Long rootId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("parentId", rootId));
        List<ErpCrmTerritory> children = dao().findAllByQuery(q);
        int maxChild = 0;
        for (ErpCrmTerritory child : children) {
            maxChild = Math.max(maxChild, 1 + maxSubtreeDepthFrom(child.getId()));
        }
        return maxChild;
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
}
