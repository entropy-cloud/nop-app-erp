package app.erp.common.org;

import io.nop.api.core.annotations.core.Description;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.FilterBeans;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.api.IBizObject;
import io.nop.biz.crud.IQueryTransformer;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoEntity;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.model.IEntityModel;
import jakarta.inject.Inject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局 orgId 读路径隔离 QueryTransformer（plan 2026-07-30-0841-3-r1-29，P1-MA2-093）。
 *
 * <p>注册为 IoC bean {@code nopGlobalQueryTransformer}，由平台 {@code CrudBizModel} 经
 * {@code @Named("nopGlobalQueryTransformer")} 自动注入并应用于全部 findPage/findList 查询
 * （见 {@code CrudBizModel#appendDataAuthFilter} 注册点）。
 *
 * <p>行为：仅当 {@link ErpOrgContext#isActive(IServiceContext)} 为真且目标实体含 orgId 列时，
 * 追加 {@code eq("orgId", currentOrgId)}。config-gated 默认关闭 → 单组织基线零回归。
 * org 列缺失实体（系统配置实体等）经 {@link #entityHasOrgId(IBizObject)} 白名单透明跳过。
 *
 * <p>Non-Goal：dashboard/report BizModel 经 {@code IDaoProvider} 直访绕过 CrudBizModel 管道，
 * 不被本 transformer 覆盖（归 P1-MA1-022 读侧豁免 + 各域查询方法显式补 filter，如 P1-MA2-095）。
 */
@Description("全局 orgId 读路径隔离 QueryTransformer（config-gated，默认关闭）")
public class ErpOrgIsolationQueryTransformer implements IQueryTransformer {

    @Inject
    IDaoProvider daoProvider;

    private final ConcurrentHashMap<String, Boolean> orgIdPropCache = new ConcurrentHashMap<>();

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    @Override
    public void transform(@Name("filter") QueryBean filter,
                          @Name("authObjName") String authObjName,
                          @Name("action") String action,
                          @Name("bizObj") IBizObject bizObj,
                          IServiceContext context) {
        if (filter == null) {
            return;
        }
        if (!ErpOrgContext.isActive(context)) {
            return;
        }
        if (!entityHasOrgId(bizObj)) {
            return;
        }
        String orgId = ErpOrgContext.currentOrgId(context);
        filter.addFilter(FilterBeans.eq(ErpOrgIsolationConstants.PROP_ORG_ID, orgId));
    }

    /** 缓存解析实体是否含 orgId 列（系统配置等无 orgId 实体透明跳过）。 */
    boolean entityHasOrgId(IBizObject bizObj) {
        if (bizObj == null || bizObj.getObjMeta() == null) {
            return false;
        }
        String entityName = bizObj.getObjMeta().getEntityName();
        if (entityName == null || entityName.isEmpty()) {
            return false;
        }
        Boolean cached = orgIdPropCache.get(entityName);
        if (cached != null) {
            return cached;
        }
        boolean has = resolveEntityHasOrgId(entityName);
        orgIdPropCache.put(entityName, has);
        return has;
    }

    @SuppressWarnings("unchecked")
    private boolean resolveEntityHasOrgId(String entityName) {
        if (daoProvider == null) {
            return false;
        }
        try {
            Class<?> clazz = Class.forName(entityName, false, Thread.currentThread().getContextClassLoader());
            IEntityDao<?> dao = daoProvider.daoFor((Class<? extends IDaoEntity>) clazz);
            if (dao instanceof IOrmEntityDao) {
                IEntityModel model = ((IOrmEntityDao<?>) dao).getEntityModel();
                return model != null && model.getColumn(ErpOrgIsolationConstants.PROP_ORG_ID, true) != null;
            }
        } catch (Throwable e) {
            return false;
        }
        return false;
    }
}
