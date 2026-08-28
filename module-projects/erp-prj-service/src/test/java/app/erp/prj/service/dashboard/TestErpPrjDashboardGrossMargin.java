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
            seedPnl("PNL-S-1", "101", "10000", "6000", "4000");
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
            seedPnl("PNL-M-1", "201", "10000", "6000", "4000");
            // P2: revenue 5000, cost 4000, profit 1000
            seedPnl("PNL-M-2", "202", "5000", "4000", "1000");
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
            seedPnl("PNL-F-1", "301", "10000", "6000", "4000");
            seedPnl("PNL-F-2", "302", "5000", "4000", "1000");
        });

        Map<String, Object> kpi = dashboardBiz.getProjectGrossMargin("301", CTX);
        assertEquals(1L, kpi.get("projectCount"), "仅 301 项目");
        assertEquals(0, ((BigDecimal) kpi.get("totalRevenue")).compareTo(new BigDecimal("10000")));
        assertEquals(0, ((BigDecimal) kpi.get("grossMarginPct")).compareTo(new BigDecimal("0.4000")));
    }

    /**
     * P1-CK-prj-007：每项目仅取最新 CALCULATED 快照（periodTo DESC，id DESC 决胜）。
     * 修复前多快照全量求和使收入/成本/毛利随快照数线性放大（默认 job 每日一快照）。
     */
    @Test
    public void testLatestCalculatedSnapshotPerProjectWins() {
        ormTemplate.runInSession(() -> {
            // 项目 5001 两份 CALCULATED 快照：旧 10000 收入 / 新 5000 收入 → 仅新计入
            seedPnl("PNL-OLD-1", "5001", "2026-01-01", "2026-01-31",
                    "10000", "6000", "4000");
            seedPnl("PNL-NEW-1", "5001", "2026-02-01", "2026-02-28",
                    "5000", "3000", "2000");
            // 项目 5002 仅一份快照（对照组）
            seedPnl("PNL-P-OLD", "5002", "2026-01-01", "2026-01-31",
                    "8000", "5000", "3000");
            // 非 CALCULATED 快照应被忽略（直接落库 DRAFT 状态）
            seedPnlDraft("PNL-DRAFT", "5001", "2026-03-01", "2026-03-31",
                    "99999", "99999", "99999");
        });

        Map<String, Object> kpi = dashboardBiz.getProjectGrossMargin(null, CTX);
        assertEquals(2L, kpi.get("projectCount"), "2 个不同项目（CALCULATED 各一份最新）");
        // 5001 最新=5000；5002=8000；合计 13000
        assertEquals(0, ((BigDecimal) kpi.get("totalRevenue")).compareTo(new BigDecimal("13000")),
                "仅最新 CALCULATED 快照计入（DRAFT 快照忽略）");
        assertEquals(0, ((BigDecimal) kpi.get("totalCost")).compareTo(new BigDecimal("8000")),
                "5001 新 3000 + 5002 5000 = 8000");
        assertEquals(0, ((BigDecimal) kpi.get("totalGrossProfit")).compareTo(new BigDecimal("5000")),
                "5001 新 2000 + 5002 3000 = 5000");
    }

    // ---------- helpers ----------

    private void seedPnl(String code, String projectId, String revenue, String cost, String profit) {
        seedPnl(code, projectId, "2026-01-01", "2026-01-31", revenue, cost, profit);
    }

    private void seedPnl(String code, String projectId, String periodFrom, String periodTo,
                         String revenue, String cost, String profit) {
        IEntityDao<ErpPrjProjectPnl> dao = daoProvider.daoFor(ErpPrjProjectPnl.class);
        ErpPrjProjectPnl p = dao.newEntity();
        p.setCode(code);
        p.setProjectId(projectId);
        p.setPeriodFrom(LocalDate.parse(periodFrom));
        p.setPeriodTo(LocalDate.parse(periodTo));
        p.setRevenueAmount(new BigDecimal(revenue));
        p.setTotalCost(new BigDecimal(cost));
        p.setGrossProfit(new BigDecimal(profit));
        p.setCalcStatus(ErpPrjConstants.PNL_CALC_STATUS_CALCULATED);
        p.setDocStatus(ErpPrjConstants.PROJECT_STATUS_OPEN);
        p.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(p);
    }

    /** P1-CK-prj-007 测试辅助：DRAFT 快照（应被聚合忽略）。 */
    private void seedPnlDraft(String code, String projectId, String periodFrom, String periodTo,
                              String revenue, String cost, String profit) {
        IEntityDao<ErpPrjProjectPnl> dao = daoProvider.daoFor(ErpPrjProjectPnl.class);
        ErpPrjProjectPnl p = dao.newEntity();
        p.setCode(code);
        p.setProjectId(projectId);
        p.setPeriodFrom(LocalDate.parse(periodFrom));
        p.setPeriodTo(LocalDate.parse(periodTo));
        p.setRevenueAmount(new BigDecimal(revenue));
        p.setTotalCost(new BigDecimal(cost));
        p.setGrossProfit(new BigDecimal(profit));
        p.setCalcStatus("DRAFT");
        p.setDocStatus(ErpPrjConstants.PROJECT_STATUS_OPEN);
        p.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(p);
    }
}
