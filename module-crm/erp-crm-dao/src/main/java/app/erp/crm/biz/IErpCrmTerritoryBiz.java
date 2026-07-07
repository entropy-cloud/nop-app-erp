
package app.erp.crm.biz;

import app.erp.crm.dao.entity.ErpCrmTerritory;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

/**
 * 销售区域业务接口。除标准 CRUD 外，定义区域树维护方法（建子节点 / 移动 / 子树查询）。
 *
 * <p>对齐 {@code docs/design/crm/territory.md §业务规则 1 区域树}（最大深度 4、有子节点禁删、成环校验）。
 */
public interface IErpCrmTerritoryBiz extends ICrudBiz<ErpCrmTerritory> {

    /**
     * 在指定父区域下创建子节点，自动回填 level / fullPath / isLeaf。
     *
     * <ul>
     *   <li>深度超 {@code erp-crm.territory.max-depth}（默认 4）→ {@code ERR_TERRITORY_MAX_DEPTH_EXCEEDED}</li>
     *   <li>parentId 成环 → {@code ERR_TERRITORY_CYCLE}</li>
     * </ul>
     */
    @BizMutation
    ErpCrmTerritory createChild(@Name("parentId") Long parentId,
                                @Name("code") String code,
                                @Name("name") String name,
                                @Name("territoryType") String territoryType,
                                @Optional @Name("managerId") Long managerId,
                                IServiceContext context);

    /**
     * 将区域移动到新父节点下，重算 level / fullPath 并递归更新子树。
     *
     * <ul>
     *   <li>newParentId 等于自身或其子树节点 → {@code ERR_TERRITORY_CYCLE}</li>
     *   <li>新位置深度超限 → {@code ERR_TERRITORY_MAX_DEPTH_EXCEEDED}</li>
     * </ul>
     */
    @BizMutation
    ErpCrmTerritory moveTerritory(@Name("territoryId") Long territoryId,
                                  @Name("newParentId") Long newParentId,
                                  IServiceContext context);

    /**
     * 返回指定父节点下的子树（直接子节点 + 各自 fullPath，便于前端树渲染）。
     * parentId=null 返回根节点列表。
     */
    @BizQuery
    List<ErpCrmTerritory> getTerritoryTree(@Optional @Name("parentId") Long parentId,
                                            IServiceContext context);
}
