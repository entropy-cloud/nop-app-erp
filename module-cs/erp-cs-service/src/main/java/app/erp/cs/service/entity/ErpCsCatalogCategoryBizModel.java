package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsCatalogCategoryBiz;
import app.erp.cs.dao.entity.ErpCsCatalogCategory;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 目录分类 BizModel（{@code docs/design/customer-service/service-catalog.md §1.2/§4}）。
 *
 * <p>树形维护校验经 {@link #defaultPrepareSave}/{@link #defaultPrepareUpdate}/{@link #defaultPrepareDelete} 钩子：
 * <ul>
 *   <li>parentId 自环拒绝（ERR_CATALOG_CATEGORY_CYCLE）。</li>
 *   <li>parentId 链成环检测（沿 parentId 上溯到根，命中自身即成环）。</li>
 *   <li>层级深度超 {@code erp-cs.catalog-category-max-depth}（默认 3）拒绝。</li>
 *   <li>有子节点禁删（ERR_CATALOG_CATEGORY_HAS_CHILDREN）。</li>
 * </ul>
 *
 * <p>对齐 projects 0930-3 范式（{@code ErpPrjTaskBizModel} 的 defaultPrepareSave 钩子）。
 */
@BizModel("ErpCsCatalogCategory")
public class ErpCsCatalogCategoryBizModel extends CrudBizModel<ErpCsCatalogCategory>
        implements IErpCsCatalogCategoryBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpCsCatalogCategoryBizModel.class);

    public ErpCsCatalogCategoryBizModel() {
        setEntityName(ErpCsCatalogCategory.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCsCatalogCategory> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        validateParentChain(entityData.getEntity(), context);
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpCsCatalogCategory> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        validateParentChain(entityData.getEntity(), context);
    }

    @Override
    protected void defaultPrepareDelete(ErpCsCatalogCategory entity, IServiceContext context) {
        super.defaultPrepareDelete(entity, context);
        // 有子节点禁删（service-catalog.md §4.1）
        if (hasChildren(entity.getId())) {
            throw new NopException(ErpCsErrors.ERR_CATALOG_CATEGORY_HAS_CHILDREN)
                    .param(ErpCsErrors.ARG_CATEGORY_ID, entity.getId());
        }
    }

    /**
     * 校验 parentId 合法性：自环 / 成环 / 深度超限。
     * parentId 为 null 时直接放行（根节点）。
     */
    private void validateParentChain(ErpCsCatalogCategory category, IServiceContext context) {
        if (category == null || category.getParentId() == null) {
            return;
        }
        Long selfId = category.getId();
        Long parentId = category.getParentId();

        // 自环拒绝
        if (selfId != null && selfId.equals(parentId)) {
            throw new NopException(ErpCsErrors.ERR_CATALOG_CATEGORY_CYCLE)
                    .param(ErpCsErrors.ARG_CATEGORY_ID, selfId);
        }

        int maxDepth = ErpCsConfigs.getCatalogCategoryMaxDepth();
        ErpCsCatalogCategory parent = loadCategory(parentId);
        if (parent == null) {
            // 父节点不存在：交由 ORM 外键校验处理（不在此重复校验）
            return;
        }

        // 成环检测：沿 parentId 上溯，若命中 selfId 即成环
        Set<Long> visited = new HashSet<>();
        visited.add(parentId);
        if (selfId != null) {
            visited.add(selfId);
        }
        ErpCsCatalogCategory cursor = parent;
        int depth = 1;
        while (cursor.getParentId() != null) {
            Long ancestorId = cursor.getParentId();
            if (selfId != null && selfId.equals(ancestorId)) {
                throw new NopException(ErpCsErrors.ERR_CATALOG_CATEGORY_CYCLE)
                        .param(ErpCsErrors.ARG_CATEGORY_ID, selfId);
            }
            if (!visited.add(ancestorId)) {
                // 数据库中已存在环路（历史脏数据），拒绝继续以避免无限循环
                throw new NopException(ErpCsErrors.ERR_CATALOG_CATEGORY_CYCLE)
                        .param(ErpCsErrors.ARG_CATEGORY_ID, ancestorId);
            }
            ErpCsCatalogCategory ancestor = loadCategory(ancestorId);
            if (ancestor == null) {
                break;
            }
            cursor = ancestor;
            depth++;
            if (depth > maxDepth + 1) {
                // 安全阀：链长度异常长，避免恶意构造深链拖垮校验
                break;
            }
        }

        // 深度校验：新节点深度 = 父链深度 + 1（自身）
        int newDepth = depth + 1;
        if (newDepth > maxDepth) {
            throw new NopException(ErpCsErrors.ERR_CATALOG_CATEGORY_MAX_DEPTH_EXCEEDED)
                    .param(ErpCsErrors.ARG_CATEGORY_NAME, category.getName())
                    .param(ErpCsErrors.ARG_CURRENT_DEPTH, newDepth)
                    .param(ErpCsErrors.ARG_MAX_DEPTH, maxDepth);
        }
    }

    private boolean hasChildren(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("parentId", categoryId));
        q.setLimit(1);
        IEntityDao<ErpCsCatalogCategory> dao = daoProvider().daoFor(ErpCsCatalogCategory.class);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private ErpCsCatalogCategory loadCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        IEntityDao<ErpCsCatalogCategory> dao = daoProvider().daoFor(ErpCsCatalogCategory.class);
        return dao.getEntityById(categoryId);
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCsCatalogCategory.class)
    public List<String> orgName(@ContextSource List<ErpCsCatalogCategory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsCatalogCategory row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCsCatalogCategory.class)
    public List<String> parentName(@ContextSource List<ErpCsCatalogCategory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("parent"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsCatalogCategory row : rows) {
            result.add(row.orm_attached() && row.getParent() != null ? row.getParent().getName() : null);
        }
        return result;
    }

}
