package app.erp.mnt.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
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
import io.nop.api.core.config.AppConfig;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：备件消耗→inventory 出库（{@code generateMove} OUTGOING + 余额扣减 + posted）
 * + 可用量不足回滚 + 维护计划到期生成访问（{@code generateDueVisits} PLANNED + nextDueDate 推进 + 门控）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntSparePartAndSchedule extends JunitAutoTestCase {

    static final Long ORG_ID = 1201L;
    static final Long EQUIPMENT_ID = 101L;
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

    private final AtomicLong idSeq = new AtomicLong(100000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    // ---------- 备件消耗出库 ----------

    @Test
    public void testSparePartConfirmIssuesStockAndPosts() {
        seedPeriodAndSubjects();
        Long usageId = nextId();
        Long lineId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID);
            seedUsage(usageId, EQUIPMENT_ID, "SP-POST-001");
            seedUsageLine(lineId, usageId, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });
        seedStock("SEED-SP-POST-001", new BigDecimal("20"), new BigDecimal("5"));

        assertEquals(0, confirm(usageId).getStatus(), "备件消耗确认应成功");

        ErpMntSparePartUsage usage = loadUsage(usageId);
        assertEquals(ErpMntDaoConstants.DOC_STATUS_ACTIVE, usage.getDocStatus(), "docStatus→ACTIVE");
        assertEquals(ErpMntDaoConstants.APPROVE_STATUS_APPROVED, usage.getApproveStatus(), "approveStatus→APPROVED");
        assertTrue(usage.getPosted(), "posted=true（库存已出库）");
        assertEquals(0, usage.getTotalAmount().compareTo(new BigDecimal("50")),
                "totalAmount 聚合 = 10×5=50");

        ErpInvStockMove move = findMove("SP-POST-001");
        assertNotNull(move, "应生成出库移动单");
        assertEquals(ErpInvConstants.MOVE_TYPE_OUTGOING, move.getMoveType(), "出库类型");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "业务联动自动 DONE");

        ErpInvStockBalance balance = findBalance();
        assertNotNull(balance, "应存在库存余额");
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("10")),
                "余额 = 20(预置) - 10(出库) = 10");
    }

    @Test
    public void testSparePartConfirmInsufficientRollsBack() {
        seedPeriodAndSubjects();
        Long usageId = nextId();
        Long lineId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID);
            seedUsage(usageId, EQUIPMENT_ID, "SP-INSUF-001");
            seedUsageLine(lineId, usageId, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });
        // 仅预置 5（不足出库需要的 10）
        seedStock("SEED-SP-INSUF-001", new BigDecimal("5"), new BigDecimal("5"));

        ApiResponse<?> bad = confirm(usageId);
        assertEquals(ErpInvErrors.ERR_AVAILABLE_INSUFFICIENT.getErrorCode(), bad.getCode(),
                "可用量不足应返回 ERR_AVAILABLE_INSUFFICIENT 致整笔回滚");

        ErpMntSparePartUsage after = loadUsage(usageId);
        assertFalse(after.getPosted(), "回滚 → posted 仍为 false");

        ErpInvStockBalance balance = findBalance();
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("5")),
                "回滚 → 库存余额未扣减");
    }

    // ---------- 维护计划到期生成 ----------

    @Test
    public void testGenerateDueVisitsCreatesPlannedVisitAndAdvancesNextDueDate() {
        Long scheduleId = nextId();
        LocalDate dueDate = LocalDate.of(2026, 7, 1);
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID);
            seedSchedule(scheduleId, EQUIPMENT_ID, dueDate,
                    ErpMntDaoConstants.RECURRENCE_TYPE_MONTHLY, 1, "SCH-DUE-001");
            return null;
        });

        Integer generated = generateDueVisits(LocalDate.of(2026, 7, 1));
        assertEquals(1, generated, "应生成 1 个 PLANNED 访问");

        ErpMntSchedule schedule = daoProvider.daoFor(ErpMntSchedule.class).getEntityById(scheduleId);
        assertEquals(LocalDate.of(2026, 8, 1), schedule.getNextDueDate(),
                "MONTHLY+1 → nextDueDate 推进 1 月");

        ErpMntVisit visit = findVisitBySchedule(scheduleId);
        assertNotNull(visit, "应生成关联 scheduleId 的访问");
        assertEquals(ErpMntDaoConstants.VISIT_TYPE_PLANNED, visit.getVisitType(), "visitType=PLANNED");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_DRAFT, visit.getStatus(), "DRAFT");
        assertEquals(scheduleId, visit.getScheduleId());
    }

    @Test
    public void testGenerateDueVisitsGateDisabled() {
        Long scheduleId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID);
            seedSchedule(scheduleId, EQUIPMENT_ID, LocalDate.of(2026, 7, 1),
                    ErpMntDaoConstants.RECURRENCE_TYPE_MONTHLY, 1, "SCH-GATE-001");
            return null;
        });

        setAutoGenerateDueVisits(false);
        try {
            Integer generated = generateDueVisits(LocalDate.of(2026, 7, 1));
            assertEquals(0, generated, "门控关闭 → 不生成访问");
            assertNull(findVisitBySchedule(scheduleId), "门控关闭 → 无访问生成");
        } finally {
            setAutoGenerateDueVisits(true);
        }
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> confirm(Long usageId) {
        ApiResponse<?> resp = executeRpc(mutation, "ErpMntSparePartUsage__confirm", ApiRequest.build(Map.of("usageId", usageId)));
        if (resp.getStatus() != 0) {
            System.out.println("[DEBUG confirm] status=" + resp.getStatus() + " code=" + resp.getCode() + " msg=" + resp.getMsg());
        }
        return resp;
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

    private void setAutoGenerateDueVisits(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpMntConstants.CONFIG_AUTO_GENERATE_DUE_VISITS, String.valueOf(value));
    }

    // ---------- finance / stock seed ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 10);
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
        schema.setNature(10);
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus(10);
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, int status) {
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
        subject.setSubjectClass(10);
        subject.setDirection(10);
        subject.setStatus(10);
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

    private void seedEquipment(Long id) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment equipment = new ErpMntEquipment();
        equipment.setId(id);
        equipment.setCode("EQ-" + id);
        equipment.setName("设备" + id);
        equipment.setStatus(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
        dao.saveEntity(equipment);
    }

    private void seedUsage(Long id, Long equipmentId, String code) {
        IEntityDao<ErpMntSparePartUsage> dao = daoProvider.daoFor(ErpMntSparePartUsage.class);
        ErpMntSparePartUsage usage = new ErpMntSparePartUsage();
        usage.setId(id);
        usage.setCode(code);
        usage.setEquipmentId(equipmentId);
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

    private void seedSchedule(Long id, Long equipmentId, LocalDate nextDueDate,
                              int recurrenceType, int frequency, String code) {
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

    // ---------- query helpers ----------

    private ErpMntSparePartUsage loadUsage(Long usageId) {
        return daoProvider.daoFor(ErpMntSparePartUsage.class).getEntityById(usageId);
    }

    private ErpInvStockMove findMove(String usageCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpMntConstants.RELATED_BILL_TYPE_MNT_SPARE_PART));
        q.addFilter(eq("relatedBillCode", usageCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpMntVisit findVisitBySchedule(Long scheduleId) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("scheduleId", scheduleId));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
