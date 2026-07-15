package app.erp.prj.service.dashboard;

import app.erp.prj.dao.entity.ErpPrjBudget;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 项目看板聚合（{@code ErpPrjDashboard__*}）集成测试。覆盖：在手项目数/预算/成本/预算执行率算术、
 * 状态分布、成本超支预警触发、项目延期预警触发/不触发、空数据集零值。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjDashboard extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpPrjDashboardBizModel dashboardBiz;

    @Test
    public void testKpiEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(CTX);
        assertEquals(0L, kpi.get("openProjectCount"));
        assertEquals(0, ((BigDecimal) kpi.get("totalBudget")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("incurredCost")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("executionRate")).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testKpiBudgetExecutionArithmetic() {
        ormTemplate.runInSession(() -> {
            // OPEN 项目 P1: 预算 1000, 成本 400
            seedProject(101L, "P-1", ErpPrjConstants.PROJECT_STATUS_OPEN, null);
            seedBudget(201L, 101L, new BigDecimal("1000"));
            seedCost(301L, 101L, new BigDecimal("400"));
            // OPEN 项目 P2: 预算 2000, 成本 1000
            seedProject(102L, "P-2", ErpPrjConstants.PROJECT_STATUS_OPEN, null);
            seedBudget(202L, 102L, new BigDecimal("2000"));
            seedCost(302L, 102L, new BigDecimal("1000"));
            // COMPLETED 项目 P3 不计入（预算 9999, 成本 9999）
            seedProject(103L, "P-3", ErpPrjConstants.PROJECT_STATUS_COMPLETED, null);
            seedBudget(203L, 103L, new BigDecimal("9999"));
            seedCost(303L, 103L, new BigDecimal("9999"));
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(CTX);
        // 2 个 OPEN 项目
        assertEquals(2L, kpi.get("openProjectCount"));
        // 预算 1000+2000=3000（P3 COMPLETED 不计入）
        assertEquals(0, ((BigDecimal) kpi.get("totalBudget")).compareTo(new BigDecimal("3000")));
        // 成本 400+1000=1400
        assertEquals(0, ((BigDecimal) kpi.get("incurredCost")).compareTo(new BigDecimal("1400")));
        // 执行率 1400/3000 ≈ 0.4667
        assertEquals(0, ((BigDecimal) kpi.get("executionRate")).compareTo(new BigDecimal("0.4667")));
    }

    @Test
    public void testProjectStatusDistribution() {
        ormTemplate.runInSession(() -> {
            seedProject(111L, "A", ErpPrjConstants.PROJECT_STATUS_OPEN, null);
            seedProject(112L, "B", ErpPrjConstants.PROJECT_STATUS_OPEN, null);
            seedProject(113L, "C", ErpPrjConstants.PROJECT_STATUS_COMPLETED, null);
        });
        List<Map<String, Object>> dist = dashboardBiz.getProjectStatusDistribution(CTX);
        assertEquals(2, dist.size(), "2 种状态");
        // OPEN 2 > COMPLETED 1 → OPEN 排第一
        assertEquals(ErpPrjConstants.PROJECT_STATUS_OPEN, dist.get(0).get("status"));
        assertEquals(2L, dist.get(0).get("count"));
    }

    @Test
    public void testCostOverrunAlertTriggers() {
        ormTemplate.runInSession(() -> {
            // P1: 预算 500, 成本 800 → 超支 300
            seedProject(121L, "OVER", ErpPrjConstants.PROJECT_STATUS_OPEN, null);
            seedBudget(211L, 121L, new BigDecimal("500"));
            seedCost(311L, 121L, new BigDecimal("800"));
            // P2: 预算 1000, 成本 200 → 不超支
            seedProject(122L, "OK", ErpPrjConstants.PROJECT_STATUS_OPEN, null);
            seedBudget(212L, 122L, new BigDecimal("1000"));
            seedCost(312L, 122L, new BigDecimal("200"));
        });
        List<Map<String, Object>> alerts = dashboardBiz.findCostOverrunAlert(CTX);
        assertEquals(1, alerts.size(), "仅 P1 超支");
        assertEquals(121L, alerts.get(0).get("projectId"));
        assertEquals(0, ((BigDecimal) alerts.get(0).get("overrunAmount")).compareTo(new BigDecimal("300")));
    }

    @Test
    public void testDelayedProjectAlertTriggersAndNot() {
        LocalDate past = CoreMetrics.currentDate().minusDays(10);
        LocalDate future = CoreMetrics.currentDate().plusDays(10);
        ormTemplate.runInSession(() -> {
            // P1: endDate 过去, status=OPEN → 触发
            seedProject(131L, "LATE", ErpPrjConstants.PROJECT_STATUS_OPEN, past);
            // P2: endDate 过去, status=COMPLETED → 不触发（已完成）
            seedProject(132L, "DONE", ErpPrjConstants.PROJECT_STATUS_COMPLETED, past);
            // P3: endDate 未来, status=OPEN → 不触发（未到期）
            seedProject(133L, "FUTURE", ErpPrjConstants.PROJECT_STATUS_OPEN, future);
        });
        List<Map<String, Object>> alerts = dashboardBiz.findDelayedProjectAlert(CTX);
        assertEquals(1, alerts.size(), "仅 P1 延期");
        assertEquals(131L, alerts.get(0).get("projectId"));
        assertEquals(10L, alerts.get(0).get("overdueDays"));
    }

    // ---------- helpers ----------

    private void seedProject(long id, String code, String status, LocalDate endDate) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode(code);
        p.setName("项目-" + code);
        p.setOrgId(1L);
        p.setStartDate(LocalDate.of(2026, 1, 1));
        if (endDate != null) p.setEndDate(endDate);
        p.setCurrencyId(1L);
        p.setStatus(status);
        dao.saveEntity(p);
    }

    private void seedBudget(long id, long projectId, BigDecimal totalAmount) {
        IEntityDao<ErpPrjBudget> dao = daoProvider.daoFor(ErpPrjBudget.class);
        ErpPrjBudget b = dao.newEntity();
        b.orm_propValue(1, id);
        b.setCode("BUD-" + id);
        b.setProjectId(projectId);
        b.setOrgId(1L);
        b.setBusinessDate(CoreMetrics.currentDate());
        b.setCurrencyId(1L);
        b.setTotalAmount(totalAmount);
        b.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
        b.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(b);
    }

    private void seedCost(long id, long projectId, BigDecimal totalAmount) {
        IEntityDao<ErpPrjCostCollection> dao = daoProvider.daoFor(ErpPrjCostCollection.class);
        ErpPrjCostCollection c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("COST-" + id);
        c.setProjectId(projectId);
        c.setOrgId(1L);
        c.setBusinessDate(CoreMetrics.currentDate());
        c.setCurrencyId(1L);
        c.setTotalAmount(totalAmount);
        c.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
        c.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
        c.setPosted(true);
        c.setExchangeRate(BigDecimal.ONE);
        dao.saveEntity(c);
    }
}
