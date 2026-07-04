package app.erp.mfg.service;

import app.erp.mfg.biz.ApsLoadSlot;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Phase 3 测试：CRP 负荷来源切换（plan 2026-07-05-0306-2）。
 *
 * <p>覆盖三场景：
 * <ul>
 *   <li>(a) {@code crp-load-source=WORK_ORDER}（默认）→ 负荷按 WorkOrder 计划日期分派（既有断言不变）。</li>
 *   <li>(b) {@code =APS} + 工单有已排程 OperationOrder → 负荷按 APS 排程时段分派（断言落在 OperationOrder 排程日期）。</li>
 *   <li>(c) {@code =APS} 但工单无 OperationOrder → 回退 WorkOrder 计划日期（混合 tolerated）。</li>
 * </ul>
 *
 * <p>APS SPI 经 {@link TestStubApsLoadSourceProvider} 桩（{@code test-aps-load-source.beans.xml} 注册，
 * {@code ioc:collect-beans by-type} 收集），避免在 mfg-service 测试引入 aps-service 形成 reactor 环。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/mfg/beans/test-aps-load-source.beans.xml")
public class TestErpMfgCrpLoadSource extends JunitAutoTestCase {

    static final Long P = 8801L;
    static final Long WC1 = 8802L;
    static final Long WC2 = 8803L;
    static final Long ROUTING_1 = 8804L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    TestStubApsLoadSourceProvider apsStub;

    @BeforeEach
    public void resetStubAndConfig() {
        apsStub.clear();
        setConfig(ErpMfgConstants.CONFIG_CRP_LOAD_SOURCE, ErpMfgConstants.CRP_LOAD_SOURCE_WORK_ORDER);
    }

    // ---------- (a) WORK_ORDER 默认回归 ----------

    @Test
    public void testWorkOrderSourceDefaultDistributesByWorkOrderDates() {
        seedWorkcenter(WC1, "WC-WO");
        seedCalendar(WC1, "08:00", "17:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK, null, null);
        seedCapacity(WC1, P, bd("10"), bd("1.0"));
        seedRouting(ROUTING_1, "R-WO");
        seedRoutingOperation(ROUTING_1, WC1, bd("480"), bd("60"));  // 8h load + 1h setup
        // 工单跨 2 天（7-10 ~ 7-11）
        Long woId = seedWorkOrder("WO-WO-SRC", ROUTING_1,
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 11),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);

        Integer written = calculateLoad(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 11), null);
        // 默认 WORK_ORDER：均匀分派 8h/2day = 4h/day，2 行
        assertEquals(2, written, "WORK_ORDER 模式应分派 2 行 CrpLoad");

