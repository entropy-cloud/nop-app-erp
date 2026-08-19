package app.erp.mnt.service;

import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCalendar;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCapacity;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntEquipmentStatusLog;
import app.erp.mnt.service.dashboard.ErpMntDashboardBizModel;
import app.erp.qa.dao.entity.ErpQaInspection;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-MAIN-10 OEE 按需计算测试组（RC-R1.78 / plan 2026-08-20-0518-1 Phase 3）。
 *
 * <p>覆盖：①三分量数学断言（Σ RUNNING 段 + TimeLog 产量 + qa 质量，逐分量分子分母期望值）
 * ②OEE=乘积恒等式 ③零分母/空窗口/无工作中心/无产能行 null 语义（无数据 ≠ 零效率）
 * ④跨设备隔离 ⑤GraphQL 冒烟（computeOee/getDashboardOoeKpi 数据面）⑥月度窗口聚合。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntOee extends JunitAutoTestCase {

    @RegisterExtension
    static MntFrozenClockExtension frozenClock = new MntFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    // 固定 3 日窗口（周一至周三）
    private static final LocalDate FROM = LocalDate.of(2026, 7, 13);
    private static final LocalDate TO = LocalDate.of(2026, 7, 15);

    // EQ-A @ WC-81001（完整数据链设备）
    private static final Long EQ_A = 71001L;
    private static final Long WC_A = 81001L;
    // EQ-B @ WC-81002（隔离对照设备：不同日历/产能/报工，无质检走 mfg 回退）
    private static final Long EQ_B = 71002L;
    private static final Long WC_B = 81002L;
    // EQ-C：无工作中心桥接（null 语义设备）
    private static final Long EQ_C = 71003L;

    private static final Long MATERIAL_1 = 88001L;
    private static final Long WO_A = 73001L;
    private static final String WO_A_CODE = "WO-OEE-A";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    ErpMntDashboardBizModel dashboardBiz;

    // ---------- Proof ①②：三分量数学 + 乘积恒等式 ----------

    @Test
    public void testThreeComponentMath() {
        seedFullScenarioA();
        Map<String, Object> row = dashboardBiz.computeOee(EQ_A, FROM, TO, CTX);

        // 可用率 = 21h / (日历 3×8h − 停机 2h = 22h)
        assertEquals(0, ((BigDecimal) row.get("calendarHours")).compareTo(new BigDecimal("24")),
                "日历班次 = 3 天 × 8h");
        assertEquals(0, ((BigDecimal) row.get("downtimeHours")).compareTo(BigDecimal.valueOf(2)),
                "停机扣减 = 2h");
        assertEquals(0, ((BigDecimal) row.get("plannedHours")).compareTo(BigDecimal.valueOf(22)),
                "计划运行时间 = 24 − 2 = 22h（排除停机）");
        assertEquals(0, ((BigDecimal) row.get("runningHours")).compareTo(BigDecimal.valueOf(21)),
                "实际运行时间 = Σ RUNNING 窗口段 = 7+6+8 = 21h");
        assertEquals(0, ((BigDecimal) row.get("availability")).compareTo(new BigDecimal("0.9545")),
                "可用率 = 21/22");

        // 性能效率 = 150 / (10/h × 21h = 210)
        assertEquals(0, ((BigDecimal) row.get("capacityPerHour")).compareTo(BigDecimal.TEN),
                "产能行 = 唯一 active 行 10/h");
        assertEquals(0, ((BigDecimal) row.get("theoreticalOutput")).compareTo(BigDecimal.valueOf(210)),
                "理论产量 = 标准产能 × 实际运行时间 = 10×21");
        assertEquals(0, ((BigDecimal) row.get("actualOutput")).compareTo(BigDecimal.valueOf(150)),
                "实际产量 = Σ TimeLog.completedQuantity = 60+50+40");
        assertEquals(0, ((BigDecimal) row.get("performance")).compareTo(new BigDecimal("0.7143")),
                "性能效率 = 150/210");

        // 质量合格率 = 120 / 150（qa 主路径）
        assertEquals(0, ((BigDecimal) row.get("qualifiedQuantity")).compareTo(BigDecimal.valueOf(120)),
                "合格品数 = Σ ACCEPTED 质检批量");
        assertEquals("QA", row.get("qualitySource"), "主数据源 = qa Inspection");
        assertEquals(0, ((BigDecimal) row.get("quality")).compareTo(new BigDecimal("0.8000")),
                "质量合格率 = 120/150");

        // 乘积恒等式（Proof ②）
        BigDecimal availability = (BigDecimal) row.get("availability");
        BigDecimal performance = (BigDecimal) row.get("performance");
        BigDecimal quality = (BigDecimal) row.get("quality");
        BigDecimal oee = (BigDecimal) row.get("oee");
        assertNotNull(oee, "三分量齐备 → OEE 非 null");
        assertEquals(0, oee.compareTo(availability.multiply(performance).multiply(quality)
                        .setScale(4, RoundingMode.HALF_UP)),
                "OEE = 可用率 × 性能效率 × 质量合格率");
    }

    // ---------- Proof ③：零分母 / 空窗口 / 无工作中心 / 无产能行 null 语义 ----------

    @Test
    public void testNoWorkcenterYieldsNullComponents() {
        seedEquipment(EQ_C, "EQ-OEE-C", ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, null);
        Map<String, Object> row = dashboardBiz.computeOee(EQ_C, FROM, TO, CTX);
        assertNull(row.get("calendarHours"), "无工作中心 → 无日历");
        assertNull(row.get("plannedHours"), "无工作中心 → 计划运行时间 null");
        assertNull(row.get("availability"), "无工作中心 → 可用率 null（非 0）");
        assertNull(row.get("capacityPerHour"));
        assertNull(row.get("performance"), "无工作中心 → 性能效率 null");
        assertNull(row.get("quality"), "无产量 → 质量合格率 null");
        assertNull(row.get("oee"), "任一分量 null → OEE null");
    }

    @Test
    public void testNoCapacityRowYieldsNullPerformance() {
        Long equipmentId = 71004L;
        Long workcenterId = 81004L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, "EQ-OEE-D", ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, workcenterId);
            seedCalendar(84004L, workcenterId, "08:00", "16:00", null);
            seedStatusLog(72041L, equipmentId, null, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING,
                    ts(2026, 7, 13, 8, 0));
            seedStatusLog(72042L, equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE, ts(2026, 7, 15, 16, 0));
            return null;
        });
        Map<String, Object> row = dashboardBiz.computeOee(equipmentId, FROM, TO, CTX);
        assertNotNull(row.get("availability"), "有日历有运行 → 可用率可计算");
        assertNull(row.get("capacityPerHour"), "无产能行 → 产能 null");
        assertNull(row.get("theoreticalOutput"));
        assertNull(row.get("performance"), "无产能行 → 性能效率 null");
        assertNull(row.get("oee"), "OEE null");
    }

    @Test
    public void testZeroPlannedHoursYieldsNullAvailability() {
        Long equipmentId = 71005L;
        Long workcenterId = 81005L;
        ormTemplate.runInSession(s -> {
            seedEquipment(equipmentId, "EQ-OEE-E", ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, workcenterId);
            seedCalendar(84005L, workcenterId, "08:00", "16:00", null);
            // 停机覆盖全部日历时长 → 计划运行时间 0 → 可用率 null（D4 零分母）
            seedDowntime(73005L, equipmentId, ts(2026, 7, 13, 0, 0), ts(2026, 7, 16, 0, 0));
            return null;
        });
        Map<String, Object> row = dashboardBiz.computeOee(equipmentId, FROM, TO, CTX);
        assertEquals(0, ((BigDecimal) row.get("plannedHours")).compareTo(BigDecimal.ZERO),
                "停机 ≥ 日历 → 计划运行时间钳制 0");
        assertNull(row.get("availability"), "零分母 → 可用率 null（无数据 ≠ 零效率）");
    }

    @Test
    public void testEmptyWindowNoDataSemantics() {
        seedFullScenarioA();
        // 空窗口（窗口外无任何报工/质检；RUNNING 段不与窗口相交）
        Map<String, Object> row = dashboardBiz.computeOee(EQ_A,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2), CTX);
        assertEquals(0, ((BigDecimal) row.get("runningHours")).compareTo(BigDecimal.ZERO),
                "窗口外 RUNNING 段不计入");
        assertEquals(0, ((BigDecimal) row.get("actualOutput")).compareTo(BigDecimal.ZERO),
                "窗口外报工不计入");
        // 日历 ALL_WEEK 覆盖 8 月 → planned=16h 有效 → 可用率 = 0/16 = 0（合法零，非 null）
        assertEquals(0, ((BigDecimal) row.get("availability")).compareTo(BigDecimal.ZERO),
                "有效分母 + 零分子 = 合法零可用率");
        assertNull(row.get("performance"), "零运行 → 理论产量 null → 性能 null");
        assertNull(row.get("quality"), "无质检 + 回退分母 0 → 质量 null");
        assertNull(row.get("oee"), "OEE null 不抛错");
    }

    // ---------- Proof ④：跨设备隔离 ----------

    @Test
    public void testEquipmentIsolation() {
        seedFullScenarioA();
        seedScenarioB();
        Map<String, Object> a = dashboardBiz.computeOee(EQ_A, FROM, TO, CTX);
        Map<String, Object> b = dashboardBiz.computeOee(EQ_B, FROM, TO, CTX);

        // EQ-B：日历 24h 无停机，运行 12h → 0.5；产能 20/h → 理论 240，产量 60 → 0.25；
        // 无质检 → 回退 60/(60+20)=0.75；OEE = 0.5×0.25×0.75 = 0.0938
        assertEquals(0, ((BigDecimal) b.get("availability")).compareTo(new BigDecimal("0.5000")),
                "EQ-B 可用率 = 12/24（仅消费 WC-81002 数据）");
        assertEquals(0, ((BigDecimal) b.get("performance")).compareTo(new BigDecimal("0.2500")),
                "EQ-B 性能 = 60/240");
        assertEquals("MFG_FALLBACK", b.get("qualitySource"), "无关联质检 → mfg 报工数量回退");
        assertEquals(0, ((BigDecimal) b.get("quality")).compareTo(new BigDecimal("0.7500")),
                "EQ-B 质量 = 60/(60+20)");
        assertEquals(0, ((BigDecimal) b.get("oee")).compareTo(new BigDecimal("0.0938")), "EQ-B OEE");

        // EQ-A 数值不受 EQ-B 数据影响
        assertEquals(0, ((BigDecimal) a.get("actualOutput")).compareTo(BigDecimal.valueOf(150)),
                "EQ-A 产量隔离（不吞并 WC-81002 报工）");
        assertEquals("QA", a.get("qualitySource"), "EQ-A 仍走 qa 主路径");
    }

    // ---------- Proof ⑤：GraphQL 数据面冒烟 ----------

    @Test
    public void testComputeOeeGraphqlSmoke() {
        seedFullScenarioA();
        ApiRequest<?> request = ApiRequest.build(Map.of(
                "equipmentId", EQ_A, "dateFrom", FROM.toString(), "dateTo", TO.toString()));
        ApiResponse<?> resp = executeRpc(GraphQLOperationType.query,
                "ErpMntDashboard__computeOee", request);
        assertEquals(0, resp.getStatus(), "computeOee RPC 成功");
        assertNotNull(resp.getData(), "computeOee 返回数据面");
        assertEquals(EQ_A, ((Map<?, ?>) resp.getData()).get("equipmentId"));
        assertNotNull(((Map<?, ?>) resp.getData()).get("oee"), "GraphQL 可返回 OEE");
    }

    @Test
    public void testDashboardOeeKpiGraphqlSmoke() {
        seedFullScenarioA();
        seedScenarioB();
        seedEquipment(EQ_C, "EQ-OEE-C", ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, null);
        ApiRequest<?> request = ApiRequest.build(Map.of(
                "startDate", FROM.toString(), "endDate", TO.toString()));
        ApiResponse<?> resp = executeRpc(GraphQLOperationType.query,
                "ErpMntDashboard__getDashboardOeeKpi", request);
        assertEquals(0, resp.getStatus(), "getDashboardOeeKpi RPC 成功");
        Map<?, ?> data = (Map<?, ?>) resp.getData();
        assertEquals(3L, ((Number) data.get("equipmentTotal")).longValue(), "非停用设备 3 台");
        assertEquals(2L, ((Number) data.get("computedCount")).longValue(), "可计算 OEE 设备 2 台（EQ-C null 不计入）");
        assertNotNull(data.get("oeeDisplay"), "展示字符串非空");
    }

    // ---------- Proof ⑥：月度窗口聚合 + 列表级查询 ----------

    @Test
    public void testMonthlyWindowAggregationAndList() {
        seedFullScenarioA();
        // 7-20 追加报工（3 日窗口外，月度窗口内）
        ormTemplate.runInSession(s -> {
            seedTimeLog(72504L, 72001L, WO_A, LocalDate.of(2026, 7, 20), "50", "0");
            return null;
        });

        Map<String, Object> monthly = dashboardBiz.computeOee(EQ_A,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), CTX);
        assertEquals(0, ((BigDecimal) monthly.get("actualOutput")).compareTo(BigDecimal.valueOf(200)),
                "月度窗口聚合 3 日 150 + 7-20 报工 50");
        assertEquals(0, ((BigDecimal) monthly.get("plannedHours")).compareTo(new BigDecimal("246")),
                "月度计划运行时间 = 31×8 − 2 停机");

        List<Map<String, Object>> list = dashboardBiz.computeOeeList(FROM, TO, CTX);
        assertEquals(1, list.size(), "列表级按设备聚合（仅 EQ-A 在册）");
        assertEquals(EQ_A, list.get(0).get("equipmentId"));
        assertEquals(0, ((BigDecimal) list.get(0).get("actualOutput")).compareTo(BigDecimal.valueOf(150)),
                "列表级 3 日窗口 = 150（7-20 月度报工不混入）");
    }

    // ===================== seed helpers =====================

    /** EQ-A 完整数据链：3 日班次内 RUNNING 21h + 停机 2h + 产能 10/h + 报工 150 + 质检合格 120。 */
    private void seedFullScenarioA() {
        ormTemplate.runInSession(s -> {
            seedEquipment(EQ_A, "EQ-OEE-A", ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, WC_A);
            seedCalendar(84001L, WC_A, "08:00", "16:00", null);
            seedCapacity(85001L, WC_A, MATERIAL_1, "10");
            seedDowntime(73001L, EQ_A, ts(2026, 7, 14, 10, 0), ts(2026, 7, 14, 12, 0));
            seedWorkOrder(WO_A, WO_A_CODE, MATERIAL_1);
            seedJobCard(72001L, WO_A, WC_A);
            // 7-13：RUNNING 08-12(4h) / IDLE 12-13 / RUNNING 13-16(3h)
            seedStatusLog(72011L, EQ_A, null, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, 13, 8, 0));
            seedStatusLog(72012L, EQ_A, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE, ts(2026, 7, 13, 12, 0));
            seedStatusLog(72013L, EQ_A, ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, 13, 13, 0));
            seedStatusLog(72014L, EQ_A, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE, ts(2026, 7, 13, 16, 0));
            // 7-14：RUNNING 08-10(2h) / DOWN 10-12（停机）/ RUNNING 12-16(4h)
            seedStatusLog(72015L, EQ_A, ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, 14, 8, 0));
            seedStatusLog(72016L, EQ_A, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN, ts(2026, 7, 14, 10, 0));
            seedStatusLog(72017L, EQ_A, ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, 14, 12, 0));
            seedStatusLog(72018L, EQ_A, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE, ts(2026, 7, 14, 16, 0));
            // 7-15：RUNNING 08-16(8h)
            seedStatusLog(72019L, EQ_A, ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, 15, 8, 0));
            seedStatusLog(72020L, EQ_A, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING,
                    ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE, ts(2026, 7, 15, 16, 0));
            // 报工：60 + 50 + 40 = 150
            seedTimeLog(72501L, 72001L, WO_A, LocalDate.of(2026, 7, 13), "60", "0");
            seedTimeLog(72502L, 72001L, WO_A, LocalDate.of(2026, 7, 14), "50", "10");
            seedTimeLog(72503L, 72001L, WO_A, LocalDate.of(2026, 7, 15), "40", "0");
            // 质检：ACCEPTED 批量 120（关联 WO-OEE-A）
            seedInspection(74001L, "QA-OEE-1", WO_A_CODE, LocalDate.of(2026, 7, 15),
                    "ACCEPTED", "120");
            return null;
        });
    }

    /** EQ-B 隔离对照：WC-81002 自有日历/产能/报工，无质检。 */
    private void seedScenarioB() {
        ormTemplate.runInSession(s -> {
            seedEquipment(EQ_B, "EQ-OEE-B", ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, WC_B);
            seedCalendar(84002L, WC_B, "08:00", "16:00", null);
            seedCapacity(85002L, WC_B, 88002L, "20");
            Long woB = 73002L;
            seedWorkOrder(woB, "WO-OEE-B", 88002L);
            seedJobCard(72002L, woB, WC_B);
            // 3 日各 RUNNING 4h = 12h
            for (int d = 13; d <= 15; d++) {
                seedStatusLog(72100L + d, EQ_B, null,
                        ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, ts(2026, 7, d, 8, 0));
                seedStatusLog(72200L + d, EQ_B, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING,
                        ErpMntDaoConstants.EQUIPMENT_STATUS_IDLE, ts(2026, 7, d, 12, 0));
                seedTimeLog(72600L + d, 72002L, woB, LocalDate.of(2026, 7, d), "20",
                        d == 14 ? "20" : "0");
            }
            return null;
        });
    }

    private void seedEquipment(Long id, String code, String status, Long workcenterId) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment equipment = dao.newEntity();
        equipment.setId(id);
        equipment.setCode(code);
        equipment.setName("OEE 测试设备" + id);
        equipment.setStatus(status);
        equipment.setWorkcenterId(workcenterId);
        dao.saveEntity(equipment);
    }

    private void seedStatusLog(Long id, Long equipmentId, String fromStatus, String toStatus, Timestamp changeAt) {
        IEntityDao<ErpMntEquipmentStatusLog> dao = daoProvider.daoFor(ErpMntEquipmentStatusLog.class);
        ErpMntEquipmentStatusLog log = dao.newEntity();
        log.setId(id);
        log.setEquipmentId(equipmentId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setChangeAt(changeAt);
        log.setSource(ErpMntDaoConstants.STATUS_LOG_SOURCE_MANUAL);
        dao.saveEntity(log);
    }

    private void seedDowntime(Long id, Long equipmentId, Timestamp startTime, Timestamp endTime) {
        IEntityDao<ErpMntDowntimeEntry> dao = daoProvider.daoFor(ErpMntDowntimeEntry.class);
        ErpMntDowntimeEntry entry = dao.newEntity();
        entry.setId(id);
        entry.setEquipmentId(equipmentId);
        entry.setStartTime(startTime);
        entry.setEndTime(endTime);
        dao.saveEntity(entry);
    }

    private void seedCalendar(Long id, Long workcenterId, String startTime, String endTime, String pattern) {
        IEntityDao<ErpMfgWorkcenterCalendar> dao = daoProvider.daoFor(ErpMfgWorkcenterCalendar.class);
        ErpMfgWorkcenterCalendar calendar = dao.newEntity();
        calendar.setId(id);
        calendar.setWorkcenterId(workcenterId);
        calendar.setCalendarName("OEE 测试日历" + id);
        calendar.setShiftType("MORNING");
        calendar.setWorkDatePattern(pattern);
        calendar.setStartTime(startTime);
        calendar.setEndTime(endTime);
        calendar.setIsActive(true);
        dao.saveEntity(calendar);
    }

    private void seedCapacity(Long id, Long workcenterId, Long materialId, String capacityPerHour) {
        IEntityDao<ErpMfgWorkcenterCapacity> dao = daoProvider.daoFor(ErpMfgWorkcenterCapacity.class);
        ErpMfgWorkcenterCapacity capacity = dao.newEntity();
        capacity.setId(id);
        capacity.setWorkcenterId(workcenterId);
        capacity.setMaterialId(materialId);
        capacity.setCapacityPerHour(new BigDecimal(capacityPerHour));
        capacity.setIsActive(true);
        dao.saveEntity(capacity);
    }

    private void seedWorkOrder(Long id, String code, Long productId) {
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        ErpMfgWorkOrder workOrder = dao.newEntity();
        workOrder.setId(id);
        workOrder.setCode(code);
        workOrder.setProductId(productId);
        workOrder.setPlannedQuantity(BigDecimal.valueOf(300));
        workOrder.setBusinessDate(FROM);
        workOrder.setDocStatus("COMPLETED");
        dao.saveEntity(workOrder);
    }

    private void seedJobCard(Long id, Long workOrderId, Long workcenterId) {
        IEntityDao<ErpMfgJobCard> dao = daoProvider.daoFor(ErpMfgJobCard.class);
        ErpMfgJobCard card = dao.newEntity();
        card.setId(id);
        card.setWorkOrderId(workOrderId);
        card.setLineNo(10);
        card.setPlannedQuantity(BigDecimal.valueOf(300));
        card.setStatus("COMPLETED");
        card.setWorkcenterId(workcenterId);
        dao.saveEntity(card);
    }

    private void seedTimeLog(Long id, Long jobCardId, Long workOrderId, LocalDate workDate,
                             String completed, String scrapped) {
        IEntityDao<ErpMfgJobCardTimeLog> dao = daoProvider.daoFor(ErpMfgJobCardTimeLog.class);
        ErpMfgJobCardTimeLog log = dao.newEntity();
        log.setId(id);
        log.setJobCardId(jobCardId);
        log.setWorkOrderId(workOrderId);
        log.setOperatorId("op-oee");
        log.setWorkDate(workDate);
        log.setDurationMins(new BigDecimal("480"));
        log.setCompletedQuantity(new BigDecimal(completed));
        log.setScrappedQuantity(new BigDecimal(scrapped));
        dao.saveEntity(log);
    }

    private void seedInspection(Long id, String code, String relatedBillCode, LocalDate inspectionDate,
                                String result, String lotQuantity) {
        IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
        ErpQaInspection inspection = dao.newEntity();
        inspection.setId(id);
        inspection.setCode(code);
        inspection.setInspectionType("FINAL");
        inspection.setRelatedBillType("ERP_MFG_WORK_ORDER");
        inspection.setRelatedBillCode(relatedBillCode);
        inspection.setMaterialId(MATERIAL_1);
        inspection.setBusinessDate(inspectionDate);
        inspection.setInspectionDate(inspectionDate);
        inspection.setLotQuantity(new BigDecimal(lotQuantity));
        inspection.setResult(result);
        inspection.setDocStatus("COMPLETED");
        inspection.setApproveStatus("APPROVED");
        dao.saveEntity(inspection);
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(opType, action, request));
    }

    private static Timestamp ts(int year, int month, int day, int hour, int minute) {
        return Timestamp.valueOf(LocalDateTime.of(year, month, day, hour, minute));
    }
}
