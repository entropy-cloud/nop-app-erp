package app.erp.prj.service.dashboard;

import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 项目毛利率看板聚合（{@code ErpPrjDashboard__getProjectGrossMargin}）集成测试
 * （plan 2026-07-07-1100-3 Phase 3）。覆盖：空数据零值、单项目 Σ、多项目加权毛利率、projectId 过滤。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjDashboardGrossMargin extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpPrjDashboardBizModel dashboardBiz;

    @Test
    public void testEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getProjectGrossMargin(null, CTX);
        assertEquals(0L, kpi.get("projectCount"));
        assertEquals(0, ((BigDecimal) kpi.get("totalRevenue")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("totalCost")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("totalGrossProfit")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) kpi.get("grossMarginPct")).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testSingleProjectSum() {
        ormTemplate.runInSession(() -> {
            // P1: revenue 10000, cost 6000, profit 4000 → 毛利率 0.4
            seedPnl("PNL-S-1", 101L, "10000", "6000", "4000");
        });

        Map<String, Object> kpi = dashboardBiz.getProjectGrossMargin(null, CTX);
        assertEquals(1L, kpi.get("projectCount"));
        assertEquals(0, ((BigDecimal) kpi.get("totalRevenue")).compareTo(new BigDecimal("10000")));
        assertEquals(0, ((BigDecimal) kpi.get("totalCost")).compareTo(new BigDecimal("6000")));
        assertEquals(0, ((BigDecimal) kpi.get("totalGrossProfit")).compareTo(new BigDecimal("4000")));
        assertEquals(0, ((BigDecimal) kpi.get("grossMarginPct")).compareTo(new BigDecimal("0.4000")));
    }

    @Test
    public void testMultiProjectWeightedMargin() {
        ormTemplate.runInSession(() -> {
            // P1: revenue 10000, cost 6000, profit 4000
            seedPnl("PNL-M-1", 201L, "10000", "6000", "4000");
            // P2: revenue 5000, cost 4000, profit 1000
            seedPnl("PNL-M-2", 202L, "5000", "4000", "1000");
        });

        Map<String, Object> kpi = dashboardBiz.getProjectGrossMargin(null, CTX);
        assertEquals(2L, kpi.get("projectCount"));
        // Σ revenue 15000, Σ cost 10000, Σ profit 5000
        assertEquals(0, ((BigDecimal) kpi.get("totalRevenue")).compareTo(new BigDecimal("15000")));
        assertEquals(0, ((BigDecimal) kpi.get("totalCost")).compareTo(new BigDecimal("10000")));
        assertEquals(0, ((BigDecimal) kpi.get("totalGrossProfit")).compareTo(new BigDecimal("5000")));
        // 整体毛利率 = 5000 / 15000 = 0.3333
        assertEquals(0, ((BigDecimal) kpi.get("grossMarginPct")).compareTo(new BigDecimal("0.3333")));
    }

    @Test
    public void testProjectIdFilter() {
        ormTemplate.runInSession(() -> {
            seedPnl("PNL-F-1", 301L, "10000", "6000", "4000");
            seedPnl("PNL-F-2", 302L, "5000", "4000", "1000");
        });

        Map<String, Object> kpi = dashboardBiz.getProjectGrossMargin(301L, CTX);
        assertEquals(1L, kpi.get("projectCount"), "仅 301 项目");
        assertEquals(0, ((BigDecimal) kpi.get("totalRevenue")).compareTo(new BigDecimal("10000")));
        assertEquals(0, ((BigDecimal) kpi.get("grossMarginPct")).compareTo(new BigDecimal("0.4000")));
    }

    // ---------- helpers ----------

    private void seedPnl(String code, long projectId, String revenue, String cost, String profit) {
        IEntityDao<ErpPrjProjectPnl> dao = daoProvider.daoFor(ErpPrjProjectPnl.class);
        ErpPrjProjectPnl p = dao.newEntity();
        p.setCode(code);
        p.setProjectId(projectId);
        p.setPeriodFrom(LocalDate.of(2026, 1, 1));
        p.setPeriodTo(LocalDate.of(2026, 1, 31));
        p.setRevenueAmount(new BigDecimal(revenue));
        p.setTotalCost(new BigDecimal(cost));
        p.setGrossProfit(new BigDecimal(profit));
        p.setCalcStatus(ErpPrjConstants.PNL_CALC_STATUS_CALCULATED);
        p.setDocStatus(ErpPrjConstants.PROJECT_STATUS_OPEN);
        p.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(p);
    }
}
