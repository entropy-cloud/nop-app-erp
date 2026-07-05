package app.erp.mfg.service;

import app.erp.mfg.biz.ApsLoadSlot;
import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.md.dao.entity.ErpMdMaterial;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 测试：APS 排程 → 工序卡自动生成（plan 2026-07-05-0427-3）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>手动建卡全链：WO + 已排程 OperationOrder → 每工序一 JobCard（OPEN、plannedQuantity=工单计划量、
 *       sourceScheduleId 回写、WorkOrder.sourceOrderType=APS_SCHEDULE）。</li>
 *   <li>幂等：重复调用默认抛 ERR_JOB_CARDS_ALREADY_GENERATED。</li>
 *   <li>incremental 补缺：config 开时仅补建缺失工序卡，既有卡不重建不删。</li>
 *   <li>状态门控：DRAFT / 终态（COMPLETED）拒绝。</li>
 *   <li>无排程：WO 无已排程 OperationOrder → ERR_NO_SCHEDULED_OPERATIONS。</li>
 *   <li>批量入口 config-gated：findWorkOrdersPendingJobCards + generatePendingJobCards。</li>
 * </ul>
 *
 * <p>APS SPI 经 {@link TestStubApsLoadSourceProvider} 桩（{@code test-aps-load-source.beans.xml} 注册，
 * 经 {@code ioc:collect-beans by-type} 同时注入 {@code CrpLoadCalculator} 与本计划新增的
 * {@code ErpMfgScheduleToJobCardProcessor}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/mfg/beans/test-aps-load-source.beans.xml")
public class TestErpMfgScheduleToJobCard extends JunitAutoTestCase {

    static final Long UOM_ID = 5101L;
    static final Long P = 7001L;          // 产成品
    static final Long WC1 = 7002L;        // 工作中心1
    static final Long WC2 = 7003L;        // 工作中心2
    static final Long OP_ORDER_BASE = 7500L; // 模拟 ErpApsOperationOrder id 起点

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    TestStubApsLoadSourceProvider apsStub;

    @BeforeEach
    public void resetStub() {
        apsStub.clear();
    }

    @Test
    public void testHappyPath_GeneratesOneJobCardPerOperation() {
        Long woId = seedWorkOrder("WO-APS-HAPPY", ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, bd("10"));
        apsStub.putSlots(woId, slots(woId, 2));

        rpcOk(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woId));

        List<ErpMfgJobCard> cards = findJobCards(woId);
        assertEquals(2, cards.size(), "每道已排程工序各生成一 JobCard");

        for (ErpMfgJobCard jc : cards) {
            assertEquals(ErpMfgConstants.JOB_CARD_STATUS_OPEN, jc.getStatus(), "初始状态对齐 2237-1 入口 OPEN");
            assertEquals(0, bd("10").compareTo(jc.getPlannedQuantity()), "JobCard 计划数量 = 工单计划生产量");
            assertTrue(jc.getSourceScheduleId() != null && jc.getSourceScheduleId() >= OP_ORDER_BASE,
                    "sourceScheduleId 回写为对应 OperationOrder id");
            assertTrue(jc.getWorkcenterId() != null, "workcenterId 来自 APS 排程");
            assertTrue(jc.getCode() != null && jc.getCode().contains("OP"), "JobCard code 含工序序号");
        }

