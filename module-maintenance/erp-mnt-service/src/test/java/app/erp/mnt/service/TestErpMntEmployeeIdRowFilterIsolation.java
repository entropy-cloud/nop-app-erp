package app.erp.mnt.service;

import app.erp.common.auth.ErpRoleDataAuthChecker;
import app.erp.common.auth.ErpRoleDataAuthConstants;
import app.erp.mnt.biz.IErpMntVisitBiz;
import app.erp.mnt.dao.entity.ErpMntVisit;
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
 * E2.2 employee-id 域列行级过滤后端 Proof（plan 2026-08-11-0915-1 Phase 2）。
 *
 * <p>验证 employee-id 域列（{@code ErpMntVisit.assignedTo}，BIGINT ref {@code ErpMdEmployee.id}）
 * 经默认等效方案（user.id==employee.id 种子对齐）后，{@code eq(assignedTo, ${userContext.userId})}
 * 整数直比行级隔离行为：
 * <ul>
 *   <li>灰度 OFF（默认）：维护人员可见他人维护访问（无 filter 附加，零回归）；</li>
 *   <li>灰度 ON + 维护人员 A：仅见 {@code assignedTo == A.userId} 的维护访问（A 查 B 的数据 → 空）；</li>
 *   <li>灰度 ON + 管理员：全量可见（admin role-auth 首位无 filter）；</li>
 *   <li>灰度 OFF 回归：维护人员再次可见全部。</li>
 * </ul>
 *
 * <p>机制同源性：与 {@code TestErpRoleRowFilterIsolation}（sal/createdBy userId 域列）同范式，
 * 区别仅 filter 列从 createdBy（VARCHAR）改为 assignedTo（BIGINT employee-id）——证明 mnt 域 employee-id
 * 列规则等价生效。
 *
 * <p>E2E 边界：{@code ErpMntVisit-main} 在 mnt action-auth SUBM {@code mnt-work}
 * （roles=维护主管/维护人员）下 → 维护人员有 query 授权，E2E proof 可跑（见 e2-2 smoke spec）。
 * 后端 Proof 不依赖 action-auth（enableActionAuth=FALSE）。
 */
@NopTestConfig(localDb = true, initDatabaseSchema = OptionalBoolean.TRUE, enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntEmployeeIdRowFilterIsolation extends JunitAutoTestCase {

    private static final String CFG_ENABLED = ErpRoleDataAuthConstants.CONFIG_ROLE_ROW_FILTER_ENABLED;
    private static final String CFG_DATA_AUTH_PATH = "nop.auth.data-auth-config-path";
    private static final String MNT_DATA_AUTH_PATH = "/erp/mnt/auth/erp-mnt.data-auth.xml";
    private static final String DEFAULT_DATA_AUTH_PATH = "/nop/main/auth/app.data-auth.xml";
    private static final String ROLE_TECHNICIAN = "维护人员";
    private static final String ROLE_ADMIN = "管理员";

    private static ErpRoleDataAuthChecker SHARED_CHECKER;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpMntVisitBiz mntVisitBiz;

    @BeforeEach
    void setMntDataAuthPath() {
        AppConfig.getConfigProvider().assignConfigValue(CFG_DATA_AUTH_PATH, MNT_DATA_AUTH_PATH);
    }

    @AfterEach
    void resetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "false");
        AppConfig.getConfigProvider().assignConfigValue(CFG_DATA_AUTH_PATH, DEFAULT_DATA_AUTH_PATH);
        IUserContext.set(null);
        ContextProvider.getOrCreateContext().setUserRefNo(null);
    }

    @Test
    public void testEmployeeIdRowFilterIsolatesByAssignedTo() {
        // techA userId=17 (== employee.id=17 种子对齐), techB userId=18
        runAs("17", Set.of(ROLE_TECHNICIAN), () -> ormTemplate.runInSession(() -> seedVisit("MNT-ISO-A", 17L)));
        runAs("18", Set.of(ROLE_TECHNICIAN), () -> ormTemplate.runInSession(() -> seedVisit("MNT-ISO-B", 18L)));

        // 1. 灰度 OFF：checker 未启用，techA 可见两单（零回归）
        IServiceContext ctxOff = ctxFor("17", Set.of(ROLE_TECHNICIAN));
        assertEquals(2, queryByCodes(ctxOff, "MNT-ISO-A", "MNT-ISO-B").size(),
                "灰度 OFF：两单均可见（零回归）");

        // 2. 灰度 ON + 维护人员 A（userId=17）：仅自己 assignedTo=17 的单（B 的数据被过滤）
        ErpRoleDataAuthChecker checker = getChecker();
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "true");

        IServiceContext ctxA = ctxFor("17", Set.of(ROLE_TECHNICIAN));
        ctxA.setDataAuthChecker(checker);
        List<ErpMntVisit> own = queryByCodes(ctxA, "MNT-ISO-A", "MNT-ISO-B");
        assertEquals(1, own.size(), "灰度 ON + 维护人员 A：仅自己 assignedTo=17 的单（B 的数据被隔离）");
        assertEquals("MNT-ISO-A", own.get(0).getCode());

        // 3. 灰度 ON + 管理员：全量可见
        IServiceContext ctxAdmin = ctxFor("adminUser", Set.of(ROLE_ADMIN));
        ctxAdmin.setDataAuthChecker(checker);
        assertEquals(2, queryByCodes(ctxAdmin, "MNT-ISO-A", "MNT-ISO-B").size(),
                "灰度 ON + 管理员：全量可见");

        // 4. 灰度 OFF 回归：techA 再次可见两单
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "false");
        IServiceContext ctxOff2 = ctxFor("17", Set.of(ROLE_TECHNICIAN));
        assertEquals(2, queryByCodes(ctxOff2, "MNT-ISO-A", "MNT-ISO-B").size(),
                "灰度 OFF 回归：两单恢复可见");
    }

    /** 管理员全见——证明 admin role-auth 声明在 user 兜底之前，不被 user 兜底 shadow。 */
    @Test
    public void testAdminSeesAllAndUserFallbackNoShadow() {
        runAs("17", Set.of(ROLE_TECHNICIAN), () -> ormTemplate.runInSession(() -> seedVisit("MNT-ISO-C", 17L)));

        ErpRoleDataAuthChecker checker = getChecker();
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "true");

        IServiceContext ctxAdmin = ctxFor("boss", Set.of(ROLE_ADMIN));
        ctxAdmin.setDataAuthChecker(checker);
        assertTrue(queryByCodes(ctxAdmin, "MNT-ISO-C").size() >= 1,
                "管理员应全量可见（admin role-auth 首位匹配，未被 user 兜底 shadow）");
    }

    private ErpRoleDataAuthChecker getChecker() {
        if (SHARED_CHECKER == null) {
            SHARED_CHECKER = new ErpRoleDataAuthChecker();
            SHARED_CHECKER.setDaoProvider(daoProvider);
        }
        return SHARED_CHECKER;
    }

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

    private List<ErpMntVisit> queryByCodes(IServiceContext ctx, String... codes) {
        QueryBean q = new QueryBean();
        q.addFilter(in("code", Arrays.asList(codes)));
        return mntVisitBiz.findList(q, null, ctx);
    }

    private void seedVisit(String code, Long assignedTo) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        ErpMntVisit o = dao.newEntity();
        o.setCode(code);
        o.setEquipmentId(1L);
        o.setVisitDate(LocalDate.of(2026, 8, 11));
        o.setStatus("PLANNED");
        o.setAssignedTo(assignedTo);
        dao.saveEntity(o);
    }
}
