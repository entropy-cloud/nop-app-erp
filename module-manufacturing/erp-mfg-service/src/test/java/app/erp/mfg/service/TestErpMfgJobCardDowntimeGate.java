package app.erp.mfg.service;

import app.erp.mfg.biz.ApsLoadSlot;
import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.processor.ErpMfgScheduleToJobCardProcessor;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
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
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 停机→排产门控消费侧测试（RC-R1.76 / P1-RC-068 / UC-MAIN-06，plan 2026-08-19-0445-3 Phase 2 Proof；
 * 发布侧 notify/开放窗口查询见 mnt-service {@code TestErpMntDowntimeSchedulingLinkage}）。
 *
 * <p>mfg-service 测试 classpath 含 app-erp-maintenance-service（test-only）→ 真实
 * {@code IErpMntDowntimeEntryBiz} Bean 解析（@Nullable 注入命中）。覆盖：
 * ① 工作中心开放停机 → 该 workcenter 工单不生成 job card（工单级暂停保持 pending）+ 其他 workcenter
 *    工单不受影响 ② 停机 complete 后窗口关闭 → 重新生成自然恢复 ③ mnt bean 缺失 @Nullable 容错
 * （{@code new} 构造无 bean 场景开放集恒空，零门控零回归）。
 *
 * <p>APS SPI 经 {@link TestStubApsLoadSourceProvider} 桩（镜像 {@link TestErpMfgScheduleToJobCard}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/mfg/beans/test-aps-load-source.beans.xml")
public class TestErpMfgJobCardDowntimeGate extends JunitAutoTestCase {

    static final Long UOM_ID = 5101L;
    static final Long P = 7001L;          // 产成品
    static final Long WC1 = 7002L;        // 工作中心1（停机）
    static final Long WC2 = 7003L;        // 工作中心2（健康）
    static final Long MNT_EQUIPMENT_ID = 7901L;
    static final Long OP_ORDER_BASE = 7800L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    TestStubApsLoadSourceProvider apsStub;

    private final AtomicLong idSeq = new AtomicLong(700000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    @BeforeEach
    public void resetStub() {
        apsStub.clear();
    }

    // ---------- ① 开放停机 → 工单级暂停 + 其他工作中心不受影响 ----------

    @Test
    public void testOpenDowntimePausesWorkOrderOnWorkcenter() {
        Long woPaused = seedWorkOrder("WO-DT-PAUSED", ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, bd("2"));
        Long woHealthy = seedWorkOrder("WO-DT-HEALTHY", ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, bd("3"));
        apsStub.putSlots(woPaused, slotsOn(woPaused, WC1, 2));
        apsStub.putSlots(woHealthy, slotsOn(woHealthy, WC2, 1));
        seedOpenDowntimeOnWorkcenter(WC1);

        rpcOk(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woPaused));
        assertEquals(0, findJobCards(woPaused).size(), "开放停机工作中心的工单本轮不建卡（排产暂停）");
        ErpMfgWorkOrder reloadedPaused = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woPaused);
        assertTrue(reloadedPaused.getSourceOrderType() == null,
                "工单保持 pending（不标记 APS_SCHEDULE，下次排产执行自然重试）");

