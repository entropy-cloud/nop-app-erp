package app.erp.common.auth;

import io.nop.api.core.auth.IDataAuthChecker;
import io.nop.api.core.auth.ISecurityContext;
import io.nop.api.core.beans.ITreeBean;
import io.nop.api.core.config.AppConfig;
import io.nop.auth.service.auth.DefaultDataAuthChecker;
import io.nop.dao.api.IDaoProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

/**
 * 角色侧行级数据权限 config-gated checker（plan 2026-07-31-1023-3-r3-4，P1-MA6-002）。
 *
 * <p>注册为 IoC bean {@code nopDataAuthChecker}（非 default，覆盖平台 {@link DefaultDataAuthChecker}），
 * 由 {@code GraphQLEngine}（{@code nop.auth.enable-data-auth=true} 时）/ {@code CrudBizModel} 经
 * {@code context.getDataAuthChecker()} 应用（读路径），{@code DataAuthEntityFilterProvider} 在 ORM SQL 层应用。
 *
 * <p>行为：仅当 {@link ErpRoleDataAuthConstants#CONFIG_ROLE_ROW_FILTER_ENABLED} 为 true 时，
 * 委托平台 {@link DefaultDataAuthChecker} 应用 {@code /nop/main/auth/app.data-auth.xml}（聚合各域
 * {@code erp-*.data-auth.xml}）规则，含 fail-closed 语义；否则 {@code getFilter} 返回 {@code null}
 * （{@code AuthHelper.appendFilter} 不附加条件）、{@code isPermitted} 返回 {@code true}（全放行），
 * 保证单组织基线零回归。命名与机制对齐 R1.29 {@code ErpOrgIsolationQueryTransformer} config-gate 范式。
 *
 * <p>二级保险：平台 {@code nop.auth.enable-data-auth} 默认 false——即使本开关误开，data-auth 管道在
 * enableDataAuth=false 时不激活，checker 不被调用（生产 enforcement flip 须同时翻转两者，归 successor）。
 */
public class ErpRoleDataAuthChecker implements IDataAuthChecker {

    @Inject
    IDaoProvider daoProvider;
    private volatile DefaultDataAuthChecker delegate;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /** 角色侧行级过滤总开关是否开启（{@link ErpRoleDataAuthConstants#CONFIG_ROLE_ROW_FILTER_ENABLED}，默认 false）。 */
    public static boolean isEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(ErpRoleDataAuthConstants.CONFIG_ROLE_ROW_FILTER_ENABLED, Boolean.FALSE));
    }

    @Override
    public ITreeBean getFilter(String bizObj, String action, ISecurityContext context) {
        if (!isEnabled()) {
            return null;
        }
        return delegate().getFilter(bizObj, action, context);
    }

    @Override
    public boolean isPermitted(String bizObj, String action, Object entity, ISecurityContext context) {
        if (!isEnabled()) {
            return true;
        }
        return delegate().isPermitted(bizObj, action, entity, context);
    }

    @PreDestroy
    public void destroy() {
        if (delegate != null) {
            delegate.destroy();
        }
    }

    private DefaultDataAuthChecker delegate() {
        DefaultDataAuthChecker d = delegate;
        if (d == null) {
            synchronized (this) {
                d = delegate;
                if (d == null) {
                    d = new DefaultDataAuthChecker();
                    d.setDaoProvider(daoProvider);
                    d.lazyInit();
                    delegate = d;
                }
            }
        }
        return d;
    }
}
