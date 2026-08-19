package app.erp.aps.service;

import app.erp.aps.biz.IErpApsOperationOrderBiz;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.service.loadsource.ApsLoadSourceProvider;
import app.erp.mfg.biz.ApsLoadSlot;
import app.erp.mfg.biz.IErpApsLoadSourceProvider;
import app.erp.mfg.dao.entity.ErpMfgRouting;
import app.erp.mfg.dao.entity.ErpMfgRoutingOperation;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-APS-01 WorkOrder 下达→OperationOrder 自动创建行为测试（RC-R1.86 / P1-RC-088，D1 裁决选项 B 拉取模型）。
 *
 * <p>覆盖 L1 基本流程 + 双异常分支 + 幂等 + 扫描门控 + CRP 负荷盲闭合（A4.2.178 场景翻转：
 * 有 OperationOrder 的 WorkOrder 出现在 CRP 负荷来源）。
 *
 * <p>mfg 域实体（WorkOrder/Routing/RoutingOperation/Workcenter）经 daoProvider 直种（aps-service
 * 已有 mfg-dao compile 依赖，ATP/CTP 跨域只读先例）；notify 模板 USER_LIST 种子镜像 mnt R1.76 测试范式。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsWorkOrderToOperationOrder extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpApsLoadSourceProvider loadSourceProvider;

    static final Long WC_1 = 9301L;
    static final Long WC_2 = 9302L;
    static final Long ROUTING_1 = 9201L;
    static final Long ROUTING_2 = 9202L;
    static final Long WO_FULL = 9101L;
    static final Long WO_NO_ROUTING = 9102L;
    static final Long WO_BAD_WC = 9103L;
    static final Long WO_DRAFT = 9104L;
    static final String RECIPIENT = "planner-1";

    private static final LocalDateTime HORIZON_START = LocalDateTime.parse("2026-07-10T00:00:00");
    private static final LocalDateTime HORIZON_END = LocalDateTime.parse("2026-07-20T00:00:00");

    // ---------------- 1. 批量创建 + 字段继承 + totalDuration ----------------

    @Test
    public void testBatchCreateInheritsFieldsAndComputesTotalDuration() {
        seedWorkOrder(WO_FULL, "NOT_STARTED", ROUTING_1, "10");
        seedRoutingWithTwoOps(ROUTING_1, WC_1, WC_2);

        Map<String, Object> r = createFromWorkOrder(WO_FULL);

        assertEquals(2, ((Number) r.get("createdCount")).intValue(), "两道工序各建一条 DRAFT 工序工单");
        assertFalse(Boolean.TRUE.equals(r.get("alreadyCreated")));
        assertFalse(Boolean.TRUE.equals(r.get("skippedNoRouting")));

        List<ErpApsOperationOrder> ops = findOps(WO_FULL);
        assertEquals(2, ops.size());

        ErpApsOperationOrder op10 = opBySequence(ops, 10);
        assertEquals("WO-9101-OP10", op10.getCode());
        assertEquals(WO_FULL, op10.getWorkOrderId());
        assertEquals("下料", op10.getOperationName());
        assertEquals(WC_1, op10.getMachineId());
        assertEquals(0, new BigDecimal("5").compareTo(op10.getSetupTime()));
        assertEquals(0, new BigDecimal("2").compareTo(op10.getRuntimePerUnit()));
        assertEquals(0, new BigDecimal("10").compareTo(op10.getQty()));
        assertEquals("DRAFT", op10.getStatus());
        // totalDuration = setupTime + runtimePerUnit × qty = 5 + 2×10 = 25
        assertEquals(0, new BigDecimal("25").compareTo(op10.getTotalDuration()), "totalDuration 公式断言");

        ErpApsOperationOrder op20 = opBySequence(ops, 20);
        assertEquals("车削", op20.getOperationName());
        assertEquals(WC_2, op20.getMachineId());
        // 15 + 3×10 = 45
        assertEquals(0, new BigDecimal("45").compareTo(op20.getTotalDuration()));
    }

    // ---------------- 2. sequence 按 lineNo 排序（无序种入，创建后 sequence 语义正确） ----------------

    @Test
    public void testSequenceFollowsRoutingLineNo() {
        seedWorkOrder(WO_FULL, "NOT_STARTED", ROUTING_2, "1");
        // 乱序种入：lineNo 30 先插，20 后插
        seedRoutingOp(ROUTING_2, 30, WC_1, "1", "1");
        seedRoutingOp(ROUTING_2, 20, WC_2, "1", "1");

        createFromWorkOrder(WO_FULL);

        List<ErpApsOperationOrder> ops = findOps(WO_FULL);
        ops.sort(java.util.Comparator.comparing(ErpApsOperationOrder::getSequence));
        assertEquals(20, ops.get(0).getSequence().intValue(), "工序 sequence 继承工艺路线 lineNo");
        assertEquals(30, ops.get(1).getSequence().intValue());
    }

    // ---------------- 3. 工艺路线缺失 → 整单跳过 + notify 告警 ----------------

    @Test
    public void testMissingRoutingSkipsAndNotifies() {
        seedNotifyTemplate(9401L, ErpApsConstants.NOTIFY_EVENT_WORKORDER_NO_ROUTING);
        seedWorkOrder(WO_NO_ROUTING, "NOT_STARTED", null, "10");

        Map<String, Object> r = createFromWorkOrder(WO_NO_ROUTING);

        assertTrue(Boolean.TRUE.equals(r.get("skippedNoRouting")), "工艺路线缺失应整单跳过");
        assertEquals(0, ((Number) r.get("createdCount")).intValue());
        assertTrue(findOps(WO_NO_ROUTING).isEmpty(), "不应创建任何工序工单");
        assertEquals(1, countNotifications(ErpApsConstants.NOTIFY_EVENT_WORKORDER_NO_ROUTING),
                "应派发 aps.workorder-no-routing 告警");
    }

    // ---------------- 4. 工作中心不存在 → 该工序拒绝创建 + 告警（其余照建） ----------------

    @Test
    public void testWorkcenterMissingRejectsOperationAndNotifies() {
        seedNotifyTemplate(9402L, ErpApsConstants.NOTIFY_EVENT_OPERATION_WORKCENTER_MISSING);
        seedWorkOrder(WO_BAD_WC, "NOT_STARTED", ROUTING_1, "5");
        seedRoutingOp(ROUTING_1, 10, WC_1, "0", "1");
        seedRoutingOp(ROUTING_1, 20, 999999L, "0", "1"); // 不存在的工作中心

        Map<String, Object> r = createFromWorkOrder(WO_BAD_WC);

        assertEquals(1, ((Number) r.get("createdCount")).intValue(), "合法工序照常创建");
        List<Integer> rejected = (List<Integer>) r.get("rejectedSequences");
        assertEquals(List.of(20), rejected, "工作中心缺失工序被拒绝并返回 sequence");
        List<ErpApsOperationOrder> ops = findOps(WO_BAD_WC);
        assertEquals(1, ops.size());
        assertEquals(10, ops.get(0).getSequence().intValue());
        assertEquals(1, countNotifications(ErpApsConstants.NOTIFY_EVENT_OPERATION_WORKCENTER_MISSING),
                "应派发 aps.operation-workcenter-missing 告警");
    }

    // ---------------- 5. 幂等：重复触发不重复建单 ----------------

    @Test
    public void testIdempotentReTriggerCreatesNoDuplicates() {
        seedWorkOrder(WO_FULL, "NOT_STARTED", ROUTING_1, "10");
        seedRoutingWithTwoOps(ROUTING_1, WC_1, WC_2);

        createFromWorkOrder(WO_FULL);
        Map<String, Object> second = createFromWorkOrder(WO_FULL);

        assertTrue(Boolean.TRUE.equals(second.get("alreadyCreated")), "重复触发命中幂等守卫");
        assertEquals(0, ((Number) second.get("createdCount")).intValue());
        assertEquals(2, findOps(WO_FULL).size(), "仍只有 2 条工序工单，零重复");
    }

    // ---------------- 6. 扫描：仅已下达工单 + 跨轮幂等 ----------------

    @Test
    public void testScanReleasedOnlyAndIdempotentAcrossRuns() {
        seedWorkOrder(WO_FULL, "NOT_STARTED", ROUTING_1, "10");     // 已下达
        seedRoutingWithTwoOps(ROUTING_1, WC_1, WC_2);
        seedWorkOrder(WO_DRAFT, "SUBMITTED", ROUTING_2, "10");      // 未审核（未下达）
        seedRoutingOp(ROUTING_2, 10, WC_1, "0", "1");

        ApiResponse<?> first = rpc(mutation, "ErpApsOperationOrder__scanReleasedWorkOrders",
                ApiRequest.build(new LinkedHashMap<>()));
        assertEquals(0, first.getStatus(), "扫描应成功");
        assertEquals(2, ((Number) first.getData()).intValue(), "仅已下达工单的 2 道工序被创建");
        assertTrue(findOps(WO_DRAFT).isEmpty(), "未审核工单不应被扫描创建");

        ApiResponse<?> second = rpc(mutation, "ErpApsOperationOrder__scanReleasedWorkOrders",
                ApiRequest.build(new LinkedHashMap<>()));
        assertEquals(0, ((Number) second.getData()).intValue(), "第二轮扫描幂等零新建");
    }

    // ---------------- 7. CRP 负荷盲闭合（A4.2.178 场景翻转，Exit Criteria 佐证） ----------------

    @Test
    public void testScheduledOpsAppearInCrpLoadSource() {
        seedWorkOrder(WO_FULL, "NOT_STARTED", ROUTING_1, "10");
        seedRoutingWithTwoOps(ROUTING_1, WC_1, WC_2);
        createFromWorkOrder(WO_FULL);

        // 排产后（DRAFT→PLANNED + planned 时间），WorkOrder 出现在 CRP 负荷来源（此前 apsSlotsByWo.get 为 null 盲区）
        Long scheduleId = createSchedule("S-WO-CRP");
        for (ErpApsOperationOrder op : findOps(WO_FULL)) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id", op.getId());
            d.put("earliestStartDateT", "2026-07-10T08:00:00");
            rpcOk(mutation, "ErpApsOperationOrder__update", Map.of("data", d));
        }
        rpcOk(mutation, "ErpApsOperationOrder__scheduleForward", Map.of("scheduleId", scheduleId));

        List<ApsLoadSlot> slots = loadSourceProvider.findScheduledSlots(
                List.of(WO_FULL), LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 20));
        assertEquals(2, slots.size(), "已排程工序进入 CRP 负荷来源（负荷盲缺口闭合）");
    }

    // ==================== helpers ====================

    private Map<String, Object> createFromWorkOrder(Long workOrderId) {
        ApiResponse<?> r = rpc(mutation, "ErpApsOperationOrder__createOperationOrdersFromWorkOrder",
                ApiRequest.build(Map.of("workOrderId", workOrderId)));
        assertEquals(0, r.getStatus(), "createOperationOrdersFromWorkOrder 应成功: " + r);
        return (Map<String, Object>) r.getData();
    }

    private List<ErpApsOperationOrder> findOps(Long workOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        q.addOrderField("sequence", false);
        return daoProvider.daoFor(ErpApsOperationOrder.class).findAllByQuery(q);
    }

    private ErpApsOperationOrder opBySequence(List<ErpApsOperationOrder> ops, int sequence) {
        return ops.stream().filter(o -> o.getSequence() != null && o.getSequence() == sequence)
                .findFirst().orElseThrow();
    }

    private Long createSchedule(String code) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("name", code);
        d.put("scheduleDate", "2026-07-10");
        d.put("schedulingMode", "FORWARD");
        d.put("horizonStart", HORIZON_START.toString());
        d.put("horizonEnd", HORIZON_END.toString());
        d.put("status", "DRAFT");
        ApiResponse<?> r = rpc(mutation, "ErpApsSchedule__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus());
        Object id = ((Map<?, ?>) r.getData()).get("id");
        return Long.valueOf(String.valueOf(id));
    }

    private void seedWorkOrder(Long id, String docStatus, Long routingId, String qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode("WO-" + id);
            wo.setProductId(9901L);
            wo.setPlannedQuantity(new BigDecimal(qty));
            wo.setBusinessDate(LocalDate.of(2026, 7, 5));
            wo.setDocStatus(docStatus);
            wo.setRoutingId(routingId);
            dao.saveEntity(wo);
        });
    }

    private void seedRoutingWithTwoOps(Long routingId, Long wc1, Long wc2) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgRouting> dao = daoProvider.daoFor(ErpMfgRouting.class);
            if (dao.getEntityById(routingId) == null) {
                ErpMfgRouting r = new ErpMfgRouting();
                r.orm_propValueByName("id", routingId);
                r.setCode("RT-" + routingId);
                r.orm_propValueByName("isActive", Boolean.TRUE);
                dao.saveEntity(r);
            }
        });
        seedRoutingOp(routingId, 10, wc1, "5", "2");
        seedRoutingOp(routingId, 20, wc2, "15", "3");
    }

    private void seedRoutingOp(Long routingId, int lineNo, Long workcenterId, String setup, String run) {
        seedWorkcenterIfAbsent(workcenterId);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgRoutingOperation> dao = daoProvider.daoFor(ErpMfgRoutingOperation.class);
            ErpMfgRoutingOperation op = new ErpMfgRoutingOperation();
            op.orm_propValueByName("id", routingId * 1000 + lineNo);
            op.setRoutingId(routingId);
            op.setLineNo(lineNo);
            op.setOperationCode("OPC-" + lineNo);
            op.setOperationName(lineNo == 10 ? "下料" : "车削");
            op.setWorkcenterId(workcenterId);
            op.setSetupTime(new BigDecimal(setup));
            op.setRunTime(new BigDecimal(run));
            dao.saveEntity(op);
        });
    }

    private void seedWorkcenterIfAbsent(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenter> dao = daoProvider.daoFor(ErpMfgWorkcenter.class);
            if (dao.getEntityById(id) != null || id > 99999L) {
                return;
            }
            ErpMfgWorkcenter wc = new ErpMfgWorkcenter();
            wc.orm_propValueByName("id", id);
            wc.setCode("WC-" + id);
            wc.setName("工作中心" + id);
            dao.saveEntity(wc);
        });
    }

    private void seedNotifyTemplate(Long id, String eventType) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(eventType);
            t.setName("APS 通知 " + eventType);
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("APS 告警: " + eventType);
            t.setBodyTpl("workOrderCode=${workOrderCode}");
            t.setRecipientResolver("USER_LIST");
            t.setRecipientConfig("{\"userIds\":[\"" + RECIPIENT + "\"]}");
            t.setMergeWindowSeconds(300);
            t.setMergeStrategy("MERGE_BY_USER_TYPE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
        });
    }

    private int countNotifications(String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        List<ErpSysNotification> list = daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
        return list.size();
    }

    private ApiResponse<?> rpc(GraphQLOperationType op, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(GraphQLOperationType op, String action, Map<String, Object> args) {
        assertEquals(0, rpc(op, action, ApiRequest.build(args)).getStatus(), action + " 应成功");
    }
}
