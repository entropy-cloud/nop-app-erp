package app.erp.inv.service;

import app.erp.inv.biz.IErpInvReservationBiz;
import app.erp.inv.biz.ReservationConsumeLine;
import app.erp.inv.biz.ReservationConsumeRequest;
import app.erp.inv.biz.ReservationCreateRequest;
import app.erp.inv.biz.ReservationLineRequest;
import app.erp.inv.dao.ErpInvDaoConstants;
import app.erp.inv.dao.entity.ErpInvReservation;
import app.erp.inv.dao.entity.ErpInvReservationLine;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
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
import java.util.ArrayList;
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
 * RC-R1.48 inventory 侧预留写接口定向测试（Phase 2/4）：
 * createReservation / releaseReservation / consumeReservation 三写方法 + 余额断言 + min 语义 +
 * 乐观锁并发 lost-update 防护（A4.2.3 SP-3 运行时义务）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpInvReservation__*}（对齐 TestErpInvStockMoveBizModel 范式）；
 * 并发场景直接注入 {@link IErpInvReservationBiz}（对齐 TestErpInvConcurrentDeduct 多线程框架）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvReservationWriteApi extends JunitBaseTestCase {

    static final Long ORG_ID = 10101L;
    static final Long MATERIAL_ID = 12002L;
    static final Long WAREHOUSE_ID = 13002L;
    static final Long UOM_ID = 15002L;
    static final Long CURRENCY_ID = 16002L;
    static final String SOURCE_BILL_TYPE = "WORK_ORDER";
    static final String SOURCE_BILL_CODE = "WO-RSV-001";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpInvReservationBiz reservationBiz;

    // ---------- 创建（UC-MFG-05 ①②③） ----------

    @Test
    public void testCreateReservationReservesBalance() {
        generateIncoming(new BigDecimal("10"));
        ApiResponse<?> resp = rpcCreate(new BigDecimal("5"));
        assertEquals(0, resp.getStatus(), "createReservation 应成功: " + resp);

        ErpInvReservation header = findHeader();
        assertNotNull(header, "预留头应落库");
        assertEquals(ErpInvDaoConstants.RESERVATION_STATUS_OPEN, header.getStatus(), "头状态=OPEN");
        assertEquals(SOURCE_BILL_TYPE, header.getSourceBillType());
        assertEquals(SOURCE_BILL_CODE, header.getSourceBillCode());
        List<ErpInvReservationLine> lines = findLines(header.getId());
        assertEquals(1, lines.size(), "每个子件一条预留行");
        ErpInvReservationLine line = lines.get(0);
        assertEquals(0, line.getReservedQuantity().compareTo(new BigDecimal("5")), "预留量 = min(需求5, 可用10) = 5");
        assertEquals(0, line.getConsumedQuantity().compareTo(BigDecimal.ZERO), "初始未消耗");

        ErpInvStockBalance balance = findBalance();
        assertEquals(0, balance.getReservedQuantity().compareTo(new BigDecimal("5")), "库存余额.预留量 += 5");
        assertEquals(0, balance.getAvailableQuantity().compareTo(new BigDecimal("5")), "可用量 = 10 − 5");
    }

    @Test
    public void testCreateReservationMinSemantics() {
        generateIncoming(new BigDecimal("3"));
        ApiResponse<?> resp = rpcCreate(new BigDecimal("5"));
        assertEquals(0, resp.getStatus(), "createReservation 应成功: " + resp);

        ErpInvReservation header = findHeader();
        List<ErpInvReservationLine> lines = findLines(header.getId());
        assertEquals(0, lines.get(0).getReservedQuantity().compareTo(new BigDecimal("3")),
                "预留量 = min(需求5, 可用3) = 3（预留量=min(需求,可用)）");
        assertEquals(0, findBalance().getReservedQuantity().compareTo(new BigDecimal("3")));
        assertEquals(0, findBalance().getAvailableQuantity().compareTo(BigDecimal.ZERO), "可用量 = 3 − 3 = 0");
    }

    @Test
    public void testCreateReservationIdempotent() {
        generateIncoming(new BigDecimal("10"));
        rpcCreate(new BigDecimal("5"));
        ApiResponse<?> second = rpcCreate(new BigDecimal("5"));
        assertEquals(0, second.getStatus(), "重复创建应幂等成功: " + second);

        assertEquals(1, countHeaders(), "同源单不重复建头");
        assertEquals(0, findBalance().getReservedQuantity().compareTo(new BigDecimal("5")), "不重复占用余额");
    }

    // ---------- 释放（UC-MFG-08 ⑤⑥⑦） ----------

    @Test
    public void testReleaseCancelledReleasesAll() {
        generateIncoming(new BigDecimal("10"));
        rpcCreate(new BigDecimal("5"));

        ApiResponse<?> resp = rpcRelease("CANCELLED");
        assertEquals(0, resp.getStatus(), "releaseReservation 应成功: " + resp);

        ErpInvReservation header = findHeader();
        assertEquals(ErpInvDaoConstants.RESERVATION_STATUS_CANCELLED, header.getStatus(),
                "取消释放 → 头状态=CANCELLED（D2 映射）");
        List<ErpInvReservationLine> lines = findLines(header.getId());
        assertEquals(0, lines.get(0).getReservedQuantity().compareTo(BigDecimal.ZERO), "未领料全释放 → 行预留量归 0");
        assertEquals(0, findBalance().getReservedQuantity().compareTo(BigDecimal.ZERO), "库存余额.预留量 -= 5");
        assertEquals(0, findBalance().getAvailableQuantity().compareTo(new BigDecimal("10")), "可用量恢复 10");
    }

    @Test
    public void testReleaseCompletedAfterPartialConsume() {
        generateIncoming(new BigDecimal("10"));
        rpcCreate(new BigDecimal("10"));
        rpcConsume(new BigDecimal("4"));

        ApiResponse<?> resp = rpcRelease("COMPLETED");
        assertEquals(0, resp.getStatus(), "完工释放应成功: " + resp);

        ErpInvReservation header = findHeader();
        assertEquals(ErpInvDaoConstants.RESERVATION_STATUS_PARTIALLY_CONSUMED, header.getStatus(),
                "部分领料 + 释放剩余 → PARTIALLY_CONSUMED");
        List<ErpInvReservationLine> lines = findLines(header.getId());
        assertEquals(0, lines.get(0).getConsumedQuantity().compareTo(new BigDecimal("4")), "consumedQuantity 保留");
        assertEquals(0, lines.get(0).getReservedQuantity().compareTo(new BigDecimal("4")),
                "未领料部分（6）释放 → 行预留量 = 已消耗量 4");
        assertEquals(0, findBalance().getReservedQuantity().compareTo(BigDecimal.ZERO), "余额预留量清零");
    }

    @Test
    public void testReleaseCompletedFullyConsumed() {
        generateIncoming(new BigDecimal("10"));
        rpcCreate(new BigDecimal("5"));
        rpcConsume(new BigDecimal("5"));

        ApiResponse<?> resp = rpcRelease("COMPLETED");
        assertEquals(0, resp.getStatus(), "完工释放应成功: " + resp);
        assertEquals(ErpInvDaoConstants.RESERVATION_STATUS_CONSUMED, findHeader().getStatus(),
                "已全领 → 头状态=CONSUMED");
    }

    // ---------- 消耗（UC-MFG-06 ⑬⑭⑯） ----------

    @Test
    public void testConsumeTracksAndDecrements() {
        generateIncoming(new BigDecimal("10"));
        rpcCreate(new BigDecimal("10"));

        rpcConsume(new BigDecimal("4"));
        ErpInvReservation header = findHeader();
        assertEquals(ErpInvDaoConstants.RESERVATION_STATUS_PARTIALLY_CONSUMED, header.getStatus(),
                "部分消耗 → PARTIALLY_CONSUMED");
        List<ErpInvReservationLine> lines = findLines(header.getId());
        assertEquals(0, lines.get(0).getConsumedQuantity().compareTo(new BigDecimal("4")), "consumedQuantity += 4");
        assertEquals(0, lines.get(0).getReservedQuantity().compareTo(new BigDecimal("10")),
                "行 reservedQuantity 保持初始预留量（未消耗 = 10 − 4 = 6，L1 ⑧语义载体）");
        assertEquals(0, findBalance().getReservedQuantity().compareTo(new BigDecimal("6")), "库存余额.预留量 -= 4");

        rpcConsume(new BigDecimal("6"));
        assertEquals(ErpInvDaoConstants.RESERVATION_STATUS_CONSUMED, findHeader().getStatus(), "领完 → CONSUMED");
        assertEquals(0, findBalance().getReservedQuantity().compareTo(BigDecimal.ZERO), "余额预留量清零");
    }

    @Test
    public void testConsumeCapsAtRemaining() {
        generateIncoming(new BigDecimal("10"));
        rpcCreate(new BigDecimal("5"));

        rpcConsume(new BigDecimal("8"));
        ErpInvReservation header = findHeader();
        List<ErpInvReservationLine> lines = findLines(header.getId());
        assertEquals(0, lines.get(0).getConsumedQuantity().compareTo(new BigDecimal("5")),
                "超预留按 min 封顶：只消耗剩余 5");
        assertEquals(0, lines.get(0).getReservedQuantity().compareTo(new BigDecimal("5")),
                "行预留量保持初始 5（已全消耗）");
        assertEquals(ErpInvDaoConstants.RESERVATION_STATUS_CONSUMED, header.getStatus(), "全部消耗 → CONSUMED");
        assertEquals(0, findBalance().getReservedQuantity().compareTo(BigDecimal.ZERO));
    }

    // ---------- no-op 语义（MINOR-4） ----------

    @Test
    public void testNoOpWhenNoReservation() {
        generateIncoming(new BigDecimal("10"));

        ApiResponse<?> releaseResp = rpcRelease("CANCELLED");
        assertEquals(0, releaseResp.getStatus(), "查无预留 release 应静默成功: " + releaseResp);
        ApiResponse<?> consumeResp = rpcConsume(new BigDecimal("3"));
        assertEquals(0, consumeResp.getStatus(), "查无预留 consume 应静默成功: " + consumeResp);
        assertEquals(0, findBalance().getReservedQuantity().compareTo(BigDecimal.ZERO), "零写入");
    }

    // ---------- 并发 lost-update 防护（A4.2.3 SP-3） ----------

    /**
     * 多线程并发 createReservation 同一余额（available=10，每线程申请 4）：
     * 双写冲突经 {@code updateBalanceWithRetry} 乐观锁重试 → 无丢失更新，最终 reserved = 8。
     */
    @Test
    public void testConcurrentCreateReservationNoLostUpdate() throws Exception {
        generateIncoming(new BigDecimal("10"));
        int threadCount = 2;
        BigDecimal perQty = new BigDecimal("4");
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                pool.submit(() -> {
                    ContextProvider.newContext();
                    try {
                        startGate.await();
                        ormTemplate.runInSession(workerSession -> {
                            ReservationCreateRequest req = new ReservationCreateRequest();
                            req.setOrgId(ORG_ID);
                            req.setSourceBillType(SOURCE_BILL_TYPE);
                            req.setSourceBillCode(SOURCE_BILL_CODE + "-CONC-" + idx);
                            req.setLines(Collections.singletonList(lineReq(MATERIAL_ID, perQty)));
                            reservationBiz.createReservation(req, new io.nop.core.context.ServiceContextImpl());
                            return null;
                        });
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

        ormTemplate.runInSession(checkSession -> {
            ErpInvStockBalance finalBalance = findBalance();
            assertEquals(0, finalBalance.getReservedQuantity().compareTo(new BigDecimal("8")),
                    "并发双写预留：4 + 4 = 8（无丢失更新）");
            assertEquals(0, finalBalance.getAvailableQuantity().compareTo(new BigDecimal("2")),
                    "可用量 = 10 − 8 = 2");
            return null;
        });
    }

    // ---------- helpers ----------

    private void generateIncoming(BigDecimal qty) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("acctSchemaId", 7001L);
        req.put("currencyId", CURRENCY_ID);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", "PR-RSV-" + System.nanoTime());
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", MATERIAL_ID);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", new BigDecimal("5"));
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));
        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
        assertEquals(0, resp.getStatus(), "generateMove 应成功: " + resp);
    }

    private ApiResponse<?> rpcCreate(BigDecimal requested) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", MATERIAL_ID);
        line.put("warehouseId", WAREHOUSE_ID);
        line.put("requestedQuantity", requested);
        line.put("uomId", UOM_ID);
        line.put("sourceLineCode", "10");
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("sourceBillType", SOURCE_BILL_TYPE);
        req.put("sourceBillCode", SOURCE_BILL_CODE);
        req.put("lines", Collections.singletonList(line));
        return executeRpc(mutation, "ErpInvReservation__createReservation", ApiRequest.build(Map.of("request", req)));
    }

    private ApiResponse<?> rpcRelease(String reason) {
        return executeRpc(mutation, "ErpInvReservation__releaseReservation",
                ApiRequest.build(Map.of("sourceBillType", SOURCE_BILL_TYPE, "sourceBillCode", SOURCE_BILL_CODE, "reason", reason)));
    }

    private ApiResponse<?> rpcConsume(BigDecimal qty) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", MATERIAL_ID);
        line.put("warehouseId", WAREHOUSE_ID);
        line.put("quantity", qty);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("sourceBillType", SOURCE_BILL_TYPE);
        req.put("sourceBillCode", SOURCE_BILL_CODE);
        req.put("lines", Collections.singletonList(line));
        return executeRpc(mutation, "ErpInvReservation__consumeReservation", ApiRequest.build(Map.of("request", req)));
    }

    private ReservationLineRequest lineReq(Long materialId, BigDecimal qty) {
        ReservationLineRequest line = new ReservationLineRequest();
        line.setMaterialId(materialId);
        line.setWarehouseId(WAREHOUSE_ID);
        line.setRequestedQuantity(qty);
        line.setUomId(UOM_ID);
        return line;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpInvReservation findHeader() {
        IEntityDao<ErpInvReservation> dao = daoProvider.daoFor(ErpInvReservation.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillType", SOURCE_BILL_TYPE));
        q.addFilter(eq("sourceBillCode", SOURCE_BILL_CODE));
        List<ErpInvReservation> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private long countHeaders() {
        IEntityDao<ErpInvReservation> dao = daoProvider.daoFor(ErpInvReservation.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillType", SOURCE_BILL_TYPE));
        q.addFilter(eq("sourceBillCode", SOURCE_BILL_CODE));
        return dao.findAllByQuery(q).size();
    }

    private List<ErpInvReservationLine> findLines(Long reservationId) {
        IEntityDao<ErpInvReservationLine> dao = daoProvider.daoFor(ErpInvReservationLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("reservationId", reservationId));
        q.addOrderField("lineNo", false);
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
