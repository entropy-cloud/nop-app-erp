package app.erp.mfg.service;

import app.erp.mfg.dao.entity.ErpMfgCrpLoad;
import app.erp.mfg.dao.entity.ErpMfgRouting;
import app.erp.mfg.dao.entity.ErpMfgRoutingOperation;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCalendar;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCapacity;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 测试：CRP 负荷计算（workcenter×date 聚合 + 可用工时 + 效率折算 + loadRate）+ 报表查询 + 超负荷告警。
 *
 * <p>覆盖 {@code docs/design/manufacturing/crp.md §负载报表}：单工单分派到工作中心×日期、日历出勤工时×效率折算
 * 得 capacityHours、loadRate=loadHours/capacityHours、超负荷 loadRate&gt;阈值 标 overloaded、多工单同工作中心同日累加、
 * 重算清旧快照、空 WorkOrder（capacityHours 非零 loadHours 零 loadRate=0）、阈值门控。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgCrpLoad extends JunitAutoTestCase {

    static final Long P = 7901L;
    static final Long WC1 = 7902L;
    static final Long WC2 = 7903L;
    static final Long ROUTING_1 = 7904L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testSingleWorkOrderLoadCapacityAndOverload() {
        seedWorkcenter(WC1, "WC-1");
        seedCalendar(WC1, "08:00", "17:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK, null, null);
        seedCapacity(WC1, P, bd("10"), bd("0.5"));  // efficiency=0.5
        seedRouting(ROUTING_1, "R-1");
        seedRoutingOperation(ROUTING_1, WC1, bd("540"), bd("60"));  // 9h load + 1h setup
        Long woId = seedWorkOrder("WO-CRP-1", ROUTING_1,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);

        Integer written = calculateLoad(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), null);
        assertEquals(1, written, "单日单工序 → 1 行 CrpLoad");

        List<Map<String, Object>> report = getLoadReport(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), null);
        assertEquals(1, report.size(), "WC1×1 日 → 1 报表行");
        Map<String, Object> row = report.get(0);

        // loadHours = 540min/60 = 9h（单日全量）；setupHours = 60min/60 = 1h（首日）
        assertEquals(0, bd("9").compareTo(toBd(row.get("loadHours"))), "loadHours=9h");
        assertEquals(0, bd("1").compareTo(toBd(row.get("setupHours"))), "setupHours=1h");
        // capacityHours = 9h 出勤 × 0.5 效率 = 4.5h
        assertEquals(0, bd("4.5").compareTo(toBd(row.get("capacityHours"))), "capacityHours=9×0.5=4.5h");
        // loadRate = 9 / 4.5 = 2.0 > 1.0 → overloaded
        assertEquals(0, bd("2").compareTo(toBd(row.get("loadRate"))), "loadRate=2.0");
        assertEquals(Boolean.TRUE, row.get("overloaded"), "loadRate>阈值 → overloaded");

        // CrpLoad 行核对 workOrderId 弱指针
        ErpMfgCrpLoad load = findLoad(WC1, LocalDate.of(2026, 7, 1));
        assertEquals(woId, load.getWorkOrderId());
    }

    @Test
    public void testMultipleWorkOrdersAccumulateSameDay() {
        seedWorkcenter(WC2, "WC-2");
        seedCalendar(WC2, "08:00", "20:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK, null, null);  // 12h 出勤
        seedCapacity(WC2, P, bd("10"), bd("1.0"));
        Long r = 7905L;
        seedRouting(r, "R-2");
        seedRoutingOperation(r, WC2, bd("180"), bd("0"));  // 3h load each
        seedWorkOrder("WO-A", r, LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        seedWorkOrder("WO-B", r, LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);

        calculateLoad(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2), null);
        List<Map<String, Object>> report = getLoadReport(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2), null);
        assertEquals(1, report.size());
        // 两个工单各 3h → 累加 6h
        assertEquals(0, bd("6").compareTo(toBd(report.get(0).get("loadHours"))), "两工单累加 loadHours=6h");
        // capacity=12×1.0=12，loadRate=6/12=0.5 ≤ 1.0 → 不超负荷
        assertEquals(0, bd("0.5").compareTo(toBd(report.get(0).get("loadRate"))));
        assertEquals(Boolean.FALSE, report.get(0).get("overloaded"));
    }

    @Test
    public void testRecalcClearsOldSnapshot() {
        seedWorkcenter(WC1, "WC-1X");
        seedCalendar(WC1, "08:00", "17:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK, null, null);
        seedCapacity(WC1, P, bd("10"), bd("1.0"));
        Long r = 7906L;
        seedRouting(r, "R-3");
        seedRoutingOperation(r, WC1, bd("120"), bd("0"));  // 2h
        seedWorkOrder("WO-RECALC", r, LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 3),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);

        Integer first = calculateLoad(LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 3), null);
        assertEquals(1, first);
        // 人为多塞一行旧快照
        seedExtraLoad(WC1, LocalDate.of(2026, 7, 3), bd("99"));
        assertEquals(2, countLoads(WC1, LocalDate.of(2026, 7, 3)));

        // 重算 → 旧快照（含手工塞入）被清，只剩重算结果
        Integer second = calculateLoad(LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 3), null);
        assertEquals(1, second);
        assertEquals(1, countLoads(WC1, LocalDate.of(2026, 7, 3)));
    }

    @Test
    public void testEmptyWorkOrderCapacityNonZeroLoadZero() {
        seedWorkcenter(WC1, "WC-EMPTY");
        seedCalendar(WC1, "08:00", "16:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK, null, null);  // 8h
        seedCapacity(WC1, P, bd("10"), bd("1.0"));
        // 无工单
        calculateLoad(LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 4), null);
        List<Map<String, Object>> report = getLoadReport(LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 4), null);
        assertEquals(1, report.size());
        assertEquals(0, BigDecimal.ZERO.compareTo(toBd(report.get(0).get("loadHours"))), "空工单 loadHours=0");
        assertEquals(0, bd("8").compareTo(toBd(report.get(0).get("capacityHours"))), "capacityHours=8（日历出勤）");
        assertEquals(0, BigDecimal.ZERO.compareTo(toBd(report.get(0).get("loadRate"))), "loadRate=0");
        assertEquals(Boolean.FALSE, report.get(0).get("overloaded"));
    }

    @Test
    public void testOverloadThresholdGating() {
        seedWorkcenter(WC2, "WC-GATE");
        seedCalendar(WC2, "08:00", "17:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK, null, null);  // 9h
        seedCapacity(WC2, P, bd("10"), bd("1.0"));
        Long r = 7907L;
        seedRouting(r, "R-GATE");
        seedRoutingOperation(r, WC2, bd("1080"), bd("0"));  // 18h load → loadRate=2.0
        seedWorkOrder("WO-GATE", r, LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 5),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);

        calculateLoad(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 5), null);

        // 默认阈值 1.0 → loadRate=2.0 超负荷
        List<Map<String, Object>> def = getLoadReport(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 5), null);
        assertTrue(Boolean.TRUE.equals(def.get(0).get("overloaded")), "默认阈值 1.0 → 超负荷");

        // 阈值调高到 3.0 → 不超负荷
        setConfig(ErpMfgConstants.CONFIG_CRP_OVERLOAD_THRESHOLD, "3.0");
        try {
            List<Map<String, Object>> high = getLoadReport(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 5), null);
            assertFalse(Boolean.TRUE.equals(high.get(0).get("overloaded")), "阈值 3.0 → loadRate=2.0 不超负荷");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_CRP_OVERLOAD_THRESHOLD, "1.0");
        }
    }

    @Test
    public void testMultiDayDistributionEvenly() {
        seedWorkcenter(WC1, "WC-MD");
        seedCalendar(WC1, "08:00", "17:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK, null, null);
        seedCapacity(WC1, P, bd("10"), bd("1.0"));
        Long r = 7908L;
        seedRouting(r, "R-MD");
        seedRoutingOperation(r, WC1, bd("480"), bd("120"));  // 8h load + 2h setup(首日)
        // 工单跨 2 天
        seedWorkOrder("WO-MD", r, LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 7),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);

        calculateLoad(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 7), null);
        List<Map<String, Object>> report = getLoadReport(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 7), null);
        assertEquals(2, report.size());
        // 每天 loadHours = 8h / 2 = 4h
        for (Map<String, Object> row : report) {
            assertEquals(0, bd("4").compareTo(toBd(row.get("loadHours"))), "日均分派 loadHours=4h");
        }
        // 首日 setupHours=2h，次日 0
        Map<String, Object> day1 = findByDate(report, LocalDate.of(2026, 7, 6));
        Map<String, Object> day2 = findByDate(report, LocalDate.of(2026, 7, 7));
        assertEquals(0, bd("2").compareTo(toBd(day1.get("setupHours"))), "首日 setupHours=2h");
        assertEquals(0, BigDecimal.ZERO.compareTo(toBd(day2.get("setupHours"))), "次日 setupHours=0");
    }

    @Test
    public void testCancelledWorkOrderExcluded() {
        seedWorkcenter(WC1, "WC-CNL");
        seedCalendar(WC1, "08:00", "17:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK, null, null);
        Long r = 7909L;
        seedRouting(r, "R-CNL");
        seedRoutingOperation(r, WC1, bd("60"), bd("0"));
        seedWorkOrder("WO-CNL", r, LocalDate.of(2026, 7, 8), LocalDate.of(2026, 7, 8),
                ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED);

        Integer written = calculateLoad(LocalDate.of(2026, 7, 8), LocalDate.of(2026, 7, 8), null);
        assertEquals(0, written, "CANCELLED 工单不计入负荷");
    }

    // ---------- helpers ----------

    private Integer calculateLoad(LocalDate from, LocalDate to, List<Long> workcenterIds) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("periodFrom", from);
        args.put("periodTo", to);
        if (workcenterIds != null) {
            args.put("workcenterIds", workcenterIds);
        }
        ApiResponse<?> resp = execute(mutation, "ErpMfgCrpLoad__calculateLoad", args);
        assertEquals(0, resp.getStatus(), "calculateLoad 应成功: " + resp);
        return (Integer) resp.getData();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getLoadReport(LocalDate from, LocalDate to, List<Long> workcenterIds) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("periodFrom", from);
        args.put("periodTo", to);
        if (workcenterIds != null) {
            args.put("workcenterIds", workcenterIds);
        }
        ApiResponse<?> resp = execute(query, "ErpMfgCrpLoad__getLoadReport", args);
        assertEquals(0, resp.getStatus(), "getLoadReport 应成功: " + resp);
        Object data = resp.getData();
        return (List<Map<String, Object>>) data;
    }

    private ApiResponse<?> execute(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpMfgCrpLoad findLoad(Long workcenterId, LocalDate date) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workcenterId", workcenterId));
        q.addFilter(eq("loadDate", date));
        List<ErpMfgCrpLoad> list = daoProvider.daoFor(ErpMfgCrpLoad.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private int countLoads(Long workcenterId, LocalDate date) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workcenterId", workcenterId));
        q.addFilter(eq("loadDate", date));
        return daoProvider.daoFor(ErpMfgCrpLoad.class).findAllByQuery(q).size();
    }

    private Map<String, Object> findByDate(List<Map<String, Object>> report, LocalDate date) {
        for (Map<String, Object> row : report) {
            if (date.toString().equals(String.valueOf(row.get("loadDate")))) {
                return row;
            }
        }
        throw new AssertionError("未找到日期 " + date + " 的报表行: " + report);
    }

    private BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(String.valueOf(v));
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private void seedExtraLoad(Long workcenterId, LocalDate date, BigDecimal hours) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgCrpLoad> dao = daoProvider.daoFor(ErpMfgCrpLoad.class);
            ErpMfgCrpLoad l = new ErpMfgCrpLoad();
            l.orm_propValueByName("id", 55500L + workcenterId);
            l.setWorkcenterId(workcenterId);
            l.setLoadDate(date);
            l.setLoadHours(hours);
            l.setSetupHours(BigDecimal.ZERO);
            dao.saveEntity(l);
        });
    }

    private void seedWorkcenter(Long id, String code) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenter> dao = daoProvider.daoFor(ErpMfgWorkcenter.class);
            ErpMfgWorkcenter wc = new ErpMfgWorkcenter();
            wc.orm_propValueByName("id", id);
            wc.setCode(code);
            wc.setName("WC " + code);
            dao.saveEntity(wc);
        });
    }

    private void seedCalendar(Long workcenterId, String start, String end, String pattern, LocalDate from, LocalDate to) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenterCalendar> dao = daoProvider.daoFor(ErpMfgWorkcenterCalendar.class);
            ErpMfgWorkcenterCalendar c = new ErpMfgWorkcenterCalendar();
            c.orm_propValueByName("id", 60000L + workcenterId);
            c.setWorkcenterId(workcenterId);
            c.setCalendarName("CAL-" + workcenterId);
            c.orm_propValueByName("shiftType", ErpMfgConstants.SHIFT_TYPE_ONE_SHIFT);
            c.orm_propValueByName("workDatePattern", pattern);
            c.setStartTime(start);
            c.setEndTime(end);
            if (from != null) c.setEffectiveFrom(from);
            if (to != null) c.setEffectiveTo(to);
            c.setIsActive(Boolean.TRUE);
            dao.saveEntity(c);
        });
    }

    private void seedCapacity(Long workcenterId, Long materialId, BigDecimal capPerHour, BigDecimal efficiency) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenterCapacity> dao = daoProvider.daoFor(ErpMfgWorkcenterCapacity.class);
            ErpMfgWorkcenterCapacity cap = new ErpMfgWorkcenterCapacity();
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
            ErpMfgRouting r = new ErpMfgRouting();
            r.orm_propValueByName("id", id);
            r.setCode(code);
            r.setIsActive(Boolean.TRUE);
            dao.saveEntity(r);
        });
    }

    private void seedRoutingOperation(Long routingId, Long workcenterId, BigDecimal standardTime, BigDecimal setupTime) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgRoutingOperation> dao = daoProvider.daoFor(ErpMfgRoutingOperation.class);
            ErpMfgRoutingOperation op = new ErpMfgRoutingOperation();
            op.orm_propValueByName("id", 62000L + routingId + workcenterId);
            op.setRoutingId(routingId);
            op.setLineNo(10);
            op.setWorkcenterId(workcenterId);
            op.setStandardTime(standardTime);
            op.setSetupTime(setupTime);
            dao.saveEntity(op);
        });
    }

    private Long seedWorkOrder(String code, Long routingId, LocalDate start, LocalDate end, String docStatus) {
        Long id = 8000L + (long) Math.abs(code.hashCode() % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
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
        return id;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