        ErpMfgWorkOrder reloaded = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.SOURCE_ORDER_TYPE_APS_SCHEDULE, reloaded.getSourceOrderType(),
                "WorkOrder 来源标记回写为 APS_SCHEDULE");
        assertTrue(reloaded.getSourceScheduleId() != null, "WorkOrder sourceScheduleId 弱参照回写");
    }

    @Test
    public void testIdempotent_DefaultRejectsRepeatCall() {
        Long woId = seedWorkOrder("WO-APS-IDEM", ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, bd("1"));
        apsStub.putSlots(woId, slots(woId, 1));

        rpcOk(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woId));
        assertEquals(1, findJobCards(woId).size());

        ApiResponse<?> resp = rpc(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woId));
        assertEquals(ErpMfgErrors.ERR_JOB_CARDS_ALREADY_GENERATED.getErrorCode(), resp.getCode(),
                "默认重复调用抛 ERR_JOB_CARDS_ALREADY_GENERATED");
        assertEquals(1, findJobCards(woId).size(), "抛错时不新增卡");
    }

    @Test
    public void testIncrementalRebuild_OnlyBuildsMissing() {
        Long woId = seedWorkOrder("WO-APS-INCR", ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, bd("5"));
        apsStub.putSlots(woId, slots(woId, 2));
        rpcOk(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woId));
        assertEquals(2, findJobCards(woId).size());

        // 新增第 3 道工序排程（模拟 APS 重排后追加）
        apsStub.clear();
        apsStub.putSlots(woId, slots(woId, 3));

        setConfig(ErpMfgConstants.CONFIG_JOBCARD_INCREMENTAL_REBUILD, "true");
        try {
            rpcOk(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woId));
            List<ErpMfgJobCard> cards = findJobCards(woId);
            assertEquals(3, cards.size(), "incremental 模式仅补建缺失的第 3 道工序卡");
            long withSource = cards.stream().filter(c -> c.getSourceScheduleId() != null).count();
            assertEquals(3, withSource, "所有卡均带 sourceScheduleId（既有不重建）");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_JOBCARD_INCREMENTAL_REBUILD, "false");
        }
    }

    @Test
    public void testStateGate_DraftRejected() {
        Long woId = seedWorkOrder("WO-APS-DRAFT", ErpMfgConstants.WORK_ORDER_STATUS_DRAFT, bd("1"));
        apsStub.putSlots(woId, slots(woId, 1));

        ApiResponse<?> resp = rpc(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woId));
        assertEquals(ErpMfgErrors.ERR_WORK_ORDER_STATUS_NOT_ALLOWED_FOR_JOB_CARD_GEN.getErrorCode(), resp.getCode(),
                "DRAFT 未审核不允许建卡");
        assertEquals(0, findJobCards(woId).size());
    }

    @Test
    public void testStateGate_TerminalRejected() {
        Long woId = seedWorkOrder("WO-APS-DONE", ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, bd("1"));
        apsStub.putSlots(woId, slots(woId, 1));

        ApiResponse<?> resp = rpc(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woId));
        assertEquals(ErpMfgErrors.ERR_WORK_ORDER_STATUS_NOT_ALLOWED_FOR_JOB_CARD_GEN.getErrorCode(), resp.getCode(),
                "终态 COMPLETED 不允许建卡");
    }

    @Test
    public void testNoSchedule_Rejected() {
        Long woId = seedWorkOrder("WO-APS-NOSCHED", ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, bd("1"));
        // 不向 stub 注入任何 slot

        ApiResponse<?> resp = rpc(mutation, "ErpMfgWorkOrder__generateJobCardsFromSchedule", Map.of("workOrderId", woId));
        assertEquals(ErpMfgErrors.ERR_NO_SCHEDULED_OPERATIONS.getErrorCode(), resp.getCode(),
                "无已排程工序 → ERR_NO_SCHEDULED_OPERATIONS");
        assertEquals(0, findJobCards(woId).size());
    }

    @Test
    public void testFindPendingAndBatchGenerate_ConfigGated() {
        Long wo1 = seedWorkOrder("WO-APS-BATCH1", ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, bd("2"));
        Long wo2 = seedWorkOrder("WO-APS-BATCH2", ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED, bd("3"));
        // wo3：已有 JobCard，不应出现在 pending 列表
        Long wo3 = seedWorkOrder("WO-APS-HASCARD", ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, bd("1"));
        apsStub.putSlots(wo1, slots(wo1, 1));
        apsStub.putSlots(wo2, slots(wo2, 2));
        apsStub.putSlots(wo3, slots(wo3, 1));
        seedJobCardDirectly(wo3, 7700L, 1);

        // 总开关默认 false → 批量入口直接返回 0，不建卡
        ApiResponse<?> off = rpc(mutation, "ErpMfgWorkOrder__generatePendingJobCards", new LinkedHashMap<>());
        assertEquals(0, off.getStatus());
        assertEquals(0, findJobCards(wo1).size(), "总开关关 → 不建卡");

        // 查询入口返回 wo1/wo2（wo3 已有卡排除）
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("limit", 10);
        ApiResponse<?> qResp = rpc(query, "ErpMfgWorkOrder__findWorkOrdersPendingJobCards", q);
        assertEquals(0, qResp.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) qResp.getData();
        assertEquals(2, data.size(), "pending 列表含 wo1/wo2，wo3 已有卡排除");

        setConfig(ErpMfgConstants.CONFIG_JOBCARD_AUTO_GENERATE_ON_SCHEDULE, "true");
        try {
            ApiResponse<?> on = rpc(mutation, "ErpMfgWorkOrder__generatePendingJobCards", new LinkedHashMap<>());
            assertEquals(0, on.getStatus());
            assertEquals(2, on.getData(), "批量建卡 2 个工单（wo1 + wo2）");
            assertTrue(findJobCards(wo1).size() >= 1, "wo1 已建卡");
            assertTrue(findJobCards(wo2).size() >= 1, "wo2 已建卡");
            assertEquals(1, findJobCards(wo3).size(), "wo3 既有卡不被批量重建");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_JOBCARD_AUTO_GENERATE_ON_SCHEDULE, "false");
        }
    }

    // ---------- helpers ----------

    private List<ErpMfgJobCard> findJobCards(Long woId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", woId));
        return daoProvider.daoFor(ErpMfgJobCard.class).findAllByQuery(q);
    }

    private List<ApsLoadSlot> slots(Long woId, int count) {
        LocalDateTime base = LocalDateTime.of(2026, 7, 6, 8, 0);
        List<ApsLoadSlot> list = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ApsLoadSlot s = new ApsLoadSlot();
            s.setOperationOrderId(OP_ORDER_BASE + i);
            s.setWorkOrderId(woId);
            s.setSequence(i + 1);
            s.setWorkcenterId(i % 2 == 0 ? WC1 : WC2);
            s.setPlannedStartT(base.plusHours(i * 2L));
            s.setPlannedEndT(base.plusHours(i * 2L + 1));
            s.setSetupTime(bd("10"));
            list.add(s);
        }
        return list;
    }

    private Long seedWorkOrder(String code, String docStatus, BigDecimal plannedQty) {
        Long id = 8000L + (long) Math.abs(code.hashCode() % 1000);
        ormTemplate.runInSession(() -> {
            seedMaterial(P);
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setPlannedQuantity(plannedQty);
            wo.setBusinessDate(LocalDate.of(2026, 7, 5));
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

    private void seedJobCardDirectly(Long woId, Long jcId, int lineNo) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgJobCard> dao = daoProvider.daoFor(ErpMfgJobCard.class);
            ErpMfgJobCard jc = new ErpMfgJobCard();
            jc.orm_propValueByName("id", jcId);
            jc.setWorkOrderId(woId);
            jc.setLineNo(lineNo);
            jc.setPlannedQuantity(bd("1"));
            jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_OPEN);
            dao.saveEntity(jc);
        });
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功，但返回: " + resp);
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
