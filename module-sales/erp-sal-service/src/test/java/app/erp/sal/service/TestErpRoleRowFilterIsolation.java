package app.erp.sal.service;

import app.erp.common.auth.ErpRoleDataAuthChecker;
import app.erp.common.auth.ErpRoleDataAuthConstants;
import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.context.ContextProvider;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.in;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 角色侧行级数据权限负向隔离测试（plan 2026-07-31-1023-3-r3-4，P1-MA6-002 Phase 3 Proof）。
 *
 * <p>验证 config-gated {@link ErpRoleDataAuthChecker} 的行级隔离行为（经 {@code IErpSalOrderBiz.findList}
 * 走 {@code CrudBizModel} 管道 → {@code AuthHelper.appendFilter}）：
 * <ul>
 *   <li>灰度 OFF（默认）：业务员可见他人创建的单据（无 filter 附加，零回归）；</li>
 *   <li>灰度 ON + 业务员：只看 {@code createdBy == 自己 userId} 的单据（A 查 B 的数据 → 空）；</li>
 *   <li>灰度 ON + 管理员：全量可见（admin role-auth 首位无 filter）；</li>
 *   <li>灰度 OFF 回归：业务员再次可见全部。</li>
 * </ul>
 *
 * <p>上下文对齐：createdBy 经平台 {@code OrmTimestampHelper.onCreate} auto-stamp 读
 * {@code ContextProvider.currentContext().getUserRefNo()}（autotest 默认 "autotest-ref"），而 filter EL
 * {@code ${userContext.userId}} 读 {@code IUserContext.getUserId()}。本测试同步设置两者为同一 userId，
 * 使 stamp 与 filter 对齐（模拟生产 login 同时设置 IUserContext 与 context.userRefNo）。
 *
 * <p>config 路径：erp-sal-service 测试 VFS 无 {@code /nop/main/auth/app.data-auth.xml}（聚合文件在 app-erp-all），
 * 故翻转 {@code nop.auth.data-auth-config-path} 直指 sales 规则文件。checker 跨测试共享单例，避免
 * {@code DefaultDataAuthChecker} 的 data-auth-cache 全局重复注册。
 */
@NopTestConfig(localDb = true, initDatabaseSchema = OptionalBoolean.TRUE, enableActionAuth = OptionalBoolean.FALSE)
public class TestErpRoleRowFilterIsolation extends JunitAutoTestCase {

    private static final String CFG_ENABLED = ErpRoleDataAuthConstants.CONFIG_ROLE_ROW_FILTER_ENABLED;
    private static final String CFG_DATA_AUTH_PATH = "nop.auth.data-auth-config-path";
    private static final String SALES_DATA_AUTH_PATH = "/erp/sal/auth/erp-sal.data-auth.xml";
    private static final String DEFAULT_DATA_AUTH_PATH = "/nop/main/auth/app.data-auth.xml";
    private static final String ROLE_SALESPERSON = "销售员";
    private static final String ROLE_ADMIN = "管理员";

    private static ErpRoleDataAuthChecker SHARED_CHECKER;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpSalOrderBiz salOrderBiz;

    @BeforeEach
    void setSalesDataAuthPath() {
        AppConfig.getConfigProvider().assignConfigValue(CFG_DATA_AUTH_PATH, SALES_DATA_AUTH_PATH);
    }

