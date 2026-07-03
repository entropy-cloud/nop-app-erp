package app.erp.inv.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 1 服务层集成测试：generateMove 契约 + 状态机 + 幂等。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpInvStockMove__generateMove/confirm/cancel/reverse}，
 * 引擎负责建 session/事务/管道（直调会因缺 OrmSession 报错，见 lessons/04）。测试自包含：seed 占位 ID。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvStockMoveBizModel extends JunitAutoTestCase {

    static final Long ORG_ID = 1001L;
    static final Long MATERIAL_ID = 2001L;
    static final Long WAREHOUSE_ID = 3001L;
    static final Long LOCATION_ID = 4001L;
    static final Long UOM_ID = 5001L;
    static final Long CURRENCY_ID = 6001L;
    static final Long ACCT_SCHEMA_ID = 7001L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testGenerateMoveBusinessLinkedAutoCompletes() {
        generateIncoming("PUR_RECEIPT", "PR-001", new BigDecimal("10"));

        ErpInvStockMove move = findMove("PUR_RECEIPT", "PR-001");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(),
                "业务联动应自动推进到 DONE");
        assertEquals(false, move.getPosted(), "未接入过账前 posted=false");
        assertEquals(1, countLines(move.getId()), "应生成 1 行");
    }

    @Test
    public void testGenerateMoveIdempotent() {
        Long first = generateIncoming("PUR_RECEIPT", "PR-IDEM-001", new BigDecimal("10"));
        Long second = generateIncoming("PUR_RECEIPT", "PR-IDEM-001", new BigDecimal("10"));

        assertEquals(first, second, "同源单重复触发应返回同一移动单");
        assertEquals(1, countMovesByRelatedBill("PUR_RECEIPT", "PR-IDEM-001"),
                "幂等：不应产生第二张移动单");
    }

    @Test
    public void testManualMoveStopsAtConfirmed() {
        Long moveId = idOf(genMove(incomingReq(null, null, new BigDecimal("10"))));

        ErpInvStockMove move = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(moveId);
        assertEquals(ErpInvConstants.DOC_STATUS_CONFIRMED, move.getDocStatus(),
                "独立移动单（无源单）应停在 CONFIRMED");
    }

    @Test
    public void testIllegalTransitionRejected() {
        Long doneId = generateIncoming("PUR_RECEIPT", "PR-ILL-001", new BigDecimal("10"));

        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__confirm",
                ApiRequest.build(Map.of("moveId", doneId)));
        assertEquals(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "DONE→CONFIRMED 非法迁移应返回错误（经 GraphQL，不抛异常）");
    }

    @Test
    public void testCancelReleasesReservation() {
        generateIncoming("PUR_RECEIPT", "PR-CANCEL-STOCK", new BigDecimal("10"));

        Long manualId = idOf(genMove(outgoingReq(null, null, new BigDecimal("5"))));
        ErpInvStockBalance reserved = findBalance();
        assertEquals(0, reserved.getReservedQuantity().compareTo(new BigDecimal("5")),
                "CONFIRMED 应占预留 5");
        assertEquals(0, reserved.getAvailableQuantity().compareTo(new BigDecimal("5")),
                "可用量 = total(10) - reserved(5) - locked(0) = 5");

        ApiResponse<?> cancelResp = executeRpc(mutation, "ErpInvStockMove__cancel",
                ApiRequest.build(Map.of("moveId", manualId)));
        assertEquals(0, cancelResp.getStatus(), "cancel 应成功");

        ErpInvStockBalance released = findBalance();
        assertEquals(0, released.getReservedQuantity().compareTo(BigDecimal.ZERO),
                "CANCELLED 应释放预留");
        assertEquals(0, released.getAvailableQuantity().compareTo(new BigDecimal("10")),
                "释放后可用量恢复为 total(10)");
    }

    @Test
    public void testReverseCreatesReverseMove() {
        Long originalId = generateIncoming("PUR_RECEIPT", "PR-REV-001", new BigDecimal("12"));
        ErpInvStockMove original = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(originalId);

        ApiResponse<?> reverseResp = executeRpc(mutation, "ErpInvStockMove__reverse",
                ApiRequest.build(Map.of("moveId", originalId)));
        assertEquals(0, reverseResp.getStatus(), "reverse 应成功");

        ErpInvStockMove reversal = findMove("REVERSAL", original.getCode());
        assertNotNull(reversal, "冲销应生成新移动单");
        assertNotEquals(originalId, reversal.getId(), "冲销单是新单，非原单");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, reversal.getDocStatus(), "冲销单自动推进到 DONE");
        assertEquals("REVERSAL", reversal.getRelatedBillType(), "冲销单关联原单");
        assertEquals(original.getCode(), reversal.getRelatedBillCode());

        ErpInvStockMove originalAfter = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(originalId);
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, originalAfter.getDocStatus(), "原单保持 DONE（非反审核）");
    }

    // ---------- helpers ----------

    private Long generateIncoming(String billType, String billCode, BigDecimal qty) {
        return idOf(genMove(incomingReq(billType, billCode, qty)));
    }

    private ApiResponse<?> genMove(Map<String, Object> req) {
        return executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
    }

    private Long idOf(ApiResponse<?> resp) {
        Object id = ((Map<?, ?>) resp.getData()).get("id");
        return id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
    }

    private Map<String, Object> incomingReq(String billType, String billCode, BigDecimal qty) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        if (billType != null) {
            req.put("relatedBillType", billType);
        }
        if (billCode != null) {
            req.put("relatedBillCode", billCode);
        }
        req.put("lines", Collections.singletonList(line(qty)));
        return req;
    }

    private Map<String, Object> outgoingReq(String billType, String billCode, BigDecimal qty) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        if (billType != null) {
            req.put("relatedBillType", billType);
        }
        if (billCode != null) {
            req.put("relatedBillCode", billCode);
        }
        req.put("lines", Collections.singletonList(line(qty)));
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

    private Map<String, Object> line(BigDecimal qty) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", MATERIAL_ID);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("currencyId", CURRENCY_ID);
        return line;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private long countLines(Long moveId) {
        IEntityDao<app.erp.inv.dao.entity.ErpInvStockMoveLine> dao = daoProvider
                .daoFor(app.erp.inv.dao.entity.ErpInvStockMoveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return dao.findAllByQuery(q).size();
    }

    private long countMovesByRelatedBill(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        return daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q).size();
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
