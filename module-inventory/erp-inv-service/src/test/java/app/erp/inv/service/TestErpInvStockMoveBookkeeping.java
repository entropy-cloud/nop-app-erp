package app.erp.inv.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
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
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：不可变流水 + 余额驱动（移动加权平均）+ 可用量校验 + 负库存配置。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpInvStockMove__generateMove/complete}，引擎负责建 session/事务/管道。
 * 余额与流水断言用 DAO 直查（测试 session 内合法）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvStockMoveBookkeeping extends JunitAutoTestCase {

    static final Long ORG_ID = 1001L;
    static final Long MATERIAL_ID = 2002L;
    static final Long WAREHOUSE_ID = 3002L;
    static final Long LOCATION_ID = 4002L;
    static final Long UOM_ID = 5002L;
    static final Long CURRENCY_ID = 6002L;
    static final Long ACCT_SCHEMA_ID = 7002L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCompleteWritesImmutableLedger() {
        Long moveId = generateIncoming("PR-LEDG-001", new BigDecimal("10"), new BigDecimal("5"));

        List<ErpInvStockLedger> ledgers = findLedgers(moveId);
        assertEquals(1, ledgers.size(), "应写 1 条不可变流水");
        ErpInvStockLedger ledger = ledgers.get(0);
        assertEquals(0, ledger.getQuantity().compareTo(new BigDecimal("10")), "入库 quantity 为正 10");
        assertEquals(0, ledger.getUnitCost().compareTo(new BigDecimal("5")), "单位成本 5");
        assertEquals(0, ledger.getTotalCost().compareTo(new BigDecimal("50")), "总成本 50");
        assertEquals(0, ledger.getBalanceQuantity().compareTo(new BigDecimal("10")),
                "结存快照 balanceQuantity=10");
        assertEquals(0, ledger.getBalanceTotalCost().compareTo(new BigDecimal("50")),
                "结存快照 balanceTotalCost=50");
        assertNotEquals(null, ledger.getCode(), "流水号非空");

        ApiResponse<?> completeResp = executeRpc(mutation, "ErpInvStockMove__complete",
                ApiRequest.build(Map.of("moveId", moveId)));
        assertEquals(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), completeResp.getCode(),
                "DONE 后再 complete 应返回非法迁移错误（经 GraphQL，不抛异常）");
        assertEquals(1, findLedgers(moveId).size(), "DONE 后不可再写流水");
        output("1_complete_rejection.json5", completeResp.getCode());
    }

    @Test
    public void testIncomingUpdatesBalanceAvgCost() {
        generateIncoming("PR-AVG-001", new BigDecimal("10"), new BigDecimal("6"));
        ErpInvStockBalance balance = findBalance();
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("10")), "totalQty=10");
        assertEquals(0, balance.getAvgCost().compareTo(new BigDecimal("6")), "avgCost=6");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("60")), "totalCost=60");

        generateIncoming("PR-AVG-002", new BigDecimal("10"), new BigDecimal("8"));
        ErpInvStockBalance updated = findBalance();
        assertEquals(0, updated.getTotalQuantity().compareTo(new BigDecimal("20")), "totalQty=20");
        assertEquals(0, updated.getAvgCost().compareTo(new BigDecimal("7")), "avgCost=(60+80)/20=7");
        assertEquals(0, updated.getTotalCost().compareTo(new BigDecimal("140")), "totalCost=140");
        output("1_balance_state.json5", balanceState(updated));
    }

    @Test
    public void testOutgoingDeductsBalance() {
        generateIncoming("PR-OUT-001", new BigDecimal("20"), new BigDecimal("10"));
        generateOutgoing("SS-OUT-001", new BigDecimal("8"));

        ErpInvStockBalance balance = findBalance();
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("12")), "totalQty=20-8=12");
        assertEquals(0, balance.getAvgCost().compareTo(new BigDecimal("10")), "avgCost 不变 10");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("120")), "totalCost=12*10=120");

        List<ErpInvStockLedger> outLedgers = daoProvider.daoFor(ErpInvStockLedger.class).findAllByQuery(
                new QueryBean());
        ErpInvStockLedger outLedger = outLedgers.stream()
                .filter(l -> l.getQuantity().signum() < 0).findFirst().orElseThrow();
        assertEquals(0, outLedger.getUnitCost().compareTo(new BigDecimal("10")),
                "出库流水 unitCost 快照=当前 avgCost 10");
        assertEquals(0, outLedger.getQuantity().compareTo(new BigDecimal("-8")), "出库 quantity 负 -8");
        output("1_balance_state.json5", balanceState(findBalance()));
    }

    @Test
    public void testConfirmInsufficientAvailableRejected() {
        ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-INSUF-001", new BigDecimal("5")));
        assertEquals(ErpInvErrors.ERR_AVAILABLE_INSUFFICIENT.getErrorCode(), resp.getCode(),
                "可用量不足应返回 ERR_AVAILABLE_INSUFFICIENT");
        output("1_rejection_code.json5", resp.getCode());

        ErpInvStockMove move = findMove("SALES_SHIP", "SS-INSUF-001");
        assertNull(move, "可用量不足整笔回滚，不应残留移动单");

        ErpInvStockBalance balance = findBalance();
        assertTrue(balance == null
                        || balance.getReservedQuantity().compareTo(BigDecimal.ZERO) == 0,
                "拒绝不应增加预留量");
    }

    @Test
    public void testNegativeStockConfigAllowsShortage() {
        setNegativeStock(true);
        try {
            Long moveId = generateOutgoing("SS-NEG-001", new BigDecimal("5"));
            ErpInvStockMove move = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(moveId);
            assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "负库存放行应完成");

            ErpInvStockBalance balance = findBalance();
            assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("-5")),
                    "totalQty 允许为负 -5");
            output("1_balance_state.json5", balanceState(balance));
        } finally {
            setNegativeStock(false);
        }
    }

    // ---------- UC-INV-09 三量交互验证（plan 2026-07-07-0024-2 Phase 2） ----------

    /**
     * UC-INV-09 (1/4)：放行后 available 变负。
     * 配置 erp-inv.allow-negative-stock=true，无初始库存出库 5 → total 与 available 同步变 -5。
     */
    @Test
    public void testNegativeStockAvailableGoesNegative() {
        setNegativeStock(true);
        try {
            generateOutgoing("SS-NEG-AVAIL-001", new BigDecimal("5"));
            ErpInvStockBalance balance = findBalance();
            assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("-5")),
                    "totalQuantity 允许为负 -5");
            assertEquals(0, balance.getAvailableQuantity().compareTo(new BigDecimal("-5")),
                    "availableQuantity 同步为负 -5（reserved/locked 均为 0）");
            assertEquals(0, balance.getReservedQuantity().compareTo(BigDecimal.ZERO),
                    "DONE 后 reserved 已释放 = 0");
            assertEquals(0, balance.getLockedQuantity().compareTo(BigDecimal.ZERO),
                    "locked = 0");
            // 不变量：available = total - reserved - locked（负库存下仍成立）
            assertEquals(0, balance.getAvailableQuantity().compareTo(
                    balance.getTotalQuantity().subtract(balance.getReservedQuantity())
                            .subtract(balance.getLockedQuantity())),
                    "available = total - reserved - locked 不变量在负库存下成立");
            output("1_balance_state.json5", balanceState(balance));
        } finally {
            setNegativeStock(false);
        }
    }

    /**
     * UC-INV-09 (2/4)：后续入库补回负余额，avgCost 平滑（无负成本异常）。
     * 先 -5（无初始库存，avgCost 兜底 0，totalCost=0）→ 再 +10@8（total=5, totalCost=80, avgCost=16）。
     */
    @Test
    public void testSubsequentIncomingReplenishes() {
        setNegativeStock(true);
        try {
            // 先出库 5（无初始库存，走负库存放行）：total=-5, totalCost=0, avgCost=0
            generateOutgoing("SS-NEG-REPL-001", new BigDecimal("5"));
            ErpInvStockBalance negative = findBalance();
            assertEquals(0, negative.getTotalQuantity().compareTo(new BigDecimal("-5")),
                    "出库后 total = -5");
            assertEquals(0, negative.getTotalCost().compareTo(BigDecimal.ZERO),
                    "出库后 totalCost = 0（avgCost 兜底 0，无成本可扣）");

            // 后续入库 10@8 补回：total=-5+10=5, totalCost=0+80=80, avgCost=80/5=16
            generateIncoming("PR-NEG-REPL-002", new BigDecimal("10"), new BigDecimal("8"));
            ErpInvStockBalance replenished = findBalance();
            assertEquals(0, replenished.getTotalQuantity().compareTo(new BigDecimal("5")),
                    "补回后 total = -5 + 10 = 5（正值）");
            assertEquals(0, replenished.getTotalCost().compareTo(new BigDecimal("80")),
                    "totalCost = 0 + 80 = 80");
            assertEquals(0, replenished.getAvgCost().compareTo(new BigDecimal("16")),
                    "avgCost = 80 / 5 = 16（平滑过渡，无除零/负成本异常）");
            assertEquals(0, replenished.getAvailableQuantity().compareTo(new BigDecimal("5")),
                    "available = 5（全部可用）");
            output("1_replenished_balance_state.json5", balanceState(replenished));
        } finally {
            setNegativeStock(false);
        }
    }

    /**
     * UC-INV-09 (3/4)：reserved/locked 不被负库存绕过，available=total−reserved−locked 在负库存下仍成立。
     *
     * <p>场景：allow-negative-stock=true 下，CONFIRMED 出库（无 DONE）应正确占用 reserved，
     * available = total - reserved - locked 不变量保持。CONFIRMED 状态出库占 reserved，DONE 释放 reserved 并扣 total。
     */
    @Test
    public void testReservedNotBypassedByNegativeStock() {
        setNegativeStock(true);
        try {
            // 初始入库 10：total=10, reserved=0, available=10
            generateIncoming("PR-RESERVE-001", new BigDecimal("10"), new BigDecimal("5"));

            // CONFIRMED 出库 3（不经 DONE，不设 relatedBillType → generateMove 停在 CONFIRMED）
            Long confirmedMoveId = generateOutgoingConfirmed("SS-RESERVE-002", new BigDecimal("3"));
            ErpInvStockMove confirmedMove = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(confirmedMoveId);
            assertEquals(ErpInvConstants.DOC_STATUS_CONFIRMED, confirmedMove.getDocStatus(),
                    "无 relatedBillType 的出库停在 CONFIRMED（不自动 DONE）");

            ErpInvStockBalance afterConfirm = findBalance();
            assertEquals(0, afterConfirm.getTotalQuantity().compareTo(new BigDecimal("10")),
                    "CONFIRMED 不影响 total（仍为 10）");
            assertEquals(0, afterConfirm.getReservedQuantity().compareTo(new BigDecimal("3")),
                    "CONFIRMED 占用 reserved = 3");
            assertEquals(0, afterConfirm.getAvailableQuantity().compareTo(new BigDecimal("7")),
                    "available = 10 - 3 - 0 = 7");
            // 不变量
            assertEquals(0, afterConfirm.getAvailableQuantity().compareTo(
                    afterConfirm.getTotalQuantity().subtract(afterConfirm.getReservedQuantity())
                            .subtract(afterConfirm.getLockedQuantity())),
                    "available = total - reserved - locked 不变量成立");

            // 再 DONE：releaseReservation (reserved=0) + 扣 total（-3）
            ApiResponse<?> completeResp = executeRpc(mutation, "ErpInvStockMove__complete",
                    ApiRequest.build(Map.of("moveId", confirmedMoveId)));
            assertEquals(0, completeResp.getStatus(),
                    "DONE 应成功（status=OK）");

            ErpInvStockBalance afterDone = findBalance();
            assertEquals(0, afterDone.getTotalQuantity().compareTo(new BigDecimal("7")),
                    "DONE 后 total = 10 - 3 = 7");
            assertEquals(0, afterDone.getReservedQuantity().compareTo(BigDecimal.ZERO),
                    "DONE 释放 reserved = 0");
            assertEquals(0, afterDone.getAvailableQuantity().compareTo(new BigDecimal("7")),
                    "available = 7 - 0 - 0 = 7");
            output("1_after_done_balance.json5", balanceState(afterDone));
        } finally {
            setNegativeStock(false);
        }
    }

    /**
     * UC-INV-09 (4/4)：默认配置 off 拒绝（覆盖既有 {@link #testConfirmInsufficientAvailableRejected} 的默认态）。
     *
     * <p>不显式设置 allow-negative-stock（默认 false），无库存出库 → ERR_AVAILABLE_INSUFFICIENT，
     * 移动单未生成，余额未变。
     */
    @Test
    public void testNegativeStockOffRejectsByDefault() {
        // 确保默认 off（不调用 setNegativeStock）
        setNegativeStock(false);

        ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-DEF-REJECT-001", new BigDecimal("5")));
        assertEquals(ErpInvErrors.ERR_AVAILABLE_INSUFFICIENT.getErrorCode(), resp.getCode(),
                "默认 allow-negative-stock=false 时可用量不足应返回 ERR_AVAILABLE_INSUFFICIENT");
        output("1_rejection_code.json5", resp.getCode());

        ErpInvStockMove move = findMove("SALES_SHIP", "SS-DEF-REJECT-001");
        assertNull(move, "拒绝应整笔回滚，不残留移动单");

        ErpInvStockBalance balance = findBalance();
        assertTrue(balance == null
                        || balance.getReservedQuantity().compareTo(BigDecimal.ZERO) == 0,
                "拒绝不应增加预留量");
    }

    // ---------- helpers ----------

    private java.util.Map<String, Object> balanceState(ErpInvStockBalance b) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("totalQuantity", b.getTotalQuantity());
        m.put("availableQuantity", b.getAvailableQuantity());
        m.put("reservedQuantity", b.getReservedQuantity());
        m.put("lockedQuantity", b.getLockedQuantity());
        m.put("avgCost", b.getAvgCost());
        m.put("totalCost", b.getTotalCost());
        return m;
    }

    private Long generateIncoming(String billCode, BigDecimal qty, BigDecimal unitCost) {
        return idOf(genMove(incomingReq("PUR_RECEIPT", billCode, qty, unitCost)));
    }

    private Long generateOutgoing(String billCode, BigDecimal qty) {
        return idOf(genMove(outgoingReq("SALES_SHIP", billCode, qty)));
    }

    /**
     * 生成无 relatedBillType 的出库移动单 → generateMove 停在 CONFIRMED（不自动 DONE），供 reserved 状态测试。
     */
    private Long generateOutgoingConfirmed(String billCode, BigDecimal qty) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(qty, null)));
        return idOf(genMove(req));
    }

    private ApiResponse<?> genMove(Map<String, Object> req) {
        return executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
    }

    private Long idOf(ApiResponse<?> resp) {
        Object id = ((Map<?, ?>) resp.getData()).get("id");
        return id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
    }

    private Map<String, Object> incomingReq(String billType, String billCode, BigDecimal qty,
                                             BigDecimal unitCost) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        if (billType != null) {
            req.put("relatedBillType", billType);
        }
        if (billCode != null) {
            req.put("relatedBillCode", billCode);
        }
        req.put("lines", Collections.singletonList(line(qty, unitCost)));
        return req;
    }

    private Map<String, Object> outgoingReq(String billType, String billCode, BigDecimal qty) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", billType);
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(qty, null)));
        return req;
    }

    private Map<String, Object> baseReq(String moveType) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", moveType);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        return req;
    }

    private Map<String, Object> line(BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", MATERIAL_ID);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        if (unitCost != null) {
            line.put("unitCost", unitCost);
        }
        line.put("currencyId", CURRENCY_ID);
        return line;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpInvStockLedger> findLedgers(Long moveId) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return dao.findAllByQuery(q);
    }

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void setNegativeStock(boolean value) {
        io.nop.api.core.config.AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, String.valueOf(value));
    }
}