    @AfterEach
    void resetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "false");
        AppConfig.getConfigProvider().assignConfigValue(CFG_DATA_AUTH_PATH, DEFAULT_DATA_AUTH_PATH);
        IUserContext.set(null);
        ContextProvider.getOrCreateContext().setUserRefNo(null);
    }

    @Test
    public void testRoleRowFilterIsolatesByCreator() {
        runAs("saleA", Set.of(ROLE_SALESPERSON), () -> ormTemplate.runInSession(() -> seedOrder("ORD-ISO-A")));
        runAs("saleB", Set.of(ROLE_SALESPERSON), () -> ormTemplate.runInSession(() -> seedOrder("ORD-ISO-B")));

        // 1. 灰度 OFF：checker 未启用（ctx 不挂 checker），saleA 可见两单（零回归）
        IServiceContext ctxOff = ctxFor("saleA", Set.of(ROLE_SALESPERSON));
        assertEquals(2, queryByCodes(ctxOff, "ORD-ISO-A", "ORD-ISO-B").size(),
                "灰度 OFF：两单均可见（零回归）");

        // 2. 灰度 ON + 业务员 saleA：仅自己创建的 ORD-ISO-A（A 查 B 的数据被过滤）
        ErpRoleDataAuthChecker checker = getChecker();
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "true");

        IServiceContext ctxA = ctxFor("saleA", Set.of(ROLE_SALESPERSON));
        ctxA.setDataAuthChecker(checker);
        List<ErpSalOrder> own = queryByCodes(ctxA, "ORD-ISO-A", "ORD-ISO-B");
        assertEquals(1, own.size(), "灰度 ON + 业务员 saleA：仅自己创建的单据（B 的数据被隔离）");
        assertEquals("ORD-ISO-A", own.get(0).getCode());

        // 3. 灰度 ON + 管理员：全量可见
        IServiceContext ctxAdmin = ctxFor("adminUser", Set.of(ROLE_ADMIN));
        ctxAdmin.setDataAuthChecker(checker);
        assertEquals(2, queryByCodes(ctxAdmin, "ORD-ISO-A", "ORD-ISO-B").size(),
                "灰度 ON + 管理员：全量可见");

        // 4. 灰度 OFF 回归：saleA 再次可见两单
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "false");
        IServiceContext ctxOff2 = ctxFor("saleA", Set.of(ROLE_SALESPERSON));
        assertEquals(2, queryByCodes(ctxOff2, "ORD-ISO-A", "ORD-ISO-B").size(),
                "灰度 OFF 回归：两单恢复可见");
    }

    /** 管理员（仅 管理员 角色）全见——证明 admin role-auth 声明在 user 兜底之前，不被 user 兜底 shadow。 */
    @Test
    public void testAdminSeesAllAndUserFallbackNoShadow() {
        runAs("saleA", Set.of(ROLE_SALESPERSON), () -> ormTemplate.runInSession(() -> seedOrder("ORD-ISO-C")));

        ErpRoleDataAuthChecker checker = getChecker();
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "true");

        IServiceContext ctxAdmin = ctxFor("boss", Set.of(ROLE_ADMIN));
        ctxAdmin.setDataAuthChecker(checker);
        assertTrue(queryByCodes(ctxAdmin, "ORD-ISO-C").size() >= 1,
                "管理员应全量可见（admin role-auth 首位匹配，未被 user 兜底 shadow）");
    }

    private ErpRoleDataAuthChecker getChecker() {
        if (SHARED_CHECKER == null) {
            SHARED_CHECKER = new ErpRoleDataAuthChecker();
            SHARED_CHECKER.setDaoProvider(daoProvider);
        }
        return SHARED_CHECKER;
    }

    /** 同步设置 IUserContext（filter EL 源）与 ContextProvider.userRefNo（createdBy stamp 源）。 */
    private void applyUser(String userId, Set<String> roles) {
        UserContextImpl uc = new UserContextImpl();
        uc.setUserId(userId);
        uc.setRoles(roles);
        IUserContext.set(uc);
        ContextProvider.getOrCreateContext().setUserRefNo(userId);
    }

    private IServiceContext ctxFor(String userId, Set<String> roles) {
        applyUser(userId, roles);
        return new ServiceContextImpl();
    }

    private void runAs(String userId, Set<String> roles, Runnable fn) {
        applyUser(userId, roles);
        try {
            fn.run();
        } finally {
            IUserContext.set(null);
            ContextProvider.getOrCreateContext().setUserRefNo(null);
        }
    }

    private List<ErpSalOrder> queryByCodes(IServiceContext ctx, String... codes) {
        QueryBean q = new QueryBean();
        q.addFilter(in("code", Arrays.asList(codes)));
        return salOrderBiz.findList(q, null, ctx);
    }

    private void seedOrder(String code) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder o = dao.newEntity();
        o.setCode(code);
        o.setCustomerId(1L);
        o.setBusinessDate(LocalDate.of(2026, 7, 1));
        o.setCurrencyId(1L);
        o.setDocStatus("DRAFT");
        o.setApproveStatus("UNSUBMITTED");
        dao.saveEntity(o);
    }
}
