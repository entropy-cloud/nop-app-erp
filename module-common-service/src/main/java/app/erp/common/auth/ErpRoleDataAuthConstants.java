package app.erp.common.auth;

/**
 * 角色侧行级数据权限基础设施常量（plan 2026-07-31-1023-3-r3-4，P1-MA6-002）。
 *
 * <p>过滤能力默认关闭（保护单组织基线 + 既有测试零回归）：
 * <ul>
 *   <li>{@link #CONFIG_ROLE_ROW_FILTER_ENABLED} = {@code erp.data-auth.role-row-filter-enabled} 默认 false；</li>
 *   <li>开启后由 {@code ErpRoleDataAuthChecker} 委托平台 {@code DefaultDataAuthChecker} 应用
 *       {@code erp-*.data-auth.xml} 规则（角色 × bizObj 行级过滤）。</li>
 * </ul>
 *
 * <p>命名对齐 R1.29 {@code erp.multi-company.org-isolation-enabled}（orgId 维度，互补协同）。
 * 权威：{@code docs/design/roles-and-permissions.md §数据权限}。
 */
public final class ErpRoleDataAuthConstants {

    private ErpRoleDataAuthConstants() {
    }

    /** 角色侧行级过滤总开关，默认 false（单组织基线无角色侧行级过滤，多角色部署须显式开启）。 */
    public static final String CONFIG_ROLE_ROW_FILTER_ENABLED = "erp.data-auth.role-row-filter-enabled";
}
