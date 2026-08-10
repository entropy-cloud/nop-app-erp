package app.erp.qa.service;

import app.erp.common.auth.ErpRoleDataAuthChecker;
import app.erp.common.auth.ErpRoleDataAuthConstants;
import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.dao.entity.ErpQaInspection;
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
 * <p>验证 employee-id 域列（{@code ErpQaInspection.inspectorId}，BIGINT ref {@code ErpMdEmployee.id}）
 * 经默认等效方案（user.id==employee.id 种子对齐）后，{@code eq(inspectorId, ${userContext.userId})}
 * 整数直比行级隔离行为：
 * <ul>
 *   <li>灰度 OFF（默认）：质检员可见他人检验单（无 filter 附加，零回归）；</li>
 *   <li>灰度 ON + 质检员 A：仅见 {@code inspectorId == A.userId} 的检验单（A 查 B 的数据 → 空）；</li>
 *   <li>灰度 ON + 管理员：全量可见（admin role-auth 首位无 filter）；</li>
 *   <li>灰度 OFF 回归：质检员再次可见全部。</li>
 * </ul>
 *
 * <p>机制同源性：与 {@code TestErpRoleRowFilterIsolation}（sal/createdBy userId 域列）同范式，
 * 区别仅 filter 列从 createdBy（VARCHAR）改为 inspectorId（BIGINT employee-id）——证明 employee-id 域列
 * 规则等价生效（DefaultDataAuthChecker + eq 整数比较）。
 *
 * <p>E2E 边界：{@code ErpQaInspection-main} 在 qa action-auth SUBM {@code qa-inspection}
 * （roles=质检员/质量主管）下 → 质检员有 query 授权，E2E proof 可跑（见 e2-2 smoke spec）。
 * 后端 Proof 不依赖 action-auth（enableActionAuth=FALSE）。
 */
@NopTestConfig(localDb = true, initDatabaseSchema = OptionalBoolean.TRUE, enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaEmployeeIdRowFilterIsolation extends JunitAutoTestCase {

    private static final String CFG_ENABLED = ErpRoleDataAuthConstants.CONFIG_ROLE_ROW_FILTER_ENABLED;
    private static final String CFG_DATA_AUTH_PATH = "nop.auth.data-auth-config-path";
    private static final String QA_DATA_AUTH_PATH = "/erp/qa/auth/erp-qa.data-auth.xml";
    private static final String DEFAULT_DATA_AUTH_PATH = "/nop/main/auth/app.data-auth.xml";
    private static final String ROLE_INSPECTOR = "质检员";
    private static final String ROLE_ADMIN = "管理员";

    private static ErpRoleDataAuthChecker SHARED_CHECKER;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpQaInspectionBiz qaInspectionBiz;

    @BeforeEach
    void setQaDataAuthPath() {
        AppConfig.getConfigProvider().assignConfigValue(CFG_DATA_AUTH_PATH, QA_DATA_AUTH_PATH);
    }

    @AfterEach
    void resetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "false");
        AppConfig.getConfigProvider().assignConfigValue(CFG_DATA_AUTH_PATH, DEFAULT_DATA_AUTH_PATH);
        IUserContext.set(null);
        ContextProvider.getOrCreateContext().setUserRefNo(null);
    }

    @Test
    public void testEmployeeIdRowFilterIsolatesByInspector() {
        // inspectorA userId=21 (== employee.id=21 种子对齐), inspectorB userId=22
        runAs("21", Set.of(ROLE_INSPECTOR), () -> ormTemplate.runInSession(() -> seedInspection("QA-ISO-A", 21L)));
        runAs("22", Set.of(ROLE_INSPECTOR), () -> ormTemplate.runInSession(() -> seedInspection("QA-ISO-B", 22L)));

        // 1. 灰度 OFF：checker 未启用，inspectorA 可见两单（零回归）
        IServiceContext ctxOff = ctxFor("21", Set.of(ROLE_INSPECTOR));
        assertEquals(2, queryByCodes(ctxOff, "QA-ISO-A", "QA-ISO-B").size(),
                "灰度 OFF：两单均可见（零回归）");

        // 2. 灰度 ON + 质检员 A（userId=21）：仅自己 inspectorId=21 的单（B 的数据被过滤）
        ErpRoleDataAuthChecker checker = getChecker();
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "true");

        IServiceContext ctxA = ctxFor("21", Set.of(ROLE_INSPECTOR));
        ctxA.setDataAuthChecker(checker);
        List<ErpQaInspection> own = queryByCodes(ctxA, "QA-ISO-A", "QA-ISO-B");
        assertEquals(1, own.size(), "灰度 ON + 质检员 A：仅自己 inspectorId=21 的单（B 的数据被隔离）");
        assertEquals("QA-ISO-A", own.get(0).getCode());

        // 3. 灰度 ON + 管理员：全量可见
        IServiceContext ctxAdmin = ctxFor("adminUser", Set.of(ROLE_ADMIN));
        ctxAdmin.setDataAuthChecker(checker);
        assertEquals(2, queryByCodes(ctxAdmin, "QA-ISO-A", "QA-ISO-B").size(),
                "灰度 ON + 管理员：全量可见");

        // 4. 灰度 OFF 回归：inspectorA 再次可见两单
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "false");
        IServiceContext ctxOff2 = ctxFor("21", Set.of(ROLE_INSPECTOR));
        assertEquals(2, queryByCodes(ctxOff2, "QA-ISO-A", "QA-ISO-B").size(),
                "灰度 OFF 回归：两单恢复可见");
    }

    /** 管理员全见——证明 admin role-auth 声明在 user 兜底之前，不被 user 兜底 shadow。 */
    @Test
    public void testAdminSeesAllAndUserFallbackNoShadow() {
        runAs("21", Set.of(ROLE_INSPECTOR), () -> ormTemplate.runInSession(() -> seedInspection("QA-ISO-C", 21L)));

        ErpRoleDataAuthChecker checker = getChecker();
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "true");

        IServiceContext ctxAdmin = ctxFor("boss", Set.of(ROLE_ADMIN));
        ctxAdmin.setDataAuthChecker(checker);
        assertTrue(queryByCodes(ctxAdmin, "QA-ISO-C").size() >= 1,
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

    private List<ErpQaInspection> queryByCodes(IServiceContext ctx, String... codes) {
        QueryBean q = new QueryBean();
        q.addFilter(in("code", Arrays.asList(codes)));
        return qaInspectionBiz.findList(q, null, ctx);
    }

    private void seedInspection(String code, Long inspectorId) {
        IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
        ErpQaInspection o = dao.newEntity();
        o.setCode(code);
        o.setInspectionType("INCOMING");
        o.setMaterialId(1L);
        o.setBusinessDate(LocalDate.of(2026, 8, 11));
        o.setInspectionDate(LocalDate.of(2026, 8, 11));
        o.setInspectorId(inspectorId);
        o.setResult("PENDING");
        o.setDocStatus("DRAFT");
        o.setApproveStatus("UNSUBMITTED");
        dao.saveEntity(o);
    }
}
