package io.nop.app.all.auth;

import io.nop.core.initialize.ICoreInitializer;
import io.nop.core.initialize.impl.ReflectionHelperMethodInitializer;
import io.nop.core.initialize.impl.VirtualFileSystemInitializer;
import io.nop.core.lang.xml.XNode;
import io.nop.core.resource.IResource;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.xlang.initialize.RegisterModelCoreInitializer;
import io.nop.xlang.initialize.XLangCoreInitializer;
import io.nop.xlang.xdsl.DslNodeLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 角色侧行级数据权限结构断言测试（plan 2026-07-31-1023-3-r3-4，P1-MA6-002 Phase 3 Proof）。
 *
 * <p>用平台运行时同一机制（{@link DslNodeLoader} 执行 {@code x:extends} 节点级合并）加载聚合的
 * {@code /nop/main/auth/app.data-auth.xml}，验证：
 * <ul>
 *   <li>sales（6 obj，createdBy）+ quality（1 obj，ownerId）规则经聚合合并后全部出现；</li>
 *   <li>每 obj 三层 role-auth：管理员无 filter + 角色带 {@code <filter><eq name="列" value="${userContext.userId}"/></filter>}
 *       + user 兜底无 filter（防 fail-closed）；</li>
 *   <li>EL 表达式为正确的 {@code ${userContext.userId}}（scope 变量 = IUserContext），而非无效的
 *       {@code ${$context.user.userId}}（$context→IContext 无 getUser()，Phase 1 修正的 bug）；</li>
 *   <li>过滤列名与列域分类正确（sales createdBy / quality ownerId，均为 userId 域）。</li>
 * </ul>
 *
 * <p>不依赖运行时 enforcement——结构 + EL 正确性即规则正确性证明。负向隔离行为由
 * {@code TestErpRoleRowFilterIsolation}（erp-sal-service）覆盖。
 */
public class TestErpDataAuthStructure {

    private static final String DATA_AUTH_PATH = "/nop/main/auth/app.data-auth.xml";

    private static final List<String> SALES_OBJS = Arrays.asList(
            "ErpSalOrder", "ErpSalQuotation", "ErpSalDelivery",
            "ErpSalInvoice", "ErpSalReceipt", "ErpSalReturn");

    private static final String QUALITY_OBJ = "ErpQaRiskRegister";

    /** 业务员过滤列；质检员过滤列（均为 userId 域）。 */
    private static final String SALES_FILTER_COL = "createdBy";
    private static final String QUALITY_FILTER_COL = "ownerId";
    private static final String ROLE_SALESPERSON = "业务员";
    private static final String ROLE_INSPECTOR = "质检员";
    private static final String ROLE_ADMIN = "管理员";

    private static final List<ICoreInitializer> INITIALIZERS = new ArrayList<>();

    @BeforeAll
    static void initCore() {
        INITIALIZERS.add(new ReflectionHelperMethodInitializer());
        INITIALIZERS.add(new XLangCoreInitializer());
        INITIALIZERS.add(new VirtualFileSystemInitializer());
        INITIALIZERS.add(new RegisterModelCoreInitializer());
        INITIALIZERS.forEach(ICoreInitializer::initialize);
    }

    @AfterAll
    static void destroyCore() {
        for (int i = INITIALIZERS.size() - 1; i >= 0; i--) {
            INITIALIZERS.get(i).destroy();
        }
        INITIALIZERS.clear();
    }

    @Test
    public void testAggregatedDataAuthMergesSalesAndQuality() {
        XNode root = loadMergedDataAuth();
        List<XNode> objs = root.childByTag("objs").childrenByTag("obj");
        Set<String> names = new HashSet<>();
        for (XNode o : objs) {
            names.add(o.attrText("name"));
        }
        for (String name : SALES_OBJS) {
            assertTrue(names.contains(name), "sales obj missing in merged data-auth: " + name);
        }
        assertTrue(names.contains(QUALITY_OBJ), "quality obj missing in merged data-auth: " + QUALITY_OBJ);
        assertEquals(SALES_OBJS.size() + 1, objs.size(),
                "expected " + (SALES_OBJS.size() + 1) + " objs (6 sales + 1 quality), got: " + names);
    }

