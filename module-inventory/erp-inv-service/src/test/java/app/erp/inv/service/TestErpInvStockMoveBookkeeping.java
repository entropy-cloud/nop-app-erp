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
    }

    @Test
    public void testConfirmInsufficientAvailableRejected() {
        ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-INSUF-001", new BigDecimal("5")));
        assertEquals(ErpInvErrors.ERR_AVAILABLE_INSUFFICIENT.getErrorCode(), resp.getCode(),
                "可用量不足应返回 ERR_AVAILABLE_INSUFFICIENT");

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
        } finally {
            setNegativeStock(false);
        }
    }

    // ---------- helpers ----------

    private Long generateIncoming(String billCode, BigDecimal qty, BigDecimal unitCost) {
        return idOf(genMove(incomingReq("PUR_RECEIPT", billCode, qty, unitCost)));
    }

    private Long generateOutgoing(String billCode, BigDecimal qty) {
        return idOf(genMove(outgoingReq("SALES_SHIP", billCode, qty)));
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
