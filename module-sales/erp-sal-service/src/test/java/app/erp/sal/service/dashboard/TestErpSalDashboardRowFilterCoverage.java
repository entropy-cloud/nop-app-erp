package app.erp.sal.service.dashboard;

import app.erp.common.auth.ErpRoleDataAuthChecker;
import app.erp.common.auth.ErpRoleDataAuthConstants;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.context.ContextProvider;
import io.nop.api.core.time.CoreMetrics;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.in;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E3.1b 看板行级安全运行时抽样实测（roadmap E3.1b / plan 2026-08-26-0735-2 Phase 2 Proof）。
 *
 * <p>核实 role-row-filter 对 {@code ErpSalDashboard__getDashboardKpi} 的实际覆盖：
 * <ul>
 *   <li><b>静态表征（实证于运行时）</b>：看板聚合走 {@code daoProvider.daoFor(...).findAllByQuery/countByQuery}
 *       直连 DAO，不经 {@code CrudBizModel.prepareFindPageQuery} 的 {@code AuthHelper.appendFilter} 检查点，
 *       QueryBean→SQL 编译 {@code enableFilter=false} 亦不产生 ORM 层 FILTER marker →
 *       销售员上下文下 {@code orderCount/salesAmount} 跨用户聚合（与 admin 同值）——缺口实证；</li>
 *   <li><b>对照面</b>：同一销售员上下文经 {@code IErpSalOrderBiz.findList}（CrudBizModel 管道）仅见自己创建的单据
 *       ——检查点在 CRUD 路径生效，证明差异来自查询路径而非规则失效；</li>
 *   <li><b>管道内路径</b>：{@code arBalance} 经 {@code IErpFinArApItemBiz.findOpenItems} 走 CrudBizModel 管道
 *       （过检查点；ErpFinArApItem 无销售员规则 → filter=null 全见，finance 全见设计决定）。</li>
 * </ul>
 *
 * <p>上下文对齐 {@code TestErpRoleRowFilterIsolation}：createdBy stamp（userRefNo）与 filter EL
 * （{@code ${userContext.userId}}）同步设为同一 userId；checker 跨测试共享单例。
 * config 路径指 sales 规则文件（erp-sal-service 测试 VFS 无聚合 app.data-auth.xml）。
 */