        rpcOk(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woHealthy));
        assertEquals(1, findJobCards(woHealthy).size(), "健康工作中心工单不受影响正常建卡");
    }

    // ---------- ② 停机 complete → 窗口关闭 → 重新生成自然恢复 ----------

    @Test
    public void testDowntimeCompleteRecoversGeneration() {
        Long woId = seedWorkOrder("WO-DT-RECOVER", ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, bd("1"));
        apsStub.putSlots(woId, slotsOn(woId, WC1, 2));
        Long downtimeId = seedOpenDowntimeOnWorkcenter(WC1);

        rpcOk(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woId));
        assertEquals(0, findJobCards(woId).size(), "停机期不建卡");

        // 停机 complete（真实 mnt 流程：endTime + 设备恢复 RUNNING → 窗口关闭）
        assertEquals(0, rpc(mutation, "ErpMntDowntimeEntry__complete", Map.of("downtimeId", downtimeId)).getStatus(),
                "停机 complete 应成功");

        rpcOk(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woId));
        assertEquals(2, findJobCards(woId).size(), "窗口关闭后重新生成恢复排产（拉取模型免 push）");
        ErpMfgWorkOrder reloaded = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.SOURCE_ORDER_TYPE_APS_SCHEDULE, reloaded.getSourceOrderType(),
                "恢复后正常标记");
    }

    // ---------- ③ mnt bean 缺失 @Nullable 容错 ----------

    @Test
    public void testMntBeanAbsentTolerance() {
        // new 构造 = mnt 模块未部署场景（@Nullable 字段保持 null）
        ErpMfgScheduleToJobCardProcessor bare = new ErpMfgScheduleToJobCardProcessor();
        assertTrue(bare.findOpenDowntimeWorkcenterIds(null).isEmpty(),
                "mnt bean 缺失 → 开放集恒空 → 门控零命中零回归");
    }

    // ---------- helpers ----------

    /** 开放停机窗口：设备 DOWN + workcenterId 映射 + endTime null 停机记录。返回 downtimeId。 */
    private Long seedOpenDowntimeOnWorkcenter(Long workcenterId) {
        Long downtimeId = nextId();
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMntEquipment> equipmentDao = daoProvider.daoFor(ErpMntEquipment.class);
            ErpMntEquipment equipment = new ErpMntEquipment();
            equipment.setId(MNT_EQUIPMENT_ID);
            equipment.setCode("EQ-MFG-DT");
            equipment.setName("停机设备");
            equipment.setWorkcenterId(workcenterId);
            equipment.setStatus(ErpMntDaoConstants.EQUIPMENT_STATUS_DOWN);
            equipmentDao.saveEntity(equipment);

            IEntityDao<ErpMntDowntimeEntry> dao = daoProvider.daoFor(ErpMntDowntimeEntry.class);
            ErpMntDowntimeEntry downtime = new ErpMntDowntimeEntry();
            downtime.setId(downtimeId);
            downtime.setEquipmentId(MNT_EQUIPMENT_ID);
            downtime.setStartTime(java.sql.Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
            downtime.setReason("mfg-gate-test");
            dao.saveEntity(downtime);
        });
        return downtimeId;
    }

    private List<ApsLoadSlot> slotsOn(Long woId, Long workcenterId, int count) {
        LocalDateTime base = LocalDateTime.of(2026, 8, 19, 8, 0);
        java.util.ArrayList<ApsLoadSlot> list = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ApsLoadSlot s = new ApsLoadSlot();
            s.setOperationOrderId(OP_ORDER_BASE + i);
            s.setWorkOrderId(woId);
            s.setSequence(i + 1);
            s.setWorkcenterId(workcenterId);
            s.setPlannedStartT(base.plusHours(i * 2L));
            s.setPlannedEndT(base.plusHours(i * 2L + 1));
            s.setSetupTime(bd("10"));
            list.add(s);
        }
        return list;
    }

    private List<ErpMfgJobCard> findJobCards(Long woId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", woId));
        return daoProvider.daoFor(ErpMfgJobCard.class).findAllByQuery(q);
    }

    private Long seedWorkOrder(String code, String docStatus, BigDecimal plannedQty) {
        Long id = 8600L + (long) Math.abs(code.hashCode() % 500);
        ormTemplate.runInSession(() -> {
            seedMaterial(P);
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setPlannedQuantity(plannedQty);
            wo.setBusinessDate(LocalDate.of(2026, 8, 19));
            wo.setDocStatus(docStatus);
            dao.saveEntity(wo);
        });
        return id;
    }

    private void seedMaterial(Long id) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        if (dao.getEntityById(id) != null) {
            return;
        }
        ErpMdMaterial m = new ErpMdMaterial();
        m.orm_propValueByName("id", id);
        m.setCode("MAT-" + id);
        m.setName("Material " + id);
        m.orm_propValueByName("materialType", "GOODS");
        m.setUoMId(UOM_ID);
        m.setStatus("ACTIVE");
        dao.saveEntity(m);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功，但返回: " + resp);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
