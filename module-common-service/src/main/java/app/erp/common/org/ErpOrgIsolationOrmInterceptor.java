package app.erp.common.org;

import io.nop.api.core.util.ProcessResult;
import io.nop.orm.IOrmEntity;
import io.nop.orm.IOrmInterceptor;
import io.nop.orm.model.IEntityModel;

/**
 * 全局 orgId 写路径 auto-stamp 拦截器（plan 2026-07-30-0841-3-r1-29，P1-MA2-094）。
 *
 * <p>注册为 IoC bean（{@code io.nop.orm.IOrmInterceptor} 类型），由平台 {@code nopOrmSessionFactory}
 * 经 {@code <ioc:collect-beans by-type="io.nop.orm.IOrmInterceptor"/>} 自动收集并应用于全部实体保存。
 *
 * <p>行为：仅当 {@link ErpOrgContext#isIsolationEnabled()} 且上下文 currentOrgId 已设置且实体含 orgId 列时，
 * 在 {@link #preSave(IOrmEntity)} 强制将 orgId stamp 为 currentOrgId（覆盖客户端传入）。
 * config-gated 默认关闭 → 单组织基线零回归。
 *
 * <p>currentOrgId 经 {@link ErpOrgContext#currentOrgId} 从线程绑定 {@code IContext} 解析（{@code ContextProvider}）。
 */
public class ErpOrgIsolationOrmInterceptor implements IOrmInterceptor {

    @Override
    public ProcessResult preSave(IOrmEntity entity) {
        stampOrgId(entity);
        return ProcessResult.CONTINUE;
    }

    @Override
    public ProcessResult preUpdate(IOrmEntity entity) {
        // update 时不覆盖既有 orgId（orgId 为单据所属组织，跨组织迁移非自动 stamp 职责）。
        return ProcessResult.CONTINUE;
    }

    private void stampOrgId(IOrmEntity entity) {
        if (entity == null) {
            return;
        }
        if (!ErpOrgContext.isIsolationEnabled()) {
            return;
        }
        String orgId = ErpOrgContext.currentOrgId(null);
        if (orgId == null) {
            return;
        }
        IEntityModel model = entity.orm_entityModel();
        if (model == null || model.getColumn(ErpOrgIsolationConstants.PROP_ORG_ID, true) == null) {
            return;
        }
        Object current = entity.orm_propValueByName(ErpOrgIsolationConstants.PROP_ORG_ID);
        if (orgId.equals(ErpOrgContext.toStringValue(current))) {
            return;
        }
        entity.orm_propValueByName(ErpOrgIsolationConstants.PROP_ORG_ID, orgId);
    }
}
