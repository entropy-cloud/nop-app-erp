package app.erp.aps.service;

import app.erp.aps.biz.IErpApsOperationOrderBiz;
import app.erp.aps.dao.entity.ErpApsDispatchLog;
import app.erp.aps.dao.entity.ErpApsDispatchRule;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.service.job.ErpApsAutoDispatchJob;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-APS-07 自动派工引擎行为测试（RC-R1.88 / P1-RC-090，auto-dispatch.md §二/三）。
 *
 * <p>派工/跳过/保持/强制四模式 + ON_HOLD 通知可观察；时间以 {@code CoreMetrics.currentDateTime()}
 * 相对偏移构造（eligible 窗口语义确定性）。mfg 工单/BOM + inv 库存经 daoProvider 直种
 * （D5 裁决选项 A 只读通道同构验证）；notify 模板 USER_LIST 种子镜像 mnt 范式。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsAutoDispatch extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpApsOperationOrderBiz operationOrderBiz;

    static final Long WC = 4001L;
    static final Long WO = 5001L;
    static final Long BOM = 5101L;
    static final Long MATERIAL_CHILD = 5201L;
    static final Long RULE_ID = 5301L;
    static final Long NOTIFY_TPL = 5401L;
    static final String RECIPIENT = "planner-1";

    @AfterEach
    public void resetConfig() {
        setConfig(ErpApsConfigs.CONFIG_AUTO_DISPATCH_ENABLED, "false");
        setConfig(ErpApsConfigs.CONFIG_AUTO_DISPATCH_CRON, "");
    }

    // ---------------- 1. 规则跳过：enableAuto=false / holdUntil 未到 / enabledHours 窗口外 ----------------

    @Test
    public void testRuleSkipsDisabledHoldUntilAndOutsideHours() {
        enableDispatch();
        seedRule(r -> r.orm_propValueByName("enableAuto", Boolean.FALSE));
        Long opDisabled = seedPlannedOp("AD-1A", inMinutes(5));
        scan();
        assertEquals("PLANNED", status(opDisabled), "enableAuto=false 工作中心不自动派工");

        seedRule(r -> {
            r.orm_propValueByName("enableAuto", Boolean.TRUE);
            r.setHoldUntil(java.sql.Timestamp.valueOf(inMinutes(30))); // 暂停到 30 分钟后
        });
        Long opHold = seedPlannedOp("AD-1B", inMinutes(5));
        scan();
        assertEquals("PLANNED", status(opHold), "holdUntil 未到应跳过");

        seedRule(r -> {
            r.orm_propValueByName("enableAuto", Boolean.TRUE);
            r.setEnabledHours("[{\"start\":\"01:00\",\"end\":\"02:00\"}]"); // 当前时刻必然窗外（宽 1h）
        });
        Long opHours = seedPlannedOp("AD-1C", inMinutes(5));
        scan();
        assertEquals("PLANNED", status(opHours), "enabledHours 窗口外应跳过");
    }

    // ---------------- 2. eligible 过滤：窗口/优先级阈值/maxConcurrentOps/HOLD 态排除 ----------------

    @Test
    public void testEligibleFiltersWindowPriorityConcurrencyAndHold() {
        enableDispatch();
        seedRule(null); // 默认 enableAuto=true, require 全关（隔离窗口过滤断言）

        Long tooEarly = seedPlannedOp("AD-2A", inMinutes(200)); // > lookahead 120
        Long tooLate = seedPlannedOp("AD-2B", inMinutes(-60));  // < -dispatchAhead 15
        scan();
        assertEquals("PLANNED", status(tooEarly), "超出前瞻窗口不派工");
        assertEquals("PLANNED", status(tooLate), "已过期工序不派工");

        seedRule(r -> r.setPriorityThreshold(20));
        Long lowPriority = seedPlannedOp("AD-2C", inMinutes(5), 80);
        scan();
        assertEquals("PLANNED", status(lowPriority), "优先级低于阈值不派工");

        // HOLD 态排除（status dict 值承载保持态）
        Long held = seedOp("AD-2D", inMinutes(5), "HOLD");
        scan();
        assertEquals("HOLD", status(held), "HOLD 保持态不被自动派工");

        // maxConcurrentOps 满额：已有 1 条 IN_PROGRESS，maxConcurrentOps=1 → 不再派
        seedRule(r -> r.orm_propValueByName("maxConcurrentOps", 1));
        seedOp("AD-2E-RUNNING", inMinutes(5), "IN_PROGRESS");
        Long queued = seedPlannedOp("AD-2F", inMinutes(6));
        scan();
        assertEquals("PLANNED", status(queued), "maxConcurrentOps 满额不派工");
    }

    // ---------------- 3. 三条件组合满足派工 + DispatchLog 完整 ----------------

    @Test
    public void testAllConditionsPassDispatchesWithFullLog() {
        enableDispatch();
        seedKittedWorkOrder("8"); // BOM 子件需求 5×8=40，库存 100 → 齐套
        seedRule(r -> {
            r.orm_propValueByName("requireMaterial", Boolean.TRUE);
            r.orm_propValueByName("requireOperator", Boolean.TRUE);
            r.orm_propValueByName("requireTooling", Boolean.TRUE);
        });
        Long op = seedPlannedOp("AD-3", inMinutes(5));
        seedOpWorkOrder(op, WO);

        Integer dispatched = scan();
        assertEquals(1, dispatched, "应派工 1 条");

        Map<String, Object> reloaded = reloadOp(op);
        assertEquals("IN_PROGRESS", reloaded.get("status"));
        assertNotNull(reloaded.get("realStartDateT"), "派工写实际开工时间");

        ErpApsDispatchLog log = latestLog(op);
        assertNotNull(log, "应记 DispatchLog");
        assertEquals("AUTO", log.getDispatchType());
        assertEquals("PLANNED", log.getPreviousStatus());
        assertEquals("IN_PROGRESS", log.getNewStatus());
        assertEquals("system", log.getDispatchedBy());
        assertEquals(WC, log.getWorkcenterId());
        // 物料=true（齐套）；操作工/工装 null（无排班/无载体降级放行）
        assertEquals(Boolean.TRUE, log.getMaterialAvailable());
        assertNull(log.getOperatorAvailable(), "无排班载体条件结果记 null");
        assertNull(log.getToolingAvailable(), "无工装载体条件结果记 null");
        assertNotNull(log.getConditionCheckResult());
        assertTrue(log.getConditionCheckResult().contains("\"material\":true"));
    }

    // ---------------- 4. 缺料 → ON_HOLD + 通知 ----------------

    @Test
    public void testMaterialShortageHoldsAndNotifies() {
        enableDispatch();
        seedNotifyTemplate(NOTIFY_TPL, ErpApsConstants.NOTIFY_EVENT_DISPATCH_MATERIAL_SHORTAGE);
        seedKittedWorkOrder("100"); // 需求 5×100=500，库存 100 → 缺料
        seedRule(r -> r.orm_propValueByName("requireMaterial", Boolean.TRUE));
        Long op = seedPlannedOp("AD-4", inMinutes(5));
        seedOpWorkOrder(op, WO);

        Integer dispatched = scan();
        assertEquals(0, dispatched, "缺料不派工");

        Map<String, Object> reloaded = reloadOp(op);
        assertEquals("ON_HOLD", reloaded.get("status"), "窗口内缺料应置 ON_HOLD");
        ErpApsDispatchLog log = latestLog(op);
        assertNotNull(log);
        assertEquals("HOLD", log.getDispatchType());
        assertEquals("PLANNED", log.getPreviousStatus());
        assertEquals("ON_HOLD", log.getNewStatus());
        assertEquals(Boolean.FALSE, log.getMaterialAvailable());
        assertTrue(log.getConditionCheckResult().contains("\"material\":false"));

        assertEquals(1, countNotifications(ErpApsConstants.NOTIFY_EVENT_DISPATCH_MATERIAL_SHORTAGE),
                "应通知计划员缺料暂停");
    }

    // ---------------- 5. 无 BOM 需求 → 物料维度 null 放行 ----------------

    @Test
    public void testNoBomRequirementPassesAsNull() {
        enableDispatch();
        // 工单无 BOM：物料维度条件结果 null 放行
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", 5502L);
            wo.setCode("WO-NOBOM");
            wo.setProductId(9901L);
            wo.setPlannedQuantity(new BigDecimal("10"));
            wo.setBusinessDate(CoreMetrics.today());
            wo.setDocStatus("NOT_STARTED");
            dao.saveEntity(wo);
        });
        seedRule(r -> r.orm_propValueByName("requireMaterial", Boolean.TRUE));
        Long op = seedPlannedOp("AD-5", inMinutes(5));
        seedOpWorkOrder(op, 5502L);

        assertEquals(1, scan());
        assertEquals("IN_PROGRESS", status(op), "无 BOM 需求（null 放行）应可派工");
        assertNull(latestLog(op).getMaterialAvailable(), "条件结果应记 null");
    }

    // ---------------- 6. 手动强制派工：跳过检查 + 原因必填 ----------------

    @Test
    public void testManualDispatchSkipsChecksAndRequiresNote() {
        seedRule(null);
        Long op = seedPlannedOp("AD-6", inMinutes(5));

        ApiResponse<?> missing = rpc(mutation, "ErpApsOperationOrder__dispatchManually",
                ApiRequest.build(Map.of("operationOrderId", op, "note", "")));
        assertTrue(missing.getStatus() != 0, "空跳检原因应拒绝（ERR_APS_DISPATCH_REASON_REQUIRED）");

        ApiResponse<?> ok = rpc(mutation, "ErpApsOperationOrder__dispatchManually",
                ApiRequest.build(Map.of("operationOrderId", op, "note", "紧急插单，物料在途")));
        assertEquals(0, ok.getStatus(), "手动强制派工应成功: " + ok);
        assertEquals("IN_PROGRESS", status(op));

        ErpApsDispatchLog log = latestLog(op);
        assertEquals("MANUAL", log.getDispatchType());
        assertEquals("紧急插单，物料在途", log.getNote(), "note 承载跳检原因");
        assertNull(log.getMaterialAvailable(), "跳检三维结果留空");
        assertTrue(log.getConditionCheckResult().contains("\"skipped\":true"));
    }

    // ---------------- 7. hold/unhold：status 迁移 + DispatchLog ----------------

    @Test
    public void testHoldAndUnholdTransitionsWithLogs() {
        Long op = seedPlannedOp("AD-7", inMinutes(5));

        ApiResponse<?> hold = rpc(mutation, "ErpApsOperationOrder__hold",
                ApiRequest.build(Map.of("operationOrderId", op)));
        assertEquals(0, hold.getStatus(), "hold 应成功");
        assertEquals("HOLD", status(op));
        assertEquals("HOLD", latestLog(op).getDispatchType());
        assertEquals("PLANNED", latestLog(op).getPreviousStatus());
        assertEquals("HOLD", latestLog(op).getNewStatus());

        ApiResponse<?> unhold = rpc(mutation, "ErpApsOperationOrder__unhold",
                ApiRequest.build(Map.of("operationOrderId", op)));
        assertEquals(0, unhold.getStatus(), "unhold 应成功");
        assertEquals("PLANNED", status(op), "解除保持回到 PLANNED 重入派工循环");
        assertEquals("UNHOLD", latestLog(op).getDispatchType());

        // ON_HOLD 也经 unhold 解除
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("id", op);
        d.put("status", "ON_HOLD");
        assertEquals(0, rpc(mutation, "ErpApsOperationOrder__update", ApiRequest.build(Map.of("data", d))).getStatus());
        ApiResponse<?> unhold2 = rpc(mutation, "ErpApsOperationOrder__unhold",
                ApiRequest.build(Map.of("operationOrderId", op)));
        assertEquals(0, unhold2.getStatus(), "ON_HOLD unhold 应成功");
        assertEquals("PLANNED", status(op));

        // 非法源态拒绝
        ApiResponse<?> badHold = rpc(mutation, "ErpApsOperationOrder__hold",
                ApiRequest.build(Map.of("operationOrderId", op)));
        assertEquals(0, badHold.getStatus());
        seedOp("AD-7B", inMinutes(5), "IN_PROGRESS");
        ApiResponse<?> holdInProgress = rpc(mutation, "ErpApsOperationOrder__hold",
                ApiRequest.build(Map.of("operationOrderId", idOf("AD-7B"))));
        assertTrue(holdInProgress.getStatus() != 0, "IN_PROGRESS hold 应拒绝");
    }

    // ---------------- 8. 并发派工冲突：二次派工拒绝（乐观锁 version + 状态守卫双保险） ----------------

    @Test
    public void testConcurrentDoubleDispatchRejected() {
        enableDispatch();
        seedRule(null);
        Long op = seedPlannedOp("AD-8", inMinutes(5));

        assertEquals(1, scan(), "首轮派工成功");
        assertEquals("IN_PROGRESS", status(op));

        // 第二次派工（状态守卫：PLANNED 源态校验失败 → 领域非法迁移码；与实体 version 乐观锁构成双保险）
        ApiResponse<?> manual = rpc(mutation, "ErpApsOperationOrder__dispatchManually",
                ApiRequest.build(Map.of("operationOrderId", op, "note", "dup")));
        assertTrue(manual.getStatus() != 0, "已派工工序二次派工应被拒绝");

        assertEquals(0, scan(), "重复扫描幂等零派工（已 IN_PROGRESS 非 eligible）");
        assertEquals(1, countLogs(op), "仅一条派工日志，零重复派工");
    }

    // ---------------- 9. 全局开关关闭 ----------------

    @Test
    public void testGlobalSwitchOffSkipsScan() {
        // 默认 false，不开启
        seedRule(null);
        Long op = seedPlannedOp("AD-9", inMinutes(5));
        assertEquals(0, scan());
        assertEquals("PLANNED", status(op), "全局开关关闭不派工");
    }

    // ---------------- 10. job cron 空值跳过 + job 全链路派工 ----------------

    @Test
    public void testJobCronEmptySkipsAndFullJobPathDispatches() {
        seedRule(null);
        Long op = seedPlannedOp("AD-10", inMinutes(5));

        ErpApsAutoDispatchJob job = new ErpApsAutoDispatchJob();
        job.setOperationOrderBiz(operationOrderBiz);

        // 全局开关开但 cron 空（默认）→ 跳过
        setConfig(ErpApsConfigs.CONFIG_AUTO_DISPATCH_ENABLED, "true");
        job.execute();
        assertEquals("PLANNED", status(op), "cron 空值 job 应跳过");

        // cron 非空 → job 全链路派工
        setConfig(ErpApsConfigs.CONFIG_AUTO_DISPATCH_CRON, "0 * * * * ?");
        job.setOrmTemplate(ormTemplate);
        job.execute();
        assertEquals("IN_PROGRESS", status(op), "job 链路应完成派工");
    }

    // ==================== helpers ====================

    private void enableDispatch() {
        setConfig(ErpApsConfigs.CONFIG_AUTO_DISPATCH_ENABLED, "true");
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private static LocalDateTime inMinutes(long offset) {
        return CoreMetrics.currentDateTime().plusMinutes(offset);
    }

    private Integer scan() {
        ApiResponse<?> r = rpc(mutation, "ErpApsOperationOrder__scanAutoDispatch",
                ApiRequest.build(new LinkedHashMap<>()));
        assertEquals(0, r.getStatus(), "scanAutoDispatch 应成功: " + r);
        return (Integer) r.getData();
    }

    private void seedRule(java.util.function.Consumer<ErpApsDispatchRule> customizer) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpApsDispatchRule> dao = daoProvider.daoFor(ErpApsDispatchRule.class);
            ErpApsDispatchRule r = dao.getEntityById(RULE_ID);
            boolean isNew = r == null;
            if (isNew) {
                r = dao.newEntity();
                r.orm_propValueByName("id", RULE_ID);
                r.setWorkcenterId(WC);
                r.setRuleName("rule-test");
            }
            // 可变字段复位默认（同测试多次 seedRule 覆盖语义）
            r.orm_propValueByName("enableAuto", Boolean.TRUE);
            r.orm_propValueByName("requireMaterial", Boolean.FALSE);
            r.orm_propValueByName("requireOperator", Boolean.FALSE);
            r.orm_propValueByName("requireTooling", Boolean.FALSE);
            r.setHoldUntil(null);
            r.setEnabledHours(null);
            r.setPriorityThreshold(null);
            r.setMaxConcurrentOps(null);
            if (customizer != null) {
                customizer.accept(r);
            }
            if (isNew) {
                dao.saveEntity(r);
            } else {
                dao.updateEntity(r);
            }
        });
    }

    private Long seedPlannedOp(String code, LocalDateTime plannedStart) {
        return seedPlannedOp(code, plannedStart, 50);
    }

    private Long seedPlannedOp(String code, LocalDateTime plannedStart, int priority) {
        return seedOp(code, plannedStart, "PLANNED", priority);
    }

    private Long seedOp(String code, LocalDateTime plannedStart, String status) {
        return seedOp(code, plannedStart, status, 50);
    }

    private Long seedOp(String code, LocalDateTime plannedStart, String status, int priority) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpApsOperationOrder> dao = daoProvider.daoFor(ErpApsOperationOrder.class);
            ErpApsOperationOrder op = dao.newEntity();
            op.setCode(code);
            op.setWorkOrderId(6601L);
            op.setOperationName(code);
            op.setSequence(10);
            op.setMachineId(WC);
            op.setPriority(priority);
            op.setSetupTime(BigDecimal.TEN);
            op.setRuntimePerUnit(BigDecimal.ONE);
            op.setQty(BigDecimal.TEN);
            op.setStatus(status);
            op.setBusinessDate(CoreMetrics.today());
            op.setPlannedStartDateT(java.sql.Timestamp.valueOf(plannedStart));
            op.setPlannedEndDateT(java.sql.Timestamp.valueOf(plannedStart.plusMinutes(20)));
            dao.saveEntity(op);
        });
        return idOf(code);
    }

    private void seedOpWorkOrder(Long opId, Long workOrderId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpApsOperationOrder> dao = daoProvider.daoFor(ErpApsOperationOrder.class);
            ErpApsOperationOrder op = dao.getEntityById(opId);
            op.setWorkOrderId(workOrderId);
            dao.updateEntity(op);
        });
    }

    /** 工单（qty）+ 默认 BOM（子件 unitQty=5）+ 子件库存 100：需求 5×qty vs 库存 100。 */
    private void seedKittedWorkOrder(String qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> woDao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", WO);
            wo.setCode("WO-KIT");
            wo.setProductId(9901L);
            wo.setPlannedQuantity(new BigDecimal(qty));
            wo.setBusinessDate(CoreMetrics.today());
            wo.setDocStatus("NOT_STARTED");
            wo.setBomId(BOM);
            woDao.saveEntity(wo);

            IEntityDao<ErpMfgBom> bomDao = daoProvider.daoFor(ErpMfgBom.class);
            if (bomDao.getEntityById(BOM) == null) {
                ErpMfgBom bom = new ErpMfgBom();
                bom.orm_propValueByName("id", BOM);
                bom.setCode("BOM-KIT");
                bom.setProductId(9901L);
                bom.orm_propValueByName("bomType", "NORMAL");
                bom.orm_propValueByName("isDefault", Boolean.TRUE);
                bom.orm_propValueByName("isActive", Boolean.TRUE);
                bomDao.saveEntity(bom);

                IEntityDao<ErpMfgBomLine> lineDao = daoProvider.daoFor(ErpMfgBomLine.class);
                ErpMfgBomLine line = new ErpMfgBomLine();
                line.orm_propValueByName("id", BOM * 100 + 1);
                line.setBomId(BOM);
                line.setLineNo(10);
                line.setMaterialId(MATERIAL_CHILD);
                line.setUoMId(1L);
                line.setQuantity(new BigDecimal("5"));
                lineDao.saveEntity(line);
            }

            IEntityDao<ErpInvStockBalance> balDao = daoProvider.daoFor(ErpInvStockBalance.class);
            if (balDao.getEntityById(7001L) == null) {
                ErpInvStockBalance bal = balDao.newEntity();
                bal.orm_propValueByName("id", 7001L);
                bal.setMaterialId(MATERIAL_CHILD);
                bal.setWarehouseId(1L);
                bal.setTotalQuantity(new BigDecimal("100"));
                bal.setAvailableQuantity(new BigDecimal("100"));
                balDao.saveEntity(bal);
            }
        });
    }

    private void seedNotifyTemplate(Long id, String eventType) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(eventType);
            t.setName("APS 派工通知");
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("缺料暂停: ${operationOrderCode}");
            t.setBodyTpl("工序 ${operationOrderCode} 缺料暂停");
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
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q).size();
    }

    private Long idOf(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpApsOperationOrder> found = daoProvider.daoFor(ErpApsOperationOrder.class).findAllByQuery(q);
        assertEquals(1, found.size(), "op " + code + " 应唯一");
        return found.get(0).getId();
    }

    private String status(Long opId) {
        return String.valueOf(reloadOp(opId).get("status"));
    }

    private Map<String, Object> reloadOp(Long id) {
        ApiResponse<?> r = rpc(GraphQLOperationType.query, "ErpApsOperationOrder__get",
                ApiRequest.build(Map.of("id", String.valueOf(id))));
        assertEquals(0, r.getStatus());
        return (Map<String, Object>) r.getData();
    }

    private ErpApsDispatchLog latestLog(Long opId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("operationOrderId", opId));
        q.addOrderField("id", true); // true=降序，取最新
        List<ErpApsDispatchLog> logs = daoProvider.daoFor(ErpApsDispatchLog.class).findAllByQuery(q);
        return logs.isEmpty() ? null : logs.get(0);
    }

    private int countLogs(Long opId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("operationOrderId", opId));
        return daoProvider.daoFor(ErpApsDispatchLog.class).findAllByQuery(q).size();
    }

    private ApiResponse<?> rpc(GraphQLOperationType op, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
