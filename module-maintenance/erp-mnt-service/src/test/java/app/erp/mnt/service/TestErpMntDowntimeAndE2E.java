package app.erp.mnt.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntSchedule;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntSparePartUsageLine;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 服务层集成测试：停机记录（record→设备 DOWN + complete→totalMinutes + 设备恢复）
 * + 维护全场景端到端（计划到期生成 PLANNED 访问→schedule→start→备件消耗出库→complete；
 * 报修请求→accept 生成 RESPONSIVE 访问→执行→complete）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntDowntimeAndE2E extends JunitAutoTestCase {

    static final Long ORG_ID = 1201L;
    static final Long EQUIPMENT_ID = 101L;
    static final Long ASSIGNEE_ID = 201L;
    static final Long WAREHOUSE_ID = 3201L;
    static final Long MATERIAL_ID = 4201L;
    static final Long UOM_ID = 5201L;
    static final Long CURRENCY_ID = 6201L;
    static final Long ACCT_SCHEMA_ID = 7201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(300000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    // ---------- 停机记录 ----------

    @Test
    public void testDowntimeRecordSetsDownAndCompleteRestores() {
        Long downtimeId = nextId();
        LocalDateTime startedAt = LocalDateTime.now();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedDowntime(downtimeId, EQUIPMENT_ID, startedAt, 9001L, "DT-DOWN-001");
            return null;
        });

        assertEquals(0, recordDowntime(downtimeId).getStatus(), "record 应成功");
        ErpMntDowntimeEntry recorded = loadDowntime(downtimeId);
        assertNotNull(recorded.getStartTime(), "startTime 已记录");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN, equipmentStatus(),
                "record 设备置 DOWN");

        assertEquals(0, completeDowntime(downtimeId).getStatus(), "complete 应成功");
        ErpMntDowntimeEntry completed = loadDowntime(downtimeId);
        assertNotNull(completed.getEndTime(), "complete 设置 endTime");
        assertNotNull(completed.getTotalMinutes(), "complete 计算 totalMinutes");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, equipmentStatus(),
                "complete 恢复设备 RUNNING");

        // 已结束的停机记录不可再次 record/complete（终态保护）
        ApiResponse<?> badRecord = recordDowntime(downtimeId);
        assertNotEquals(0, badRecord.getStatus(), "已结束不可再次 record");
        assertEquals(ErpMntErrors.ERR_DOWNTIME_ALREADY_COMPLETED.getErrorCode(), badRecord.getCode());

        ApiResponse<?> badComplete = completeDowntime(downtimeId);
        assertNotEquals(0, badComplete.getStatus(), "已结束不可再次 complete");
        assertEquals(ErpMntErrors.ERR_DOWNTIME_ALREADY_COMPLETED.getErrorCode(), badComplete.getCode());
    }

    @Test
    public void testDowntimeTotalMinutesReflectsDuration() {
        Long downtimeId = nextId();
        LocalDateTime startedAt = LocalDateTime.now().minusHours(2);
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN);
            seedDowntime(downtimeId, EQUIPMENT_ID, startedAt, 9002L, "DT-DUR-001");
            return null;
        });

        assertEquals(0, completeDowntime(downtimeId).getStatus(), "complete 应成功");
        ErpMntDowntimeEntry completed = loadDowntime(downtimeId);
        long minutes = completed.getTotalMinutes().longValue();
        assertTrue(minutes >= 119, "totalMinutes 反映 2 小时停机（>=119）: " + minutes);
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, equipmentStatus(),
                "complete 恢复设备 RUNNING");
    }

    // ---------- 维护全场景：计划到期生成 PLANNED 访问 ----------

    @Test
    public void testPlannedVisitFullFlowWithSparePartIssue() {
        seedPeriodAndSubjects();
        Long scheduleId = nextId();
        LocalDate dueDate = LocalDate.of(2026, 7, 1);
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedSchedule(scheduleId, EQUIPMENT_ID, dueDate,
                    ErpMntDaoConstants.RECURRENCE_TYPE_MONTHLY, 1, "SCH-E2E-001");
            return null;
        });

        Integer generated = generateDueVisits(dueDate);
        assertEquals(1, generated, "应生成 1 个 PLANNED 访问");

        ErpMntVisit visit = findVisitBySchedule(scheduleId);
        assertNotNull(visit, "应生成关联 scheduleId 的访问");
        assertEquals(ErpMntDaoConstants.VISIT_TYPE_PLANNED, visit.getVisitType(), "visitType=PLANNED");
        assignVisit(visit.getId(), ASSIGNEE_ID);

        assertEquals(0, scheduleVisit(visit.getId()).getStatus(), "DRAFT→SCHEDULED");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_SCHEDULED, visitStatus(visit.getId()));

        assertEquals(0, startVisit(visit.getId()).getStatus(), "SCHEDULED→IN_PROGRESS");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE, equipmentStatus(),
                "执行中设备置 UNDER_MAINTENANCE");

        Long usageId = nextId();
        Long lineId = nextId();
        ormTemplate.runInSession(session -> {
            seedUsage(usageId, EQUIPMENT_ID, visit.getId(), "SP-E2E-001");
            seedUsageLine(lineId, usageId, new BigDecimal("4"), new BigDecimal("5"));
            return null;
        });
        seedStock("SEED-SP-E2E-001", new BigDecimal("10"), new BigDecimal("5"));

        assertEquals(0, confirmUsage(usageId).getStatus(), "备件消耗出库应成功");
        ErpMntSparePartUsage usage = loadUsage(usageId);
        assertTrue(usage.getPosted(), "posted=true（库存已出库）");
        ErpInvStockBalance balance = findBalance();
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("6")),
                "余额 = 10 - 4 = 6");

        assertEquals(0, completeVisit(visit.getId()).getStatus(), "IN_PROGRESS→COMPLETED");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_COMPLETED, visitStatus(visit.getId()));
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, equipmentStatus(),
                "完成恢复设备 RUNNING");
    }

    // ---------- 维护全场景：报修请求→RESPONSIVE 访问 ----------

    @Test
    public void testResponsiveRequestFullFlow() {
        Long requestId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedRequest(requestId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_OPEN, "REQ-E2E-001");
            return null;
        });

        assertEquals(0, accept(requestId).getStatus(), "OPEN→ACCEPTED");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED, requestStatus(requestId));

        ErpMntVisit visit = findVisitByCode("VST-REQ-" + requestId);
        assertNotNull(visit, "受理应生成维护访问");
        assertEquals(ErpMntDaoConstants.VISIT_TYPE_RESPONSIVE, visit.getVisitType(), "visitType=RESPONSIVE");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_DRAFT, visit.getStatus(), "生成访问 DRAFT");

        assertEquals(0, scheduleVisit(visit.getId()).getStatus(), "DRAFT→SCHEDULED");
        assertEquals(0, startVisit(visit.getId()).getStatus(), "SCHEDULED→IN_PROGRESS");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE, equipmentStatus(),
                "执行中设备置 UNDER_MAINTENANCE");

        assertEquals(0, startRepair(requestId).getStatus(), "ACCEPTED→IN_PROGRESS");
        assertEquals(0, completeVisit(visit.getId()).getStatus(), "访问 IN_PROGRESS→COMPLETED");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, equipmentStatus(),
                "完成恢复设备 RUNNING");

        assertEquals(0, completeRequest(requestId).getStatus(), "请求 IN_PROGRESS→COMPLETED");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_COMPLETED, requestStatus(requestId));
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> recordDowntime(Long downtimeId) {
        return executeRpc(mutation, "ErpMntDowntimeEntry__record", ApiRequest.build(Map.of("downtimeId", downtimeId)));
    }

    private ApiResponse<?> completeDowntime(Long downtimeId) {
        return executeRpc(mutation, "ErpMntDowntimeEntry__complete", ApiRequest.build(Map.of("downtimeId", downtimeId)));
    }

    private ApiResponse<?> scheduleVisit(Long visitId) {
        return executeRpc(mutation, "ErpMntVisit__schedule", ApiRequest.build(Map.of("visitId", visitId)));
    }

    private ApiResponse<?> startVisit(Long visitId) {
        return executeRpc(mutation, "ErpMntVisit__start", ApiRequest.build(Map.of("visitId", visitId)));
    }

    private ApiResponse<?> completeVisit(Long visitId) {
        return executeRpc(mutation, "ErpMntVisit__complete", ApiRequest.build(Map.of("visitId", visitId)));
    }

    private ApiResponse<?> accept(Long requestId) {
        return executeRpc(mutation, "ErpMntRequest__accept", ApiRequest.build(Map.of("requestId", requestId)));
    }

    private ApiResponse<?> startRepair(Long requestId) {
        return executeRpc(mutation, "ErpMntRequest__startRepair", ApiRequest.build(Map.of("requestId", requestId)));
    }

    private ApiResponse<?> completeRequest(Long requestId) {
        return executeRpc(mutation, "ErpMntRequest__complete", ApiRequest.build(Map.of("requestId", requestId)));
    }

    private ApiResponse<?> confirmUsage(Long usageId) {
        return executeRpc(mutation, "ErpMntSparePartUsage__confirm", ApiRequest.build(Map.of("usageId", usageId)));
    }

    private Integer generateDueVisits(LocalDate asOfDate) {
        ApiResponse<?> resp = executeRpc(mutation, "ErpMntSchedule__generateDueVisits",
                ApiRequest.build(Map.of("asOfDate", asOfDate.toString())));
        assertEquals(0, resp.getStatus(), "generateDueVisits 应成功");
        return ((Number) resp.getData()).intValue();
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- finance / stock seed ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
            seedSubject("6401", "主营业务成本");
            seedAcctSchema();
            return null;
        });
    }

    private void seedAcctSchema() {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setId(ACCT_SCHEMA_ID);
        schema.setCode("AS-" + ORG_ID);
        schema.setName("账套" + ORG_ID);
        schema.setOrgId(ORG_ID);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass("ASSET");
        subject.setDirection("DEBIT");
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedStock(String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        req.put("relatedBillType", "SEED_STOCK");
        req.put("relatedBillCode", billCode);

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", MATERIAL_ID);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));

        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__generateMove",
                ApiRequest.build(Map.of("request", req)));
        assertEquals(0, resp.getStatus(), "seedStock generateMove 应成功");
    }

    // ---------- maintenance seed ----------

    private void seedEquipment(Long id, String status) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment equipment = new ErpMntEquipment();
        equipment.setId(id);
        equipment.setCode("EQ-" + id);
        equipment.setName("设备" + id);
        equipment.setStatus(status);
        dao.saveEntity(equipment);
    }

    private void seedDowntime(Long id, Long equipmentId, LocalDateTime startTime, Long relatedJobOrderId, String reason) {
        IEntityDao<ErpMntDowntimeEntry> dao = daoProvider.daoFor(ErpMntDowntimeEntry.class);
        ErpMntDowntimeEntry downtime = new ErpMntDowntimeEntry();
        downtime.setId(id);
        downtime.setEquipmentId(equipmentId);
        downtime.setStartTime(startTime);
        downtime.setReason(reason);
        downtime.setRelatedJobOrderId(relatedJobOrderId);
        dao.saveEntity(downtime);
    }

    private void seedSchedule(Long id, Long equipmentId, LocalDate nextDueDate,
                              String recurrenceType, int frequency, String code) {
        IEntityDao<ErpMntSchedule> dao = daoProvider.daoFor(ErpMntSchedule.class);
        ErpMntSchedule schedule = new ErpMntSchedule();
        schedule.setId(id);
        schedule.setCode(code);
        schedule.setName("计划" + code);
        schedule.setEquipmentId(equipmentId);
        schedule.setScheduleType(ErpMntDaoConstants.SCHEDULE_TYPE_PREVENTIVE);
        schedule.setFrequency(frequency);
        schedule.setRecurrenceType(recurrenceType);
        schedule.setStartDate(LocalDate.of(2026, 1, 1));
        schedule.setNextDueDate(nextDueDate);
        schedule.setIsActive(1);
        dao.saveEntity(schedule);
    }

    private void seedUsage(Long id, Long equipmentId, Long visitId, String code) {
        IEntityDao<ErpMntSparePartUsage> dao = daoProvider.daoFor(ErpMntSparePartUsage.class);
        ErpMntSparePartUsage usage = new ErpMntSparePartUsage();
        usage.setId(id);
        usage.setCode(code);
        usage.setEquipmentId(equipmentId);
        usage.setVisitId(visitId);
        usage.setWarehouseId(WAREHOUSE_ID);
        usage.setOrgId(ORG_ID);
        usage.setBusinessDate(LocalDate.of(2026, 7, 1));
        usage.setDocStatus(ErpMntDaoConstants.DOC_STATUS_DRAFT);
        usage.setApproveStatus(ErpMntDaoConstants.APPROVE_STATUS_UNSUBMITTED);
        usage.setPosted(false);
        dao.saveEntity(usage);
    }

    private void seedUsageLine(Long id, Long usageId, BigDecimal qty, BigDecimal unitCost) {
        IEntityDao<ErpMntSparePartUsageLine> dao = daoProvider.daoFor(ErpMntSparePartUsageLine.class);
        ErpMntSparePartUsageLine line = new ErpMntSparePartUsageLine();
        line.setId(id);
        line.setSparePartUsageId(usageId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitCost(unitCost);
        line.setAmount(qty.multiply(unitCost));
        dao.saveEntity(line);
    }

    private void seedRequest(Long id, Long equipmentId, String status, String code) {
        IEntityDao<ErpMntRequest> dao = daoProvider.daoFor(ErpMntRequest.class);
        ErpMntRequest request = new ErpMntRequest();
        request.setId(id);
        request.setCode(code);
        request.setEquipmentId(equipmentId);
        request.setRequestDate(LocalDate.of(2026, 7, 1));
        request.setDescription("报修" + code);
        request.setPriority(ErpMntDaoConstants.PRIORITY_NORMAL);
        request.setStatus(status);
        request.setRequestedBy(ASSIGNEE_ID);
        request.setAssignedTo(ASSIGNEE_ID);
        dao.saveEntity(request);
    }

    private void assignVisit(Long visitId, Long assignedTo) {
        // 访问由 generateDueVisits 在前一会话创建（MANAGED），于本会话内修改即可，flush 时持久化
        ormTemplate.runInSession(session -> {
            ErpMntVisit visit = daoProvider.daoFor(ErpMntVisit.class).getEntityById(visitId);
            visit.setAssignedTo(assignedTo);
            return null;
        });
    }

    // ---------- query helpers ----------

    private ErpMntDowntimeEntry loadDowntime(Long downtimeId) {
        return daoProvider.daoFor(ErpMntDowntimeEntry.class).getEntityById(downtimeId);
    }

    private String visitStatus(Long visitId) {
        return daoProvider.daoFor(ErpMntVisit.class).getEntityById(visitId).getStatus();
    }

    private String requestStatus(Long requestId) {
        return daoProvider.daoFor(ErpMntRequest.class).getEntityById(requestId).getStatus();
    }

    private String equipmentStatus() {
        return daoProvider.daoFor(ErpMntEquipment.class).getEntityById(EQUIPMENT_ID).getStatus();
    }

    private ErpMntSparePartUsage loadUsage(Long usageId) {
        return daoProvider.daoFor(ErpMntSparePartUsage.class).getEntityById(usageId);
    }

    private ErpMntVisit findVisitBySchedule(Long scheduleId) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("scheduleId", scheduleId));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpMntVisit findVisitByCode(String code) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
