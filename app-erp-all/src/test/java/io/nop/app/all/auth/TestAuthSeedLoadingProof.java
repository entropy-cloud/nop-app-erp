package io.nop.app.all.auth;

import io.nop.api.core.ioc.BeanContainer;
import io.nop.core.initialize.CoreInitialization;
import io.nop.core.unittest.BaseTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * auth 表 CSV 种子加载 Proof（plan 2026-08-09-2107-1，P1.5b Phase 1 Proof2 + Phase 4 Proof）。
 *
 * <p>经平台 {@code DataInitInitializer}（生产部署期种子基建，{@code orm-defaults.beans.xml} 装配，
 * {@code nop.orm.init-database-data=true} 门控）加载 {@code _vfs/_init-data/} 下的 auth CSV 后，
 * 查询确认四项：
 * <ul>
 *   <li>(1) {@code nop_auth_role} 24 行（21 业务 + 3 平台）；</li>
 *   <li>(2) {@code nop_auth_user} nop 行 userId="1"（小整数存活——显式非空值经 seq null-guard 保留，
 *       Phase 1 Proof2）<b>且 STATUS=1（ACTIVE——{@code isAllowLogin} 拒绝非 ACTIVE）</b>；</li>
 *   <li>(3) {@code nop_auth_user_role} 绑定 (1, admin) 存在（B2 修复——平台 admin 角色非业务「管理员」）；</li>
 *   <li>(4) 密码往返：CSV 中的 SALT+PASSWORD 经 {@code passwordMatches("123")} 成立。</li>
 * </ul>
 *
 * <p>采用 {@code BaseTestCase} + 手动 {@code CoreInitialization.initialize()}（非 {@code JunitBaseTestCase}），
 * 因 NopJunitExtension 强制 ALL_LAZY 模式下 {@code DataBaseSchemaInitializer} 的 @PostConstruct 不先于
 * DB 访问 bean 运行（pre-existing 仓库行为）。手动初始化使用 eager 容器启动，schema 先于 DB 访问 bean 创建。
 */
public class TestAuthSeedLoadingProof extends BaseTestCase {

    @BeforeAll
    public static void initialize() {
        // 清理文件型 H2（生产 application.yaml 配 jdbc:h2:./db/erp），确保每次运行 fresh-DB。
        // 复用 playwright.config.ts:18 的 rm -f db/*.mv.db 模式。setTestConfig 的 in-memory 覆盖会被
        // CoreInitialization.initialize() 重新加载的 application.yaml datasource 覆盖，故用文件清理保 fresh。
        for (String suffix : new String[]{".mv.db", ".trace.db"}) {
            new java.io.File("db/erp" + suffix).delete();
        }
        setTestConfig("nop.orm.init-database-schema", true);
        setTestConfig("nop.orm.init-database-data", true);
        setTestConfig("nop.orm.init-database-data-location", "/_init-data/");
        CoreInitialization.initialize();
    }

    @AfterAll
    public static void destroy() {
        CoreInitialization.destroy();
    }

    @Test
    public void testRoleSeedLoads24Records() {
        IEntityDao<IOrmEntity> roleDao = daoForTable("nop_auth_role");
        List<IOrmEntity> roles = roleDao.findAll();
        assertEquals(24, roles.size(),
                "nop_auth_role 应加载 24 行（21 业务 + 3 平台），实际: " + roles.size());

        long businessCount = roles.stream()
                .filter(r -> !isAdminRole(r)).count();
        assertEquals(21, businessCount, "业务角色应为 21 个");
    }

    @Test
    public void testUserSeedSmallIntegerIdSurvivesAndActive() {
        IEntityDao<IOrmEntity> userDao = daoForTable("nop_auth_user");
        IOrmEntity nopUser = userDao.getEntityById("1");
        assertNotNull(nopUser, "nop 用户 userId='1' 必须存在（显式小整数经 seq null-guard 存活）");
        assertEquals("1", nopUser.orm_propValueByName("userId"),
                "userId 必须保留为 '1'（未被 seq 覆盖为 UUID）—— Phase 1 Proof2");
        assertEquals("nop", nopUser.orm_propValueByName("userName"));
        int status = (int) nopUser.orm_propValueByName("status");
        assertEquals(1, status,
                "STATUS 必须为 1 (ACTIVE)——isAllowLogin 拒绝非 ACTIVE，disabled 种子会使 P2.2a 登录失败");

        verifyPasswordRoundtrip(nopUser);
    }

    @Test
    public void testUserRoleBindingIsPlatformAdmin() {
        IEntityDao<IOrmEntity> userRoleDao = daoForTable("nop_auth_user_role");
        List<IOrmEntity> bindings = userRoleDao.findAll();
        boolean found = bindings.stream().anyMatch(b ->
                "1".equals(b.orm_propValueByName("userId"))
                        && "admin".equals(b.orm_propValueByName("roleId")));
        assertTrue(found, "nop_auth_user_role (userId=1, roleId=admin) 绑定必须存在——B2 修复：平台 admin 角色非业务「管理员」");
    }

    private void verifyPasswordRoundtrip(IOrmEntity nopUser) {
        String salt = (String) nopUser.orm_propValueByName("salt");
        String password = (String) nopUser.orm_propValueByName("password");
        assertNotNull(salt, "SALT 必须非空");
        assertNotNull(password, "PASSWORD 必须非空");

        io.nop.auth.core.password.IPasswordEncoder encoder = newPlatformEncoder();
        assertTrue(encoder.passwordMatches(salt, "123", password),
                "CSV 种子密码必须经 passwordMatches(salt, '123', password) 验证（明文=123，与 E2E fixture 一致）");
    }

    private io.nop.auth.core.password.IPasswordEncoder newPlatformEncoder() {
        io.nop.auth.core.password.CompositePasswordEncoder encoder =
                new io.nop.auth.core.password.CompositePasswordEncoder();
        encoder.setFirstEncoder(new io.nop.auth.core.password.SHA256PasswordEncoder());
        encoder.setSecondEncoder(new io.nop.auth.core.password.BCryptPasswordEncoder());
        return encoder;
    }

    private boolean isAdminRole(IOrmEntity role) {
        String roleId = (String) role.orm_propValueByName("roleId");
        return "admin".equals(roleId) || "nop-admin".equals(roleId) || "user".equals(roleId);
    }

    @SuppressWarnings("unchecked")
    private IEntityDao<IOrmEntity> daoForTable(String tableName) {
        IDaoProvider daoProvider = BeanContainer.getBeanByType(IDaoProvider.class);
        return daoProvider.daoForTable(tableName);
    }
}
