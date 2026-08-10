package app.erp.common.service;

import io.nop.api.core.auth.IUserContext;

import java.math.BigDecimal;
import java.util.Set;

/**
 * E3.1 后端响应层脱敏共享工具（plan 2026-08-10-2059-2）。
 *
 * <p>由各域 entity BizModel 的 {@code @BizLoader} 方法委托：授权角色见明文，非授权角色见打码值。
 * role 判定经 {@link IUserContext#get()} + {@link IUserContext#isUserInRole(String)}（与
 * {@code ErpHrAttendanceBizModel.checkMakeUpRole} 同范式；fail-closed：无用户上下文 = 打码）。
 *
 * <p><b>mask 格式裁决</b>（Phase 1 Decision (c)，类型安全——不改 GraphQL schema）：
 * <ul>
 *   <li>数值字段（DECIMAL/BIGINT）：非授权返 {@code null}（字段类型不可表示打码字符串，null = 值保留不披露，零位数泄漏）。</li>
 *   <li>VARCHAR PII：非授权返打码字符串（{@link StringMaskFormat}）。</li>
 * </ul>
 *
 * <p><b>roleId 字面</b>：与 {@code nop_auth_role.csv} seed + 各域 {@code erp-*.action-auth.xml}
 * {@code roles} 属性一致（同 {@code ErpHrConstants.HR_ROLE_ID="HR 专员"} 范式）。
 *
 * <p><b>E4.2 读访问审计挂钩</b>（plan 2026-08-11-1030-1，Phase 1 Decision (a) 单一 chokepoint）：
 * 新增重载 {@link #maskDecimal(BigDecimal, Set, Object, String)} / {@link #maskLong(Long, Set, Object, String)}
 * / {@link #maskString(String, StringMaskFormat, Set, Object, String)} 接收审计上下文（entity + fieldName），
 * 在 authorized-clear-text 分支委托 {@link MaskAuditRecorder#recordDisclosure(Object, String, String)} 写
 * 审计记录。旧无审计重载保留（back-compat，审计 OFF）。
 *
 * <p>此为 masking 层工作假设，不 preempt E4.1 正式 field-level visibility + 代理视图裁决。
 */
public final class MaskHelper {

    private MaskHelper() {
    }

    // ---- 授权角色常量（与 nop_auth_role.csv seed + erp-*.action-auth.xml roles 字面一致）----

    public static final String ROLE_HR_SPECIALIST = "HR 专员";
    public static final String ROLE_SALARY_APPROVER = "薪酬审批人";
    public static final String ROLE_CT_APPROVER = "合同审批人";
    public static final String ROLE_CT_CLERK = "合同专员";
    public static final String ROLE_PURCHASER = "采购员";
    public static final String ROLE_BIZ_ADMIN = "管理员";
    public static final String ROLE_FINANCE_STAFF = "财务员";

    // ---- 数值字段（DECIMAL/BIGINT）：非授权返 null ----

    public static BigDecimal maskDecimal(BigDecimal value, Set<String> authorizedRoles) {
        return isAuthorized(authorizedRoles) ? value : null;
    }

    public static Long maskLong(Long value, Set<String> authorizedRoles) {
        return isAuthorized(authorizedRoles) ? value : null;
    }

    // ---- VARCHAR 字段：非授权返打码字符串 ----

    public static String maskString(String value, StringMaskFormat format, Set<String> authorizedRoles) {
        return isAuthorized(authorizedRoles) ? value : format.mask(value);
    }

    // ---- E4.2 带审计上下文重载（plan 2026-08-11-1030-1）—— authorized-clear-text 分支委托 MaskAuditRecorder ----

    /**
     * 数值字段（DECIMAL）masking + 读访问审计。授权角色见明文 → 委托 {@link MaskAuditRecorder}
     * 记录披露事件；非授权返 {@code null}。
     */
    public static BigDecimal maskDecimal(BigDecimal value, Set<String> authorizedRoles,
                                         Object entity, String fieldName) {
        String matchedRole = findAuthorizedRole(authorizedRoles);
        if (matchedRole != null) {
            MaskAuditRecorder.recordDisclosureIfEnabled(entity, fieldName, matchedRole);
            return value;
        }
        return null;
    }

    /**
     * 数值字段（BIGINT）masking + 读访问审计。授权角色见明文 → 委托 {@link MaskAuditRecorder}
     * 记录披露事件；非授权返 {@code null}。
     */
    public static Long maskLong(Long value, Set<String> authorizedRoles,
                                Object entity, String fieldName) {
        String matchedRole = findAuthorizedRole(authorizedRoles);
        if (matchedRole != null) {
            MaskAuditRecorder.recordDisclosureIfEnabled(entity, fieldName, matchedRole);
            return value;
        }
        return null;
    }

    /**
     * VARCHAR 字段 masking + 读访问审计。授权角色见明文 → 委托 {@link MaskAuditRecorder}
     * 记录披露事件；非授权返打码字符串。
     */
    public static String maskString(String value, StringMaskFormat format, Set<String> authorizedRoles,
                                    Object entity, String fieldName) {
        String matchedRole = findAuthorizedRole(authorizedRoles);
        if (matchedRole != null) {
            MaskAuditRecorder.recordDisclosureIfEnabled(entity, fieldName, matchedRole);
            return value;
        }
        return format.mask(value);
    }

    // ---- 授权判定（fail-closed：无用户上下文 = 非授权 = 打码）----

    public static boolean isAuthorized(Set<String> authorizedRoles) {
        return findAuthorizedRole(authorizedRoles) != null;
    }

    /**
     * 返回命中的授权角色（roleId 字面），无授权返 {@code null}（fail-closed）。
     *
     * <p>E4.2 引入：匹配的首个授权角色用于审计记录的 {@code authorizedRole} 字段。
     */
    public static String findAuthorizedRole(Set<String> authorizedRoles) {
        if (authorizedRoles == null || authorizedRoles.isEmpty()) {
            return null;
        }
        IUserContext ctx = IUserContext.get();
        if (ctx == null) {
            return null;
        }
        for (String role : authorizedRoles) {
            if (ctx.isUserInRole(role)) {
                return role;
            }
        }
        return null;
    }
}