        ErpMfgCrpLoad d1 = findLoad(WC1, LocalDate.of(2026, 7, 10));
        ErpMfgCrpLoad d2 = findLoad(WC1, LocalDate.of(2026, 7, 11));
        assertEquals(0, bd("4").compareTo(d1.getLoadHours()), "首日 loadHours=4h（8h/2）");
        assertEquals(0, bd("1").compareTo(d1.getSetupHours()), "首日 setupHours=1h");
        assertEquals(0, bd("4").compareTo(d2.getLoadHours()), "次日 loadHours=4h");
        assertEquals(0, BigDecimal.ZERO.compareTo(d2.getSetupHours()), "次日 setupHours=0");
        assertEquals(woId, d1.getWorkOrderId());
    }

    // ---------- (b) APS 模式 + 有 OperationOrder 排程时段 ----------

    @Test
    public void testApsSourceDistributesByOperationOrderSchedule() {
        seedWorkcenter(WC2, "WC-APS");
        seedCalendar(WC2, "08:00", "20:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK, null, null);
        seedCapacity(WC2, P, bd("10"), bd("1.0"));
        seedRouting(ROUTING_1, "R-APS");
        // WorkOrder 计划日期 7-15（单日），但 APS 排程时段在 7-20（不同日期以区分来源）
        Long woId = seedWorkOrder("WO-APS-SRC", ROUTING_1,
                LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 15),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        seedRoutingOperation(ROUTING_1, WC2, bd("9999"), bd("0"));  // 占位（不会被使用，因 APS 分支不读 RoutingOperation）

        // 注入 APS 排程时段：7-20 09:00~13:00（4h），setupTime=60min（首日 1h）
        ApsLoadSlot slot = new ApsLoadSlot();
        slot.setWorkOrderId(woId);
        slot.setSequence(10);
        slot.setWorkcenterId(WC2);
        slot.setPlannedStartT(LocalDateTime.of(2026, 7, 20, 9, 0));
        slot.setPlannedEndT(LocalDateTime.of(2026, 7, 20, 13, 0));
        slot.setSetupTime(bd("60"));
        apsStub.putSlots(woId, List.of(slot));

        setConfig(ErpMfgConstants.CONFIG_CRP_LOAD_SOURCE, ErpMfgConstants.CRP_LOAD_SOURCE_APS);

        Integer written = calculateLoad(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 31), null);
        assertEquals(1, written, "APS 模式应按排程时段写 1 行 CrpLoad（落在 7-20）");

        // 关键断言：负荷落在 APS 排程日期 7-20，不是 WorkOrder 计划日期 7-15
        ErpMfgCrpLoad apsDay = findLoad(WC2, LocalDate.of(2026, 7, 20));
        assertEquals(0, bd("4").compareTo(apsDay.getLoadHours()), "APS loadHours=4h（09:00~13:00）");
        assertEquals(0, bd("1").compareTo(apsDay.getSetupHours()), "APS setupHours=1h（setup→首日）");
        assertEquals(woId, apsDay.getWorkOrderId());

        // WorkOrder 计划日期 7-15 无负荷行（证明来源是 APS 不是 WorkOrder）
        assertNull(findLoad(WC2, LocalDate.of(2026, 7, 15)),
                "APS 模式下 WorkOrder 计划日期 7-15 不应有 CrpLoad 行");
    }

    // ---------- (c) APS 模式 + 无 OperationOrder（回退 WorkOrder） ----------

    @Test
    public void testApsSourceFallsBackWhenNoScheduledSlot() {
        seedWorkcenter(WC1, "WC-FB");
        seedCalendar(WC1, "08:00", "17:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK, null, null);
        seedCapacity(WC1, P, bd("10"), bd("1.0"));
        seedRouting(ROUTING_1, "R-FB");
        seedRoutingOperation(ROUTING_1, WC1, bd("480"), bd("60"));  // 8h load + 1h setup
        // 工单单日 7-25
        Long woId = seedWorkOrder("WO-FB-SRC", ROUTING_1,
                LocalDate.of(2026, 7, 25), LocalDate.of(2026, 7, 25),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);

        // APS 模式但不为该工单注入任何 slot（模拟工单无 OperationOrder 或时间未回填）
        setConfig(ErpMfgConstants.CONFIG_CRP_LOAD_SOURCE, ErpMfgConstants.CRP_LOAD_SOURCE_APS);

        Integer written = calculateLoad(LocalDate.of(2026, 7, 25), LocalDate.of(2026, 7, 25), null);
        assertEquals(1, written, "APS 模式无 slot 应回退 WorkOrder 日期分派 1 行");

        // 回退后负荷按 WorkOrder 计划日期 7-25 分派（RoutingOperation.standardTime=480min=8h）
        ErpMfgCrpLoad fallback = findLoad(WC1, LocalDate.of(2026, 7, 25));
        assertEquals(0, bd("8").compareTo(fallback.getLoadHours()),
                "回退 WorkOrder：loadHours=8h（RoutingOperation.standardTime=480min）");
        assertEquals(0, bd("1").compareTo(fallback.getSetupHours()),
                "回退 WorkOrder：setupHours=1h（RoutingOperation.setupTime=60min）");
        assertEquals(woId, fallback.getWorkOrderId());
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
        return (List<Map<String, Object>>) resp.getData();
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

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
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
            c.orm_propValueByName("id", 68000L + workcenterId);
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
            cap.orm_propValueByName("id", 69000L + workcenterId);
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
            op.orm_propValueByName("id", 70000L + routingId + workcenterId);
            op.setRoutingId(routingId);
            op.setLineNo(10);
            op.setWorkcenterId(workcenterId);
            op.setStandardTime(standardTime);
            op.setSetupTime(setupTime);
            dao.saveEntity(op);
        });
    }

    private Long seedWorkOrder(String code, Long routingId, LocalDate start, LocalDate end, String docStatus) {
        Long id = 9000L + (long) Math.abs(code.hashCode() % 1000);
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
