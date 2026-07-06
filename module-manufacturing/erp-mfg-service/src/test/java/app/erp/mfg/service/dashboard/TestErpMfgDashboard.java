package app.erp.mfg.service.dashboard;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 制造看板聚合（{@code ErpMfgDashboard__*}）集成测试。覆盖：在制工单数/本期完工量/齐套待产/准时率算术、
 * 状态分布、产出趋势、工单延期预警触发/不触发、空数据集零值。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgDashboard extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpMfgDashboardBizModel dashboardBiz;

    @Test
    public void testKpiEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0L, kpi.get("inProcessCount"));
        assertEquals(0, ((BigDecimal) kpi.get("periodCompletedQty")).compareTo(BigDecimal.ZERO));
        assertEquals(0L, kpi.get("stockPartialCount"));
        assertEquals(0.0, (double) kpi.get("onTimeRate"), 0.001);
    }

    @Test
    public void testKpiAggregationAndOnTimeRate() {
        LocalDate today = LocalDate.now();
        ormTemplate.runInSession(() -> {
            // 在制工单 2 个（IN_PROCESS + STOCK_RESERVED）
            seedWorkOrder(101L, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, null, null, null);
            seedWorkOrder(102L, ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED, null, null, null);
            // 齐套待产 1 个（STOCK_PARTIAL）
            seedWorkOrder(103L, ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL, null, null, null);
            // 本期完工 2 个（COMPLETED 且 actualEndDate=今天/昨天）: 准时 100 + 延期 200
            // 准时：actualEndDate ≤ plannedEndDate → actual=今天-1, planned=今天
            seedWorkOrder(104L, ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED,
                    new BigDecimal("100"), today.minusDays(1), today);
            // 延期：actualEndDate > plannedEndDate → actual=今天, planned=今天-5
            seedWorkOrder(105L, ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED,
                    new BigDecimal("200"), today, today.minusDays(5));
            // actualEndDate 不在本期（不计入本期完工量）
            seedWorkOrder(106L, ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED,
                    new BigDecimal("999"), today.minusMonths(2), today.minusMonths(3));
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        // 在制 2 个
        assertEquals(2L, kpi.get("inProcessCount"));
        // 本期完工量 100+200=300（106 不在本期）
        assertEquals(0, ((BigDecimal) kpi.get("periodCompletedQty")).compareTo(new BigDecimal("300")));
        // 齐套待产 1 个
        assertEquals(1L, kpi.get("stockPartialCount"));
        // 准时率 1/2=0.5（104 准时, 105 延期; 106 不在 COMPLETED 期内但不影响分母——分母为全部 COMPLETED）
        // 注意：onTimeRate 分母 = 全部 COMPLETED 工单数（106 也算），分子 = COMPLETED 且 actual ≤ planned
        // 106: actual=today-2m, planned=today-3m → actual > planned → 延期
        // 故准时率 = 1/3 ≈ 0.3333
        assertEquals(0.3333, (double) kpi.get("onTimeRate"), 0.001);
    }

    @Test
    public void testWorkOrderStatusDistribution() {
        ormTemplate.runInSession(() -> {
            seedWorkOrder(111L, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, null, null, null);
            seedWorkOrder(112L, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, null, null, null);
            seedWorkOrder(113L, ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, null, null, null);
        });
        List<Map<String, Object>> dist = dashboardBiz.getWorkOrderStatusDistribution(CTX);
        assertEquals(2, dist.size(), "2 种状态");
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, dist.get(0).get("status"));
        assertEquals(2L, dist.get(0).get("count"));
    }

    @Test
    public void testTrendMonthlySeries() {
        LocalDate today = LocalDate.now();
        ormTemplate.runInSession(() -> {
            seedWorkOrder(121L, ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED,
                    new BigDecimal("50"), today, today.minusDays(1));
            seedWorkOrder(122L, ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED,
                    new BigDecimal("70"), today.minusMonths(1), today.minusMonths(2));
        });
        List<Map<String, Object>> trend = dashboardBiz.getDashboardTrend(2, CTX);
        assertEquals(2, trend.size());
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : trend) {
            total = total.add((BigDecimal) row.get("completedQty"));
        }
        assertEquals(0, total.compareTo(new BigDecimal("120")), "近 2 月完工合计 50+70=120");
    }

    @Test
    public void testDelayedWorkOrderAlertTriggersAndNot() {
        LocalDate past = LocalDate.now().minusDays(10);
        LocalDate future = LocalDate.now().plusDays(10);
        ormTemplate.runInSession(() -> {
            // W1: plannedEndDate 过去, IN_PROCESS → 触发
            seedWorkOrder(131L, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, null, null, past);
            // W2: plannedEndDate 过去, COMPLETED → 不触发（已完工）
            seedWorkOrder(132L, ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, null, null, past);
            // W3: plannedEndDate 未来, IN_PROCESS → 不触发（未到期）
            seedWorkOrder(133L, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, null, null, future);
        });
        List<Map<String, Object>> alerts = dashboardBiz.findDelayedWorkOrderAlert(CTX);
        assertEquals(1, alerts.size(), "仅 W1 延期");
        assertEquals(131L, alerts.get(0).get("workOrderId"));
        assertEquals(10L, alerts.get(0).get("overdueDays"));
    }

    // ---------- helpers ----------

    private void seedWorkOrder(long id, String docStatus, BigDecimal completedQty,
                               LocalDate actualEndDate, LocalDate plannedEndDate) {
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        ErpMfgWorkOrder o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("WO-" + id);
        o.setOrgId(1L);
        o.setProductId(1L);
        o.setPlannedQuantity(new BigDecimal("1000"));
        o.setCompletedQuantity(completedQty != null ? completedQty : BigDecimal.ZERO);
        o.setBusinessDate(LocalDate.of(2026, 1, 1));
        o.setPlannedEndDate(plannedEndDate);
        o.setActualEndDate(actualEndDate);
        o.setCurrencyId(1L);
        o.setDocStatus(docStatus);
        o.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_APPROVED);
        o.setExchangeRate("1");
        dao.saveEntity(o);
    }
}
