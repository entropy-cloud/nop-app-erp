package app.erp.mfg.service.dashboard;

import app.erp.mfg.dao.entity.ErpMfgRouting;
import app.erp.mfg.dao.entity.ErpMfgRoutingOperation;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCalendar;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCapacity;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRP 负荷/产能对比图看板聚合（{@code ErpMfgDashboard__getCrpLoadChartData}）集成测试
 * （plan 2026-07-17-2010-1 Phase 3）。覆盖：空数据零值结构、日期区间过滤、workcenterId 过滤、
 * 近 N 天默认窗口、负荷率派生（loadHours/capacityHours）。
 *
 * <p>数据源委派链：本测试种入 Workcenter + WorkcenterCalendar + WorkcenterCapacity + Routing +
 * RoutingOperation + WorkOrder，先经 {@code ErpMfgCrpLoad__calculateLoad} 物化 ErpMfgCrpLoad 行，
 * 再经看板 {@code getCrpLoadChartData}（委派 {@code CrpLoadCalculator.getLoadReport} 后按 loadDate 聚合）
 * 校验聚合与负荷率。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgDashboardCrpChart extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    static final Long P = 8801L;
    static final Long WC1 = 8802L;
    static final Long WC2 = 8803L;
    static final Long ROUTING = 8804L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpMfgDashboardBizModel dashboardBiz;
    @Inject
    app.erp.mfg.service.entity.ErpMfgCrpLoadBizModel crpLoadBiz;

    @Test
    public void testEmptyDatasetReturnsZeroValueStructure() {
        // 无任何工单/工作中心 → 委派 getLoadReport 返回空 list → 聚合后 series 为窗口日数（默认 7）全 0
        Map<String, Object> result = dashboardBiz.getCrpLoadChartData(null, null, null, CTX);
        assertNotNull(result, "空数据返回零值结构非 null");
        assertNotNull(result.get("series"), "series 列表非 null");
        List<?> series = (List<?>) result.get("series");
        // 默认 7 天窗口
        assertEquals(7, series.size(), "默认近 7 天 → series 含 7 个日期行");
        assertEquals(0, ((BigDecimal) result.get("totalLoadHours")).compareTo(BigDecimal.ZERO),
                "空数据 totalLoadHours=0");
        assertEquals(0, ((BigDecimal) result.get("overallLoadRate")).compareTo(BigDecimal.ZERO),
                "空数据 overallLoadRate=0");
        for (Object row : series) {
            Map<?, ?> r = (Map<?, ?>) row;
            assertEquals(0, ((BigDecimal) r.get("loadHours")).compareTo(BigDecimal.ZERO));
            assertEquals(0, ((BigDecimal) r.get("capacityHours")).compareTo(BigDecimal.ZERO));
            assertEquals(0, ((BigDecimal) r.get("loadRate")).compareTo(BigDecimal.ZERO));
        }
    }

    @Test
    public void testDateRangeFilter() {
        seedWorkcenter(WC1, "WC-DR");
        seedCalendar(WC1, "08:00", "17:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK);
        seedCapacity(WC1, P, bd("10"), bd("1.0"));
        Long r = 8805L;
        seedRouting(r, "R-DR");
        seedRoutingOperation(r, WC1, bd("540"), bd("0"));  // 9h load
        seedWorkOrder("WO-DR", r, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        runCalculateLoad(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1));

        // 窗口仅 7-1 一天
        Map<String, Object> result = dashboardBiz.getCrpLoadChartData(null,
                "2026-07-01", "2026-07-01", CTX);
        List<?> series = (List<?>) result.get("series");
        assertEquals(1, series.size(), "单日窗口仅 1 行");
        Map<?, ?> row = (Map<?, ?>) series.get(0);
        assertEquals(0, bd("9").compareTo((BigDecimal) row.get("loadHours")), "loadHours=9h");
        // capacityHours = 9h 出勤 × 1.0 = 9h
        assertEquals(0, bd("9").compareTo((BigDecimal) row.get("capacityHours")),
                "capacityHours=9h（9h 出勤 × 1.0）");
        assertEquals(0, bd("1").compareTo((BigDecimal) row.get("loadRate")), "loadRate=1.0");
    }

    @Test
    public void testWorkcenterIdFilter() {
        seedWorkcenter(WC1, "WC-F1");
        seedWorkcenter(WC2, "WC-F2");
        seedCalendar(WC1, "08:00", "17:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK);
        seedCalendar(WC2, "08:00", "17:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK);
        seedCapacity(WC1, P, bd("10"), bd("1.0"));
        seedCapacity(WC2, P, bd("10"), bd("1.0"));
        Long r1 = 8806L;
        Long r2 = 8807L;
        seedRouting(r1, "R-F1");
        seedRoutingOperation(r1, WC1, bd("480"), bd("0"));  // 8h
        seedRouting(r2, "R-F2");
        seedRoutingOperation(r2, WC2, bd("240"), bd("0"));  // 4h
        seedWorkOrder("WO-F1", r1, LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        seedWorkOrder("WO-F2", r2, LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        runCalculateLoad(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2));

        // 仅筛 WC1
        Map<String, Object> only1 = dashboardBiz.getCrpLoadChartData(WC1,
                "2026-07-02", "2026-07-02", CTX);
        List<?> series1 = (List<?>) only1.get("series");
        assertEquals(1, series1.size());
        // 仅 WC1 的 8h 计入
        assertEquals(0, bd("8").compareTo((BigDecimal) ((Map<?, ?>) series1.get(0)).get("loadHours")),
                "workcenterId=WC1 仅聚 WC1 负荷（8h）");

        // 不过滤 → 聚合 WC1+WC2 = 8+4 = 12h
        Map<String, Object> all = dashboardBiz.getCrpLoadChartData(null,
                "2026-07-02", "2026-07-02", CTX);
        List<?> seriesAll = (List<?>) all.get("series");
        assertEquals(0, bd("12").compareTo((BigDecimal) ((Map<?, ?>) seriesAll.get(0)).get("loadHours")),
                "无过滤 → WC1+WC2 累加 = 12h");
    }

    @Test
    public void testNearNDaysDefaultWindow() {
        // 无 dateFrom/dateTo → 默认近 7 天；每个日期行均存在
        Map<String, Object> result = dashboardBiz.getCrpLoadChartData(null, null, null, CTX);
        List<?> series = (List<?>) result.get("series");
        LocalDate today = LocalDate.now();
        assertEquals(7, series.size(), "默认窗口 7 天");
        // 第一行 = today - 6，最后一行 = today
        Map<?, ?> first = (Map<?, ?>) series.get(0);
        Map<?, ?> last = (Map<?, ?>) series.get(6);
        assertEquals(today.minusDays(6), first.get("loadDate"), "首日 = today-6");
        assertEquals(today, last.get("loadDate"), "末日 = today");
    }

    @Test
    public void testLoadRateDerivedAndAccumulated() {
        seedWorkcenter(WC2, "WC-RATE");
        seedCalendar(WC2, "08:00", "20:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK);  // 12h 出勤
        seedCapacity(WC2, P, bd("10"), bd("1.0"));
        Long r = 8808L;
        seedRouting(r, "R-RATE");
        seedRoutingOperation(r, WC2, bd("360"), bd("0"));  // 6h load → loadRate=6/12=0.5
        seedWorkOrder("WO-RATE", r, LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 3),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        runCalculateLoad(LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 3));

        Map<String, Object> result = dashboardBiz.getCrpLoadChartData(WC2,
                "2026-07-03", "2026-07-03", CTX);
        List<?> series = (List<?>) result.get("series");
        assertEquals(1, series.size());
        Map<?, ?> row = (Map<?, ?>) series.get(0);
        // loadHours=6h, capacityHours=12h, loadRate=0.5
        assertEquals(0, bd("6").compareTo((BigDecimal) row.get("loadHours")));
        assertEquals(0, bd("12").compareTo((BigDecimal) row.get("capacityHours")));
        // loadRate = 6/12 = 0.5
        assertEquals(0, bd("0.5").compareTo((BigDecimal) row.get("loadRate")), "loadRate 派生 = 0.5");
        // overallLoadRate = totalLoad / totalCap = 6/12 = 0.5
        BigDecimal overall = (BigDecimal) result.get("overallLoadRate");
        assertEquals(0, bd("0.5").compareTo(overall), "overallLoadRate=0.5");
    }

    // ---------- helpers ----------

    private void runCalculateLoad(LocalDate from, LocalDate to) {
        ormTemplate.runInSession(() -> crpLoadBiz.calculateLoad(from, to, null, CTX));
    }

    private void seedWorkcenter(Long id, String code) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenter> dao = daoProvider.daoFor(ErpMfgWorkcenter.class);
            ErpMfgWorkcenter wc = dao.newEntity();
            wc.orm_propValueByName("id", id);
            wc.setCode(code);
            wc.setName("WC " + code);
            dao.saveEntity(wc);
        });
    }

    private void seedCalendar(Long workcenterId, String start, String end, String pattern) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenterCalendar> dao = daoProvider.daoFor(ErpMfgWorkcenterCalendar.class);
            ErpMfgWorkcenterCalendar c = dao.newEntity();
            c.orm_propValueByName("id", 60000L + workcenterId);
            c.setWorkcenterId(workcenterId);
            c.setCalendarName("CAL-" + workcenterId);
            c.orm_propValueByName("shiftType", ErpMfgConstants.SHIFT_TYPE_ONE_SHIFT);
            c.orm_propValueByName("workDatePattern", pattern);
            c.setStartTime(start);
            c.setEndTime(end);
            c.setIsActive(Boolean.TRUE);
            dao.saveEntity(c);
        });
    }

    private void seedCapacity(Long workcenterId, Long materialId, BigDecimal capPerHour, BigDecimal efficiency) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenterCapacity> dao = daoProvider.daoFor(ErpMfgWorkcenterCapacity.class);
            ErpMfgWorkcenterCapacity cap = dao.newEntity();
            cap.orm_propValueByName("id", 61000L + workcenterId);
            cap.setWorkcenterId(workcenterId);
            cap.setMaterialId(materialId);
            cap.setCapacityPerHour(capPerHour);
            cap.setEfficiencyFactor(efficiency);
            cap.setIsActive(Boolean.TRUE);
            dao.saveEntity(cap);
        });
    }

    private void seedRouting(Long id, String code) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgRouting> dao = daoProvider.daoFor(ErpMfgRouting.class);
            ErpMfgRouting r = dao.newEntity();
            r.orm_propValueByName("id", id);
            r.setCode(code);
            r.setIsActive(Boolean.TRUE);
            dao.saveEntity(r);
        });
    }

    private void seedRoutingOperation(Long routingId, Long workcenterId, BigDecimal standardTime, BigDecimal setupTime) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgRoutingOperation> dao = daoProvider.daoFor(ErpMfgRoutingOperation.class);
            ErpMfgRoutingOperation op = dao.newEntity();
            op.orm_propValueByName("id", 62000L + routingId + workcenterId);
            op.setRoutingId(routingId);
            op.setLineNo(10);
            op.setWorkcenterId(workcenterId);
            op.setStandardTime(standardTime);
            op.setSetupTime(setupTime);
            dao.saveEntity(op);
        });
    }

    private void seedWorkOrder(String code, Long routingId, LocalDate start, LocalDate end, String docStatus) {
        Long id = 8900L + (long) Math.abs(code.hashCode() % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = dao.newEntity();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setRoutingId(routingId);
            wo.setPlannedQuantity(bd("1"));
            wo.setBusinessDate(start);
            wo.setPlannedStartDate(start);
            wo.setPlannedEndDate(end);
            wo.setDocStatus(docStatus);
            dao.saveEntity(wo);
        });
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
