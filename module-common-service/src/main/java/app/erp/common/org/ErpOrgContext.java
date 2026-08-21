package app.erp.common.org;

import io.nop.api.core.config.AppConfig;
import io.nop.api.core.context.ContextProvider;
import io.nop.api.core.context.IContext;
import io.nop.core.context.IServiceContext;

/**
 * 当前组织 id 解析器（plan 2026-07-30-0841-3-r1-29，P1-MA2-093/094 Phase 1 Explore 选定路径 (a)）。
 *
 * <p>从 app 层 {@link IContext} attribute 解析 {@code currentOrgId}（{@link ErpOrgIsolationConstants#CONTEXT_ATTR_CURRENT_ORG_ID}），
 * 不修改 nop-entropy 平台 {@code IServiceContext}/{@code IContext} 接口（平台内核变更归 successor）。
 *
 * <p>orgId 语义为 String（id-string 迁移 M1.3，plan 2026-08-21-1045-2，先于各域 orgId 列迁移）。
 * 过渡期宽容：未迁移写入方仍可能向 context attr 放 Long/Number，读取经 {@link #toStringValue(Object)}
 * 归一为 String；非法/空白/null 静默返回 null（校验式契约，与迁移前 toLong 的 no-op 语义对齐）。
 *
 * <p>部署时由登录链路（{@code AuthHttpServerFilter} 等价扩展点）或调用方将 orgId 写入 context；
 * 测试时经 {@link #setCurrentOrgId(IServiceContext, String)} 直接置入。后续可扩展为
 * {@code IUserContext.getDeptId()} → 组织映射表的解析（nop-auth 关联，触发条件：多公司生产部署）。
 */
public final class ErpOrgContext {

    private ErpOrgContext() {
    }

    /** 隔离总开关是否开启（{@link ErpOrgIsolationConstants#CONFIG_ORG_ISOLATION_ENABLED}，默认 false）。 */
    public static boolean isIsolationEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(ErpOrgIsolationConstants.CONFIG_ORG_ISOLATION_ENABLED, Boolean.FALSE));
    }

    /** 从 {@link IServiceContext} 解析当前组织 id（String 语义）；隔离关闭或未设置时返回 null（调用方据此 no-op）。 */
    public static String currentOrgId(IServiceContext context) {
        if (context == null) {
            return readFromProvider();
        }
        Object value = context.getAttribute(ErpOrgIsolationConstants.CONTEXT_ATTR_CURRENT_ORG_ID);
        return toStringValue(value);
    }

    /** 仅当隔离开启且 currentOrgId 已设置时返回 true。 */
    public static boolean isActive(IServiceContext context) {
        return isIsolationEnabled() && currentOrgId(context) != null;
    }

    /** 测试 / 部署链路向 context 写入当前组织 id（String 语义）。 */
    public static void setCurrentOrgId(IServiceContext context, String orgId) {
        if (context != null) {
            context.setAttribute(ErpOrgIsolationConstants.CONTEXT_ATTR_CURRENT_ORG_ID, orgId);
        }
    }

    private static String readFromProvider() {
        Object value = ContextProvider.getContextAttr(ErpOrgIsolationConstants.CONTEXT_ATTR_CURRENT_ORG_ID);
        return toStringValue(value);
    }

    /**
     * 过渡期宽容归一：String 先做数字合法性校验，合法直接返回、非法返回 null；Number 经 {@code String.valueOf}；
     * null/空白返回 null（非法输入静默 no-op，供本包隔离拦截器对列值做 String 归一比较）。
     */
    static String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return String.valueOf(value);
        }
        String s = value.toString().trim();
        if (s.isEmpty() || !isSignedDigits(s)) {
            return null;
        }
        return s;
    }

    private static boolean isSignedDigits(String s) {
        int i = s.charAt(0) == '-' || s.charAt(0) == '+' ? 1 : 0;
        if (i == s.length()) {
            return false;
        }
        for (; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
