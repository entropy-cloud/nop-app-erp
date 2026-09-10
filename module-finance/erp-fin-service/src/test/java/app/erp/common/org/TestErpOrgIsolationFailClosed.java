package app.erp.common.org;

import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.api.IBizObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * P3-CK-common-012-r3 org 隔离解析失败两态回归。
 *
 * <p>缺陷面：{@code resolveEntityHasOrgId} catch(Throwable)→false fail-open 零日志——解析失败
 * （类加载/dao/模型反射失败）与「实体无 orgId 列」混为一谈，隔离启用下静默跳过读侧隔离过滤器不可观测。
 *
 * <p>修复裁决：两态区分（无列 = 合法跳过；解析失败 = WARN + 可选 fail-closed）。
 * config {@code erp.multi-company.org-isolation-fail-closed}=true 时解析失败抛
 * {@code ERR_ORG_ISOLATION_RESOLVE_FAILED}（拒答优于未隔离放行）；缺省 fail-open 保持兼容但可观测。
 */
public class TestErpOrgIsolationFailClosed {

    private static final String FAIL_CLOSED_KEY = ErpOrgIsolationConstants.CONFIG_ORG_ISOLATION_FAIL_CLOSED;

    @AfterEach
    void resetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(FAIL_CLOSED_KEY, "false");
    }

    /** fail-closed 开启：解析失败实体拒答（抛 ERR_ORG_ISOLATION_RESOLVE_FAILED），不再静默放行。 */
    @Test
    public void testFailClosedThrowsOnResolveFailure() {
        AppConfig.getConfigProvider().assignConfigValue(FAIL_CLOSED_KEY, "true");
        ErpOrgIsolationQueryTransformer transformer = newTransformer();

        // java.lang.String 类存在但非 IDaoEntity 实体 → dao 解析失败（非「无 orgId 列」合法跳过态）
        NopException ex = assertThrows(NopException.class,
                () -> transformer.entityHasOrgId(fakeBizObj("java.lang.String")));
        assertNotNull(ex.getErrorCode());
        assertFalse(ex.getErrorCode().isEmpty(), "fail-closed 拒绝应携带领域错误码");
    }

    /** 缺省 fail-open：解析失败不阻断（兼容面），按无列跳过（WARN 可观测）。 */
    @Test
    public void testDefaultFailOpenReturnsFalseOnResolveFailure() {
        ErpOrgIsolationQueryTransformer transformer = newTransformer();
        assertFalse(transformer.entityHasOrgId(fakeBizObj("java.lang.String")),
                "fail-open 缺省：解析失败按无列跳过（不阻断查询），但已 WARN 可观测");
    }

    private ErpOrgIsolationQueryTransformer newTransformer() {
        ErpOrgIsolationQueryTransformer t = new ErpOrgIsolationQueryTransformer();
        // daoProvider 置 null：属解析失败态（daoProvider 缺失分支），足以驱动两态断言
        t.setDaoProvider(null);
        return t;
    }

    /** 构造仅暴露 entityName 的 IBizObject 测试桩（动态代理，避免实现全接口）。 */
    private IBizObject fakeBizObj(String entityName) {
        Object meta = Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{io.nop.xlang.xmeta.IObjMeta.class},
                (p, m, a) -> {
                    if ("getEntityName".equals(m.getName())) {
                        return entityName;
                    }
                    return defaultValue(m.getReturnType());
                });
        return (IBizObject) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IBizObject.class},
                (p, m, a) -> {
                    if ("getObjMeta".equals(m.getName())) {
                        return meta;
                    }
                    return defaultValue(m.getReturnType());
                });
    }

    private Object defaultValue(Class<?> type) {
        if (type == null || !type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        return null;
    }
}
