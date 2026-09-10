package app.erp.common.org;

import io.nop.api.core.annotations.core.Description;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.FilterBeans;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.api.IBizObject;
import io.nop.biz.crud.IQueryTransformer;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoEntity;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.model.IEntityModel;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(ErpOrgIsolationQueryTransformer.class);

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

    /**
     * 解析实体是否含 orgId 列（P3-CK-common-012-r3 修复面：区分「无 orgId 列」与「解析失败」两态）。
     *
     * <ul>
     *   <li>解析成功且无 orgId 列 → false（合法白名单跳过，静默）。</li>
     *   <li>解析失败（类加载/dao/模型反射失败，重部署窗口/类加载器失配）→ WARN 英文日志；
     *       config {@code erp.multi-company.org-isolation-fail-closed}=true 时抛
     *       {@link ErpOrgCommonErrors#ERR_ORG_ISOLATION_RESOLVE_FAILED}（fail-closed，拒答优于未隔离放行），
     *       缺省 false 保持 fail-open 兼容（WARN 可观测）。</li>
     * </ul>
     */
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
            return onResolveFailure(entityName, null);
        }
        try {
            Class<?> clazz = Class.forName(entityName, false, Thread.currentThread().getContextClassLoader());
            IEntityDao<?> dao = daoProvider.daoFor((Class<? extends IDaoEntity>) clazz);
            if (dao instanceof IOrmEntityDao) {
                IEntityModel model = ((IOrmEntityDao<?>) dao).getEntityModel();
                if (model == null) {
                    return onResolveFailure(entityName, null);
                }
                return model.getColumn(ErpOrgIsolationConstants.PROP_ORG_ID, true) != null;
            }
            return false;
        } catch (Throwable e) {
            return onResolveFailure(entityName, e);
        }
    }

    private boolean onResolveFailure(String entityName, Throwable e) {
        LOG.warn("org-isolation: entity model resolve failed for [{}], isolation filter skipped for this entity (fail-open). "
                + "Enable erp.multi-company.org-isolation-fail-closed=true to fail-closed.", entityName,
                e == null ? new IllegalStateException("daoProvider unavailable") : e);
        if (AppConfig.var(ErpOrgIsolationConstants.CONFIG_ORG_ISOLATION_FAIL_CLOSED, false)) {
            throw new NopException(app.erp.common.service.ErpCommonErrors.ERR_ORG_ISOLATION_RESOLVE_FAILED)
                    .param(app.erp.common.service.ErpCommonErrors.ARG_ENTITY_NAME, entityName);
        }
        return false;
    }
}
