package app.erp.mfg.service;

import app.erp.inv.dao.entity.ErpInvReservation;
import app.erp.inv.dao.entity.ErpInvReservationLine;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitBaseTestCase;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.48 mfg 侧物料预留写路径集成测试（Phase 4）：工单审核创建预留 → 领料消耗 → 取消/完工释放全链
 * + config 门控 + no-op 语义 + 无 BOM 不阻断。
 *
 * <p>覆盖 UC-MFG-05（①②③④）/ UC-MFG-08（⑤⑥⑦）/ UC-MFG-06（⑧⑨⑩⑪）的预留写路径运行时行为
 * （对齐 TestErpMfgWorkOrderEndToEnd 范式：IGraphQLEngine + 直断言）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgReservationLifecycle extends JunitBaseTestCase {

    static final Long ORG_ID = 1401L;
    static final Long WAREHOUSE_ID = 3401L;
    static final Long UOM_ID = 5401L;
    static final Long CURRENCY_ID = 6401L;
    static final Long P = 1101L;     // 产成品
    static final Long M1 = 1102L;    // 子件
    static final String MOVE_TYPE_INCOMING = "INCOMING";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- ① 审核创建预留（UC-MFG-05） ----------

    @Test
    public void testApproveCreatesReservation() {
        seedBase(9101L, "WO-RSV-APPROVE", "2");
        generateIncoming(M1, "PR-RSV-AP", bd("10"), bd("5"));

        Long woId = seedWorkOrder("WO-RSV-APPROVE", 9101L);
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));

        ErpInvReservation reservation = findReservation("WO-RSV-APPROVE");
        assertNotNull(reservation, "审核后应创建预留头");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_OPEN, reservation.getStatus(),
                "头状态=OPEN（生效中）");
        assertEquals("WORK_ORDER", reservation.getSourceBillType());
        List<ErpInvReservationLine> lines = findReservationLines(reservation.getId());
        assertEquals(1, lines.size(), "每个子件一条预留行");
        ErpInvReservationLine line = lines.get(0);
        assertEquals(M1, line.getMaterialId());
        // BOM qty=2 × planned=2 → 需求 4，可用 10 → 预留 4（min 语义）
        assertEquals(0, line.getReservedQuantity().compareTo(bd("4")), "预留量 = min(需求4, 可用10) = 4");
        assertEquals(0, line.getConsumedQuantity().compareTo(bd("0")));
        assertEquals(WAREHOUSE_ID, line.getWarehouseId(), "行仓库 = WO 行 sourceWarehouseId");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("4")), "库存余额.预留量 += 4");
        assertEquals(0, findBalance(M1).getAvailableQuantity().compareTo(bd("6")), "可用量 = 10 − 4 = 6");
    }

    @Test
    public void testApproveReservesMinOfAvailable() {
        seedBase(9102L, "WO-RSV-MIN", "2");
        generateIncoming(M1, "PR-RSV-MIN", bd("3"), bd("5"));

        Long woId = seedWorkOrder("WO-RSV-MIN", 9102L);
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));

        ErpInvReservation reservation = findReservation("WO-RSV-MIN");
        List<ErpInvReservationLine> lines = findReservationLines(reservation.getId());
        assertEquals(0, lines.get(0).getReservedQuantity().compareTo(bd("3")),
                "预留量 = min(需求4, 可用3) = 3");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("3")));
    }

    // ---------- ② 取消释放（UC-MFG-08） ----------

    @Test
    public void testCancelReleasesReservation() {
        seedBase(9103L, "WO-RSV-CANCEL", "2");
        generateIncoming(M1, "PR-RSV-CA", bd("10"), bd("5"));
        Long woId = seedWorkOrder("WO-RSV-CANCEL", 9103L);
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("4")), "审核后占用 4");

        rpcOk(mutation, "ErpMfgWorkOrder__cancel", Map.of("workOrderId", woId));

        ErpInvReservation reservation = findReservation("WO-RSV-CANCEL");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_CANCELLED, reservation.getStatus(),
                "取消释放 → 头状态=CANCELLED（⑦ D2 映射）");
        List<ErpInvReservationLine> lines = findReservationLines(reservation.getId());
        assertEquals(0, lines.get(0).getReservedQuantity().compareTo(bd("0")), "未领料全释放 → 行预留量归 0");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "库存余额.预留量 -= 4");
        assertEquals(0, findBalance(M1).getAvailableQuantity().compareTo(bd("10")), "可用量恢复 10");
    }

    // ---------- ③ 完工释放未领料部分（UC-MFG-08）+ ④ 领料消耗（UC-MFG-06） ----------

    @Test
    public void testIssueConfirmAndCompleteRelease() {
        seedBase(9104L, "WO-RSV-FLOW", "2");
        generateIncoming(M1, "PR-RSV-FL", bd("10"), bd("5"));
        Long woId = seedWorkOrder("WO-RSV-FLOW", 9104L);
        Long wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        // ④ 领料消耗：领 M1×3（预留 4 → 消耗 3，剩 1）
        Long issueId = seedIssue("MI-RSV-FLOW", woId);
        seedIssueLine(9301L, issueId, M1, bd("3"), wolId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));

        ErpInvReservation reservation = findReservation("WO-RSV-FLOW");
        List<ErpInvReservationLine> lines = findReservationLines(reservation.getId());
        assertEquals(0, lines.get(0).getConsumedQuantity().compareTo(bd("3")), "consumedQuantity += 3");
        assertEquals(0, lines.get(0).getReservedQuantity().compareTo(bd("4")), "行预留量保持初始 4");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_PARTIALLY_CONSUMED,
                reservation.getStatus(), "部分领料 → PARTIALLY_CONSUMED（⑦）");
        // 余额：10 − 4(领料出库) = 6；预留 4 − 3(消耗) = 1
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("1")), "库存余额.预留量 = 4 − 3 = 1");

        // ③ 完工达量（planned=2, completed=2）→ 释放未领料部分（1）
        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("2"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);

        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, wo.getDocStatus(), "完工达量 → COMPLETED");
        ErpInvReservation after = findReservation("WO-RSV-FLOW");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_PARTIALLY_CONSUMED, after.getStatus(),
                "完工释放剩余 → PARTIALLY_CONSUMED（⑦）");
        List<ErpInvReservationLine> afterLines = findReservationLines(after.getId());
        assertEquals(0, afterLines.get(0).getReservedQuantity().compareTo(bd("3")),
                "释放后行 reservedQuantity = consumedQuantity = 3（D2）");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "完工释放后余额预留量清零");
    }

    @Test
    public void testIssueFullConsumptionStatusConsumed() {
        seedBase(9105L, "WO-RSV-FULL", "2");
        generateIncoming(M1, "PR-RSV-FU", bd("10"), bd("5"));
        Long woId = seedWorkOrder("WO-RSV-FULL", 9105L);
        Long wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        Long issueId = seedIssue("MI-RSV-FULL", woId);
        seedIssueLine(9302L, issueId, M1, bd("4"), wolId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));

        ErpInvReservation reservation = findReservation("WO-RSV-FULL");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_CONSUMED, reservation.getStatus(),
                "领料领完 → CONSUMED（⑦）");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "余额预留量清零");
    }

    // ---------- ⑤ 超预留警告放行（D1） ----------

    @Test
    public void testOverPickWarnsAndPasses() {
        seedBase(9106L, "WO-RSV-OVER", "2");
        generateIncoming(M1, "PR-RSV-OV", bd("10"), bd("5"));
        Long woId = seedWorkOrder("WO-RSV-OVER", 9106L);
        Long wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        // 领 6 > 预留 4 → over-pick-warning=true LOG.warn 放行（不阻断领料主链）
        Long issueId = seedIssue("MI-RSV-OVER", woId);
        seedIssueLine(9303L, issueId, M1, bd("6"), wolId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId),
                "超预留 confirm 应放行（D1 warn 不阻断）");

        ErpInvReservation reservation = findReservation("WO-RSV-OVER");
        List<ErpInvReservationLine> lines = findReservationLines(reservation.getId());
        assertEquals(0, lines.get(0).getConsumedQuantity().compareTo(bd("4")),
                "超预留按 min 封顶：消耗 = 预留 4（非 6）");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_CONSUMED, reservation.getStatus());
        // 库存侧：现有量 10 − 6（出库） = 4；预留清零
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")));
        assertEquals(0, findBalance(M1).getTotalQuantity().compareTo(bd("4")), "领料主链扣减不受预留影响");
    }

    // ---------- ⑥ config 关闭全链跳过 ----------

    @Test
    public void testConfigOffSkipsReservationChain() {
        seedBase(9107L, "WO-RSV-OFF", "2");
        generateIncoming(M1, "PR-RSV-OF", bd("10"), bd("5"));
        Long woId = seedWorkOrder("WO-RSV-OFF", 9107L);
        Long wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        setConfig(ErpMfgConstants.CONFIG_RESERVATION_ENABLED, "false");
        try {
            rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
            rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
            assertNull(findReservation("WO-RSV-OFF"), "config 关闭 → 不创建预留");

            rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));
            Long issueId = seedIssue("MI-RSV-OFF", woId);
            seedIssueLine(9304L, issueId, M1, bd("4"), wolId);
            rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId),
                    "config 关闭 → 领料消耗跳过");
            Map<String, Object> completeReq = new LinkedHashMap<>();
            completeReq.put("workOrderId", woId);
            completeReq.put("completedQty", bd("2"));
            rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);
            ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
            assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, wo.getDocStatus(), "完工主链不受影响");
            assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "余额预留量恒 0");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_RESERVATION_ENABLED, "true");
        }
    }

    // ---------- ⑧ 无预留工单（旧数据）no-op 语义 + ⑨ 无 BOM approve 不阻断 ----------

    @Test
    public void testLegacyWorkOrderNoReservationNoOp() {
        // 工单行无 sourceWarehouseId → 审核跳过预留创建（MINOR-8），后续 cancel/confirm/complete 全部 no-op
        seedBase(9108L, "WO-RSV-LEGACY", "2");
        generateIncoming(M1, "PR-RSV-LE", bd("10"), bd("5"));
        Long woId = seedWorkOrder("WO-RSV-LEGACY", 9108L);
        Long wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        assertNull(findReservation("WO-RSV-LEGACY"), "无领料仓库行 → 不创建预留");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "余额零占用");

        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));
        Long issueId = seedIssue("MI-RSV-LEGACY", woId);
        seedIssueLine(9305L, issueId, M1, bd("4"), wolId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId),
                "无预留工单 confirm 不抛异常零写入");
        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("2"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, wo.getDocStatus(), "完工主链正常");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "全程零预留写入");

        // cancel 路径 no-op（新工单走 cancel；物料已 seed，仅补 BOM + WO + 余额）
        seedBom(9109L, P, M1, bd("2"));
        generateIncoming(M1, "PR-RSV-LE2", bd("10"), bd("5"));
        Long wo2 = seedWorkOrder("WO-RSV-LEGACY2", 9109L);
        seedWorkOrderLine(wo2, M1, bd("2"), "INPUT", null, null);
        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(wo2)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(wo2)));
        rpcOk(mutation, "ErpMfgWorkOrder__cancel", Map.of("workOrderId", wo2),
                "无预留工单 cancel 不抛异常");
    }

    @Test
    public void testNoBomApproveNotBlocked() {
        // 无 bomId 且无默认 BOM → approve 跳过预留创建不阻断（MINOR-5）
        seedMaterial(P, null);
        Long woId = 8300L + (long) Math.abs("WO-RSV-NOBOM".hashCode() % 700);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", woId);
            wo.setCode("WO-RSV-NOBOM");
            wo.setProductId(P);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(bd("1"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
            dao.saveEntity(wo);
        });

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)),
                "无 BOM 工单 approve 不阻断（跳过预留 LOG.warn）");
        assertNull(findReservation("WO-RSV-NOBOM"), "无 BOM → 不创建预留");
    }

    // ---------- ⑩ 跨工单并发预留探针（A4.2.3 MA4 回队义务，无条件新增） ----------

    /**
     * 跨工单并发预留 lost-update 防护运行时核验（A4.2.3）：两工单同物料经 mfg approve 集成层并发建预留
     * （镜像 {@code TestErpInvReservationWriteApi#testConcurrentCreateReservationNoLostUpdate}
     * ExecutorService + CountDownLatch 模式，但经 {@code ErpMfgWorkOrder__approve} 集成层
     * → {@code createReservations} → {@code IErpInvReservationBiz.createReservation}
     * → {@code StockMoveBookkeeper.updateBalanceWithRetry} 乐观锁重试）。
     *
     * <p>断言：两工单预留均落库 + reservedQuantity 累加无丢失（4 + 4 = 8）+ available = total − reserved
     * 恒等式保持（10 − 8 = 2）+ 无异常/无重试耗尽。
     */
    @Test
    public void testConcurrentCrossWorkOrderApproveNoLostUpdate() throws Exception {
        seedBase(9111L, "WO-RSV-CONC-A", "2");
        generateIncoming(M1, "PR-RSV-CONC", bd("10"), bd("5"));

        Long woA = seedWorkOrder("WO-RSV-CONC-A", 9111L);
        seedWorkOrderLine(woA, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woA, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);
        Long woB = seedWorkOrder("WO-RSV-CONC-B", 9111L);
        seedWorkOrderLine(woB, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woB, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                final Long woId = i == 0 ? woA : woB;
                pool.submit(() -> {
                    ContextProvider.newContext();
                    try {
                        startGate.await();
                        ApiResponse<?> submitResp = rpc(mutation, "ErpMfgWorkOrder__submitForApproval",
                                Map.of("id", String.valueOf(woId)));
                        ApiResponse<?> approveResp = rpc(mutation, "ErpMfgWorkOrder__approve",
                                Map.of("id", String.valueOf(woId)));
                        if (submitResp.getStatus() != 0) {
                            throw new AssertionError("工单 " + woId + " submitForApproval 失败: " + submitResp);
                        }
                        if (approveResp.getStatus() != 0) {
                            throw new AssertionError("工单 " + woId + " approve 失败: " + approveResp);
                        }
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                    } finally {
                        ContextProvider.instance().detachContext();
                        doneLatch.countDown();
                    }
                });
            }
            startGate.countDown();
            assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "全部 worker 应在 60s 内完成");
            if (firstError.get() != null) {
                throw new AssertionError("worker 线程抛错: " + firstError.get().getMessage(), firstError.get());
            }
        } finally {
            pool.shutdownNow();
        }

        // 两工单预留均落库（无丢失）
        ErpInvReservation resA = findReservation("WO-RSV-CONC-A");
        assertNotNull(resA, "工单 A 审核后应创建预留头");
        List<ErpInvReservationLine> linesA = findReservationLines(resA.getId());
        assertEquals(1, linesA.size(), "工单 A 每个子件一条预留行");
        assertEquals(0, linesA.get(0).getReservedQuantity().compareTo(bd("4")),
                "工单 A 预留量 = min(需求 2×2=4, 可用 10) = 4");

        ErpInvReservation resB = findReservation("WO-RSV-CONC-B");
        assertNotNull(resB, "工单 B 审核后应创建预留头");
        List<ErpInvReservationLine> linesB = findReservationLines(resB.getId());
        assertEquals(1, linesB.size(), "工单 B 每个子件一条预留行");
        assertEquals(0, linesB.get(0).getReservedQuantity().compareTo(bd("4")),
                "工单 B 预留量 = min(需求 2×2=4, 可用) = 4");

        // reservedQuantity 累加无丢失：4 + 4 = 8；available = total − reserved 恒等式保持
        ErpInvStockBalance balance = findBalance(M1);
        assertEquals(0, balance.getTotalQuantity().compareTo(bd("10")), "total 守恒 = 10");
        assertEquals(0, balance.getReservedQuantity().compareTo(bd("8")),
                "跨工单并发预留：4 + 4 = 8（无丢失更新，乐观锁重试串行化）");
        assertEquals(0, balance.getAvailableQuantity().compareTo(bd("2")),
                "available = total − reserved = 10 − 8 = 2");
    }

    // ---------- helpers ----------

    private void seedBase(Long bomId, String woCode, String bomQty) {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(bomId, P, M1, bd(bomQty));
    }

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("currencyId", CURRENCY_ID);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));
        rpcOk(mutation, "ErpInvStockMove__generateMove", Map.of("request", req));
    }

    private void seedMaterial(Long id, String costMethod) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            m.setCostMethod(costMethod);
            dao.saveEntity(m);
        });
    }

    private void seedBom(Long bomId, Long productId, Long componentId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", bomId);
            bom.setCode("BOM-" + bomId);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(Boolean.TRUE);
            bom.setIsActive(Boolean.TRUE);
            bom.setQty(bd("1"));
            dao.saveEntity(bom);
            IEntityDao<ErpMfgBomLine> ldao = daoProvider.daoFor(ErpMfgBomLine.class);
            ErpMfgBomLine line = new ErpMfgBomLine();
            line.orm_propValueByName("id", bomId + 50000);
            line.setBomId(bomId);
            line.setLineNo(10);
            line.setMaterialId(componentId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            ldao.saveEntity(line);
        });
    }

    private Long seedWorkOrder(String code, Long bomId) {
        Long id = 8300L + (long) Math.abs(code.hashCode() % 700);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(bd("2"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
            dao.saveEntity(wo);
        });
        return id;
    }

    private Long seedWorkOrderLine(Long woId, Long materialId, BigDecimal plannedQty, String lineType,
                                   Long destWarehouseId, Long sourceWarehouseId) {
        long raw = (woId + "" + materialId + lineType).hashCode();
        Long id = 9300L + (long) Math.abs(raw % 700);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
            ErpMfgWorkOrderLine wol = new ErpMfgWorkOrderLine();
            wol.orm_propValueByName("id", id);
            wol.setWorkOrderId(woId);
            wol.setLineNo(materialId.intValue());
            wol.orm_propValueByName("lineType", lineType);
            wol.setMaterialId(materialId);
            wol.setUoMId(UOM_ID);
            wol.setPlannedQuantity(plannedQty);
            wol.setDestWarehouseId(destWarehouseId);
            wol.setSourceWarehouseId(sourceWarehouseId);
            dao.saveEntity(wol);
        });
        return id;
    }

    private Long seedIssue(String code, Long woId) {
        Long id = 8400L + (long) Math.abs(code.hashCode() % 700);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMaterialIssue> dao = daoProvider.daoFor(ErpMfgMaterialIssue.class);
            ErpMfgMaterialIssue issue = new ErpMfgMaterialIssue();
            issue.orm_propValueByName("id", id);
            issue.setCode(code);
            issue.setWorkOrderId(woId);
            issue.setOrgId(ORG_ID);
            issue.setWarehouseId(WAREHOUSE_ID);
            issue.setBusinessDate(LocalDate.of(2026, 7, 1));
            issue.setCurrencyId(CURRENCY_ID);
            issue.setDocStatus(ErpMfgConstants.ISSUE_STATUS_DRAFT);
            issue.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(issue);
        });
        return id;
    }

    private void seedIssueLine(Long id, Long issueId, Long materialId, BigDecimal qty, Long wolId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMaterialIssueLine> dao = daoProvider.daoFor(ErpMfgMaterialIssueLine.class);
            ErpMfgMaterialIssueLine line = new ErpMfgMaterialIssueLine();
            line.orm_propValueByName("id", id);
            line.setIssueId(issueId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setRequiredQuantity(qty);
            line.setIssuedQuantity(qty);
            line.setWorkOrderLineId(wolId);
            dao.saveEntity(line);
        });
    }

    // ---------- query helpers ----------

    private ErpInvReservation findReservation(String workOrderCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillType", ErpMfgConstants.SOURCE_BILL_TYPE_WORK_ORDER));
        q.addFilter(eq("sourceBillCode", workOrderCode));
        List<ErpInvReservation> list = daoProvider.daoFor(ErpInvReservation.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpInvReservationLine> findReservationLines(Long reservationId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("reservationId", reservationId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpInvReservationLine.class).findAllByQuery(q);
    }

    private ErpInvStockBalance findBalance(Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ApiResponse<?> rpc(GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private void rpcOk(GraphQLOperationType op, String action, Map<String, Object> args, String msg) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), msg + ": " + resp);
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