@NopTestConfig(localDb = true, initDatabaseSchema = OptionalBoolean.TRUE, enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalDashboardRowFilterCoverage extends JunitAutoTestCase {

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
    ErpSalDashboardBizModel dashboardBiz;
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
    public void testDashboardKpiAggregatesAcrossUsersWhileCrudPathFiltered() {
        // 两名销售员各创建 1 张 ACTIVE 订单 + 1 张已过账发票（100 / 200）
        runAs("saleA", Set.of(ROLE_SALESPERSON), () -> ormTemplate.runInSession(() -> {
            seedOrder("501", "521", ErpSalConstants.DOC_STATUS_ACTIVE);
            seedInvoice("511", "521", new BigDecimal("100"), true);
        }));
        runAs("saleB", Set.of(ROLE_SALESPERSON), () -> ormTemplate.runInSession(() -> {
            seedOrder("502", "522", ErpSalConstants.DOC_STATUS_ACTIVE);
            seedInvoice("512", "522", new BigDecimal("200"), true);
        }));

        ErpRoleDataAuthChecker checker = getChecker();
        AppConfig.getConfigProvider().assignConfigValue(CFG_ENABLED, "true");

        // 1. 缺口实证：销售员 saleA 的看板 KPI 跨用户聚合（= admin 同值，未经 role-row-filter）
        IServiceContext ctxA = ctxFor("saleA", Set.of(ROLE_SALESPERSON));
        ctxA.setDataAuthChecker(checker);
        Map<String, Object> kpiA = dashboardBiz.getDashboardKpi(null, null, ctxA);
        assertEquals(2L, kpiA.get("orderCount"), "缺口：orderCount 聚合 saleB 的订单（直连 DAO 不过检查点）");
        assertEquals(0, ((BigDecimal) kpiA.get("salesAmount")).compareTo(new BigDecimal("300")),
                "缺口：salesAmount 聚合 saleB 的发票金额");

        IServiceContext ctxAdmin = ctxFor("dashAdmin", Set.of(ROLE_ADMIN));
        ctxAdmin.setDataAuthChecker(checker);
        Map<String, Object> kpiAdmin = dashboardBiz.getDashboardKpi(null, null, ctxAdmin);
        assertEquals(kpiAdmin.get("orderCount"), kpiA.get("orderCount"),
                "销售员与 admin 看板同值（行级过滤对看板无效）");
        assertEquals(0, ((BigDecimal) kpiAdmin.get("salesAmount")).compareTo(
                (BigDecimal) kpiA.get("salesAmount")), "销售员与 admin 看板销售额同值");

        // 2. 对照面：同一销售员上下文走 CrudBizModel 管道 → 仅见自己创建的单据
        List<ErpSalOrder> own = queryByCodes(ctxA, "SO-501", "SO-502");
        assertEquals(1, own.size(), "对照：CRUD 路径 role-row-filter 生效（仅 saleA 自己的单据）");
        assertEquals("SO-501", own.get(0).getCode());
        assertEquals(2, queryByCodes(ctxAdmin, "SO-501", "SO-502").size(),
                "对照：admin 经 CrudBizModel 管道全量可见");

        // 3. 管道内路径：arBalance 经 IErpFinArApItemBiz.findOpenItems（CrudBizModel 管道）可达且不抛错
        assertEquals(0, ((BigDecimal) kpiA.get("arBalance")).compareTo(BigDecimal.ZERO),
                "arBalance 走 I*Biz 管道路径（过检查点；无 finance 规则 → 全见，无种子数据 = 0）");
    }

    /** 灰度 OFF 回归：checker 未挂 → 看板照常聚合（现状行为基线，非缺陷）。 */
    @Test
    public void testDashboardUnfilteredWhenGateOff() {
        runAs("saleC", Set.of(ROLE_SALESPERSON), () -> ormTemplate.runInSession(() ->
                seedOrder("503", "523", ErpSalConstants.DOC_STATUS_ACTIVE)));

        IServiceContext ctxOff = ctxFor("saleC", Set.of(ROLE_SALESPERSON));
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, ctxOff);
        assertEquals(1L, kpi.get("orderCount"), "灰度 OFF：正常计数（零回归基线）");
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

    private List<ErpSalOrder> queryByCodes(IServiceContext ctx, String... codes) {
        QueryBean q = new QueryBean();
        q.addFilter(in("code", Arrays.asList(codes)));
        return salOrderBiz.findList(q, null, ctx);
    }

    // ---------- seeds（对齐 TestErpSalDashboard 范式） ----------

    private void seedCustomer(String id, String code) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode(code);
        p.setName(code);
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private void seedInvoice(String id, String customerId, BigDecimal amount, boolean posted) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        ErpSalInvoice inv = dao.newEntity();
        inv.orm_propValue(1, id);
        inv.setCode("SI-" + id);
        inv.setOrgId("1");
        inv.setCustomerId(customerId);
        inv.setInvoiceNo("INV-" + id);
        inv.setBusinessDate(CoreMetrics.currentDate());
        inv.setCurrencyId("1");
        inv.setExchangeRate(BigDecimal.ONE);
        inv.setAmountSource(amount);
        inv.setAmountFunctional(amount);
        inv.setTotalAmount(amount);
        inv.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        inv.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        inv.setPosted(posted);
        dao.saveEntity(inv);
    }

    private void seedOrder(String id, String customerId, String docStatus) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("SO-" + id);
        o.setOrgId("1");
        o.setCustomerId(customerId);
        o.setBusinessDate(LocalDate.now());
        o.setCurrencyId("1");
        o.setExchangeRate(BigDecimal.ONE);
        o.setDocStatus(docStatus);
        o.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(o);
    }
}