    @Test
    public void testSalesObjsThreeTierRoleAuthWithCreatedByIdFilter() {
        XNode root = loadMergedDataAuth();
        for (String bizObj : SALES_OBJS) {
            assertThreeTierStructure(root, bizObj, ROLE_SALESPERSON, SALES_FILTER_COL);
        }
    }

    @Test
    public void testQualityObjThreeTierRoleAuthWithOwnerIdFilter() {
        XNode root = loadMergedDataAuth();
        assertThreeTierStructure(root, QUALITY_OBJ, ROLE_INSPECTOR, QUALITY_FILTER_COL);
    }

    /** 全局禁止无效 EL：合并后的 data-auth 不应出现 ${$context.user.userId}（Phase 1 修正的 bug）。 */
    @Test
    public void testNoInvalidContextUserExpression() {
        XNode root = loadMergedDataAuth();
        String xml = root.xml();
        assertFalse(xml.contains("$context.user.userId"),
                "invalid EL ${$context.user.userId} found in merged data-auth (IContext has no getUser()):\n" + xml);
        assertTrue(xml.contains("userContext.userId"),
                "correct EL ${userContext.userId} not found in merged data-auth");
    }

    // ===== helpers =====

    private XNode loadMergedDataAuth() {
        IResource resource = VirtualFileSystem.instance().getResource(DATA_AUTH_PATH);
        assertTrue(resource.exists(), "app.data-auth.xml not found in VFS: " + DATA_AUTH_PATH);
        XNode merged = DslNodeLoader.INSTANCE.loadFromResource(resource).getNode();
        assertNotNull(merged, "merged data-auth node is null");
        return merged;
    }

    private void assertThreeTierStructure(XNode root, String bizObj, String restrictRole, String filterCol) {
        XNode obj = findObj(root, bizObj);
        assertNotNull(obj, "obj must exist: " + bizObj);

        XNode admin = findRoleAuthByRole(obj, ROLE_ADMIN);
        assertNotNull(admin, bizObj + " must have admin(管理员) role-auth");
        assertNull(admin.childByTag("filter"), bizObj + " admin must have no filter (full access)");

        XNode restrictive = findRoleAuthByRole(obj, restrictRole);
        assertNotNull(restrictive, bizObj + " must have " + restrictRole + " role-auth");
        XNode filter = restrictive.childByTag("filter");
        assertNotNull(filter, bizObj + " " + restrictRole + " role-auth must have filter");
        XNode predicate = filter.child(0);
        assertNotNull(predicate, bizObj + " filter must have a predicate");
        assertEquals("eq", predicate.getTagName(), bizObj + " filter predicate must be 'eq'");
        assertEquals(filterCol, predicate.attrText("name"),
                bizObj + " filter column must be " + filterCol);
        String value = predicate.attrText("value");
        assertNotNull(value, bizObj + " filter must have value expression");
        assertTrue(value.contains("userContext.userId"),
                bizObj + " filter value must reference userContext.userId (got: " + value + ")");

        XNode fallback = findRoleAuthByRole(obj, "user");
        assertNotNull(fallback, bizObj + " must have user fallback role-auth (fail-closed guard)");
        assertNull(fallback.childByTag("filter"),
                bizObj + " user fallback must have no filter");
    }

    private XNode findObj(XNode root, String name) {
        XNode objs = root.childByTag("objs");
        if (objs == null) return null;
        for (XNode o : objs.childrenByTag("obj")) {
            if (name.equals(o.attrText("name"))) {
                return o;
            }
        }
        return null;
    }

    private XNode findRoleAuthByRole(XNode obj, String roleId) {
        XNode roleAuths = obj.childByTag("role-auths");
        if (roleAuths == null) return null;
        for (XNode ra : roleAuths.childrenByTag("role-auth")) {
            String roleIds = ra.attrText("roleIds");
            if (roleIds != null && roleIds.contains(roleId)) {
                return ra;
            }
        }
        return null;
    }
}
