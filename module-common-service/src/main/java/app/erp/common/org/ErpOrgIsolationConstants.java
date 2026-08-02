package app.erp.common.org;

/**
 * 多公司 orgId 隔离基础设施常量（plan 2026-07-30-0841-3-r1-29，P1-MA2-093/094）。
 *
 * <p>隔离能力默认关闭（保护单组织单账套基线 + 既有测试零回归）：
 * <ul>
 *   <li>{@link #CONFIG_ORG_ISOLATION_ENABLED} = {@code erp.multi-company.org-isolation-enabled} 默认 false；</li>
 *   <li>开启后读路径经全局 {@code IQueryTransformer} 自动追加 {@code eq("orgId", currentOrgId)}，
 *       写路径经全局 {@code IOrmInterceptor#preSave} 从上下文 stamp orgId（覆盖客户端传入）。</li>
 * </ul>
 *
 * <p>权威：{@code docs/architecture/multi-company.md §数据隔离}。
 */
public final class ErpOrgIsolationConstants {

    private ErpOrgIsolationConstants() {
    }

    /** 多公司 orgId 隔离总开关，默认 false（单组织基线无自动隔离，多公司部署须显式开启）。 */
    public static final String CONFIG_ORG_ISOLATION_ENABLED = "erp.multi-company.org-isolation-enabled";

    /** 当前组织 id 在 {@code IContext} 上的属性名（app 层 attribute，不改平台接口）。 */
    public static final String CONTEXT_ATTR_CURRENT_ORG_ID = "erp.currentOrgId";

    /** orgId 列名（业务单据统一的核算组织列）。 */
    public static final String PROP_ORG_ID = "orgId";
}
