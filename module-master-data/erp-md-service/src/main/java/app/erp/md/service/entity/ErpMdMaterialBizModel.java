
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import jakarta.annotation.Nullable;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.md.biz.IErpMdMaterialBiz;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.service.ErpMdConstants;
import app.erp.md.spi.IErpMdMaterialReferenceChecker;

import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 物料 BizModel。UC-MD-06 物料停用联动：物料 status → INACTIVE 后，其 SKU 不可被新单引用。
 *
 * <p>联动机制（Phase 3 Decision 选 (b)，G2 SKU 无独立 status 列）：物料停用本身仅修改 status，
 * 不直接改 SKU；SKU 侧 {@link ErpMdMaterialSkuBizModel#isMaterialActive} 在 resolveSku/findDefaultSku
 * 时读物料 status 过滤，实现「停用物料 → SKU 不可被新单引用」语义。
 *
 * <p>本 BizModel 的钩子 {@link #defaultPrepareUpdate} 记录停用事件用于审计/通知（可选扩展点），
 * 不阻断停用操作（停用是合法的物料生命周期状态迁移）。
 *
 * <p>F7 §3 扩展：
 * <ul>
 *   <li>{@link #isCodeUnique} — 编码唯一性前置校验（async validator on blur 调用入口）</li>
 *   <li>{@link #countReferences} — 删除引用预览（经 SPI 跨域解耦）</li>
 * </ul>
 */
@BizModel("ErpMdMaterial")
public class ErpMdMaterialBizModel extends CrudBizModel<ErpMdMaterial> implements IErpMdMaterialBiz {

    /**
     * 跨域引用计数 SPI（F7 §3）。master-data 不可反向依赖 purchase/sales/inventory，
     * 默认无实现返回空 Map（删除走原 __delete 路径）。
     */
    @Inject
    @Nullable
    protected IErpMdMaterialReferenceChecker materialReferenceChecker;

    public ErpMdMaterialBizModel() {
        setEntityName(ErpMdMaterial.class.getName());
    }

    /**
     * UC-MD-06：物料状态变更联动。停用（status→INACTIVE）时记录事件——
     * SKU 侧经 {@link ErpMdMaterialSkuBizModel#isMaterialActive} 过滤，实现联动。
     * 停用是合法迁移，不在此抛错（仅下游 resolveSku 时静默返回 null）。
     */
    @Override
    protected void defaultPrepareUpdate(EntityData<ErpMdMaterial> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        ErpMdMaterial entity = entityData.getEntity();
        if (entity == null) {
            return;
        }
        if (ErpMdConstants.ACTIVE_STATUS_INACTIVE.equals(entity.getStatus())) {
            onMaterialDeactivated(entity, context);
        }
    }

    /**
     * 物料停用回调（protected 供下游覆盖扩展通知/日志）。默认空实现。
     */
    @SuppressWarnings("unused")
    protected void onMaterialDeactivated(ErpMdMaterial material, IServiceContext context) {
        // 默认无操作：联动经 SKU 侧 isMaterialActive 过滤实现，此处仅作扩展点。
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

    /**
     * F7 §3 删除引用预览。经 SPI 跨域解耦；默认无实现返回空 Map。
     */
    @Override
    @BizQuery
    public Map<String, Long> countReferences(@Name("id") Long id, IServiceContext context) {
        if (id == null || materialReferenceChecker == null) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = materialReferenceChecker.countReferences(id);
        return result == null ? Collections.emptyMap() : result;
    }
}
